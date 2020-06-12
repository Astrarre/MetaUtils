package codegeneration


import api.AnyJavaType
import api.JavaClassType
import api.JavaReturnType
import com.squareup.javapoet.*
import signature.TypeArgumentDeclaration
import util.PackageName
import util.QualifiedName
import java.nio.file.Path
import javax.lang.model.element.Modifier

@DslMarker
annotation class CodeGeneratorDsl

data class ClassInfo(
    val shortName: String,
    val visibility: Visibility,
    /**
     * Interfaces are NOT considered abstract
     */
    val isAbstract: Boolean,
    val isInterface: Boolean,
    val typeArguments: List<TypeArgumentDeclaration>,
    val superClass: JavaClassType?,
    val superInterfaces: List<JavaClassType>,
    val body: JavaGeneratedClass.() -> Unit
)

@CodeGeneratorDsl
object JavaCodeGenerator {

    fun writeClass(
        info: ClassInfo,
        packageName: PackageName?,
        writeTo: Path
    ) {
        val generatedClass = generateClass(info)
        JavaFile.builder(
            packageName?.toDotQualified() ?: "",
            generatedClass.build()
        ).skipJavaLangImports(true).build().writeTo(writeTo)
    }


}

private fun generateClass(info: ClassInfo): TypeSpec.Builder  = with(info) {
    val builder = if (isInterface) TypeSpec.interfaceBuilder(shortName) else TypeSpec.classBuilder(shortName)
    builder.apply {
        visibility.toModifier()?.let { addModifiers(it) }
        if (isAbstract) addModifiers(Modifier.ABSTRACT)
        if (superClass != null) superclass(superClass.toTypeName())
        for (superInterface in superInterfaces) addSuperinterface(superInterface.toTypeName())
        addTypeVariables(typeArguments.map { it.toTypeName() })
    }
    JavaGeneratedClass(builder, isInterface).body()
    return builder
}


@CodeGeneratorDsl
class JavaGeneratedClass(
    private val typeSpec: TypeSpec.Builder,
    private val isInterface: Boolean
) {
    fun addMethod(
        visibility: Visibility,
        static: Boolean,
        final: Boolean,
        abstract: Boolean,
        typeArguments: List<TypeArgumentDeclaration>,
        name: String,
        parameters: Map<String, AnyJavaType>,
        returnType: JavaReturnType?,
        body: JavaGeneratedMethod.() -> Unit
    ) {
        addMethodImpl(visibility, parameters, typeArguments, MethodSpec.methodBuilder(name), internalConfig = {
            if (returnType != null) returns(returnType.toTypeName())

            if (abstract) addModifiers(Modifier.ABSTRACT)
            else if (static) addModifiers(Modifier.STATIC)
            else if (isInterface) addModifiers(Modifier.DEFAULT)

            if (final) addModifiers(Modifier.FINAL)
        }, userConfig = body)

    }


    fun addInnerClass(info: ClassInfo, isStatic : Boolean) {
        val generatedClass = generateClass(info)
        typeSpec.addType(generatedClass.apply {
            if (isStatic) addModifiers(Modifier.STATIC)
        }.build())
    }


    fun addConstructor(
        visibility: Visibility,
        parameters: Map<String, AnyJavaType>,
        init: JavaGeneratedMethod.() -> Unit
    ) {
        require(!isInterface) { "Interfaces don't have constructors" }
        addMethodImpl(
            visibility,
            parameters,
            listOf(),
            MethodSpec.constructorBuilder(),
            internalConfig = {},
            userConfig = init
        )
    }

    private fun addMethodImpl(
        visibility: Visibility,
        parameters: Map<String, AnyJavaType>,
        typeArguments: List<TypeArgumentDeclaration>,
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
                    addTypeVariables(typeArguments.map { it.toTypeName() })
                }).apply(userConfig)
                .build()
        )
    }

//    private fun Boolean.staticModifier() = if(this) Modifier.STATIC

    /*override*/  fun addField(
        name: String,
        type: AnyJavaType,
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

//    fun setSuperclass(type: ObjectType) {
//        typeSpec.superclass(type.toTypeName())
//    }

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


