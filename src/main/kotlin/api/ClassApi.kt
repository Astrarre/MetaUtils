package api

import QualifiedName
import ShortClassName
import codegeneration.ClassVisibility
import codegeneration.Public
import codegeneration.Visibility
import descriptor.MethodDescriptor
import descriptor.ObjectType
import includeIf
import signature.GenericReturnType
import signature.TypeArgumentDeclaration
import signature.toJvmType
import signature.toRawJavaType

interface Visible {
    val visibility: Visibility
}


/**
 * [ClassApi]es use dot.separated.format for the packageName always!
 */
class ClassApi(
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
        val typeParameters : List<TypeArgumentDeclaration>,
        override val visibility: Visibility,
        override val isStatic: Boolean
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

    override fun toString(): String {
        return "Class {\nmethods: [\n" + methods.joinToString("\n") { "\t$it" } +
                "\n]\nfields: [\n" + fields.joinToString("\n") { "\t$it" } + "\n]\n}"
    }
}


//val ClassApi.fullyQualifiedName get() = if(packageName == null) className else "$packageName.$className"
val ClassApi.isInterface get() = classVariant == ClassApi.Variant.Interface
val ClassApi.isAbstract get() = classVariant == ClassApi.Variant.AbstractClass
val ClassApi.isInnerClass get() = outerClass != null
val Visible.isPublicApi get() = isPublic || visibility == Visibility.Protected
val Visible.isPublic get() = visibility == Visibility.Public
val ClassApi.Method.isConstructor get() = name == "<init>"
fun ClassApi.Method.getJvmDescriptor() = MethodDescriptor(
    parameterDescriptors = parameters.map { (_, type) -> type.type.toJvmType() },
    returnDescriptor = returnType.toJvmType()
)
//fun ClassApi.Method.getParameters(): Map<String, ParameterSi> =
//    parameterNames.zip(signature.parameterDescriptors).toMap()
//        // Remove outer class references as parameters (they are passed as this$0)
//        .filter { '$' !in it.key }

//val ClassApi.Method.returnType get() = signature.returnDescriptor
val ClassApi.Method.isVoid get() = returnType.type == GenericReturnType.Void
fun ClassApi.nameAsType() = ObjectType(name).toRawJavaType()
fun ClassApi.innerMostClassNameAsType() = ObjectType(
    QualifiedName(
        packageName = null,
        shortName = ShortClassName(listOf(name.shortName.innermostClass()))
    )
).toRawJavaType()
//fun ClassApi.innerClassNameAsType() = ObjectType.dotQualified(this.className)
// fun ClassApi.fullInnerName() : String {
//    return if (outerClass == null) fullyQualifiedName else outerClass.value.fullInnerName() + "\$" + className
//}