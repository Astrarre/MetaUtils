package api

import codegeneration.ClassVisibility
import codegeneration.Public
import codegeneration.Visibility
import descriptor.MethodDescriptor
import descriptor.ObjectType
import signature.*
import util.QualifiedName
import util.ShortClassName
import util.includeIf

interface Visible {
    val visibility: Visibility
}


/**
 * [ClassApi]es use dot.separated.format for the packageName always!
 */
class ClassApi(
    val annotations : List<JavaAnnotation>,
    override val visibility: ClassVisibility,
    val isStatic: Boolean,
    val isFinal: Boolean,
    val classVariant: Variant,
    val name: QualifiedName,
    val typeArguments: List<TypeArgumentDeclaration>,
    val superClass: JavaClassType?,
    val superInterfaces: List<JavaClassType>,
    val methods: Collection<Method>,
    val fields: Collection<Field>,
    val innerClasses: List<ClassApi>,
    val outerClass: Lazy<ClassApi>?
) : Visible {

    override fun toString(): String {
        return visibility.toString() + "static".includeIf(isStatic) + "final".includeIf(isFinal) + name.shortName
    }

    companion object;

    enum class Variant {
        Interface,
        ConcreteClass,
        AbstractClass,
        Enum,
        Annotation
    }

    abstract class Member : Visible {
        abstract val name: String

        //        abstract val signature: Signature
        abstract val isStatic: Boolean
    }


    data class Method(
        override val name: String,
        val returnType: JavaReturnType,
        val parameters: Map<String, AnyJavaType>,
        val typeArguments: List<TypeArgumentDeclaration>,
        val throws : List<JavaThrowableType>,
        override val visibility: Visibility,
        override val isStatic: Boolean,
        val isFinal: Boolean
    ) : Member() {
        override fun toString() = "static ".includeIf(isStatic) +
                "$name(${parameters.map { (name, type) -> "$name: $type" }}): $returnType"

    }

    data class Field(
        override val name: String,
        val type: AnyJavaType,
        override val isStatic: Boolean,
        override val visibility: Visibility,
        val isFinal: Boolean
    ) : Member() {
        override fun toString() = "static ".includeIf(isStatic) + "$name: $type"
    }



//    override fun toString(): String {
//        return "Class {\nmethods: [\n" + methods.joinToString("\n") { "\t$it" } +
//                "\n]\nfields: [\n" + fields.joinToString("\n") { "\t$it" } + "\n]\n}"
//    }
}


