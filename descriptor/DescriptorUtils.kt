package metautils.descriptor

import metautils.descriptor.*
import metautils.util.QualifiedName
import metautils.util.toQualifiedName

@Suppress("UNCHECKED_CAST")
fun <T : Descriptor> T.remap(mapper: (className: QualifiedName) -> QualifiedName?): T = when (this) {
    is JvmPrimitiveType, ReturnDescriptor.Void -> this
    is ObjectType -> this.copy(mapper(fullClassName) ?: fullClassName)
    is ArrayType -> this.copy(componentType.remap(mapper))
    is MethodDescriptor -> this.copy(parameterDescriptors.remap(mapper), returnDescriptor.remap(mapper))
    else -> error("Impossible")
} as T

fun <T : Descriptor> Iterable<T>.remap(mapper: (className: QualifiedName) -> QualifiedName?) = map { it.remap(mapper) }

fun ObjectType.toJvmString() = fullClassName.toSlashQualifiedString()
fun ReturnDescriptor.toJvmString() = if (this is ObjectType) toJvmString() else classFileName
fun ReturnDescriptor.isTwoBytesWide() = this == JvmPrimitiveType.Long || this == JvmPrimitiveType.Double
fun ReturnDescriptor.byteWidth() = when{
    this == ReturnDescriptor.Void -> 0
    isTwoBytesWide() -> 2
    else -> 1
}


const val JavaLangObjectString = "java/lang/Object"
val JavaLangObjectName = JavaLangObjectString.toQualifiedName(dotQualified = false)
val JavaLangObjectJvmType = ObjectType(JavaLangObjectName)

//fun JvmType.toPresentableString() = when(this){
//    is JvmPrimitiveType -> toString()
//    is ObjectType -> TODO()
//    is ArrayType -> TODO()
//}