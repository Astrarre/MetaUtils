package descriptor

import QualifiedName
import api.AnyJavaType
import api.JavaClassType
import signature.*
import toQualifiedName

@Suppress("UNCHECKED_CAST")
fun <T : Descriptor> T.remap(mapper: (className: QualifiedName) -> QualifiedName?): T = when (this) {
    is PrimitiveType, ReturnDescriptor.Void -> this
    is ObjectType -> this.copy(mapper(fullClassName) ?: fullClassName)
    is ArrayType -> this.copy(componentType.remap(mapper))
    is MethodDescriptor -> this.copy(parameterDescriptors.remap(mapper), returnDescriptor.remap(mapper))
    else -> error("Impossible")
} as T

fun <T : Descriptor> Iterable<T>.remap(mapper: (className: QualifiedName) -> QualifiedName?) = map { it.remap(mapper) }



const val JavaLangObjectString = "java/lang/Object"
val JavaLangObjectName = JavaLangObjectString.toQualifiedName(dotQualified = false)
val JavaLangObjectJvmType = ObjectType(JavaLangObjectName)
val JavaLangObjectJavaType =  AnyJavaType(JavaLangObjectJvmType.toRawGenericType(), annotations = listOf())
//val JavaLangObjectType = ObjectType(JavaLangObject)
