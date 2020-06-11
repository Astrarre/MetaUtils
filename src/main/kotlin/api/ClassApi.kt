package api

import QualifiedName
import ShortClassName
import codegeneration.ClassVisibility
import codegeneration.Public
import codegeneration.Visibility
import descriptor.MethodDescriptor
import descriptor.ObjectType
import includeIf
import signature.*

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
        val typeArguments: List<TypeArgumentDeclaration>,
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

/**
 * Goes from top to bottom
 */
@OptIn(ExperimentalStdlibApi::class)
fun ClassApi.listInnerClassChain(): List<ClassApi> = buildList<ClassApi> { addToInnerClassChain(this) }.reversed()
private fun ClassApi.addToInnerClassChain(accumulated: MutableList<ClassApi>) {
    accumulated.add(this)
    outerClass?.value?.addToInnerClassChain(accumulated)
}

val ClassApi.Method.isVoid get() = returnType.type == GenericReturnType.Void
fun ClassApi.asType() = name.toClassGenericType(
    listInnerClassChain().map { it.typeArguments.toTypeArgumentsOfNames() }
).noAnnotations()

fun ClassApi.asRawType() = ObjectType(name).toRawJavaType()

private fun List<TypeArgumentDeclaration>.toTypeArgumentsOfNames(): List<TypeArgument>? = if(isEmpty()) null else map {
    TypeArgument.SpecificType(TypeVariable(it.name), wildcardType = null)
}

fun ClassApi.innerMostClassNameAsType() = ObjectType(
    QualifiedName(
        packageName = null,
        shortName = ShortClassName(listOf(name.shortName.innermostClass()))
    )
).toRawJavaType()
