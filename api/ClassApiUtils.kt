package metautils.api

import api.ClassApi
import api.Visible
import codegeneration.Public
import codegeneration.Visibility
import codegeneration.isAbstract
import codegeneration.isInterface
import metautils.descriptor.MethodDescriptor
import metautils.descriptor.ObjectType
import metautils.signature.*

val ClassApi.isFinal get() = access.isFinal
val ClassApi.variant get() = access.variant
val ClassApi.Method.isFinal get() = access.isFinal
val ClassApi.Method.isAbstract get() = access.isAbstract

val ClassApi.isInterface get() = variant.isInterface
val ClassApi.isAbstract get() = variant.isAbstract
val ClassApi.isInnerClass get() = outerClass != null
val Visible.isPublicApi get() = isPublic || visibility == Visibility.Protected
val Visible.isPublic get() = visibility == Visibility.Public
val Visible.isProtected get() = visibility == Visibility.Protected
val ClassApi.Method.isConstructor get() = name == "<init>"
fun ClassApi.Method.getJvmDescriptor() = MethodDescriptor(
    parameterDescriptors = parameters.map { (_, type) -> type.type.toJvmType() },
    returnDescriptor = returnType.toJvmType()
)

/**
 * Goes from top to bottom
 */
@OptIn(ExperimentalStdlibApi::class)
fun ClassApi.outerClassesToThis(): List<ClassApi> = buildList { visitThisAndOuterClasses { add(it) } }.reversed()
private fun ClassApi.outerClassCount() = outerClassesToThis().size - 1
private inline fun ClassApi.visitThisAndOuterClasses(visitor: (ClassApi) -> Unit) {
    visitor(this)
    if (outerClass != null) visitor(outerClass)
}
//private fun ClassApi.addToInnerClassChain(accumulated: MutableList<ClassApi>) {
//    accumulated.add(this)
//    outerClass?.value?.addToInnerClassChain(accumulated)
//}

val ClassApi.Method.isVoid get() = returnType.type == GenericReturnType.Void
fun ClassApi.asType(): JavaClassType = name.toClassGenericType(
    if (isStatic) {
        // Only put type arguments at the end
        (0 until outerClassCount()).map { null } + listOf(typeArguments.toTypeArgumentsOfNames())
    } else outerClassesToThis().map { it.typeArguments.toTypeArgumentsOfNames() }
).noAnnotations()

fun ClassApi.asRawType() = asJvmType().toRawJavaType()
fun ClassApi.asJvmType() = ObjectType(name)


//fun ClassApi.innerMostClassNameAsType() = ObjectType(
//    QualifiedName(
//        packageName = null,
//        shortName = ShortClassName(listOf(name.shortName.innermostClass()))
//    )
//).toRawJavaType()

fun ClassApi.visitThisAndInnerClasses(visitor: (ClassApi) -> Unit) {
    visitor(this)
    innerClasses.forEach(visitor)
}

@OptIn(ExperimentalStdlibApi::class)
fun ClassApi.allInnerClassesAndThis() = buildList { visitThisAndInnerClasses { add(it) } }