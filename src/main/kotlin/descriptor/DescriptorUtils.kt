package descriptor

import util.QualifiedName
import util.toQualifiedName

@Suppress("UNCHECKED_CAST")
fun <T : Descriptor> T.remap(mapper: (className: QualifiedName) -> QualifiedName?): T = when (this) {
    is JvmPrimitiveType, ReturnDescriptor.Void -> this
    is ObjectType -> this.copy(mapper(fullClassName) ?: fullClassName)
    is ArrayType -> this.copy(componentType.remap(mapper))
    is MethodDescriptor -> this.copy(parameterDescriptors.remap(mapper), returnDescriptor.remap(mapper))
    else -> error("Impossible")
} as T

fun <T : Descriptor> Iterable<T>.remap(mapper: (className: QualifiedName) -> QualifiedName?) = map { it.remap(mapper) }



const val JavaLangObjectString = "java/lang/Object"
val JavaLangObjectName = JavaLangObjectString.toQualifiedName(dotQualified = false)
val JavaLangObjectJvmType = ObjectType(JavaLangObjectName)
