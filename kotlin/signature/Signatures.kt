package signature

import util.PackageName
import descriptor.JvmPrimitiveType
import util.includeIf


interface Signature

data class ClassSignature(
    val typeArguments: List<TypeArgumentDeclaration>?,
    val superClass: ClassGenericType,
    val superInterfaces: List<ClassGenericType>
) : Signature {
    companion object;
    override fun toString(): String = "<${typeArguments?.joinToString(", ")}> ".includeIf(typeArguments != null) +
            "(extends $superClass" + ", implements ".includeIf(superInterfaces.isNotEmpty()) +
            superInterfaces.joinToString(", ") + ")"
}

data class MethodSignature(
    val typeArguments: List<TypeArgumentDeclaration>?,
    val parameterTypes: List<GenericTypeOrPrimitive>,
    val returnType: GenericReturnType,
    val throwsSignatures: List<ThrowableType>
) : Signature {
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

sealed class GenericReturnType {
    object Void : GenericReturnType() {
        override fun toString(): String = "void"
    }
}

sealed class GenericTypeOrPrimitive : GenericReturnType(), Signature

internal val baseTypesGenericsMap = mapOf(
    JvmPrimitiveType.Byte.classFileName to GenericsPrimitiveType.Byte,
    JvmPrimitiveType.Char.classFileName to GenericsPrimitiveType.Char,
    JvmPrimitiveType.Double.classFileName to GenericsPrimitiveType.Double,
    JvmPrimitiveType.Float.classFileName to GenericsPrimitiveType.Float,
    JvmPrimitiveType.Int.classFileName to GenericsPrimitiveType.Int,
    JvmPrimitiveType.Long.classFileName to GenericsPrimitiveType.Long,
    JvmPrimitiveType.Short.classFileName to GenericsPrimitiveType.Short,
    JvmPrimitiveType.Boolean.classFileName to GenericsPrimitiveType.Boolean
).mapKeys { it.key[0] }

class GenericsPrimitiveType private constructor(val primitive: JvmPrimitiveType) : GenericTypeOrPrimitive() {
    override fun toString(): String = primitive.toString()

    companion object {
        val Byte = GenericsPrimitiveType(JvmPrimitiveType.Byte)
        val Char = GenericsPrimitiveType(JvmPrimitiveType.Char)
        val Double = GenericsPrimitiveType(JvmPrimitiveType.Double)
        val Float = GenericsPrimitiveType(JvmPrimitiveType.Float)
        val Int = GenericsPrimitiveType(JvmPrimitiveType.Int)
        val Long = GenericsPrimitiveType(JvmPrimitiveType.Long)
        val Short = GenericsPrimitiveType(JvmPrimitiveType.Short)
        val Boolean = GenericsPrimitiveType(JvmPrimitiveType.Boolean)
    }
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
    val classNameSegments: List<SimpleClassGenericType>
) : ThrowableType() {
    init {
        check(classNameSegments.isNotEmpty())
    }

    companion object;
    override fun toString(): String = "${packageName?.toSlashQualified()}/".includeIf(packageName != null) +
            classNameSegments.joinToString("$")
}


data class SimpleClassGenericType(val name: String, val typeArguments: List<TypeArgument>?) {
    init {
        if (typeArguments != null) require(typeArguments.isNotEmpty())
    }

    override fun toString(): String =
        if (typeArguments != null) "$name<${typeArguments.joinToString(", ")}>" else name
}


data class ArrayGenericType(val componentType: GenericTypeOrPrimitive) : GenericType() {
    override fun toString(): String = "$componentType[]"
}

/**
 * i.e. T, U
 */
data class TypeVariable(val name: String) : ThrowableType() {
    override fun toString(): String = name
}

