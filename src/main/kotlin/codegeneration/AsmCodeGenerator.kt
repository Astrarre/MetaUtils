package codegeneration

import api.AnyJavaType
import api.JavaReturnType
import api.JavaType
import descriptor.JavaLangObjectString
import descriptor.MethodDescriptor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import signature.*
import util.*
import java.nio.file.Path

private fun Visibility.asmOpcode() = when (this) {
    ClassVisibility.Public -> Opcodes.ACC_PUBLIC
    ClassVisibility.Private -> Opcodes.ACC_PRIVATE
    ClassVisibility.Package -> 0
    Visibility.Protected -> Opcodes.ACC_PROTECTED
}

private fun ClassAccess.toAsmAccess(visibility: Visibility): Int {
    var access = 0
    if (isFinal) access = access or Opcodes.ACC_FINAL
    access = access or when (variant) {
        ClassVariant.Interface -> Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT
        ClassVariant.ConcreteClass -> 0
        ClassVariant.AbstractClass -> Opcodes.ACC_ABSTRACT
        ClassVariant.Enum -> Opcodes.ACC_ENUM
        ClassVariant.Annotation -> Opcodes.ACC_ANNOTATION
    }

    return access or visibility.asmOpcode()
}


private fun MethodAccess.toAsmAccess(visibility: Visibility): Int {
    var access = 0
    if (isStatic) access = access or Opcodes.ACC_STATIC
    if (isFinal) access = access or Opcodes.ACC_FINAL
    if (isAbstract) access = access or Opcodes.ACC_ABSTRACT
    access = access or visibility.asmOpcode()
    return access
}


private fun GenericTypeOrPrimitive.hasAnyTypeArguments() = getContainedClassesRecursively().size == 1

private fun writeClassImpl(
    info: ClassInfo, className: QualifiedName, srcRoot: Path
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

    //TODO: investigate if we can trick the IDE to think the original source file is the mc source file
    classWriter.visitSource(null, null)
    AsmGeneratedClass(classWriter, className, srcRoot).apply(body)
    classWriter.visitEnd()

    val path = srcRoot.resolve(className.toPath(".class"))
    path.createParentDirectories()
    path.writeBytes(classWriter.toByteArray())


}

object AsmCodeGenerator : CodeGenerator {
    override fun writeClass(info: ClassInfo, packageName: PackageName?, srcRoot: Path) {
        writeClassImpl(info, QualifiedName(packageName, ShortClassName(listOf(info.shortName))), srcRoot)
    }

}


private fun <T : GenericReturnType> Iterable<JavaType<T>>.generics() = map { it.type }

class AsmGeneratedClass(
    private val classWriter: ClassVisitor,
    private val className: QualifiedName,
    private val srcRoot: Path
) : GeneratedClass {

    override fun addInnerClass(info: ClassInfo, isStatic: Boolean) {
        writeClassImpl(info, className.innerClass(info.shortName), srcRoot)
    }

    override fun addMethod(
        methodInfo: MethodInfo,
        access: MethodAccess,
        typeArguments: List<TypeArgumentDeclaration>,
        name: String,
        returnType: JavaReturnType
    ) = with(methodInfo) {
//        val descriptor = MethodDescriptor(parameters.values.map { it.toJvmType() }, returnType.toJvmType())
//        val genericsInvolved = typeArguments.isNotEmpty() || parameters.values.any { it.type.hasAnyTypeArguments() }
//        val signature = if (genericsInvolved) {
//            MethodSignature(typeArguments, parameters.values.generics(), returnType.type, throws.generics())
//        } else null
//
//        val methodWriter = classWriter.visitMethod(
//            access.toAsmAccess(visibility),
//            name, descriptor.classFileName, signature?.toc, null
//        )
//        methodWriter.visitCode()
//        methodWriter.visitVarInsn(Opcodes.ALOAD, 0)
//        methodWriter.visitTypeInsn(Opcodes.CHECKCAST, "net/minecraft/TestConcreteClass")
//        methodWriter.visitMethodInsn(
//            Opcodes.INVOKEVIRTUAL,
//            "net/minecraft/TestConcreteClass",
//            "publicInt",
//            "()I",
//            false
//        )
//        methodWriter.visitInsn(Opcodes.IRETURN)
//        methodWriter.visitMaxs(1, 1)
//        methodWriter.visitEnd()
    }

    override fun addConstructor(info: MethodInfo) {
    }


    override fun addField(
        name: String,
        type: AnyJavaType,
        visibility: Visibility,
        isStatic: Boolean,
        isFinal: Boolean,
        initializer: Expression?
    ) {
    }

}

class AsmGeneratedMethod : GeneratedMethod {
    override fun addStatement(statement: Statement) {
    }

    override fun addComment(comment: String) {
    }

}