package api

import QualifiedName
import ShortClassName
import addIf
import codegeneration.ClassVisibility
import codegeneration.Public
import codegeneration.Visibility
import descriptor.*

interface Visible {
    val visibility: Visibility
}


/**
 * [ClassApi]es use dot.separated.format for the packageName always!
 */
class ClassApi(
    val name: QualifiedName,
//    val packageName: String?,
//    val className: String,
    val classType: Type,
    val superClass: SuperType?,
    val superInterfaces: List<SuperType>,
    val methods: Collection<Method>,
    val fields: Collection<Field>,
    val innerClasses: List<ClassApi>,
    val outerClass: Lazy<ClassApi>?,
    override val visibility: ClassVisibility,
    val isStatic: Boolean,
    val isFinal: Boolean
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
        override val isStatic: Boolean
    ) : Member() {
        override fun toString() = "static ".addIf(isStatic) + "$name$descriptor"

    }

    data class Field(
        override val name: String,
        override val descriptor: FieldDescriptor,
        override val isStatic: Boolean,
        override val visibility: Visibility,
        val isFinal: Boolean
    ) : Member() {
        override fun toString() = "static ".addIf(isStatic) + "$name: $descriptor"
    }

    override fun toString(): String {
        return "Class {\nmethods: [\n" + methods.joinToString("\n") { "\t$it" } +
                "\n]\nfields: [\n" + fields.joinToString("\n") { "\t$it" } + "\n]\n}"
    }
}


//val ClassApi.fullyQualifiedName get() = if(packageName == null) className else "$packageName.$className"
val ClassApi.isInterface get() = classType == ClassApi.Type.Interface
val ClassApi.isAbstract get() = classType == ClassApi.Type.AbstractClass
val ClassApi.isInnerClass get() = outerClass != null
val Visible.isPublicApi get() = isPublic || visibility == Visibility.Protected
val Visible.isPublic get() = visibility == Visibility.Public
val ClassApi.Method.isConstructor get() = name == "<init>"
fun ClassApi.Method.getParameters() = parameterNames.zip(descriptor.parameterDescriptors).toMap()
    // Remove outer class references as parameters (they are passed as this$0)
    .filter { '$' !in it.key }

val ClassApi.Method.returnType get() = descriptor.returnDescriptor
val ClassApi.Method.isVoid get() = returnType == ReturnDescriptor.Void
fun ClassApi.nameAsType() = ObjectType(name)
fun ClassApi.innerMostClassNameAsType() = ObjectType(
    QualifiedName(
        packageName = null,
        shortName = ShortClassName(listOf(name.shortName.innermostClass()))
    )
)
//fun ClassApi.innerClassNameAsType() = ObjectType.dotQualified(this.className)
// fun ClassApi.fullInnerName() : String {
//    return if (outerClass == null) fullyQualifiedName else outerClass.value.fullInnerName() + "\$" + className
//}