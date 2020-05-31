package api

import api.ClassApi.Type.*
import api.ClassApi.Type.Annotation
import api.ClassApi.Type.Enum
import asm.*
import descriptor.FieldDescriptor
import descriptor.MethodDescriptor
import descriptor.read
import isClassfile
import readToClassNode
import splitFullyQualifiedName
import sun.reflect.generics.parser.SignatureParser
import toDotQualified
import walkJar
import java.nio.file.Path

fun ClassApi.Companion.readFromJar(jarPath: Path): Collection<ClassApi> {
    require(jarPath.toString().endsWith(".jar")) { "Specified path $jarPath does not point to a jar" }

    //TODO: inner classes
    return jarPath.walkJar { files -> files.filter { it.isClassfile() }.map { readSingularClass(it) }.toList() }
}

private fun ClassApi.Companion.readSingularClass(classPath: Path): ClassApi {
    val classNode = readToClassNode(classPath)
    val methods = classNode.methods.map { method ->
        val descriptor = MethodDescriptor.read(method.desc)
        val nonThisLocals = method.localVariables.filter { it.name != "this" }
        check(nonThisLocals.size >= descriptor.parameterDescriptors.size) {
            "There was not enough (${method.localVariables.size}) local variable debug information for all parameters" +
                    " (${descriptor.parameterDescriptors.size} of them) in method ${method.name}"
        }

        ClassApi.Method(
            name = method.name, descriptor = descriptor, static = method.isStatic,
            parameterNames = nonThisLocals.take(descriptor.parameterDescriptors.size).map { it.name },
            visibility = method.visibility,
            signature = method.signature?.let { SignatureParser.make().parseMethodSig(it) }
        )
    }
    val fields = classNode.fields.map { field ->
        ClassApi.Field(field.name, FieldDescriptor.read(field.desc), field.isStatic, field.visibility,
            signature = field.signature?.let { SignatureParser.make().parseTypeSig(it) }
        )
    }

    val (packageName,className) = classNode.name.splitFullyQualifiedName(dotQualified = false)


    //TODO inner classes (inner classes are split across multiple classfiles)
    return ClassApi(
        packageName = packageName?.toDotQualified(), className = className.toDotQualified(),
        methods = methods.toSet(), fields = fields.toSet(),
        innerClasses = setOf(),
        classType = with(classNode) {
            when {
                isInterface -> Interface
                isAnnotation -> Annotation
                isEnum -> Enum
                isAbstract -> AbstractClass
                else -> ConcreteClass
            }
        },
        visibility = classNode.visibility,
        signature = classNode.signature?.let { SignatureParser.make().parseClassSig(it) }
    )
}
