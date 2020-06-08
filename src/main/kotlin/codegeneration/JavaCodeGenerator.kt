package codegeneration


import api.JavaType
import api.SuperType
import com.squareup.javapoet.*
import descriptor.JvmType
import descriptor.ObjectType
import descriptor.ReturnDescriptor
import java.nio.file.Path
import javax.lang.model.element.Modifier

@DslMarker
annotation class CodeGeneratorDsl

@CodeGeneratorDsl
object JavaCodeGenerator {

    fun writeClass(
        packageName: String,
        name: String,
        writeTo: Path,
        isInterface: Boolean,
        /**
         * Interfaces are NOT considered abstract
         */
        isAbstract: Boolean,
        visibility: Visibility,
        superClass : SuperType?,
        superInterfaces: List<SuperType>,
        init: JavaGeneratedClass.() -> Unit
    ) {
        val generatedClass = generateClass(isInterface, name, visibility, isAbstract,superClass, superInterfaces, init)
        JavaFile.builder(
            packageName,
            generatedClass.build()
        ).skipJavaLangImports(true).build().writeTo(writeTo)
    }


}

private fun generateClass(
    isInterface: Boolean,
    name: String,
    visibility: Visibility,
    isAbstract: Boolean,
    superClass: SuperType?,
    superInterfaces: List<SuperType>,
    init: JavaGeneratedClass.() -> Unit
): TypeSpec.Builder {
    val builder = if (isInterface) TypeSpec.interfaceBuilder(name) else TypeSpec.classBuilder(name)
    builder.apply {
        visibility.toModifier()?.let { addModifiers(it) }
        if (isAbstract) addModifiers(Modifier.ABSTRACT)
        if (superClass != null) superclass(superClass.toTypeName())
        for (superInterface in superInterfaces) addSuperinterface(superInterface.toTypeName())
    }
    JavaGeneratedClass(builder, isInterface).init()
    return builder
}


@CodeGeneratorDsl
class JavaGeneratedClass(
    private val typeSpec: TypeSpec.Builder,
    private val isInterface: Boolean
) {
    fun addMethod(
        name: String,
        returnType: ReturnDescriptor?,
        visibility: Visibility,
        abstract: Boolean,
        static: Boolean,
        final: Boolean,
        parameters: Map<String, JvmType>,
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


    fun addInnerClass(
        name: String,
        isInterface: Boolean,
        /**
         * Interfaces are NOT considered abstract
         */
        isAbstract: Boolean,
        isStatic: Boolean,
        visibility: Visibility,
        superClass: SuperType?,
        superInterfaces: List<SuperType>,
        init: JavaGeneratedClass.() -> Unit
    ) {
        val generatedClass = generateClass(isInterface, name, visibility, isAbstract, superClass, superInterfaces, init)
        typeSpec.addType(generatedClass.apply {
            if (isStatic) addModifiers(Modifier.STATIC)
        }.build())
    }


    fun addConstructor(
        visibility: Visibility,
        parameters: Map<String, JvmType>,
        init: JavaGeneratedMethod.() -> Unit
    ) {
        require(!isInterface) { "Interfaces don't have constructors" }
        addMethodImpl(visibility, parameters, MethodSpec.constructorBuilder(), internalConfig = {}, userConfig = init)
    }

    private fun addMethodImpl(
        visibility: Visibility,
        parameters: Map<String, JvmType>,
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
        type: JvmType,
        visibility: Visibility,
        static: Boolean,
        final: Boolean,
        initializer: Expression?
    ) {
        typeSpec.addField(FieldSpec.builder(type.toTypeName(), name)
            .apply {
                visibility.toModifier()?.let { addModifiers(it) }
                if (static) addModifiers(Modifier.STATIC)
                if (final) addModifiers(Modifier.FINAL)
                if (initializer != null) {
                    val (format, arguments) = JavaCodeWriter().write(initializer)
                    initializer(format, *arguments.toTypedArray())
                }
            }
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


