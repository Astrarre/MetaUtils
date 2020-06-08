data class QualifiedName(
    // Dot or slash qualified
    val packageName: PackageName?, val shortName: ShortClassName) {
//    fun toQualifiedString() = (packageName ?: QualifiedString.Empty) + shortName.toDollarQualified()

    private fun toQualified(separator: String): String = if (packageName == null) shortName.toFullString() else
        packageName.toQualified(separator) + separator + shortName.toFullString()

    // JavaPoet, reflection
    fun toDotQualifiedString() = toQualified(".")
    // ASM, JVM
    fun toSlashQualifiedString() = toQualified("/")

    override fun toString(): String  = toDotQualifiedString()
    // Inner classes
//    fun toDollarQualifiedString() = toQualified("$")

    fun packageStartsWith(vararg startingComponents: String): Boolean = packageName?.startsWith(*startingComponents) == true
}

//fun QualifiedString.toFullyQualifiedName(): FullyQualifiedName =
//    if(components.size == 1) FullyQualifiedName(packageName = null, className = components[0])
//    else FullyQualifiedName(packageName = QualifiedString(components.dropLast(1)), className = components.last())

//fun String.toDotQualified() = replace('/', '.')

fun String.toPackageName(dotQualified: Boolean): PackageName {
    val separator = if (dotQualified) '.' else '/'
    return PackageName(split(separator))
}

fun String.toShortClassName(): ShortClassName = ShortClassName(split("$"))

fun String.toQualifiedName(dotQualified: Boolean): QualifiedName {
    val separator = if (dotQualified) '.' else '/'
    val components = split(separator)
    return if (components.size == 1) QualifiedName(packageName = null, shortName = components.last().toShortClassName())
    else QualifiedName(packageName = PackageName(components.dropLast(1)), shortName = components.last().toShortClassName())
}

 fun String.prependToQualified(qualifiedString: PackageName) = PackageName(this.prependTo(qualifiedString.components))
// fun String?.prependIfNotNull(qualifiedString: QualifiedString) = if(this == null) qualifiedString else
//     QualifiedString(this.prependTo(qualifiedString.components))

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

data class PackageName(override val components: List<String>) : AbstractQualifiedString(){
    companion object {
        val Empty = PackageName(listOf())
    }

    operator fun plus(other : PackageName) = PackageName(this.components + other.components)
    fun toDotQualified() = toQualified(".")
    fun toSlashQualified() = toQualified("/")



//    fun splitPackageClass(): QualifiedName {
//        require(components.isNotEmpty())
//        return if (components.size == 1) QualifiedName(packageName = null, shortName = components[0])
//        else QualifiedName(packageName = PackageName(components.dropLast(1)), shortName = components.last())
//    }
}

data class ShortClassName(override val components: List<String>) : AbstractQualifiedString(){
    init {
        require(components.isNotEmpty())
    }
    fun toFullString() = toQualified("$")
    fun outerClass() = components[0]
    fun innerClasses() = components.drop(1)
    fun innermostClass() = components.last()
}
