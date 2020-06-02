package api

import api.ClassApi.Type.*
import api.ClassApi.Type.Annotation
import api.ClassApi.Type.Enum
import asm.*
import descriptor.FieldDescriptor
import descriptor.MethodDescriptor
import descriptor.read
import hasExtension
import isClassfile
import openJar
import readToClassNode
import splitFullyQualifiedName
import sun.reflect.generics.parser.SignatureParser
import toDotQualified
import walk
import java.nio.file.FileSystem
import java.nio.file.Path

fun ClassApi.Companion.readFromJar(jarPath: Path): Collection<ClassApi> {
    require(jarPath.hasExtension(".jar")) { "Specified path $jarPath does not point to a jar" }

    // Only pass top-level classes into readSingularClass()

    return jarPath.openJar { jar ->
        jar.getPath("/").walk()
            .filter { it.isClassfile() && '$' !in it.toString() }.map { readSingularClass(jar, it, outerClass = null) }
            .toList()
    }

}

private fun ClassApi.Companion.readSingularClass(fs: FileSystem, path: Path, outerClass: Lazy<ClassApi>?): ClassApi {
    val classNode = readToClassNode(path)
    val methods = classNode.methods.map { method ->
        val descriptor = MethodDescriptor.read(method.desc)
        val nonThisLocals = method.localVariables.filter { it.name != "this" }
        check(nonThisLocals.size >= descriptor.parameterDescriptors.size) {
            "There was not enough (${method.localVariables.size}) local variable debug information for all parameters" +
                    " (${descriptor.parameterDescriptors.size} of them) in method ${method.name}"
        }

        ClassApi.Method(
            name = method.name, descriptor = descriptor, isStatic = method.isStatic,
            parameterNames = nonThisLocals.take(descriptor.parameterDescriptors.size).map { it.name },
            visibility = method.visibility,
            signature = method.signature?.let { SignatureParser.make().parseMethodSig(it) }
        )
    }
    val fields = classNode.fields.map { field ->
        ClassApi.Field(field.name, FieldDescriptor.read(field.desc), field.isStatic, field.visibility, field.isFinal,
            signature = field.signature?.let { SignatureParser.make().parseTypeSig(it) }
        )
    }

    val (packageName, className) = classNode.name.splitFullyQualifiedName(dotQualified = false)

    // Unfortunate hack to get the outer class reference into the inner classes
    var classApi: ClassApi? = null
    classApi = ClassApi(
        packageName = packageName?.toDotQualified(),
        // For inner classes, only include the inner class name itself
        className = className.toDotQualified().substringAfterLast('$'),
        methods = methods.toSet(), fields = fields.toSet(),
        innerClasses = classNode.innerClasses.filter { it.name != classNode.name }.map {
            readSingularClass(fs, fs.getPath(it.name + ".class"), lazy { classApi!! })
        },
        outerClass = outerClass,
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
        signature = classNode.signature?.let { SignatureParser.make().parseClassSig(it) },
        isStatic = true //TODO
    )


    return classApi
}
