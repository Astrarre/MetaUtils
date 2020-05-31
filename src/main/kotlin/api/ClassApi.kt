package api

import addIf
import codegeneration.ClassVisibility
import codegeneration.Visibility
import descriptor.Descriptor
import descriptor.FieldDescriptor
import descriptor.MethodDescriptor
import sun.reflect.generics.tree.ClassSignature
import sun.reflect.generics.tree.MethodTypeSignature
import sun.reflect.generics.tree.TypeSignature

interface Visible {
    val visibility : Visibility
}


/**
 * [ClassApi]es use dot.separated.format for the packageName always!
 */
data class ClassApi(
    val packageName: String,
    val className: String,
    val type: Type,
    val methods: Set<Method>,
    val fields: Set<Field>,
    val innerClasses: Set<ClassApi>,
    override val visibility: ClassVisibility,
    val signature: ClassSignature?
) : Visible{
    companion object;

    enum class Type {
        Interface,
        ConcreteClass,
        AbstractClass,
        Enum,
        Annotation
    }

    abstract class Member : Visible {
        abstract val name: String
        abstract val descriptor: Descriptor
        abstract val static: Boolean
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


val ClassApi.fullyQualifiedName get() = "$packageName.$className"
val ClassApi.isInterface get() = type == ClassApi.Type.Interface
val Visible.isPublicApi get() = isPublic || visibility == Visibility.Protected
val Visible.isPublic get() = visibility == ClassVisibility.Public
val ClassApi.Method.isConstructor get() = name == "<init>"
