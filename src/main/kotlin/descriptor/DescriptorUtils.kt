package descriptor

fun <T : Descriptor> T.remap(mapper : (className: String) -> String?): T = when (this) {
    is PrimitiveType, ReturnDescriptor.Void -> this
    is ObjectType -> this.copy(mapper(className) ?: className)
    is ArrayType -> this.copy(componentType.remap(mapper))
    is MethodDescriptor -> this.copy(parameterDescriptors.map { it.remap(mapper) }, returnDescriptor.remap(mapper))
    else -> error("Impossible")
} as T

const val JavaLangObject = "java/lang/Object"
val JavaLangObjectType = ObjectType(JavaLangObject)
