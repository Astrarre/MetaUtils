package api

import asm.isInterface
import asm.isStatic
import asm.visibility
import descriptor.FieldDescriptor
import descriptor.MethodDescriptor
import descriptor.read
import readToClassNode
import sun.reflect.generics.parser.SignatureParser
import java.nio.file.Path

fun ApiClass.Companion.readFrom(classPath: Path): ApiClass {
    val signatureParser = SignatureParser.make()
    val classNode = readToClassNode(classPath)
    val methods = classNode.methods.map { method ->
        val descriptor = MethodDescriptor.read(method.desc)
        val nonThisLocals = method.localVariables.filter { it.name != "this" }
        check(nonThisLocals.size >= descriptor.parameterDescriptors.size) {
            "There was not enough (${method.localVariables.size}) local variable debug information for all parameters" +
                    " (${descriptor.parameterDescriptors.size} of them) in method ${method.name}"
        }

        ApiClass.Method(
            name = method.name, descriptor = descriptor, static = method.isStatic,
            parameterNames = nonThisLocals.take(descriptor.parameterDescriptors.size).map { it.name },
            visibility = method.visibility,
            signature = method.signature?.let { signatureParser.parseMethodSig(it) }
        )
    }
    val fields = classNode.fields.map { field ->
        ApiClass.Field(field.name, FieldDescriptor.read(field.desc), field.isStatic, field.visibility,
            signature = field.signature?.let { signatureParser.parseTypeSig(it) }
        )
    }

    val fullClassName = classNode.name
    val packageSplit = fullClassName.lastIndexOf("/")
    val packageName = fullClassName.substring(0, packageSplit).replace("/", ".")
    val className = fullClassName.substring(packageSplit + 1, fullClassName.length)

    //TODO inner classes (inner classes are split across multiple classfiles)
    return ApiClass(
        packageName = packageName, className = className, methods = methods.toSet(), fields = fields.toSet(),
        innerClasses = setOf(),
        type = if (classNode.isInterface) ApiClass.Type.Interface else ApiClass.Type.Baseclass,
        signature = classNode.signature?.let { signatureParser.parseClassSig(it) }
    )
}
