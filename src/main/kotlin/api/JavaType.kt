package api

import descriptor.ObjectType
import signature.*
import util.QualifiedName


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

//TODO: annotations theoretically can also have values
data class JavaAnnotation(val type: ObjectType/*, val parameters : Map<String, >*/){
    override fun toString(): String = "@$type"
}

fun <T : GenericReturnType> JavaType<T>.remap(mapper: (className: QualifiedName) -> QualifiedName?) =
    copy(type = type.remap(mapper))

