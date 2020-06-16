package util

import java.nio.file.Path
import java.nio.file.Paths

data class QualifiedName(
    // Dot or slash qualified
    val packageName: PackageName?, val shortName: ShortClassName
) {

    private fun toQualified(packageSeparator: String, classSeparator: String): String =
        if (packageName == null) shortName.toDollarQualifiedString() else
            packageName.toQualified(packageSeparator) + packageSeparator + shortName.toQualified(classSeparator)


    // JavaPoet, reflection
    fun toDotQualifiedString(dollarQualified: Boolean = true) =
        toString(dotQualified = true, dollarQualified = dollarQualified)

    // ASM, JVM
    fun toSlashQualifiedString(dollarQualified: Boolean = true) =
        toString(dotQualified = false, dollarQualified = dollarQualified)

    fun toString(dotQualified: Boolean = false, dollarQualified: Boolean = true): String {
        val packageSeparator = if (dotQualified) "." else "/"
        val classSeparator = if (dollarQualified) "$" else "."
        return toQualified(packageSeparator, classSeparator)
    }

    override fun toString(): String = toDotQualifiedString(dollarQualified = true)

    fun packageStartsWith(vararg startingComponents: String): Boolean =
        packageName?.startsWith(*startingComponents) == true
}

fun QualifiedName.innerClass(name: String) = copy(
    shortName = shortName.copy(components = shortName.components + name)
)


fun String.toPackageName(dotQualified: Boolean): PackageName {
    val separator = if (dotQualified) '.' else '/'
    return PackageName(split(separator))
}

fun String.toShortClassName(dollarQualified: Boolean = true): ShortClassName =
    ShortClassName(split(if (dollarQualified) "$" else "."))

fun String.toQualifiedName(dotQualified: Boolean, dollarQualified: Boolean = true): QualifiedName {
    val separator = if (dotQualified) '.' else '/'
    val components = split(separator)
    return if (components.size == 1) QualifiedName(
        packageName = null,
        shortName = components.last().toShortClassName(dollarQualified)
    )
    else QualifiedName(
        packageName = PackageName(components.dropLast(1)),
        shortName = components.last().toShortClassName(dollarQualified)
    )
}

fun String.prependToQualified(qualifiedString: PackageName) =
    PackageName(this.prependTo(qualifiedString.components))

sealed class AbstractQualifiedString {
    abstract val components: List<String>
    fun startsWith(vararg startingComponents: String): Boolean {
        require(startingComponents.isNotEmpty())
        for (i in startingComponents.indices) {
            if (i >= components.size || startingComponents[i] != this.components[i]) return false
        }
        return true
    }


    internal fun toQualified(separator: String) = components.joinToString(separator)

}

fun PackageName?.toPath(): Path = if (this == null || components.isEmpty()) Paths.get("") else {
    Paths.get(components[0], *components.drop(1).toTypedArray())
}

fun QualifiedName.toPath(suffix: String = ""): Path = packageName.toPath()
    .resolve(shortName.toDollarQualifiedString() + suffix)

data class PackageName(override val components: List<String>) : AbstractQualifiedString() {
    companion object {
        val Empty = PackageName(listOf())
    }

    operator fun plus(other: PackageName) =
        PackageName(this.components + other.components)

    fun toDotQualified() = toQualified(".")
    fun toSlashQualified() = toQualified("/")
    override fun toString(): String = toSlashQualified()
}

data class ShortClassName(override val components: List<String>) : AbstractQualifiedString() {
    override fun toString(): String = toDollarQualifiedString()

    init {
        require(components.isNotEmpty())
    }

    fun toDollarQualifiedString() = toQualified("$")
    fun toDotQualifiedString() = toQualified(".")
    fun outerClass() = components[0]
    fun innerClasses() = components.drop(1)
    fun innermostClass() = components.last()
}
