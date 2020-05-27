package codegeneration

import java.nio.file.Path
import javax.lang.model.element.Modifier

interface CodeGenerator {
    fun writeClass(packageName: String, name: String, writeTo: Path, init: GeneratedClass.() -> Unit)
    fun writeInterface(packageName: String, name: String, writeTo: Path, init: GeneratedInterface.() -> Unit)
}

interface GeneratedClass {
    fun method(
        name: String,
        returnType: String,
//        visibility: Visibility = Public,
//        final: Boolean = false,
        vararg parameters: Pair<String, String>
    )

    fun field(
        name: String,
        returnType: String,
//        visibility: Visibility = Public,
//        final: Boolean = false,
        vararg parameters: Pair<String, String>
    )
}

interface GeneratedInterface : GeneratedClass

enum class Visibility {
    Public,
    Private,
    Protected,
    Package
}

fun Visibility.toModifier(): Modifier? = when (this) {
    Visibility.Public -> Modifier.PUBLIC
    Visibility.Private -> Modifier.PRIVATE
    Visibility.Protected -> Modifier.PROTECTED
    Visibility.Package -> null
}