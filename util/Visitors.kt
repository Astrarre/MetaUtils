package metautils.util

import metautils.api.AnnotationValue
import metautils.api.JavaAnnotation
import metautils.api.JavaType
import metautils.descriptor.*
import metautils.signature.*

fun MethodSignature.visitNames(visitor: (QualifiedName) -> Unit) {
    typeArguments?.forEach { it.visitNames(visitor) }
    parameterTypes.forEach { it.visitNames(visitor) }
    returnType.visitNames(visitor)
    throwsSignatures.forEach { it.visitNames(visitor) }
}

fun ClassSignature.visitNames(visitor: (QualifiedName) -> Unit) {
    typeArguments?.forEach { it.visitNames(visitor) }
    superClass.visitNames(visitor)
    superInterfaces.forEach { it.visitNames(visitor) }
}

fun TypeArgumentDeclaration.visitNames(visitor: (QualifiedName) -> Unit) {
    classBound?.visitNames(visitor)
    interfaceBounds.forEach { it.visitNames(visitor) }
}

fun GenericReturnType.visitNames(visitor: (QualifiedName) -> Unit): Unit = when (this) {
    is GenericsPrimitiveType, GenericReturnType.Void -> {
    }
    is GenericType -> visitNames(visitor)
}

fun GenericType.visitNames(visitor: (QualifiedName) -> Unit) = when (this) {
    is ClassGenericType -> visitNames(visitor)
    is TypeVariable -> {
    }
    is ArrayGenericType -> componentType.visitNames(visitor)
}

fun ClassGenericType.visitNames(visitor: (QualifiedName) -> Unit) {
    visitor(toJvmQualifiedName())
    classNameSegments.forEach { segment -> segment.typeArguments?.forEach { it.visitNames(visitor) } }
}

fun JavaType<*>.visitNames(visitor: (QualifiedName) -> Unit) {
    type.visitNames(visitor)
    annotations.forEach { it.visitNames(visitor) }
}

fun JavaType<*>.getContainedNamesRecursively(): List<QualifiedName> =
    mutableListOf<QualifiedName>().apply { visitNames { add(it) } }

fun TypeArgument.visitNames(visitor: (QualifiedName) -> Unit) {
    if (this is TypeArgument.SpecificType) {
        type.visitNames(visitor)
    }
}

fun MethodDescriptor.visitNames(visitor: (QualifiedName) -> Unit) {
    parameterDescriptors.forEach { it.visitNames(visitor) }
    returnDescriptor.visitNames(visitor)
}

fun ReturnDescriptor.visitNames(visitor: (QualifiedName) -> Unit): Unit = when (this) {
    is ObjectType -> visitor(fullClassName)
    is ArrayType -> componentType.visitNames(visitor)
    ReturnDescriptor.Void, is JvmPrimitiveType -> {
    }
}

fun JavaAnnotation.visitNames(visitor: (QualifiedName) -> Unit) {
    type.visitNames(visitor)
    parameters.values.forEach { it.visitNames(visitor) }
}

fun AnnotationValue.visitNames(visitor: (QualifiedName) -> Unit): Unit = when (this) {
    is AnnotationValue.Array -> components.forEach { it.visitNames(visitor) }
    is AnnotationValue.Annotation -> annotation.visitNames(visitor)
    is AnnotationValue.Primitive -> {
    }
    is AnnotationValue.Enum -> visitor(type.fullClassName)
    is AnnotationValue.ClassType -> type.visitNames(visitor)
}