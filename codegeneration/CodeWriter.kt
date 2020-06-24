package metautils.codegeneration

import codegeneration.*
import metautils.api.JavaAnnotation
import metautils.api.JavaType
import com.squareup.javapoet.*
import metautils.descriptor.JvmPrimitiveType
import metautils.descriptor.JvmType
import metautils.descriptor.ObjectType
import metautils.signature.*
import metautils.util.prependIfNotNull

internal sealed class CodeWriter {
    /**
     * Note: This is a pure operation, I just thing "CodeWriter" is the most fitting name.
     */
    abstract fun write(code: Code): FormattedString
}


//internal object KotlinCodeWriter : JavaCodeWriter() {
//    override fun write(code: Code): FormattedString  = when(code){
//        is CastExpression -> "".format
//        else -> super.write(code)
//    }
//}

internal open class JavaCodeWriter : CodeWriter() {
    private fun write(expression: Expression) = write(expression.code)
    private fun write(rec: Receiver) = write(rec.code)
    override fun write(code: Code): FormattedString = when (code) {
        is VariableExpression -> code.name.format
        is CastExpression -> write(code.target).mapString { "($TYPE_FORMAT)$it" }
            .prependArg(code.castTo)
        is FieldExpression -> write(code.receiver).mapString { "${it.withParentheses()}.${code.name}" }
        ThisExpression -> "this".format
        is MethodCall -> {
            val (prefixStr, prefixArgs) = code.prefix()
            code.parameters.toParameterList().mapString { "$prefixStr$it" }.prependArgs(prefixArgs)
        }
        is ReturnStatement -> write(code.target).mapString { "return $it" }
        is ClassReceiver -> TYPE_FORMAT.formatType(code.type.toRawJavaType())
        SuperReceiver -> "super".format
        is AssignmentStatement -> write(code.target as Code).mapString { "$it = " } + write(code.assignedValue)
        is ArrayConstructor -> write(code.size).mapString { "new $TYPE_FORMAT[$it]" }
            .prependArg(code.componentClass)
//        is ConstructorCall.This -> code.parameters.toParameterList().mapString { "this$it" }
        is ConstructorCall.Super -> code.parameters.toParameterList().mapString { "super$it" }
    }

    private fun List<Pair<JvmType, Expression>>.toParameterList(): FormattedString {
        val parametersCode = map { write(it.second) }
        val totalArgs = parametersCode.flatMap { it.formatArguments }
        return ("(" + parametersCode.joinToString(", ") { it.string } + ")").formatType(totalArgs)
    }

    // Add parentheses to casts and constructor calls
    private fun String.withParentheses() =
        if ((startsWith("(") || startsWith("new")) && !startsWith(")")) "($this)" else this
//    private fun String.removeParentheses() = if (startsWith("(")) substring(1, length - 1) else this

    private fun MethodCall.prefix(): FormattedString = when (this) {
        is MethodCall.Method -> if (receiver == null) name.format else write(receiver as Code)
            .mapString { "${it.withParentheses()}.$name" }
        is MethodCall.Constructor -> {
            val rightSide = "new $TYPE_FORMAT".formatType(constructing)
            if (receiver == null) rightSide
            else write(receiver).mapString { "${it.withParentheses()}." } + rightSide
        }
    }

}


private fun JvmPrimitiveType.toTypeName(): TypeName = when (this) {
    JvmPrimitiveType.Byte -> TypeName.BYTE
    JvmPrimitiveType.Char -> TypeName.CHAR
    JvmPrimitiveType.Double -> TypeName.DOUBLE
    JvmPrimitiveType.Float -> TypeName.FLOAT
    JvmPrimitiveType.Int -> TypeName.INT
    JvmPrimitiveType.Long -> TypeName.LONG
    JvmPrimitiveType.Short -> TypeName.SHORT
    JvmPrimitiveType.Boolean -> TypeName.BOOLEAN
}

fun TypeArgumentDeclaration.toTypeName(): TypeVariableName = TypeVariableName.get(
    name,
    *interfaceBounds.prependIfNotNull(classBound).map { it.toTypeName() }.toTypedArray()
)

fun JavaType<*>.toTypeName(): TypeName {
    //TODO: annotations
    return type.toTypeName()
}

fun GenericReturnType.toTypeName(): TypeName = when (this) {
    is GenericsPrimitiveType -> primitive.toTypeName()
    is ClassGenericType -> toTypeName()
    is TypeVariable -> TypeVariableName.get(name)
    is ArrayGenericType -> ArrayTypeName.of(componentType.toTypeName())
    GenericReturnType.Void -> TypeName.VOID
}

private fun ClassGenericType.toTypeName(): TypeName {
    val outerClass = classNameSegments[0]
    val outerClassArgs = outerClass.typeArguments
    val innerClasses = classNameSegments.drop(1)
//    require() {
//        "Inner class type arguments cannot be translated to a JavaPoet representation"
//    }

    val isGenericType = classNameSegments.any { it.typeArguments != null }

    val outerRawType = ClassName.get(
        packageName?.toDotQualified() ?: "",
        classNameSegments[0].name,
        *(if (isGenericType) arrayOf() else innerClasses.map { it.name }.toTypedArray())
    )

    return if (!isGenericType) outerRawType else {
        if (outerClassArgs == null) {
            when (innerClasses.size) {
                0 -> outerRawType
                1 -> ParameterizedTypeName.get(
                    outerRawType.nestedClass(innerClasses[0].name),
                    *innerClasses[0].typeArguments.toTypeName().toTypedArray()
                )
                // This would require pretty complicated handling in the general case, thanks jake wharton
                else -> error("2-deep inner classes with a type argument only in a nested class are not expected")
            }

        } else {
            innerClasses.fold(
                ParameterizedTypeName.get(
                    outerRawType,
                    *outerClassArgs.toTypeName().toTypedArray()
                )
            ) { acc, classSegment ->
                acc.nestedClass(classSegment.name, classSegment.typeArguments.toTypeName())
            }
        }
    }
}

private fun List<TypeArgument>?.toTypeName() = this?.map { it.toTypeName() } ?: listOf()

private fun TypeArgument.toTypeName(): TypeName = when (this) {
    is TypeArgument.SpecificType -> {
        val bound = type.toTypeName()
        when (wildcardType) {
            WildcardType.Extends -> WildcardTypeName.subtypeOf(bound)
            WildcardType.Super -> WildcardTypeName.supertypeOf(bound)
            null -> bound
        }
    }
    TypeArgument.AnyType -> WildcardTypeName.subtypeOf(TypeName.OBJECT)
}

fun JavaAnnotation.toAnnotationSpec(): AnnotationSpec = AnnotationSpec.builder(type.toTypeName()).build()

private fun ObjectType.toTypeName(): ClassName {
    val shortName = fullClassName.shortName
    return ClassName.get(
        fullClassName.packageName?.toDotQualified() ?: "", shortName.outerMostClass(),
        *shortName.innerClasses().toTypedArray()
    )
}

//TODO: replace with normal string?
internal data class FormattedString(val string: String, val formatArguments: List<JavaType<*>>) {
    fun mapString(map: (String) -> String) = copy(string = map(string))
    fun appendArg(arg: JavaType<*>) = copy(formatArguments = formatArguments + arg)
    fun prependArg(arg: JavaType<*>) = copy(formatArguments = listOf(arg) + formatArguments)
    fun prependArgs(args: List<JavaType<*>>) = copy(formatArguments = args + formatArguments)

    operator fun plus(other: FormattedString) =
        FormattedString(this.string + other.string, formatArguments + other.formatArguments)
}


private val String.format get() = FormattedString(this, listOf())
private fun String.formatType(args: List<JavaType<*>>) = FormattedString(this, args)
private fun String.formatType(arg: JavaType<*>) = FormattedString(this, listOf(arg))

private const val TYPE_FORMAT = "\$T"

