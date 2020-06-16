package signature

import util.includeIf

fun ClassSignature.toClassfileName() = typeArguments.toDeclClassfileName() +
        superClass.toClassfileName() + superInterfaces.toClassfileName()

fun MethodSignature.toClassfileName(): String =
    typeArguments.toDeclClassfileName() + "(" + parameterTypes.toClassfileName() + ")" +
            returnType.toClassfileName() + throwsSignatures.joinToString("") { "^$it" }

private fun TypeArgumentDeclaration.toClassfileName(): String = "$name:${classBound?.toClassfileName().orEmpty()}" +
        interfaceBounds.joinToString("") { ":${it.toClassfileName()}" }

private fun List<TypeArgumentDeclaration>?.toDeclClassfileName() = if (this == null) ""
else "<" + joinToString("") { it.toClassfileName() } + ">"

fun GenericReturnType.toClassfileName(): String = when (this) {
    is ClassGenericType -> "L" + packageName?.toSlashQualified().orEmpty() + "/".includeIf(packageName != null) +
            classNameSegments.joinToString("$") { it.toClassfileName() } + ";"
    is TypeVariable -> "T$name;"
    is ArrayGenericType -> ";" + componentType.toClassfileName()
    is GenericsPrimitiveType -> primitive.classFileName
    GenericReturnType.Void -> "V"
}


private fun SimpleClassGenericType.toClassfileName() = name + typeArguments.toArgClassfileName()


private fun TypeArgument.toClassfileName() = when (this) {
    is TypeArgument.SpecificType -> wildcardType?.toClassfileName().orEmpty() + type.toClassfileName()
    TypeArgument.AnyType -> "*"
}

private fun WildcardType.toClassfileName() = when (this) {
    WildcardType.Extends -> "+"
    WildcardType.Super -> "-"
}

private fun List<GenericReturnType>.toClassfileName() = joinToString("") { it.toClassfileName() }

private fun List<TypeArgument>?.toArgClassfileName() = if (this == null) ""
else "<" + joinToString("") { it.toClassfileName() } + ">"