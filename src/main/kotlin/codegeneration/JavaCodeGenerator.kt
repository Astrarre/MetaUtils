package codegeneration


import com.squareup.javapoet.*
import descriptor.*
import java.nio.file.Path
import javax.lang.model.element.Modifier

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
        isAbstract: Boolean,
        visibility: Visibility = Visibility.Public,
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
        init: JavaGeneratedMethod.() -> Unit
    ) {
        typeSpec.addMethod(
            JavaGeneratedMethod(MethodSpec.methodBuilder(name)
                .apply {
                    if (returnType != null) returns(returnType.toTypeName())
                    addParameters(parameters.map { (name, type) ->
                        ParameterSpec.builder(type.toTypeName(), name).build()
                    })
                    visibility.toModifier()?.let { addModifiers(it) }

                    if (abstract) addModifiers(Modifier.ABSTRACT)
                    else if (static) addModifiers(Modifier.STATIC)
                    else if (isInterface) addModifiers(Modifier.DEFAULT)
                    if (final) addModifiers(Modifier.FINAL)
                }).apply(init)
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

private fun ReturnDescriptor.toTypeName(): TypeName = when (this) {
    is AnyType -> toTypeName()
    ReturnDescriptor.Void -> TypeName.VOID
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
    is ArrayType -> ArrayTypeName.of(componentType.toTypeName())
}

class JavaGeneratedMethod(private val methodSpec: MethodSpec.Builder) {
    fun addStatement(format: String, vararg typeArgs: AnyType) {
        methodSpec.addStatement(format, *typeArgs.map { it.toTypeName() }.toTypedArray())
    }

    fun addComment(comment: String) {
        methodSpec.addComment(comment)
    }

    fun build(): MethodSpec = methodSpec.build()
}

class JavaGeneratedField(private val fieldSpec: FieldSpec.Builder) {
    fun setInitializer(format: String, vararg typeArgs: AnyType) {
        fieldSpec.initializer(format, *typeArgs.map { it.toTypeName() }.toTypedArray())
    }


    fun build(): FieldSpec = fieldSpec.build()
}