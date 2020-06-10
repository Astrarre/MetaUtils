@file:Suppress("UNCHECKED_CAST")

package signature

import QualifiedName
import ShortClassName
import api.JavaClassType
import api.JavaType
import descriptor.*
import toQualifiedName

//val ClassSignature.superTypes get() = superInterfaces + superClass


fun <T : GenericReturnType> T.remap(mapper: (className: QualifiedName) -> QualifiedName?): T = when (this) {
    is GenericsPrimitiveType -> copy(primitive.remap(mapper))
    is ClassGenericType -> remap(mapper)
    is TypeVariable -> this
    is ArrayGenericType -> copy(componentType.remap(mapper))
    GenericReturnType.Void -> GenericReturnType.Void
    else -> error("impossible")
} as T


@OptIn(ExperimentalStdlibApi::class)
fun GenericReturnType.getContainedClassesRecursively(): List<ClassGenericType> =
    buildList { visitContainedClasses { add(it) } }

fun GenericReturnType.visitContainedClasses(visitor: (ClassGenericType) -> Unit): Unit = when (this) {
    is GenericsPrimitiveType, is TypeVariable, GenericReturnType.Void -> {
    }
    is ClassGenericType -> {
        visitor(this)
        for (className in classNameChain) {
            className.typeArguments?.forEach {
                if (it is TypeArgument.SpecificType) it.type.visitContainedClasses(visitor)
            }
        }
    }
    is ArrayGenericType -> componentType.visitContainedClasses(visitor)
}

private fun ClassGenericType.remap(mapper: (className: QualifiedName) -> QualifiedName?): ClassGenericType {
    val asQualifiedName = QualifiedName(packageName, ShortClassName(classNameChain.map { it.name }))
    val asMappedQualifiedName = mapper(asQualifiedName) ?: asQualifiedName
    val mappedPackage = asMappedQualifiedName.packageName

    val mappedClasses = classNameChain.zip(asMappedQualifiedName.shortName.components).map { (oldName, mappedName) ->
        SimpleClassGenericType(name = mappedName, typeArguments = oldName.typeArguments?.map { it.remap(mapper) })
    }

    return ClassGenericType(mappedPackage, mappedClasses)
}

private fun TypeArgument.remap(mapper: (className: QualifiedName) -> QualifiedName?): TypeArgument = when (this) {
    is TypeArgument.SpecificType -> copy(type = type.remap(mapper))
    TypeArgument.AnyType -> TypeArgument.AnyType
}

fun ClassGenericType.Companion.fromRawClassString(string: String, dotQualified: Boolean = false): ClassGenericType {
    return string.toQualifiedName(dotQualified).toRawGenericType()
}

fun ClassGenericType.toJvmQualifiedName() = QualifiedName(
    packageName,
    ShortClassName(classNameChain.map { it.name })
)

fun QualifiedName.toRawGenericType(): ClassGenericType = ClassGenericType(packageName,
    shortName.components.map { SimpleClassGenericType(it, null) }
)

fun <T : GenericReturnType> T.noAnnotations(): JavaType<T> = JavaType(this, listOf())

fun GenericTypeOrPrimitive.toJvmType(): JvmType = when (this) {
    is GenericsPrimitiveType -> primitive
    is ClassGenericType -> ObjectType(toJvmQualifiedName())
    is TypeVariable -> JavaLangObjectJvmType
    is ArrayGenericType -> ArrayType(componentType.toJvmType())
}

fun GenericReturnType.toJvmType(): ReturnDescriptor = when (this) {
    is GenericTypeOrPrimitive -> toJvmType()
    GenericReturnType.Void -> ReturnDescriptor.Void
}

fun MethodSignature.toJvmDescriptor() = MethodDescriptor(
    parameterDescriptors = parameterTypes.map { it.toJvmType() },
    returnDescriptor = returnType.toJvmType()
)

fun JavaType<*>.toJvmType() = type.toJvmType()

fun ObjectType.toRawGenericType(): ClassGenericType = fullClassName.toRawGenericType()
fun ObjectType.toRawJavaType(): JavaClassType = JavaClassType(fullClassName.toRawGenericType(), annotations = listOf())
fun FieldType.toRawGenericType(): GenericTypeOrPrimitive = when (this) {
    is PrimitiveType -> GenericsPrimitiveType(this)
    is ObjectType -> toRawGenericType()
    is ArrayType -> ArrayGenericType(componentType.toRawGenericType())
}


fun ReturnDescriptor.toRawGenericType(): GenericReturnType = when (this) {
    is FieldType -> toRawGenericType()
    ReturnDescriptor.Void -> GenericReturnType.Void
}