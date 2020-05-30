package api

import codegeneration.Visibility
import descriptor.Descriptor
import descriptor.FieldDescriptor
import descriptor.MethodDescriptor
import sun.reflect.generics.tree.ClassSignature
import sun.reflect.generics.tree.FieldTypeSignature
import sun.reflect.generics.tree.MethodTypeSignature
import sun.reflect.generics.tree.TypeSignature

/**
 * [ApiClass]es use dot.separated.format for the packageName always!
 */
data class ApiClass(
    val packageName: String,
    val className: String,
    val type: Type,
    val methods: Set<Method>,
    val fields: Set<Field>,
    val innerClasses: Set<ApiClass>,
    val signature: ClassSignature?
) {
    companion object;

    enum class Type {
        Interface,
        Baseclass
    }

    abstract class Member {
        abstract val name: String
        abstract val descriptor: Descriptor
        abstract val static: Boolean
        abstract val visibility: Visibility
    }


    data class Method(
        override val name: String,
        override val descriptor: MethodDescriptor,
        val parameterNames: List<String>,
        override val visibility: Visibility,
        override val static: Boolean,
        val signature: MethodTypeSignature?
    ) : Member() {
        override fun toString() = "static ".addIf(static) + "$name$descriptor"

    }

    data class Field(
        override val name: String,
        override val descriptor: FieldDescriptor,
        override val static: Boolean,
        override val visibility: Visibility,
        val signature: TypeSignature?
    ) : Member() {
        override fun toString() = "static ".addIf(static) + "$name: $descriptor"
    }

    override fun toString(): String {
        return "Class {\nmethods: [\n" + methods.joinToString("\n") { "\t$it" } +
                "\n]\nfields: [\n" + fields.joinToString("\n") { "\t$it" } + "\n]\n}"
    }
}

private fun String.addIf(boolean: Boolean) = if (boolean) this else ""

val ApiClass.fullyQualifiedName get() = "$packageName.$className"
val ApiClass.isInterface get() = type == ApiClass.Type.Interface
val ApiClass.isBaseclass get() = type == ApiClass.Type.Baseclass
val ApiClass.Member.isPublicApi get() = visibility == Visibility.Public || visibility == Visibility.Protected
val ApiClass.Method.isConstructor get() = name == "<init>"
