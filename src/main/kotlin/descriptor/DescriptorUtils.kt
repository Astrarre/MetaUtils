package descriptor

import QualifiedName
import toQualifiedName

@Suppress("UNCHECKED_CAST")
fun <T : Descriptor> T.remap(mapper : (className: QualifiedName) -> QualifiedName?): T = when (this) {
    is PrimitiveType, ReturnDescriptor.Void -> this
    is ObjectType -> this.copy(mapper(fullClassName) ?: fullClassName)
    is ArrayType -> this.copy(componentType.remap(mapper))
    is MethodDescriptor -> this.copy(parameterDescriptors.map { it.remap(mapper) }, returnDescriptor.remap(mapper))
    else -> error("Impossible")
} as T
fun <T : Descriptor> Iterable<T>.remap(mapper: (className: QualifiedName) -> QualifiedName?) = map { it.remap(mapper) }

val JavaLangObjectString = "java/lang/Object"
val JavaLangObject = JavaLangObjectString.toQualifiedName(dotQualified = false)
val JavaLangObjectType = ObjectType(JavaLangObject)
