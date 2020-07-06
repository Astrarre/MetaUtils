package metautils.api

import metautils.descriptor.JvmType
import metautils.descriptor.ObjectType
import metautils.signature.*
import metautils.util.QualifiedName
import org.objectweb.asm.AnnotationVisitor


// This is actually not a complete representation of java type since it doesn't have annotations in type arguments,
// but those are rarely used, only exist in newer versions of jetbrains annotations, and even modern decompilers
// don't know how to decompile them, so it's fine to omit them here
data class JavaType<out T : GenericReturnType>(val type: T, val annotations: List<JavaAnnotation>) {
    override fun toString(): String = annotations.joinToString("") { "$it " } + type
}
typealias JavaClassType = JavaType<ClassGenericType>
//typealias JavaSuperType = JavaType<ClassGenericType>
typealias AnyJavaType = JavaType<GenericTypeOrPrimitive>
typealias JavaReturnType = JavaType<GenericReturnType>
typealias JavaThrowableType = JavaType<ThrowableType>

data class JavaAnnotation(val type: ObjectType, val parameters : Map<String, AnnotationValue>) {
    override fun toString(): String = "@$type"
}

sealed class AnnotationValue {
    class Array(val components: List<AnnotationValue>) : AnnotationValue()
    class Annotation(val annotation: JavaAnnotation) : AnnotationValue()
    sealed class Primitive : AnnotationValue() {
        abstract val primitive: Any

        class Num(override val primitive: Number) : Primitive()
        class Bool(override val primitive: Boolean) : Primitive()
        class Cha(override val primitive: Char) : Primitive()
        class Str(override val primitive: String) : Primitive()
    }

    class Enum(val type: ObjectType, val constant: String) : AnnotationValue()
    class ClassType(val type: JvmType) : AnnotationValue()
}

fun <T : GenericReturnType> JavaType<T>.remap(mapper: (className: QualifiedName) -> QualifiedName?) =
    copy(type = type.remap(mapper))

