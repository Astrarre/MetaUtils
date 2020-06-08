package api

import descriptor.ObjectType

//data class GenericBounds(val lower: List<JavaType>, val upper: List<JavaType>, val annotations: List<JvmType>)
data class GenericDeclaration(val name: String, val upperBounds: List<JavaType>, val annotations: List<ObjectType>)

sealed class GenericJavaType {
    abstract val annotations: List<ObjectType>

    data class Wildcard(
        val bound: JavaType,
        val boundsAreUpper: Boolean,
        override val annotations: List<ObjectType>
    ) : GenericJavaType()
}

/**
 * Uses slash/separators
 */
sealed class JavaType : GenericJavaType() {

    data class Generic(val declaration: GenericDeclaration, override val annotations: List<ObjectType>) : JavaType()
    sealed class Class<T : GenericJavaType> : JavaType() {
        abstract val rawType: ObjectType
        abstract val typeArguments: List<T>

        data class Normal(
            override val rawType: ObjectType,
            override val typeArguments: List<GenericJavaType>,
            override val annotations: List<ObjectType>
        ) : Class<GenericJavaType>()

        // Super types don't accept a wildcard as a generic argument
        data class SuperType(
            override val rawType: ObjectType,
            override val typeArguments: List<JavaType>,
            override val annotations: List<ObjectType>
        ) : Class<JavaType>()
//
//        // Generic arguments can't have
//        data class GenericArg(
//            override val rawType: JvmType,
//            override val typeArguments: List<JavaType>,
//            override val annotations: List<JvmType>
//        ) : Class<JavaType>()
    }

}

