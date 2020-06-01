package codegeneration

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import descriptor.*

internal sealed class CodeWriter {
    /**
     * Note: This is a pure operation, I just thing "CodeWriter" is the most fitting name.
     */
    abstract fun write(code: Code): FormattedString
}


//internal object KotlinCodeWriter : JavaCodeWriter() {
//    override fun write(code: Code): FormattedString  = when(code){
//        is Expression.Cast -> "".format
//        else -> super.write(code)
//    }
//}

internal open class JavaCodeWriter : CodeWriter() {
    override fun write(code: Code): FormattedString = when (code) {
        is Expression.Variable -> code.name.format
        is Expression.Cast -> write(code.target).mapString { "($TYPE_FORMAT)$it" }
            .prependArg(code.castTo.toTypeName())
        is Expression.Field -> write(code.owner).mapString { "${it.withParentheses()}.${code.name}" }
        Expression.This -> "this".format
        is Expression.Call -> {
            val (prefixStr, prefixArgs) = code.prefix()
            val parametersCode = code.parameters.map { write(it) }
            val totalArgs = prefixArgs + parametersCode.flatMap { it.formatArguments }
            (prefixStr + "(" + parametersCode.joinToString(", ") { it.string } + ")").format(totalArgs)
        }
        is Statement.Return -> write(code.target).mapString { "return $it" }
        is ClassReceiver -> TYPE_FORMAT.format(code.type.toTypeName())
        SuperReceiver -> "super".format
    }

    // Add parentheses to casts and constructor calls
    private fun String.withParentheses() = if ((startsWith("(") || startsWith("new")) && !startsWith(")")) "($this)" else this
//    private fun String.removeParentheses() = if (startsWith("(")) substring(1, length - 1) else this

    private fun Expression.Call.prefix(): FormattedString = when (this) {
        is Expression.Call.This -> "this".format
        is Expression.Call.Super -> "super".format
        is Expression.Call.Method -> if (receiver == null) name.format else write(receiver as Code).mapString { "${it.withParentheses()}.$name" }
        is Expression.Call.Constructor -> "new $TYPE_FORMAT".format(constructing.toTypeName())
    }

}

private fun ObjectType.toClassName(): ClassName {
    val className = simpleName()
    val innerClassSplit = className.split("\$")
    val rootClass = innerClassSplit[0]
    val innerClasses = innerClassSplit.drop(1)
    return ClassName.get(packageName(), rootClass, *innerClasses.toTypedArray())
}

private fun AnyType.toTypeName(): TypeName = when (this) {
    PrimitiveType.Byte -> TypeName.BYTE
    PrimitiveType.Char -> TypeName.CHAR
    PrimitiveType.Double -> TypeName.DOUBLE
    PrimitiveType.Float -> TypeName.FLOAT
    PrimitiveType.Int -> TypeName.INT
    PrimitiveType.Long -> TypeName.LONG
    PrimitiveType.Short -> TypeName.SHORT
    PrimitiveType.Boolean -> TypeName.BOOLEAN
    is ObjectType -> this.toClassName()
    is ArrayType -> com.squareup.javapoet.ArrayTypeName.of(componentType.toTypeName())
}

internal data class FormattedString(val string: String, val formatArguments: List<TypeName>) {
    fun mapString(map: (String) -> String) = copy(string = map(string))
    fun appendArg(arg: TypeName) = copy(formatArguments = formatArguments + arg)
    fun prependArg(arg: TypeName) = copy(formatArguments = listOf(arg) + formatArguments)
}

private val String.format get() = FormattedString(this, listOf())
private fun String.format(args: List<TypeName>) = FormattedString(this, args)
private fun String.format(arg: TypeName) = FormattedString(this, listOf(arg))

private fun SelfConstructorType.toJavaCode() = when (this) {
    SelfConstructorType.This -> "this"
    SelfConstructorType.Super -> "super"
}

internal fun ReturnDescriptor.toTypeName(): TypeName = when (this) {
    is AnyType -> toTypeName()
    ReturnDescriptor.Void -> TypeName.VOID
}


private const val TYPE_FORMAT = "\$T"

//
//private fun Statement.toJavaCode(): FormattedString = when (this) {
////    is Statement.Class -> FormattedString(TYPE_FORMAT, listOf(type.toTypeName()))
//    is Expression.Variable -> name.format
//    is Expression.Cast -> target.toJavaCode().mapString { "(($TYPE_FORMAT)$it)" }.addArg(castTo.toTypeName())
//    is Expression.Field -> owner.toJavaCode().mapString { "$it.$name" }
//    Expression.This -> "this".format
////    Statement.Super -> FormattedString("super", listOf())
//    is Expression.Call -> {
//        val (prefixStr, prefixArgs) = this.prefix()
//        val parametersCode = parameters.map { it.toJavaCode() }
//        val totalArgs = prefixArgs + parametersCode.flatMap { it.formatArguments }
//        (prefixStr + "(" + parametersCode.joinToString(", ") { it.string } + ")").format(totalArgs)
//    }
//    is Expression.MethodCall -> TODO()
//    is Statement.Return -> TODO()
//}
