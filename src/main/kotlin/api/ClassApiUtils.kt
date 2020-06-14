package api

import codegeneration.Public
import codegeneration.Visibility
import descriptor.MethodDescriptor
import descriptor.ObjectType
import signature.*
import util.QualifiedName
import util.ShortClassName

val ClassApi.isInterface get() = classVariant == ClassApi.Variant.Interface
val ClassApi.isAbstract get() = classVariant == ClassApi.Variant.AbstractClass
val ClassApi.isInnerClass get() = outerClass != null
val Visible.isPublicApi get() = isPublic || visibility == Visibility.Protected
val Visible.isPublic get() = visibility == Visibility.Public
val ClassApi.Method.isConstructor get() = name == "<init>"
fun ClassApi.Method.getJvmDescriptor() = MethodDescriptor(
    parameterDescriptors = parameters.map { (_, type) -> type.type.toJvmType() },
    returnDescriptor = returnType.toJvmType()
)

/**
 * Goes from top to bottom
 */
@OptIn(ExperimentalStdlibApi::class)
fun ClassApi.listInnerClassChain(): List<ClassApi> = buildList<ClassApi> { addToInnerClassChain(this) }.reversed()
private fun ClassApi.outerClassCount() = listInnerClassChain().size - 1
private fun ClassApi.addToInnerClassChain(accumulated: MutableList<ClassApi>) {
    accumulated.add(this)
    outerClass?.value?.addToInnerClassChain(accumulated)
}

val ClassApi.Method.isVoid get() = returnType.type == GenericReturnType.Void
fun ClassApi.asType() = name.toClassGenericType(
    if (isStatic) {
        // Only put type arguments at the end
        (0 until outerClassCount()).map { null } + listOf(typeArguments.toTypeArgumentsOfNames())
    } else listInnerClassChain().map { it.typeArguments.toTypeArgumentsOfNames() }
).noAnnotations()

fun ClassApi.asRawType() = ObjectType(name).toRawJavaType()


fun ClassApi.innerMostClassNameAsType() = ObjectType(
    QualifiedName(
        packageName = null,
        shortName = ShortClassName(listOf(name.shortName.innermostClass()))
    )
).toRawJavaType()

fun ClassApi.visitClasses(visitor: (ClassApi) -> Unit) {
    visitor(this)
    innerClasses.forEach(visitor)
}