package metautils.util

import asm.readToClassNode
import descriptor.JavaLangObjectName
import metautils.descriptor.MethodDescriptor
import metautils.descriptor.read
import org.objectweb.asm.tree.ClassNode
import util.*
import java.lang.reflect.Method
import java.nio.file.Path

data class MethodEntry(val name: String, val descriptor: String) {
    val descriptorParsed = lazy { MethodDescriptor.read(descriptor) }
}

data class ClassEntry(
    val methods: Set<MethodEntry>,
    val superClass: QualifiedName?,
    val superInterfaces: List<QualifiedName>,
    val access: Int,
    val name: QualifiedName
)

private val ClassEntry.directSuperTypes: List<QualifiedName>
    get() = if (superClass != null) superInterfaces + superClass else superInterfaces

/**
 * class names use slash/separated/format
 */
data class ClasspathIndex(private val classes: Map<QualifiedName, ClassEntry>) {
    private val jdkClassesCache = mutableMapOf<QualifiedName, ClassEntry>()

    private fun verifyClassName(name: QualifiedName) {
        require(classes.containsKey(name)) {
            "Attempt to find class not in the specified classpath: $name"
        }
    }

    private fun QualifiedName.isJavaClass(): Boolean = packageName?.startsWith("java") == true

    private fun getClassEntry(className: QualifiedName): ClassEntry = if (className.isJavaClass()) {
        jdkClassesCache.computeIfAbsent(className) {
            val clazz = Class.forName(className.toDotQualifiedString())
            ClassEntry(
                methods = clazz.methods.map { MethodEntry(it.name, getMethodDescriptor(it)) }
                    .toSet(),
                superInterfaces = clazz.interfaces.map { it.name.toQualifiedName(dotQualified = true) },
                superClass = clazz.superclass?.name?.toQualifiedName(dotQualified = true),
                access = clazz.modifiers, // I think this is the same
                name = className
            )
        }
    } else {
        verifyClassName(className)
        classes.getValue(className)
    }


    fun classHasMethod(className: QualifiedName, methodName: String, methodDescriptor: MethodDescriptor): Boolean {
        return getClassEntry(className).methods.contains(
            MethodEntry(
                methodName,
                methodDescriptor.classFileName
            )
        )
    }

    fun accessOf(className: QualifiedName) = getClassEntry(className).access

//    /**
//     * Not including Object itself
//     */
//    fun getAllSuperClassesFromClassToObject(className: QualifiedName): List<QualifiedName> {
//        return recursiveList(getClassEntry(className)) { next -> next.superClass?.let { getClassEntry(it) } }
//            .map { it.name }
//    }

//    fun classHasMethodIgnoringReturnType(
//        className: QualifiedName,
//        methodName: String,
//        methodDescriptor: MethodDescriptor
//    ): Boolean = getClassEntry(className).methods.any {
//        if (it.name != methodName) return@any false
//        val descriptor = it.descriptorParsed.value
//        descriptor.parameterDescriptors == methodDescriptor.parameterDescriptors
//    }

    fun getSuperTypesRecursively(className: QualifiedName): Set<QualifiedName> {
        return (getSuperTypesRecursivelyImpl(className) + JavaLangObjectName).toSet()
    }

    private fun getSuperTypesRecursivelyImpl(className: QualifiedName): List<QualifiedName> {
        val directSupers = getClassEntry(className).directSuperTypes
        return (directSupers + directSupers.filter { it != JavaLangObjectName }
            .flatMap { getSuperTypesRecursivelyImpl(it) })
    }

    private fun getDescriptorForClass(c: Class<*>): String {
        if (c.isPrimitive) {
            return when (c.name) {
                "byte" -> "B"
                "char" -> "C"
                "double" -> "D"
                "float" -> "F"
                "int" -> "I"
                "long" -> "J"
                "short" -> "S"
                "boolean" -> "Z"
                "void" -> "V"
                else -> error("Unrecognized primitive $c")
            }
        }
        if (c.isArray) return c.name.replace('.', '/')
        return ('L' + c.name + ';').replace('.', '/')
    }

    private fun getMethodDescriptor(m: Method): String {
        val s = buildString {
            append('(')
            for (c in m.parameterTypes) {
                append(getDescriptorForClass(c))
            }
            append(')')
        }
        return s + getDescriptorForClass(m.returnType)
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun indexClasspath(classPath: List<Path>, additionalEntries: Map<QualifiedName, ClassEntry>): ClasspathIndex {
    val map = classPath.flatMap { path ->
        getClasses(path).map { classNode ->
            val name = classNode.name.toQualifiedName(dotQualified = false)
            name to ClassEntry(
                methods = classNode.methods.map { MethodEntry(it.name, it.desc) }.toHashSet(),
                superClass = classNode.superName.toQualifiedName(dotQualified = false),
                superInterfaces = classNode.interfaces.map { it.toQualifiedName(dotQualified = false) },
                access = classNode.access,
                name = name
            )
        }
    }.toMap()
    return ClasspathIndex(map + additionalEntries)
}


private fun getClasses(path: Path): List<ClassNode> = when {
    !path.exists() -> listOf()
    path.isDirectory() -> path.recursiveChildren().filter { it.isClassfile() }.map { readToClassNode(it) }.toList()
    path.toString().endsWith(".jar") -> path.walkJar { paths ->
        paths.filter { it.isClassfile() }.map { readToClassNode(it) }.toList()
    }
    else -> error("Got a classpath element which is not a jar or directory: $path")
}
