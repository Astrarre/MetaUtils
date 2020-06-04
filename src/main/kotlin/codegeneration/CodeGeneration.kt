package codegeneration

import java.nio.file.Path
import javax.lang.model.element.Modifier


enum class SelfConstructorType {
    This,
    Super
}

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


sealed class Visibility {
    companion object

    object Protected : Visibility()
}

sealed class ClassVisibility : Visibility() {
    object Public : ClassVisibility()
    object Private : ClassVisibility()
    object Package : ClassVisibility()
}


val Visibility.Companion.Public get() = ClassVisibility.Public
val Visibility.Companion.Private get() = ClassVisibility.Private
val Visibility.Companion.Package get() = ClassVisibility.Package


fun Visibility.toModifier(): Modifier? = when (this) {
    ClassVisibility.Public -> Modifier.PUBLIC
    ClassVisibility.Private -> Modifier.PRIVATE
    Visibility.Protected -> Modifier.PROTECTED
    ClassVisibility.Package -> null
}