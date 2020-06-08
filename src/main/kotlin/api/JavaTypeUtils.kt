package api

import QualifiedName
import api.JavaType.Class.Normal
import api.JavaType.Class.SuperType
import descriptor.remap

fun <T : GenericJavaType> T.remap(mapper: (className: QualifiedName) -> QualifiedName?): T = when (this) {
    is JavaType.Generic -> with(declaration) {
        copy(declaration = copy(upperBounds = upperBounds.remap(mapper), annotations = annotations.remap(mapper)))
    }
    is Normal -> Normal(rawType.remap(mapper), typeArguments.remap(mapper), annotations.remap(mapper))
    is SuperType -> SuperType(rawType.remap(mapper), typeArguments.remap(mapper), annotations.remap(mapper))
    is GenericJavaType.Wildcard -> copy(bound = bound.remap(mapper), annotations = annotations.remap(mapper))
    else -> error("impossible")
} as T
typealias SuperType = SuperType

fun <T : GenericJavaType> Iterable<T>.remap(mapper: (className: QualifiedName) -> QualifiedName?) = map { it.remap(mapper) }
