package signature

import QualifiedName
import ShortClassName
import descriptor.remap
import toQualifiedName

//val ClassSignature.superTypes get() = superInterfaces + superClass
fun ClassGenericType.Companion.fromRawClassString(string: String, dotQualified: Boolean = false): ClassGenericType {
    val name = string.toQualifiedName(dotQualified)
    return ClassGenericType(name.packageName, name.shortName.components.map { SimpleClassGenericType(it, null) })
}

fun <T : GenericTypeOrPrimitive> T.remap(mapper: (className: QualifiedName) -> QualifiedName?): T = when (this) {
    is PrimitiveTypeForGenerics -> copy(primitive.remap(mapper))
    is ClassGenericType -> remap(mapper)
    is TypeVariable -> this
    is ArrayGenericType -> copy(type.remap(mapper))
    else -> error("impossible")
} as T

private fun ClassGenericType.remap(mapper: (className: QualifiedName) -> QualifiedName?): ClassGenericType {
    val asQualifiedName = QualifiedName(packageName, ShortClassName(classNameChain.map { it.name }))
    val asMappedQualifiedName = mapper(asQualifiedName) ?: asQualifiedName
    val mappedPackage = asMappedQualifiedName.packageName

    val mappedClasses = classNameChain.zip(asMappedQualifiedName.shortName.components).map { (oldName, mappedName) ->
        SimpleClassGenericType(name = mappedName, typeArguments = oldName.typeArguments?.map { it.remap(mapper) })
    }

    return ClassGenericType(mappedPackage, mappedClasses)
}

private fun TypeArgument.remap(mapper: (className: QualifiedName) -> QualifiedName?): TypeArgument = when(this){
    is TypeArgument.SpecificType -> copy(type = type.remap(mapper))
    TypeArgument.AnyType -> TypeArgument.AnyType
}

//@Suppress("UNCHECKED_CAST")
//fun <T : GenericJavaType> T.remap(mapper: (className: QualifiedName) -> QualifiedName?): T = when (this) {
//    is JavaType.Generic -> with(declaration) {
//        copy(declaration = copy(upperBounds = upperBounds.remap(mapper), annotations = annotations.remap(mapper)))
//    }
//    is Normal -> Normal(rawType.remap(mapper), typeArguments.remap(mapper), annotations.remap(mapper))
//    is SuperType -> SuperType(rawType.remap(mapper), typeArguments.remap(mapper), annotations.remap(mapper))
//    is GenericJavaType.Wildcard -> copy(bound = bound.remap(mapper), annotations = annotations.remap(mapper))
//    else -> error("impossible")
//} as T
//typealias SuperType = SuperType
//
//fun <T : GenericJavaType> Iterable<T>.remap(mapper: (className: QualifiedName) -> QualifiedName?) = map { it.remap(mapper) }
