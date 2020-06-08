package api

import api.ClassApi.Type.*
import api.ClassApi.Type.Annotation
import api.ClassApi.Type.Enum
import applyIf
import asm.*
import descriptor.*
import hasExtension
import isClassfile
import openJar
import toQualifiedName
import walk
import java.nio.file.FileSystem
import java.nio.file.Path

fun ClassApi.Companion.readFromJar(jarPath: Path): Collection<ClassApi> {
    require(jarPath.hasExtension(".jar")) { "Specified path $jarPath does not point to a jar" }

    // Only pass top-level classes into readSingularClass()

    return jarPath.openJar { jar ->
        jar.getPath("/").walk()
            .filter { it.isClassfile() && '$' !in it.toString() }
            .map { readSingularClass(jar, it, outerClass = null, isStatic = false) }
            .toList()
    }

}

// isStatic information is passed by the parent class for inner classes
private fun ClassApi.Companion.readSingularClass(
    fs: FileSystem,
    path: Path,
    outerClass: Lazy<ClassApi>?,
    isStatic: Boolean
): ClassApi {

    val classNode = readToClassNode(path)
    val methods = classNode.methods.map { method ->
        val descriptor = MethodDescriptor.read(method.desc)

        val locals = method.localVariables
        val parameterNames = when {
            method.parameters != null -> {
                // Some methods use a parameter names field instead of local variables
                method.parameters.map { it.name }
            }
            locals != null -> {
                val nonThisLocalNames = locals.filter { it.name != "this" }.map { it.name }
                // Enums pass the name and ordinal into the constructor as well
                val namesWithEnum = nonThisLocalNames.applyIf(classNode.isEnum) {
                    listOf("\$enum\$name", "\$enum\$ordinal") + it
                }

                check(namesWithEnum.size >= descriptor.parameterDescriptors.size) {
                    "There was not enough (${namesWithEnum.size}) local variable debug information for all parameters" +
                            " (${descriptor.parameterDescriptors.size} of them) in method ${method.name}"
                }
                namesWithEnum.take(descriptor.parameterDescriptors.size).map { it }
            }
            else -> listOf()
        }

        val visibility = method.visibility

        ClassApi.Method(
            name = method.name, descriptor = descriptor, isStatic = method.isStatic,
            parameterNames = parameterNames,
            visibility = visibility
//            signature = method.signature?.let { SignatureParser.make().parseMethodSig(it) }
        )
    }
    val fields = classNode.fields.map { field ->
        ClassApi.Field(field.name, FieldDescriptor.read(field.desc), field.isStatic, field.visibility, field.isFinal
//            signature = field.signature?.let { SignatureParser.make().parseTypeSig(it) }
        )
    }

    val fullClassName = classNode.name.toQualifiedName(dotQualified = false)

    val innerClassShortName = with(fullClassName.shortName.components) { if(size == 1) null else last() }

    // Unfortunate hack to get the outer class reference into the inner classes
    var classApi: ClassApi? = null
    classApi = ClassApi(
        name = fullClassName,
//        packageName = packageName?.toDotQualified(),
//        className = fullClassName.toDotQualified().substringAfterLast('$'),
        //TODO: more precise translation with annotations and generics
        superClass = if (classNode.superName == JavaLangObjectString) null else {
            SuperType(ObjectType(classNode.superName, dotQualified = false), listOf(), listOf())
        },
        superInterfaces = classNode.interfaces.map { SuperType(ObjectType(it, dotQualified = false),
            listOf(), listOf()) },
        methods = methods.toSet(), fields = fields.toSet(),
        innerClasses = classNode.innerClasses
            .filter { innerClassShortName != it.innerName && it.outerName == classNode.name}
            .map {
                readSingularClass(fs, fs.getPath(it.name + ".class"), lazy { classApi!! }, isStatic = it.isStatic)
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
//        signature = classNode.signature?.let { SignatureParser.make().parseClassSig(it) },
        isStatic = isStatic,
        isFinal = classNode.isfinal
    )


    return classApi
}

private fun String.innerClassShortName(): String? = if ('$' in this) this.substringAfterLast('$') else null