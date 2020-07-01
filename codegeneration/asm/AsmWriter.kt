package metautils.codegeneration.asm

import metautils.api.JavaAnnotation
import codegeneration.*
import descriptor.*
import metautils.descriptor.*
import metautils.signature.*
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import metautils.util.ClasspathIndex
import metautils.util.QualifiedName
import metautils.util.outerClass
import metautils.util.writeBytes
import java.nio.file.Path

private fun MethodSignature.visitNames(visitor: (QualifiedName) -> Unit) {
    typeArguments?.forEach { it.visitNames(visitor) }
    parameterTypes.forEach { it.visitNames(visitor) }
    returnType.visitNames(visitor)
    throwsSignatures.forEach { it.visitNames(visitor) }
}

private fun ClassSignature.visitNames(visitor: (QualifiedName) -> Unit) {
    typeArguments?.forEach { it.visitNames(visitor) }
    superClass.visitNames(visitor)
    superInterfaces.forEach { it.visitNames(visitor) }
}

private fun TypeArgumentDeclaration.visitNames(visitor: (QualifiedName) -> Unit) {
    classBound?.visitNames(visitor)
    interfaceBounds.forEach { it.visitNames(visitor) }
}

private fun GenericReturnType.visitNames(visitor: (QualifiedName) -> Unit): Unit = when (this) {
    is GenericsPrimitiveType, GenericReturnType.Void -> {
    }
    is GenericType -> visitNames(visitor)
}

private fun GenericType.visitNames(visitor: (QualifiedName) -> Unit) = when (this) {
    is ClassGenericType -> visitNames(visitor)
    is TypeVariable -> {
    }
    is ArrayGenericType -> componentType.visitNames(visitor)
}

private fun ClassGenericType.visitNames(visitor: (QualifiedName) -> Unit) {
    visitor(toJvmQualifiedName())
    classNameSegments.forEach { segment -> segment.typeArguments?.forEach { it.visitNames(visitor) } }
}

private fun TypeArgument.visitNames(visitor: (QualifiedName) -> Unit) {
    if (this is TypeArgument.SpecificType) {
        type.visitNames(visitor)
    }
}

private fun MethodDescriptor.visitNames(visitor: (QualifiedName) -> Unit) {
    parameterDescriptors.forEach { it.visitNames(visitor) }
    returnDescriptor.visitNames(visitor)
}

private fun ReturnDescriptor.visitNames(visitor: (QualifiedName) -> Unit): Unit = when (this) {
    is ObjectType -> visitor(fullClassName)
    is ArrayType -> componentType.visitNames(visitor)
    ReturnDescriptor.Void, is JvmPrimitiveType -> {
    }
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

private fun Visibility.asmOpcode() = when (this) {
    ClassVisibility.Public -> Opcodes.ACC_PUBLIC
    ClassVisibility.Private -> Opcodes.ACC_PRIVATE
    ClassVisibility.Package -> 0
    Visibility.Protected -> Opcodes.ACC_PROTECTED
}


// Tracks which class names are passed to asm, so we can now which inner class names were used in each class
internal class AsmClassWriter(private val index: ClasspathIndex) {
    private val classWriter = ClassWriter(0)
    private val referencedNames = mutableSetOf<QualifiedName>()

//    fun getReferencedNames(): Collection<QualifiedName> = referencedNames

    private fun QualifiedName.track() {
        referencedNames.add(this)
    }

    private fun ObjectType.track() = fullClassName.track()
    private fun JvmType.track() = visitNames { it.track() }
    private fun Collection<ObjectType>.track() = forEach { it.track() }
    private fun Collection<JavaAnnotation>.trackAno() = forEach { it.type.track() }
//    private fun List<>.track(): QualifiedName = also { referencedNames.add(it) }

    inline fun writeClass(
        access: ClassAccess, visibility: Visibility, className: QualifiedName, sourceFile: String,
        signature: ClassSignature?, superClass: ObjectType, superInterfaces: List<ObjectType>,
        annotations: List<JavaAnnotation>,
        init: ClassBody.() -> Unit
    ) {
        className.track()
        signature?.visitNames { it.track() }
        superClass.track()
        superInterfaces.track()
        annotations.trackAno()

        classWriter.visit(
            Opcodes.V1_8,
            access.toAsmAccess(visibility),
            className.toSlashQualifiedString(),
            signature?.toClassfileName(),
            superClass.fullClassName.toSlashQualifiedString(),
            superInterfaces.map { it.fullClassName.toSlashQualifiedString() }.toTypedArray()
        )

        annotations.forEach { classWriter.visitAnnotation(it.type.classFileName, false).visitEnd() }

        classWriter.visitSource(sourceFile, null)
        init(ClassBody())
        classWriter.visitEnd()
    }

    fun writeBytesTo(path: Path) {
        // All inner classes referenced must be added as INNERCLASS fields
        for (name in referencedNames) {
            if (name.shortName.components.size >= 2) {
                classWriter.visitInnerClass(
                    name.toSlashQualifiedString(), name.outerClass().toSlashQualifiedString(),
                    name.shortName.innermostClass(), index.accessOf(name)
                )
            }
        }
        path.writeBytes(classWriter.toByteArray())
    }

    inner class ClassBody {
        fun trackInnerClass(name: QualifiedName) = name.track()
        fun writeMethod(
            name: String,
            access: MethodAccess,
            descriptor: MethodDescriptor,
            signature: MethodSignature?,
            annotations: List<JavaAnnotation>,
            parameterAnnotations: Map<Int, List<JavaAnnotation>>,
            throws: List<JvmType>,
            init: MethodBody.() -> Unit
        ) {

            descriptor.visitNames { it.track() }
            signature?.visitNames { it.track() }
            annotations.trackAno()
            parameterAnnotations.values.forEach { it.trackAno() }
            throws.forEach { it.track() }

            val methodWriter = classWriter.visitMethod(
                access.toAsmAccess(),
                name, descriptor.classFileName, signature?.toClassfileName(),
                throws.map { it.toJvmString() }.toTypedArray()
            )

            for (annotation in annotations) {
                methodWriter.visitAnnotation(annotation.type.classFileName, false).visitEnd()
            }

            for ((index, paramAnnotations) in parameterAnnotations) {
                paramAnnotations.forEach {
                    methodWriter.visitParameterAnnotation(index, it.type.classFileName, false).visitEnd()
                }
            }


            if (!access.isAbstract) {
                methodWriter.visitCode()
            }
            init(MethodBody(methodWriter))

            methodWriter.visitEnd()
        }

        fun writeField(
            name: String, type: JvmType, signature: FieldSignature?, visibility: Visibility, isStatic: Boolean,
            isFinal: Boolean, annotations: List<JavaAnnotation>
        ) {
            type.track()
            signature?.visitNames { it.track() }
            annotations.trackAno()

            val fieldVisitor = classWriter.visitField(
                fieldAsmAccess(visibility, isStatic, isFinal),
                name, type.classFileName, signature?.toClassfileName(), null
            )

            for (annotation in annotations) {
                fieldVisitor.visitAnnotation(annotation.type.classFileName, false).visitEnd()
            }

            fieldVisitor.visitEnd()
        }
    }

    inner class MethodBody(private val methodWriter: MethodVisitor) {
        fun writeZeroOperandInstruction(instruction: Int) {
            methodWriter.visitInsn(instruction)
        }

        fun setMaxStackAndVariablesSize(stack: Int, variables: Int) {
            methodWriter.visitMaxs(stack, variables)
        }

        fun writeLvArgInstruction(instruction: Int, lvIndex: Int) {
            methodWriter.visitVarInsn(instruction, lvIndex)
        }

        fun writeTypeArgInstruction(instruction: Int, type: JvmType) {
            type.track()
            methodWriter.visitTypeInsn(instruction, type.toJvmString())
        }

        fun writeFieldArgInstruction(instruction: Int, fieldOwner: ObjectType, fieldName: String, fieldType: JvmType) {
            fieldOwner.track()
            fieldType.track()
            methodWriter.visitFieldInsn(instruction, fieldOwner.toJvmString(), fieldName, fieldType.classFileName)
        }

        fun writeMethodCall(
            instruction: Int,
            methodOwner: ObjectType,
            methodName: String,
            methodDescriptor: MethodDescriptor,
            isInterface: Boolean
        ) {
            methodOwner.track()
            methodDescriptor.visitNames { it.track() }
            methodWriter.visitMethodInsn(
                instruction,
                methodOwner.toJvmString(),
                methodName,
                methodDescriptor.classFileName,
                isInterface
            )
        }

    }
}



