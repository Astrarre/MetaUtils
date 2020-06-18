package codegeneration

import api.AnyJavaType
import api.JavaReturnType
import api.JavaType
import descriptor.*
import org.objectweb.asm.*
import org.objectweb.asm.tree.InnerClassNode
import signature.*
import util.*
import java.nio.file.Path

private fun Visibility.asmOpcode() = when (this) {
    ClassVisibility.Public -> Opcodes.ACC_PUBLIC
    ClassVisibility.Private -> Opcodes.ACC_PRIVATE
    ClassVisibility.Package -> 0
    Visibility.Protected -> Opcodes.ACC_PROTECTED
}

fun ClassAccess.toAsmAccess(visibility: Visibility, isStatic: Boolean = false): Int {
    var access = 0
    if (isFinal) access = access or Opcodes.ACC_FINAL
    if (isStatic) access = access or Opcodes.ACC_STATIC
    access = access or when (variant) {
        ClassVariant.Interface -> Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT
        ClassVariant.ConcreteClass -> 0
        ClassVariant.AbstractClass -> Opcodes.ACC_ABSTRACT
        ClassVariant.Enum -> Opcodes.ACC_ENUM
        ClassVariant.Annotation -> Opcodes.ACC_ANNOTATION
    }

    return access or visibility.asmOpcode()
}


private fun MethodAccess.toAsmAccess(): Int {
    var access = 0
    if (isStatic) access = access or Opcodes.ACC_STATIC
    if (isFinal) access = access or Opcodes.ACC_FINAL
    if (isAbstract) access = access or Opcodes.ACC_ABSTRACT
    access = access or visibility.asmOpcode()
    return access
}

private fun fieldAsmAccess(visibility: Visibility, isStatic: Boolean, isFinal: Boolean): Int {
    var access = 0
    if (isStatic) access = access or Opcodes.ACC_STATIC
    if (isFinal) access = access or Opcodes.ACC_FINAL
    access = access or visibility.asmOpcode()
    return access
}


private fun GenericTypeOrPrimitive.hasAnyTypeArguments() = getContainedClassesRecursively().size > 1

private fun writeClassImpl(
    info: ClassInfo, className: QualifiedName, srcRoot: Path, outerClass: QualifiedName?
): Unit = with(info) {
    val classWriter = ClassWriter(0)

    val genericsInvolved = typeArguments.isNotEmpty() || superClass?.type?.hasAnyTypeArguments() == true
            || superInterfaces.any { it.type.hasAnyTypeArguments() }
    val signature = if (genericsInvolved) ClassSignature(typeArguments = typeArguments,
        superClass = superClass?.type ?: JavaLangObjectGenericType,
        superInterfaces = superInterfaces.map { it.type }
    ) else null

    //TODO: annotations

    classWriter.visit(
        Opcodes.V1_8,
        access.toAsmAccess(visibility),
        className.toSlashQualifiedString(),
        signature?.toClassfileName(),
        superClass?.toJvmType()?.fullClassName?.toSlashQualifiedString() ?: JavaLangObjectString,
        superInterfaces.map { it.toJvmType().fullClassName.toSlashQualifiedString() }.toTypedArray()
    )

    for (annotation in info.annotations) {
        classWriter.visitAnnotation(annotation.type.classFileName, false).visitEnd()
    }


    //TODO: investigate if we can trick the IDE to think the original source file is the mc source file
    classWriter.visitSource(null, null)
    AsmGeneratedClass(classWriter, className, srcRoot, info.access.variant.isInterface).apply(body).finish()
    classWriter.visitEnd()

    val path = srcRoot.resolve(className.toPath(".class"))
    path.createParentDirectories()
    path.writeBytes(classWriter.toByteArray())


}


object AsmCodeGenerator : CodeGenerator {
    override fun writeClass(info: ClassInfo, packageName: PackageName?, srcRoot: Path) {
        writeClassImpl(
            info,
            QualifiedName(packageName, ShortClassName(listOf(info.shortName))),
            srcRoot,
            outerClass = null
        )
    }

}


private fun <T : GenericReturnType> Iterable<JavaType<T>>.generics() = map { it.type }

private fun JavaReturnType.asmType(): Type = toJvmType().asmType()

class AsmGeneratedClass(
    private val classWriter: ClassVisitor,
    private val className: QualifiedName,
    private val srcRoot: Path,
    private val isInterface: Boolean
) : GeneratedClass {

    private val instanceFieldInitializers: MutableMap<FieldExpression, Expression> = mutableMapOf()
    private val staticFieldInitializers: MutableMap<FieldExpression, Expression> = mutableMapOf()
    private val constructors: MutableList<MethodInfo> = mutableListOf()

    fun addAsmInnerClasses(innerClasses: List<InnerClassNode>) {
        innerClasses.forEach { classWriter.visitInnerClass(it.name, it.outerName, it.innerName, it.access) }
    }

    fun finish() {
        assert(instanceFieldInitializers.isEmpty() || constructors.isNotEmpty())
        for (constructor in constructors) {
            constructor.addMethodImpl(
                returnType = VoidJavaType,
                name = ConstructorsName,
                typeArguments = listOf(),
                access = MethodAccess(
                    isStatic = false, visibility = constructor.visibility, isFinal = false, isAbstract = false
                )
            ) {
                for ((targetField, fieldValue) in this@AsmGeneratedClass.instanceFieldInitializers) {
                    addStatement(AssignmentStatement(target = targetField, assignedValue = fieldValue))
                }
            }
        }

        if (staticFieldInitializers.isNotEmpty()) {
            val staticInitializer = MethodInfo(
                visibility = Visibility.Package,
                parameters = mapOf(),
                throws = listOf()
            ) {
                for ((targetField, fieldValue) in this@AsmGeneratedClass.staticFieldInitializers) {
                    addStatement(AssignmentStatement(target = targetField, assignedValue = fieldValue))
                }
            }
            staticInitializer.addMethodImpl(
                returnType = VoidJavaType,
                name = "<clinit>",
                typeArguments = listOf(),
                access = MethodAccess(
                    isStatic = true, visibility = Visibility.Package, isFinal = false, isAbstract = false
                )
            )
        }
    }

    override fun addInnerClass(info: ClassInfo, isStatic: Boolean) {
        val innerClassName = className.innerClass(info.shortName)
        writeClassImpl(info, innerClassName, srcRoot, outerClass = className)
//        classWriter.visitNestMember(innerClassName.toSlashQualifiedString())
    }

    override fun addMethod(
        methodInfo: MethodInfo,
        isStatic: Boolean,
        isFinal: Boolean,
        isAbstract: Boolean,
        typeArguments: List<TypeArgumentDeclaration>,
        name: String,
        returnType: JavaReturnType
    ) {
        methodInfo.addMethodImpl(
            returnType, typeArguments, MethodAccess(isStatic, isFinal, isAbstract, methodInfo.visibility), name
        )
    }


    private fun MethodInfo.addMethodImpl(
        returnType: JavaReturnType,
        typeArguments: List<TypeArgumentDeclaration>,
        access: MethodAccess,
        name: String,
        bodyPrefix: GeneratedMethod.() -> Unit = {}
    ) {
        val descriptor = MethodDescriptor(parameters.values.map { it.toJvmType() }, returnType.toJvmType())
        val genericsInvolved = typeArguments.isNotEmpty() || parameters.values.any { it.type.hasAnyTypeArguments() }
        val signature = if (genericsInvolved) {
            MethodSignature(typeArguments, parameters.values.generics(), returnType.type, throws.generics())
        } else null

        val methodWriter = classWriter.visitMethod(
            access.toAsmAccess(),
            name, descriptor.classFileName, signature?.toClassfileName(), null
        )

        for (annotation in returnType.annotations) {
            methodWriter.visitAnnotation(annotation.type.classFileName, false).visitEnd()
        }

        parameters.values.forEachIndexed { i, paramType ->
            for (annotation in paramType.annotations) {
                methodWriter.visitParameterAnnotation(i, annotation.type.classFileName, false).visitEnd()
            }
        }

        if (!access.isAbstract) methodWriter.visitCode()
        if (access.isAbstract) {
            AbstractGeneratedMethod.apply(bodyPrefix).apply(body)
        } else {
            val builder = AsmGeneratedMethod(methodWriter, name, access.isStatic,
                parameters.mapValues { (_, type) -> type.toJvmType() }).apply(bodyPrefix).apply(body)

            // This assumes extremely simplistic method calls that just call one method and that's it
            methodWriter.visitInsn(returnType.asmType().getOpcode(Opcodes.IRETURN))
            val localVarSize = parameters.size.applyIf(!access.isStatic) { it + 1 }
            val stackSize = builder.maxStackSize()
            methodWriter.visitMaxs(stackSize, localVarSize)
        }

        methodWriter.visitEnd()
    }


    override fun addConstructor(info: MethodInfo) {
        require(!isInterface) { "Interfaces cannot have constructors" }
        // Need field initializer information before we write the constructors
        constructors.add(info)
    }


    override fun addField(
        name: String,
        type: AnyJavaType,
        visibility: Visibility,
        isStatic: Boolean,
        isFinal: Boolean,
        initializer: Expression?
    ) {
        val genericsInvolved = type.type.hasAnyTypeArguments()
        val signature = if (genericsInvolved) type.type else null
        val fieldVisitor = classWriter.visitField(
            fieldAsmAccess(visibility, isStatic, isFinal),
            name, type.toJvmType().classFileName, signature?.toClassfileName(), null
        )

        for (annotation in type.annotations) {
            fieldVisitor.visitAnnotation(annotation.type.classFileName, false).visitEnd()
        }

        fieldVisitor.visitEnd()
        if (initializer != null) {
            val targetField = FieldExpression(
                receiver = if (isStatic) ClassReceiver(ObjectType(className)) else ThisExpression,
                name = name,
                owner = ObjectType(className),
                type = type.toJvmType()
            )
            if (isStatic) {
                staticFieldInitializers[targetField] = initializer
            } else {
                instanceFieldInitializers[targetField] = initializer
            }
        }
    }

}

private fun ReturnDescriptor.asmType(): Type = Type.getType(classFileName)

private const val printStackOps = false

private class JvmState(
    private val methodName: String,
    private val methodStatic: Boolean,
    parameters: Map<String, JvmType>
) {
    fun maxStackSize(): Int {
        if (currentStack != 0) error("$currentStack element(s) on the stack were not used yet")
        if (printStackOps) {
            println("Method complete")
        }
        return maxStack
    }

    fun getVariable(variable: VariableExpression): JvmLocalVariable {
        val type = lvTable[variable.name]
        requireNotNull(type) { "Attempt to reference variable ${variable.name} when no such variable is declared" }
        return type
    }

    private val lvTable: MutableMap<String, JvmLocalVariable> = parameters.entries.mapIndexed { i, (name, type) ->
        name to JvmLocalVariable(i.applyIf(!methodStatic) { it + 1 }, type)
    }.toMap().toMutableMap()

//    private val variableTypeTable: MutableMap<String, JvmType> = parameters.toMutableMap()
//    private val variableIndexTable: MutableMap<String, Int> =
//        parameters.keys.mapIndexed { i, name -> name to i }.toMap().toMutableMap()


    // Max local variables is not tracked yet, assuming no local variables are created
    private var maxStack = 0
    private var currentStack = 0

    inline fun trackPush(amount: Int = 1, message: () -> String = { "" }) {
        val oldCurrent = currentStack
        currentStack += amount

        if (maxStack < currentStack) maxStack = currentStack

        if (printStackOps) {
            val str = message().let { if (it.isEmpty()) "operation" else it }
            println("$str: $oldCurrent -> $currentStack")
        }

    }

    inline fun trackPop(amount: Int = 1, message: () -> String = { "" }) {
        if (printStackOps) {
            val str = message().let { if (it.isEmpty()) "operation" else it }
            print("$str: ")
        }
        if (currentStack < amount) error("Attempt to pop $amount element(s) from stack when it only contains $currentStack in method $methodName")
        val oldCurrent = currentStack
        currentStack -= amount
        if (printStackOps) {
            println("$oldCurrent -> $currentStack")
        }
    }
}

private data class JvmLocalVariable(val index: Int, val type: JvmType)


private const val ConstructorsName = "<init>"

class AsmGeneratedMethod(
    private val method: MethodVisitor,
    name: String,
    isStatic: Boolean,
    parameters: Map<String, JvmType>
) : GeneratedMethod {
    fun maxStackSize(): Int = state.maxStackSize()

    private val state = JvmState(name, isStatic, parameters)


    private fun Receiver.addOpcodes() = code.addOpcodes()
    private fun Statement.addOpcodes() = code.addOpcodes()


//    private fun Expression.getVariable(): JvmLocalVariable = when(this){
//        is ArrayConstructor
//    }

    // Everything assumes we just pass the parameters in order, very simplistic, a real solution would have a variableIndexTable
    private fun Code.addOpcodes(): Unit = with(state) {
        when (this@addOpcodes) {
            is ClassReceiver -> {
            }
            SuperReceiver -> ThisExpression.code.addOpcodes()
            is ReturnStatement -> {
                // the RETURN is added by addMethod()
                target.addOpcodes()
                trackPop { "return" }
            }
            is AssignmentStatement -> {
                if (target is FieldExpression) target.receiver.addOpcodes()
                assignedValue.addOpcodes()
                target.assign()
            }
            is ConstructorCall.Super -> {
                invoke(
                    opcode = Opcodes.INVOKESPECIAL,
                    methodName = ConstructorsName,
                    isInterface = false,
                    returnType = ReturnDescriptor.Void,
                    owner = superType,
                    parameterTypes = parameters.keys,
                    parametersToLoad = ThisExpression.prependTo(parameters.values)
                )
            }
            is VariableExpression -> {
                val (index, type) = state.getVariable(this@addOpcodes)
                method.visitVarInsn(type.asmType().getOpcode(Opcodes.ILOAD), index)
                trackPush { "variable get" }
            }
            is CastExpression -> {
                target.addOpcodes()
                method.visitTypeInsn(Opcodes.CHECKCAST, castTo.toJvmType().toJvmString())
            }
            is FieldExpression -> {
                receiver.addOpcodes()
                val opcode = if (receiver is ClassReceiver) Opcodes.GETSTATIC else Opcodes.GETFIELD
                method.visitFieldInsn(opcode, owner.toJvmString(), name, type.classFileName)
                if (receiver !is ClassReceiver) trackPop { "field receiver consume" }
                trackPush { "field get" }
            }
            is MethodCall -> addMethodCall()

            is ArrayConstructor -> {
                trackPush { "push array size" }
                method.visitVarInsn(Opcodes.ILOAD, 0)
                // Assumes reference type array
                trackPop { "pass array size" }
                method.visitTypeInsn(Opcodes.ANEWARRAY, componentClass.type.toJvmString())
                trackPush { "push array result" }
            }
            ThisExpression -> {
                trackPush { "push this" }
                method.visitVarInsn(Opcodes.ALOAD, 0)
            }
        }
    }

    private fun Assignable.assign() = when (this) {
        is VariableExpression -> TODO()
        is FieldExpression -> {
//            receiver.addOpcodes()
            val isStatic = receiver is ClassReceiver
            val opcode = if (isStatic) Opcodes.PUTSTATIC else Opcodes.PUTFIELD
            if (!isStatic) state.trackPop { "pass field receiver" }
            state.trackPop { "pass field value" }
            method.visitFieldInsn(opcode, owner.toJvmString(), name, type.classFileName)
        }
        else -> error("Impossible")
    }

    private fun MethodAccess.invokeOpcode(isInterface: Boolean) = when {
        isStatic -> Opcodes.INVOKESTATIC
        // Final methods sometimes use INVOKESPECIAL but analyzing that is difficult, INVOKEVIRTUAL will work for our purposes.
        /*!methodAccess.isFinal &&*/
        !visibility.isPrivate -> {
            if (isInterface) Opcodes.INVOKEINTERFACE else Opcodes.INVOKEVIRTUAL
        }
        else -> Opcodes.INVOKESPECIAL
    }

    private fun invoke(
        opcode: Int, /*receiver: Receiver?,*/ methodName: String,
        parameterTypes: List<JvmType>,
        parametersToLoad: List<Receiver>,
//        parameters: List<Pair<JvmType, Expression>>,
        returnType: ReturnDescriptor, owner: ObjectType, isInterface: Boolean, parametersAlreadyLoaded: Int = 0
    ) {
//        val fullParameterTypes = parameters.prependIfNotNull(receiver?.let { owner to it })
//        receiver?.addOpcodes()

//        val allParameters = parametersToLoad.prependIfNotNull(receiver)
        parametersToLoad.forEach { it.addOpcodes() }

        val descriptor = MethodDescriptor(parameterTypes, returnType)

//        if (receiver != null) state.trackPop { "pass receiver" }
        state.trackPop(parametersToLoad.size + parametersAlreadyLoaded) { "pass parameters" }
        if (returnType != ReturnDescriptor.Void) state.trackPush { "push method result" }
        method.visitMethodInsn(opcode, owner.toJvmString(), methodName, descriptor.classFileName, isInterface)
    }

    private fun MethodCall.addMethodCall() = when (this) {
        is MethodCall.Method -> {
            val isInterface = receiverAccess.variant.isInterface
            val receiver = if (methodAccess.isStatic) null else receiver ?: ThisExpression
            invoke(
                opcode = methodAccess.invokeOpcode(isInterface), methodName = name,
                returnType = returnType, owner = owner, isInterface = isInterface,
                parametersToLoad = parameters.values.prependIfNotNull(receiver),
                parameterTypes = parameters.keys
            )
        }
        is MethodCall.Constructor -> {
            method.visitTypeInsn(Opcodes.NEW, constructing.toJvmType().toJvmString())
            method.visitInsn(Opcodes.DUP)
            state.trackPush(2) { "NEW operation on ${constructing.toJvmType().toJvmString()}" }

            invoke(
                opcode = Opcodes.INVOKESPECIAL,
                methodName = ConstructorsName,
                // outer class is implicitly the first parameter in java, but explicitly the first parameter in bytecode
                parameterTypes = parameters.keys.applyIf(receiver != null) {
                    // Add outer class as first param to inner class
                    constructing.outerClass().toJvmType().prependTo(it)
                },
                parametersToLoad = parameters.values.prependIfNotNull(receiver), // inner class
                returnType = ReturnDescriptor.Void,
                owner = constructing.toJvmType(),
                isInterface = false,
                parametersAlreadyLoaded = 1
            )
        }
    }

    override fun addStatement(statement: Statement) {
        if (printStackOps) println("Adding statement $statement")
        statement.addOpcodes()
    }

    override fun addComment(comment: String) {
    }

}

object AbstractGeneratedMethod : GeneratedMethod {
    override fun addStatement(statement: Statement) {
        error("Method is abstract")
    }

    override fun addComment(comment: String) {
        error("Method is abstract")
    }

}

//// We need to find all reference to classes to tell the jvm what inner classes we used
//private inline fun ClassInfo.visitNames(crossinline visitor: (QualifiedName) -> Unit) {
//    typeArguments.forEach { decl ->
//        decl.classBound?.visitNames(visitor)
//        decl.interfaceBounds.forEach { it.visitNames(visitor) }
//    }
//    superClass?.visitNames(visitor)
//    superInterfaces.forEach { it.visitNames(visitor) }
//    annotations.forEach { visitor(it.type.fullClassName) }
//}
//
//
//private inline fun JavaType<*>.visitNames(crossinline visitor: (QualifiedName) -> Unit){
//    type.visitNames(visitor)
//}
//
//private inline fun GenericReturnType.visitNames(crossinline visitor : (QualifiedName) -> Unit) = visitContainedClasses {
//    visitor(it.toJvmQualifiedName())
//}

