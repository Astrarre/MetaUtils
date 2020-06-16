package descriptor

import util.QualifiedName
import util.toQualifiedName

// Comes directly from the spec https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.2
typealias FieldDescriptor = FieldType
typealias JvmType = FieldType


sealed class Descriptor(val classFileName: String) {
    override fun equals(other: Any?) = other is Descriptor && other.classFileName == classFileName
    override fun hashCode() = classFileName.hashCode()
}


sealed class ReturnDescriptor(classFileName: String) : Descriptor(classFileName) {
    object Void : ReturnDescriptor("V") {
        override fun toString() = "void"
    }
}

sealed class FieldType(classFileName: String) : ReturnDescriptor(classFileName) {
    companion object

}

sealed class JvmPrimitiveType(classFileName: String) : FieldType(classFileName) {
    object Byte : JvmPrimitiveType("B") {
        override fun toString() = "byte"
    }

    object Char : JvmPrimitiveType("C") {
        override fun toString() = "char"
    }

    object Double : JvmPrimitiveType("D") {
        override fun toString() = "double"
    }

    object Float : JvmPrimitiveType("F") {
        override fun toString() = "float"
    }

    object Int : JvmPrimitiveType("I") {
        override fun toString() = "int"
    }

    object Long : JvmPrimitiveType("J") {
        override fun toString() = "long"
    }

    object Short : JvmPrimitiveType("S") {
        override fun toString() = "short"
    }

    object Boolean : JvmPrimitiveType("Z") {
        override fun toString() = "boolean"
    }
}


data class ObjectType(val fullClassName: QualifiedName) :
    FieldType("L${fullClassName.toSlashQualifiedString()};") {
    override fun toString() = fullClassName.shortName.toDollarQualifiedString()

    constructor(qualifiedName: String, dotQualified : Boolean) : this(qualifiedName.toQualifiedName(dotQualified))

//    companion object {
//        fun dotQualified(className: String) = ObjectType(className.replace(".", "/"))
//    }
}

//fun ObjectType.packageName() = className.substring(0, className.lastIndexOf("/").let { if (it == -1) 0 else it })
//    .replace("/", ".")
//
//fun ObjectType.simpleName() = className.substring(className.lastIndexOf("/").let { if (it == -1) 0 else it + 1 })

data class ArrayType(val componentType: FieldType) : FieldType("[" + componentType.classFileName) {
    override fun toString() = "$componentType[]"
}

typealias ParameterDescriptor = FieldType

data class MethodDescriptor(
    val parameterDescriptors: List<ParameterDescriptor>,
    val returnDescriptor: ReturnDescriptor
) : Descriptor("(${parameterDescriptors.joinToString("") { it.classFileName }})${returnDescriptor.classFileName}") {
    companion object;
    override fun toString() = "(${parameterDescriptors.joinToString(", ")}): $returnDescriptor"
}