package codegeneration


import addIf
import com.squareup.javapoet.*
import descriptor.AnyType
import descriptor.ObjectType
import descriptor.ReturnDescriptor
import java.nio.file.Path
import javax.lang.model.element.Modifier

@DslMarker
annotation class CodeGeneratorDsl

@CodeGeneratorDsl
object JavaCodeGenerator /*: CodeGenerator */ {
    //MethodSpec main = MethodSpec.methodBuilder("main")
//    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//    .returns(void.class)
//    .addParameter(String[].class, "args")
//    .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
//    .build();
//
//TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
//    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
//    .addMethod(main)
//    .build();
//
//JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
//    .build();
//
//javaFile.writeTo(System.out);

//    private fun <T : JavaGeneratedClass> writeClass(
//        packageName: String,
//        name: String,
//        writeTo: Path,
//        visibility: Visibility,
//        init: T.() -> Unit,
//        generatedClass: (TypeSpec.Builder) -> JavaGeneratedClass
//    ) {
//        JavaFile.builder(
//            packageName,
//            generatedClass(TypeSpec.classBuilder(name).apply { addModifiers(visibility.toModifier()) }).apply(init)
//                .build()
//        ).build().writeTo(writeTo)
//    }


    /*override*/ fun writeClass(
        packageName: String,
        name: String,
        writeTo: Path,
        isInterface: Boolean,
        /**
         * Interfaces are NOT considered abstract
         */
        isAbstract: Boolean,
        visibility: Visibility,
        init: JavaGeneratedClass.() -> Unit
    ) {
        val builder = if (isInterface) TypeSpec.interfaceBuilder(name) else TypeSpec.classBuilder(name)
        builder.apply {
            visibility.toModifier()?.let { addModifiers(it) }
            if (isAbstract) addModifiers(Modifier.ABSTRACT)
        }
        JavaFile.builder(
            packageName,
            JavaGeneratedClass(builder, isInterface).apply(init)
                .build()
        ).skipJavaLangImports(true).build().writeTo(writeTo)
    }

//    /*override*/ fun writeInterface(
//        packageName: String,
//        name: String,
//        writeTo: Path,
//        visibility: Visibility = Visibility.Public,
//        init: JavaGeneratedInterface.() -> Unit
//    ) = writeClass(packageName, name, writeTo, visibility, init, ::JavaGeneratedInterface)

}

//TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
//    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
//    .addMethod(main)
//    .build();
@CodeGeneratorDsl
open class JavaGeneratedClass(
    private val typeSpec: TypeSpec.Builder,
    private val isInterface: Boolean
) /*: GeneratedClass*/ {
    /*override*/ fun addMethod(
        name: String,
        returnType: ReturnDescriptor?,
        visibility: Visibility,
        abstract: Boolean,
        static: Boolean,
        final: Boolean,
        parameters: List<Pair<String, AnyType>>,
        body: JavaGeneratedMethod.() -> Unit
    ) {
        addMethodImpl(visibility, parameters, MethodSpec.methodBuilder(name), internalConfig = {
            if (returnType != null) returns(returnType.toTypeName())

            if (abstract) addModifiers(Modifier.ABSTRACT)
            else if (static) addModifiers(Modifier.STATIC)
            else if (isInterface) addModifiers(Modifier.DEFAULT)

            if (final) addModifiers(Modifier.FINAL)
        }, userConfig = body)

    }

    fun addConstructor(
        visibility: Visibility,
        parameters: List<Pair<String, AnyType>>,
        init: JavaGeneratedMethod.() -> Unit
    ) {
        require(!isInterface) { "Interfaces don't have constructors" }
        addMethodImpl(visibility, parameters, MethodSpec.constructorBuilder(), internalConfig = {}, userConfig = init)
    }

    private fun addMethodImpl(
        visibility: Visibility,
        parameters: List<Pair<String, AnyType>>,
        builder: MethodSpec.Builder,
        internalConfig: MethodSpec.Builder.() -> Unit,
        userConfig: JavaGeneratedMethod.() -> Unit
    ) {
        typeSpec.addMethod(
            JavaGeneratedMethod(builder
                .apply {
                    addParameters(parameters.map { (name, type) ->
                        ParameterSpec.builder(type.toTypeName(), name).build()
                    })
                    visibility.toModifier()?.let { addModifiers(it) }
                    internalConfig()
                }).apply(userConfig)
                .build()
        )
    }

//    private fun Boolean.staticModifier() = if(this) Modifier.STATIC

    /*override*/  fun addField(
        name: String,
        type: AnyType,
        visibility: Visibility,
        static: Boolean,
        final: Boolean,
        init: JavaGeneratedField.() -> Unit
    ) {
        typeSpec.addField(
            JavaGeneratedField(FieldSpec.builder(type.toTypeName(), name)
                .apply {
                    visibility.toModifier()?.let { addModifiers(it) }
                    if (static) addModifiers(Modifier.STATIC)
                    if (final) addModifiers(Modifier.FINAL)
                }).apply(init)
                .build()
        )
    }

    fun setSuperclass(type: ObjectType) {
        typeSpec.superclass(type.toTypeName())
    }

    fun build(): TypeSpec = typeSpec.build()


}


@CodeGeneratorDsl
class JavaGeneratedMethod(private val methodSpec: MethodSpec.Builder) {
//    fun addStatement(format: String, vararg typeArgs: AnyType) {
//        methodSpec.addStatement(format, *typeArgs.map { it.toTypeName() }.toTypedArray())
//    }

    fun addStatement(statement: Statement) {
        val (format, arguments) = JavaCodeWriter().write(statement)
        methodSpec.addStatement(format, *arguments.toTypedArray())
    }

//    fun addFunctionCall(
//        /**
//         * receiver = null for implicit receiver
//         */
//        receiver: Statement?,
//        /**
//         * methodName = null for constructors
//         */
//        methodName: String?,
//        parameters: List<Expression>,
//        returnResult: Boolean
//    ) {
//        require(receiver != null || methodName != null)
//        val allArgs = parameters.flatMap { it.toJavaCode().formatArguments }.toMutableList()
//
//        val receiverPart = if (receiver != null) {
//            val (receiverString, receiverArgs) = receiver.toJavaCode()
//            allArgs.addAll(receiverArgs)
//            receiverString
//        } else ""
//
//        val receiverMethodNameSeparator = ".".addIf(receiver != null && methodName != null)
//
//        val string = "return ".addIf(returnResult) +
//                receiverPart + receiverMethodNameSeparator + (methodName ?: "") + "(" +
//                parameters.joinToString(", ") { it.toJavaCode().string } + ")"
//        methodSpec.addStatement(string, *allArgs.toTypedArray())
//    }
//
//    // i.e. this() or super()
//    fun addSelfConstructorCall(type: SelfConstructorType, parameters: List<Expression>) {
//        val allArgs = parameters.flatMap { it.toJavaCode().formatArguments }
//        val string = type.toJavaCode() + "(" + parameters.joinToString(", ") { it.toJavaCode().string } + ")"
//        methodSpec.addStatement(string, *allArgs.toTypedArray())
//    }

    fun addComment(comment: String) {
        methodSpec.addComment(comment)
    }

    fun build(): MethodSpec = methodSpec.build()
}


@CodeGeneratorDsl
class JavaGeneratedField(private val fieldSpec: FieldSpec.Builder) {
    fun setInitializer(value: Expression) {
        val (format, arguments) = JavaCodeWriter().write(value)
        fieldSpec.initializer(format, *arguments.toTypedArray())
    }


    fun build(): FieldSpec = fieldSpec.build()
}

//private data class FormattedString(val string: String, val formatArguments: List<TypeName>) {
//    fun mapString(map: (String) -> String) = copy(string = map(string))
//    fun addArg(arg: TypeName) = copy(formatArguments = formatArguments + arg)
//}
//
//private val String.format get() = FormattedString(this, listOf())
//private fun String.format(args: List<TypeName>) = FormattedString(this, args)
//private fun String.format(arg: TypeName) = FormattedString(this, listOf(arg))
//
//private fun SelfConstructorType.toJavaCode() = when (this) {
//    SelfConstructorType.This -> "this"
//    SelfConstructorType.Super -> "super"
//}
//
//private const val TYPE_FORMAT = "\$T"
//
////TODO: make code more reusable
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
//
//private fun Expression.Call.prefix(): FormattedString = when (this) {
//    is Expression.Call.This -> "this".format
//    is Expression.Call.Super -> "super".format
//    is Expression.Call.Method -> receiver?.toJavaCode()?.mapString { "$it.$name" } ?: name.format
//    is Expression.Call.Constructor -> TYPE_FORMAT.format(constructing.toTypeName())
//}
