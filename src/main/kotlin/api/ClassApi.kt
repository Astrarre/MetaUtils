package api

import addIf
import codegeneration.ClassVisibility
import codegeneration.Visibility
import descriptor.*
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
    val packageName: String?,
    val className: String,
    val classType: Type,
    val methods: Set<Method>,
    val fields: Set<Field>,
    val innerClasses: Set<ClassApi>,
    override val visibility: ClassVisibility,
    val signature: ClassSignature?
) : Visible {
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
        abstract val isStatic: Boolean
    }


    data class Method(
        override val name: String,
        override val descriptor: MethodDescriptor,
        val parameterNames: List<String>,
        override val visibility: Visibility,
        override val isStatic: Boolean,
        val signature: MethodTypeSignature?
    ) : Member() {
        override fun toString() = "static ".addIf(isStatic) + "$name$descriptor"

    }

    data class Field(
        override val name: String,
        override val descriptor: FieldDescriptor,
        override val isStatic: Boolean,
        override val visibility: Visibility,
        val signature: TypeSignature?
    ) : Member() {
        override fun toString() = "static ".addIf(isStatic) + "$name: $descriptor"
    }

    override fun toString(): String {
        return "Class {\nmethods: [\n" + methods.joinToString("\n") { "\t$it" } +
                "\n]\nfields: [\n" + fields.joinToString("\n") { "\t$it" } + "\n]\n}"
    }
}


val ClassApi.fullyQualifiedName get() = if(packageName == null) className else "$packageName.$className"
val ClassApi.isInterface get() = classType == ClassApi.Type.Interface
val Visible.isPublicApi get() = isPublic || visibility == Visibility.Protected
val Visible.isPublic get() = visibility == Visibility.Public
val ClassApi.Method.isConstructor get() = name == "<init>"
val ClassApi.Method.parameters get() = parameterNames.zip(descriptor.parameterDescriptors).toMap()
val ClassApi.Method.returnType get() = descriptor.returnDescriptor
val ClassApi.Method.isVoid get() = returnType == ReturnDescriptor.Void
fun ClassApi.nameAsType() = ObjectType.dotQualified(this.fullyQualifiedName)