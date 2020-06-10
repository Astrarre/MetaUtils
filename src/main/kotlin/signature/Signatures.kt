package signature

import PackageName
import descriptor.PrimitiveType
import includeIf

//TODO rename everything to make sense

data class ClassSignature(
    val typeArguments: List<TypeArgumentDeclaration>?,
    val superClass: ClassGenericType,
    val superInterfaces: List<ClassGenericType>
) {
    companion object;
    override fun toString(): String = "<${typeArguments?.joinToString(", ")}> ".includeIf(typeArguments != null) +
            "(extends $superClass" + ", implements ".includeIf(superInterfaces.isNotEmpty()) +
            superInterfaces.joinToString(", ") + ")"
}

data class MethodSignature(
    val typeArguments: List<TypeArgumentDeclaration>?,
    val parameterTypes: List<GenericTypeOrPrimitive>,
    val returnType: ReturnType,
    val throwsSignatures: List<ThrowableType>
) {
    companion object;
    override fun toString(): String = "<${typeArguments?.joinToString(", ")}> ".includeIf(typeArguments != null) +
            "(${parameterTypes.joinToString(", ")}): $returnType" +
            " throws ".includeIf(throwsSignatures.isNotEmpty()) + throwsSignatures.joinToString(", ")
}

typealias FieldSignature = GenericType

sealed class TypeArgument {
    data class SpecificType(val type: GenericType, val wildcardType: WildcardType?) : TypeArgument() {
        override fun toString(): String = "? $wildcardType ".includeIf(wildcardType != null) + type
    }

    object AnyType : TypeArgument() {
        override fun toString(): String = "*"
    }
}

enum class WildcardType {
    Extends, Super;

    override fun toString(): String = when (this) {
        Extends -> "extends"
        Super -> "super"
    }
}

sealed class ReturnType {
    object Void : ReturnType() {
        override fun toString(): String = "void"
    }
}

sealed class GenericTypeOrPrimitive : ReturnType()

data class PrimitiveTypeForGenerics(val primitive: PrimitiveType) : GenericTypeOrPrimitive() {
    override fun toString(): String = primitive.toString()
}

sealed class GenericType : GenericTypeOrPrimitive() {
    companion object
}

sealed class ThrowableType : GenericType()


data class TypeArgumentDeclaration(
    val name: String,
    val classBound: GenericType?,
    val interfaceBounds: List<GenericType>
) {
    override fun toString(): String = name + " extends $classBound".includeIf(classBound != null) +
            " implements ".includeIf(interfaceBounds.isNotEmpty()) + interfaceBounds.joinToString(", ")
}


data class ClassGenericType(
    val packageName: PackageName?,
    /**
     * Outer class and then inner classes
     */
    val classNameChain: List<SimpleClassGenericType>
) : ThrowableType() {
    init {
        check(classNameChain.isNotEmpty())
    }

    companion object;
    override fun toString(): String = "${packageName?.toSlashQualified()}/".includeIf(packageName != null) +
            classNameChain.joinToString("$")
}


data class SimpleClassGenericType(val name: String, val typeArguments: List<TypeArgument>?) {
    init {
        if (typeArguments != null) require(typeArguments.isNotEmpty())
    }

    override fun toString(): String =
        if (typeArguments != null) "$name<${typeArguments.joinToString(", ")}>" else name
}


data class ArrayGenericType(val type: GenericTypeOrPrimitive) : GenericType() {
    override fun toString(): String = "$type[]"
}

/**
 * i.e. T, U
 */
data class TypeVariable(val name: String) : ThrowableType() {
    override fun toString(): String = name
}

