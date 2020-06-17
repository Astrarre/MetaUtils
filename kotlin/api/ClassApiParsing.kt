package api

import asm.*
import codegeneration.ClassAccess
import codegeneration.ClassVariant
import codegeneration.MethodAccess
import descriptor.*
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import signature.*
import util.*
import java.nio.file.Path

fun ClassApi.Companion.readFromJar(jarPath: Path): Collection<ClassApi> {
    require(jarPath.hasExtension(".jar")) { "Specified path $jarPath does not point to a jar" }

    // Only pass top-level classes into readSingularClass()

    return jarPath.openJar { jar ->
        val root = jar.getPath("/")
        root.walk().asSequence().readFromSequence(root)
    }
}


fun ClassApi.Companion.readFromDirectory(dirPath: Path): Collection<ClassApi> {
    require(dirPath.isDirectory()) { "Specified path $dirPath is not a directory" }

    return dirPath.recursiveChildren().readFromSequence(dirPath)
}

fun ClassApi.Companion.readFromList(
    list: List<Path>,
    rootPath: Path
): Collection<ClassApi> =
    list.asSequence().readFromSequence(rootPath)

private fun Sequence<Path>.readFromSequence(rootPath: Path): Collection<ClassApi> = filter {
    // Only pass top-level classes into readSingularClass()
    it.isClassfile() && '$' !in it.toString()
}
//            .filter { "Concrete" in it.toString() }
    .map { readSingularClass(rootPath, it, outerClass = null, isStatic = false) }
    .toList()


// isStatic information is passed by the parent class for inner classes
private fun readSingularClass(
    rootPath: Path,
    path: Path,
    outerClass: Lazy<ClassApi>?,
    isStatic: Boolean
): ClassApi {

    val classNode = readToClassNode(path)
    val methods = classNode.methods.map { readMethod(it, classNode) }
    val fields = classNode.fields.map { readField(it) }

    val fullClassName = classNode.name.toQualifiedName(dotQualified = false)

    val signature = if (classNode.signature != null) ClassSignature.readFrom(classNode.signature)
    else ClassSignature(
        superClass = ClassGenericType.fromRawClassString(classNode.superName),
        superInterfaces = classNode.interfaces.map {
            ClassGenericType.fromRawClassString(it)
        },
        typeArguments = listOf()
    )

//    val innerClasses = classNode.innerClasses.map { it.name to it }.toMap()
    val innerClassShortName = with(fullClassName.shortName.components) { if (size == 1) null else last() }


    // Unfortunate hack to get the outer class reference into the inner classes
    var classApi: ClassApi? = null
    classApi = ClassApi(
        name = fullClassName,
        superClass = if (classNode.superName == JavaLangObjectString) null else {
            JavaClassType(signature.superClass, annotations = listOf())
        },
        superInterfaces = signature.superInterfaces.map { JavaClassType(it, annotations = listOf()) },
        methods = methods.toSet(), fields = fields.toSet(),
        innerClasses = classNode.innerClasses
            .filter { innerClassShortName != it.innerName && it.outerName == classNode.name }
            .map {
                readSingularClass(
                    rootPath,
                    rootPath.resolve("${it.name}.class"),
                    lazy { classApi!! },
                    isStatic = it.isStatic
                )
            },
        outerClass = outerClass,
        visibility = classNode.visibility,
        access = ClassAccess(
            variant = with(classNode) {
                when {
                    isInterface -> ClassVariant.Interface
                    isAnnotation -> ClassVariant.Annotation
                    isEnum -> ClassVariant.Enum
                    isAbstract -> ClassVariant.AbstractClass
                    else -> ClassVariant.ConcreteClass
                }
            },
            isFinal = classNode.isFinal
        ),
        isStatic = isStatic,
        typeArguments = signature.typeArguments ?: listOf(),
        annotations = parseAnnotations(classNode.visibleAnnotations, classNode.invisibleAnnotations),
        asmInnerClasses = classNode.innerClasses
    )

    return classApi
}

private fun parseAnnotations(visible: List<AnnotationNode>?, invisible: List<AnnotationNode>?): List<JavaAnnotation> {
    val combined = when {
        visible == null -> invisible
        invisible == null -> visible
        else -> visible + invisible
    }
    combined ?: return listOf()
    return combined.map { JavaAnnotation(FieldType.read(it.desc) as ObjectType) }
}

private fun readField(field: FieldNode): ClassApi.Field {
    val signature = if (field.signature != null) FieldSignature.readFrom(field.signature)
    else FieldDescriptor.read(field.desc).toRawGenericType()

    return ClassApi.Field(
        name = field.name,
        type = AnyJavaType(
            signature,
            annotations = parseAnnotations(field.visibleAnnotations, field.invisibleAnnotations)
        ),
        isStatic = field.isStatic,
        isFinal = field.isFinal,
        visibility = field.visibility
    )
}


// Generated parameters are generated $this garbage that come from for example inner classes
private fun getNonGeneratedParameterDescriptors(
    descriptor: MethodDescriptor,
    method: MethodNode
): List<ParameterDescriptor> {
    if (method.parameters == null) return descriptor.parameterDescriptors
    val generatedIndices = method.parameters.mapIndexed { i, node -> i to node }.filter { '$' in it.second.name }
        .map { it.first }

    return descriptor.parameterDescriptors.filterIndexed { i, _ -> i !in generatedIndices }
}

private fun readMethod(
    method: MethodNode,
    classNode: ClassNode
): ClassApi.Method {
    val signature = if (method.signature != null) MethodSignature.readFrom(method.signature) else {
        val descriptor = MethodDescriptor.read(method.desc)
        val parameters = getNonGeneratedParameterDescriptors(descriptor, method)
        MethodSignature(
            typeArguments = null, parameterTypes = parameters.map { it.toRawGenericType() },
            returnType = descriptor.returnDescriptor.toRawGenericType(),
            throwsSignatures = method.exceptions.map { ClassGenericType.fromRawClassString(it) }
        )
    }
    val parameterNames = inferParameterNames(method, classNode, signature.parameterTypes.size)

    val visibility = method.visibility

    return ClassApi.Method(
        name = method.name,
        typeArguments = signature.typeArguments ?: listOf(),
        returnType = JavaReturnType(
            signature.returnType,
            annotations = parseAnnotations(method.visibleAnnotations, method.invisibleAnnotations)
        ),
        parameters = parameterNames.zip(signature.parameterTypes)
            .mapIndexed { index, (name, type) ->
                name to AnyJavaType(
                    type, annotations = parseAnnotations(
                        method.visibleParameterAnnotations?.get(index),
                        method.invisibleParameterAnnotations?.get(index)
                    )
                )
            }.toMap(),
        throws = signature.throwsSignatures.map { it.noAnnotations() },
        access = MethodAccess(isStatic = method.isStatic,
            isFinal = method.isFinal,
            isAbstract = method.isAbstract,
            visibility = visibility
        )
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
            method.parameters.filter { '$' !in it.name }.map { it.name }
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