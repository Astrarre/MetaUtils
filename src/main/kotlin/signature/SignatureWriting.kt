package signature

//fun ClassSignature.toClassfileName() = formalTypeParameters
//    ?.joinToString("") {  }
//
//private fun FieldTypeSignature.toClassfileName() = when(this){
//    is ClassTypeSignature -> TODO()
//    is TypeVariableSignature -> TODO()
//    is ArrayTypeSignature -> TODO()
//}
//
//private fun ClassTypeSignature.toClassfileName()
//        = "L" + packageSpecifier?.toSlashQualified().orEmpty() +
//        classNameChain.joinToString("") {}
//
//private fun SimpleClassTypeSignature.toClassfileName() = identifier + typeArguments
//    ?.joinToString("") {}
//
//private fun TypeArgument.toClassfileName() = when(this){
//    is TypeArgument.SpecificType -> TODO()
//    TypeArgument.Star -> "*"
//}