package api

import QualifiedName
import signature.ClassGenericType
import signature.GenericTypeOrPrimitive
import signature.remap


// This is actually not a complete representation of java type since it doesn't have annotations in type arguments,
// but those are rarely used, only exist in newer versions of jetbrains annotations, and even modern decompilers
// don't know how to decompile them, so it's fine to omit them here
data class JavaType<T : GenericTypeOrPrimitive>(val type: T, val annotations: List<Annotation>) {
    override fun toString(): String = annotations.joinToString("") { "@$it " } + type
}
typealias SuperType = JavaType<ClassGenericType>

class Annotation //TODO: fill out - name, parameter names and passed parameter values

fun <T : GenericTypeOrPrimitive> JavaType<T>.remap(mapper: (className: QualifiedName) -> QualifiedName?) =
    copy(type = type.remap(mapper))

