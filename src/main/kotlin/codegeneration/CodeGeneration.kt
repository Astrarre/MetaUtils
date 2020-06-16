package codegeneration

import api.*
import com.squareup.javapoet.*
import signature.TypeArgumentDeclaration
import util.PackageName
import util.ShortClassName
import java.nio.file.Path
import javax.lang.model.element.Modifier
import kotlin.contracts.contract

class ClassInfo(
    val shortName: String,
    val visibility: Visibility,
    /**
     * Interfaces are NOT considered abstract
     */
    val access: ClassAccess,
    val typeArguments: List<TypeArgumentDeclaration>,
    val superClass: JavaClassType?,
    val superInterfaces: List<JavaClassType>,
    val annotations: List<JavaAnnotation>,
    val body: GeneratedClass.() -> Unit
)

data class MethodInfo(
    val visibility: Visibility,
    val parameters: Map<String, AnyJavaType>,
    val throws: List<JavaThrowableType>,
    val body: GeneratedMethod.() -> Unit
)

@CodeGeneratorDsl
interface CodeGenerator {
    fun writeClass(
        info: ClassInfo,
        packageName: PackageName?,
        srcRoot: Path
    )
}

@CodeGeneratorDsl
interface GeneratedClass {
    fun addMethod(
        methodInfo: MethodInfo,
        access : MethodAccess,
        typeArguments: List<TypeArgumentDeclaration>,
        name: String,
        returnType: JavaReturnType
    )
    fun addConstructor(info: MethodInfo)
    fun addInnerClass(info: ClassInfo, isStatic: Boolean)
    fun addField(
        name: String,
        type: AnyJavaType,
        visibility: Visibility,
        isStatic: Boolean,
        isFinal: Boolean,
        initializer: Expression?
    )
}

@CodeGeneratorDsl
interface GeneratedMethod {
    fun addStatement(statement: Statement)
    fun addComment(comment: String)
}


fun GeneratedClass.addMethod(
    visibility: Visibility,
    parameters: Map<String, AnyJavaType>,
    throws: List<JavaThrowableType>,
    static: Boolean,
    final: Boolean,
    abstract: Boolean,
    typeArguments: List<TypeArgumentDeclaration>,
    name: String,
    returnType: JavaReturnType,
    body: GeneratedMethod.() -> Unit
): Unit = addMethod(
    MethodInfo(visibility, parameters, throws, body),
    MethodAccess(static, final, abstract), typeArguments, name, returnType
)