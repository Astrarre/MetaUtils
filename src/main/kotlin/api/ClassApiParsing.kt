package api

import api.ClassApi.Variant.*
import api.ClassApi.Variant.Annotation
import api.ClassApi.Variant.Enum
import applyIf
import asm.*
import descriptor.*
import hasExtension
import isClassfile
import openJar
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import signature.*
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
    val methods = classNode.methods.map { readMethod(it, classNode) }
    val fields = classNode.fields.map { readField(it) }

    val fullClassName = classNode.name.toQualifiedName(dotQualified = false)
    val innerClassShortName = with(fullClassName.shortName.components) { if (size == 1) null else last() }

    val signature = if (classNode.signature != null) ClassSignature.readFrom(classNode.signature)
    else ClassSignature(
        superClass = ClassGenericType.fromRawClassString(classNode.superName),
        superInterfaces = classNode.interfaces.map {
            ClassGenericType.fromRawClassString(it)
        },
        typeArguments = listOf()
    )

    // Unfortunate hack to get the outer class reference into the inner classes
    var classApi: ClassApi? = null
    classApi = ClassApi(
        name = fullClassName,
        //TODO: annotations
        superClass = if (classNode.superName == JavaLangObjectString) null else {
            JavaClassType(signature.superClass, annotations = listOf())
        },
        superInterfaces = signature.superInterfaces.map { JavaClassType(it, annotations = listOf()) },
        methods = methods.toSet(), fields = fields.toSet(),
        innerClasses = classNode.innerClasses
            .filter { innerClassShortName != it.innerName && it.outerName == classNode.name }
            .map {
                readSingularClass(fs, fs.getPath(it.name + ".class"), lazy { classApi!! }, isStatic = it.isStatic)
            },
        outerClass = outerClass,
        classVariant = with(classNode) {
            when {
                isInterface -> Interface
                isAnnotation -> Annotation
                isEnum -> Enum
                isAbstract -> AbstractClass
                else -> ConcreteClass
            }
        },
        visibility = classNode.visibility,
        isStatic = isStatic,
        isFinal = classNode.isfinal,
        typeArguments = signature.typeArguments ?: listOf()
    )

    return classApi
}

private fun readField(field: FieldNode): ClassApi.Field {
    val signature = if (field.signature != null) FieldSignature.readFrom(field.signature)
    else FieldDescriptor.read(field.desc).toRawGenericType()

    return ClassApi.Field(
        name = field.name, type = AnyJavaType(signature, annotations = listOf()),
        isStatic = field.isStatic, isFinal = field.isFinal, visibility = field.visibility
    )
}


private fun readMethod(
    method: MethodNode,
    classNode: ClassNode
): ClassApi.Method {
    val signature = if (method.signature != null) MethodSignature.readFrom(method.signature) else {
        val descriptor = MethodDescriptor.read(method.desc)
        MethodSignature(
            typeArguments = listOf(), parameterTypes = descriptor.parameterDescriptors.map { it.toRawGenericType() },
            returnType = descriptor.returnDescriptor.toRawGenericType(), throwsSignatures = listOf()
        )
    }
    val parameterNames = inferParameterNames(method, classNode, signature.parameterTypes.size)

    val visibility = method.visibility

    return ClassApi.Method(
        name = method.name,
        typeParameters = signature.typeArguments ?: listOf(),
        returnType = JavaReturnType(signature.returnType, annotations = listOf()),
        parameters = parameterNames.zip(signature.parameterTypes)
            .map { (name, type) -> name to AnyJavaType(type, annotations = listOf()) }.toMap(),
        isStatic = method.isStatic,
        visibility = visibility
    )
}

private fun inferParameterNames(
    method: MethodNode,
    classNode: ClassNode,
    parameterCount: Int
): List<String> {
    val locals = method.localVariables
    return when {
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

            check(namesWithEnum.size >= parameterCount) {
                "There was not enough (${namesWithEnum.size}) local variable debug information for all parameters" +
                        " ($parameterCount} of them) in method ${method.name}"
            }
            namesWithEnum.take(parameterCount).map { it }
        }
        else -> listOf()
    }
}

//private fun String.innerClassShortName(): String? = if ('$' in this) this.substringAfterLast('$') else null