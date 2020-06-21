@file:Suppress("UNCHECKED_CAST")

package signature

import api.AnyJavaType
import api.JavaAnnotation
import api.JavaClassType
import api.JavaType
import descriptor.*
import util.QualifiedName
import util.ShortClassName
import util.toQualifiedName

val JavaLangObjectGenericType = JavaLangObjectJvmType.toRawGenericType()
val JavaLangObjectJavaType = AnyJavaType(JavaLangObjectGenericType, annotations = listOf())
val VoidJavaType = ReturnDescriptor.Void.toRawGenericType().noAnnotations()

fun TypeArgumentDeclaration.remap(mapper: (className: QualifiedName) -> QualifiedName?) : TypeArgumentDeclaration =
    copy(classBound = classBound?.remap(mapper), interfaceBounds = interfaceBounds.map { it.remap(mapper) })

fun <T : GenericReturnType> T.remap(mapper: (className: QualifiedName) -> QualifiedName?): T = when (this) {
    is GenericsPrimitiveType -> this
    is ClassGenericType -> remap(mapper)
    is TypeVariable -> copy(declaration = declaration.remap(mapper))
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
        for (className in classNameSegments) {
            className.typeArguments?.forEach {
                if (it is TypeArgument.SpecificType) it.type.visitContainedClasses(visitor)
            }
        }
    }
    is ArrayGenericType -> componentType.visitContainedClasses(visitor)
}


fun ClassGenericType.Companion.fromRawClassString(string: String, dotQualified: Boolean = false): ClassGenericType {
    return string.toQualifiedName(dotQualified).toRawGenericType()
}

/**
 * Will only put the type args at the INNERMOST class!
 */
fun ClassGenericType.Companion.fromNameAndTypeArgs(
    name: QualifiedName,
    typeArgs: List<TypeArgument>?
): ClassGenericType {
    val outerClassesArgs: List<List<TypeArgument>?> = (0 until (name.shortName.components.size - 1)).map { null }
    return name.toClassGenericType(outerClassesArgs + listOf(typeArgs))
}

fun ClassGenericType.toJvmQualifiedName() = QualifiedName(
    packageName,
    ShortClassName(classNameSegments.map { it.name })
)

fun ClassGenericType.toJvmString() = toJvmQualifiedName().toSlashQualifiedString()

fun <T : GenericReturnType> T.noAnnotations(): JavaType<T> = JavaType(this, listOf())
fun <T : GenericReturnType> T.annotated(annotation: JavaAnnotation): JavaType<T> = JavaType(this, listOf(annotation))

fun GenericTypeOrPrimitive.toJvmType(): JvmType = when (this) {
    is GenericsPrimitiveType -> primitive
    is ClassGenericType -> toJvmType()
    is TypeVariable -> resolveJvmType()
    is ArrayGenericType -> ArrayType(componentType.toJvmType())
}

private fun TypeVariable.resolveJvmType(): JvmType = with(declaration) {
    classBound?.toJvmType() ?: if (interfaceBounds.isNotEmpty()) interfaceBounds[0].toJvmType() else JavaLangObjectJvmType
}

fun ClassGenericType.toJvmType(): ObjectType = ObjectType(toJvmQualifiedName())

fun GenericReturnType.toJvmType(): ReturnDescriptor = when (this) {
    is GenericTypeOrPrimitive -> toJvmType()
    GenericReturnType.Void -> ReturnDescriptor.Void
}

fun MethodSignature.toJvmDescriptor() = MethodDescriptor(
    parameterDescriptors = parameterTypes.map { it.toJvmType() },
    returnDescriptor = returnType.toJvmType()
)

fun JavaType<*>.toJvmType() = type.toJvmType()
fun AnyJavaType.toJvmType() = type.toJvmType()
fun JavaClassType.toJvmType() = type.toJvmType()


fun ClassGenericType.outerClass(): ClassGenericType = copy(classNameSegments = classNameSegments.dropLast(1))
fun JavaClassType.outerClass(): JavaClassType = copy(type = type.outerClass())

fun QualifiedName.toRawGenericType(): ClassGenericType = toClassGenericType(shortName.components.map { null })

/**
 *  Each element in typeArgsChain is for an element in the inner class name chain.
 *  Each element contains the type args for each class name in the chain.
 */
fun QualifiedName.toClassGenericType(typeArgsChain: List<List<TypeArgument>?>): ClassGenericType =
    ClassGenericType(packageName,
        shortName.components.zip(typeArgsChain).map { (name, args) -> SimpleClassGenericType(name, args) }
    )

fun ObjectType.toRawGenericType(): ClassGenericType = fullClassName.toRawGenericType()
fun ObjectType.toRawJavaType(): JavaClassType = JavaClassType(fullClassName.toRawGenericType(), annotations = listOf())
fun FieldType.toRawGenericType(): GenericTypeOrPrimitive = when (this) {
    is JvmPrimitiveType -> JvmPrimitiveToGenericsPrimitive.getValue(this)
    is ObjectType -> toRawGenericType()
    is ArrayType -> ArrayGenericType(componentType.toRawGenericType())
}

private val JvmPrimitiveToGenericsPrimitive = mapOf(
    JvmPrimitiveType.Byte to GenericsPrimitiveType.Byte,
    JvmPrimitiveType.Char to GenericsPrimitiveType.Char,
    JvmPrimitiveType.Double to GenericsPrimitiveType.Double,
    JvmPrimitiveType.Float to GenericsPrimitiveType.Float,
    JvmPrimitiveType.Int to GenericsPrimitiveType.Int,
    JvmPrimitiveType.Long to GenericsPrimitiveType.Long,
    JvmPrimitiveType.Short to GenericsPrimitiveType.Short,
    JvmPrimitiveType.Boolean to GenericsPrimitiveType.Boolean
)


fun ReturnDescriptor.toRawGenericType(): GenericReturnType = when (this) {
    is FieldType -> toRawGenericType()
    ReturnDescriptor.Void -> GenericReturnType.Void
}


private fun ClassGenericType.remap(mapper: (className: QualifiedName) -> QualifiedName?): ClassGenericType {
    val asQualifiedName = QualifiedName(
        packageName,
        ShortClassName(classNameSegments.map { it.name })
    )
    val asMappedQualifiedName = mapper(asQualifiedName) ?: asQualifiedName
    val mappedPackage = asMappedQualifiedName.packageName

    val mappedClasses = classNameSegments.zip(asMappedQualifiedName.shortName.components).map { (oldName, mappedName) ->
        SimpleClassGenericType(name = mappedName, typeArguments = oldName.typeArguments?.map { it.remap(mapper) })
    }

    return ClassGenericType(mappedPackage, mappedClasses)
}

private fun TypeArgument.remap(mapper: (className: QualifiedName) -> QualifiedName?): TypeArgument = when (this) {
    is TypeArgument.SpecificType -> copy(type = type.remap(mapper))
    TypeArgument.AnyType -> TypeArgument.AnyType
}

fun List<TypeArgumentDeclaration>.toTypeArgumentsOfNames(): List<TypeArgument>? = if (isEmpty()) null else map {
    TypeArgument.SpecificType(TypeVariable(it.name, it), wildcardType = null)
}