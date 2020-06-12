package signature

import util.includeIf

fun ClassSignature.toClassfileName() = typeArguments.toDeclClassfileName() +
        superClass.toClassfileName() + superInterfaces.joinToString("") { it.toClassfileName() }


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

private fun SimpleClassGenericType.toClassfileName() = name + typeArguments.toClassfileName()


private fun TypeArgument.toClassfileName() = when (this) {
    is TypeArgument.SpecificType -> wildcardType?.toClassfileName().orEmpty() + type.toClassfileName()
    TypeArgument.AnyType -> "*"
}

private fun WildcardType.toClassfileName() = when (this) {
    WildcardType.Extends -> "+"
    WildcardType.Super -> "-"
}

private fun List<TypeArgument>?.toClassfileName() = if (this == null) ""
else "<" + joinToString("") { it.toClassfileName() } + ">"