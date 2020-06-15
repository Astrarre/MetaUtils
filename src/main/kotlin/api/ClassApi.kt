package api

import codegeneration.ClassAccess
import codegeneration.ClassVisibility
import codegeneration.MethodAccess
import codegeneration.Visibility
import signature.*
import util.QualifiedName
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
    val access: ClassAccess,
    val isStatic: Boolean,
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
        val access: MethodAccess
    ) : Member() {
        override fun toString() = "static ".includeIf(isStatic) +
                "$name(${parameters.map { (name, type) -> "$name: $type" }}): $returnType"

        override val isStatic = access.isStatic
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


