package codegeneration

import descriptor.AnyType
import java.nio.file.Path
import javax.lang.model.element.Modifier

sealed class Expression {
    class Class(val type: AnyType) : Expression()

    sealed class Value : Expression() {
        class Variable(val name: String) : Value()
        class Cast(val target: Value, val castTo: AnyType) : Value()
        class Field(val owner: Expression, val name: String) : Value()
        object This : Value()
    }

    object Super : Expression()
}

fun Expression.Value.castTo(type: AnyType) = Expression.Value.Cast(this, type)

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
    object Protected : Visibility()
}

sealed class ClassVisibility : Visibility() {
    object Public : ClassVisibility()
    object Private : ClassVisibility()
    object Package : ClassVisibility()
}



fun Visibility.toModifier(): Modifier? = when (this) {
    ClassVisibility.Public -> Modifier.PUBLIC
    ClassVisibility.Private -> Modifier.PRIVATE
    Visibility.Protected -> Modifier.PROTECTED
    ClassVisibility.Package -> null
}