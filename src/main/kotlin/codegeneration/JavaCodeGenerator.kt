package codegeneration


import api.AnyJavaType
import api.JavaReturnType
import api.JavaType
import com.squareup.javapoet.*
import signature.TypeArgumentDeclaration
import util.PackageName
import java.nio.file.Path
import javax.lang.model.element.Modifier

@DslMarker
annotation class CodeGeneratorDsl


@CodeGeneratorDsl
object JavaCodeGenerator : CodeGenerator {

    override fun writeClass(
        info: ClassInfo,
        packageName: PackageName?,
        srcRoot: Path
    ) {
        val generatedClass = generateClass(info)
        JavaFile.builder(
            packageName?.toDotQualified() ?: "",
            generatedClass.build()
        ).skipJavaLangImports(true).build().writeTo(srcRoot)
    }


}

private fun generateClass(info: ClassInfo): TypeSpec.Builder = with(info) {
    val builder =
        if (access.variant.isInterface) TypeSpec.interfaceBuilder(shortName) else TypeSpec.classBuilder(shortName)
    builder.apply {
        visibility.toModifier()?.let { addModifiers(it) }
        if (access.variant.isAbstract) addModifiers(Modifier.ABSTRACT)
        if (superClass != null) superclass(superClass.toTypeName())
        for (superInterface in superInterfaces) addSuperinterface(superInterface.toTypeName())
        addTypeVariables(typeArguments.map { it.toTypeName() })
        addAnnotations(this@with.annotations.map { it.toAnnotationSpec() })
    }
    JavaGeneratedClass(builder, access.variant.isInterface).body()
    return builder
}

private fun generateMethod(info: MethodInfo, name: String?): MethodSpec.Builder = with(info) {
    val builder = if (name != null) MethodSpec.methodBuilder(name) else MethodSpec.constructorBuilder()
    builder.apply {
        JavaGeneratedMethod(this).apply(body)
        addParameters(info.parameters.map { (name, type) ->
            ParameterSpec.builder(type.toTypeName(), name).apply {
                addAnnotations(type.annotations.map { it.toAnnotationSpec() })
            }.build()
        })
        addExceptions(throws.map { it.toTypeName() })
        visibility.toModifier()?.let { addModifiers(it) }
    }

}

@CodeGeneratorDsl
class JavaGeneratedClass(
    private val typeSpec: TypeSpec.Builder,
    private val isInterface: Boolean
) : GeneratedClass {

    override fun addMethod(
        methodInfo: MethodInfo,
        access: MethodAccess,
        typeArguments: List<TypeArgumentDeclaration>,
        name: String,
        returnType: JavaReturnType
    ) {
        val method = generateMethod(methodInfo, name).apply {
            addTypeVariables(typeArguments.map { it.toTypeName() })
            //TODO: the fact the annotations are attached to the return type is a bit wrong,
            // but in java if you put the same annotation on the method and return type
            // it counts as duplicating the annotation...
            returns(returnType.toTypeName())
            addAnnotations(returnType.annotations.map { it.toAnnotationSpec() })


            if (access.isAbstract) addModifiers(Modifier.ABSTRACT)
            else if (access.isStatic) addModifiers(Modifier.STATIC)
            else if (isInterface) addModifiers(Modifier.DEFAULT)

            if (access.isFinal) addModifiers(Modifier.FINAL)

        }.build()

        typeSpec.addMethod(method)
    }


    override fun addConstructor(info: MethodInfo) {
        require(!isInterface) { "Interfaces don't have constructors" }
        typeSpec.addMethod(generateMethod(info, name = null).build())
    }

    override fun addInnerClass(info: ClassInfo, isStatic: Boolean) {
        val generatedClass = generateClass(info)
        typeSpec.addType(generatedClass.apply {
            if (isStatic) addModifiers(Modifier.STATIC)
        }.build())
    }

    override fun addField(
        name: String,
        type: AnyJavaType,
        visibility: Visibility,
        isStatic: Boolean,
        isFinal: Boolean,
        initializer: Expression?
    ) {
        typeSpec.addField(FieldSpec.builder(type.toTypeName(), name)
            .apply {
                visibility.toModifier()?.let { addModifiers(it) }
                if (isStatic) addModifiers(Modifier.STATIC)
                if (isFinal) addModifiers(Modifier.FINAL)
                if (initializer != null) {
                    val (format, arguments) = JavaCodeWriter().write(initializer)
                    initializer(format, *arguments.toTypeNames())
                }
                addAnnotations(type.annotations.map { it.toAnnotationSpec() })
            }
            .build()
        )
    }


}

private fun List<JavaType<*>>.toTypeNames() = map { it.toTypeName() }.toTypedArray()


@CodeGeneratorDsl
class JavaGeneratedMethod(private val methodSpec: MethodSpec.Builder) : GeneratedMethod {

    override fun addStatement(statement: Statement) {
        val (format, arguments) = JavaCodeWriter().write(statement)
        methodSpec.addStatement(format, *arguments.toTypeNames())
    }

    override fun addComment(comment: String) {
        methodSpec.addComment(comment)
    }

}


