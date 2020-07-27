package metautils.api

import abstractor.ClassAbstractionType
import abstractor.IAbstractionType
import abstractor.TargetSelector
import codegeneration.Public
import metautils.codegeneration.Visibility
import codegeneration.isAbstract
import codegeneration.isInterface
import metautils.descriptor.MethodDescriptor
import metautils.descriptor.ObjectType
import metautils.signature.*
import metautils.util.QualifiedName
import metautils.util.applyIf
import metautils.util.values
import metautils.util.visitNames

val ClassApi.isFinal get() = access.isFinal
val ClassApi.variant get() = access.variant
val ClassApi.Method.isFinal get() = access.isFinal
val ClassApi.Method.isAbstract get() = access.isAbstract

val ClassApi.isInterface get() = variant.isInterface
val ClassApi.isAbstract get() = variant.isAbstract
val ClassApi.isInnerClass get() = outerClass != null
val Visible.isPublicApiAsOutermostMember get() = isPublic || isProtected
val ClassApi.isPublicApi get() = outerClassesToThis().all { it.isPublicApiAsOutermostMember }
val Visible.isPublic get() = visibility == Visibility.Public
val Visible.isProtected get() = visibility == Visibility.Protected
val ClassApi.Method.isConstructor get() = name == "<init>"
fun ClassApi.Method.getJvmDescriptor() = MethodDescriptor(
    parameterDescriptors = getJvmParameters(),
    returnDescriptor = returnType.toJvmType()
)

fun ClassApi.Method.getJvmParameters() = parameters.map { (_, type) -> type.type.toJvmType() }

/**
 * Goes from top to bottom
 */
@OptIn(ExperimentalStdlibApi::class)
fun ClassApi.outerClassesToThis(): List<ClassApi> = buildList { visitThisAndOuterClasses { add(it) } }.reversed()
private fun ClassApi.outerClassCount() = outerClassesToThis().size - 1
private inline fun ClassApi.visitThisAndOuterClasses(visitor: (ClassApi) -> Unit) {
    visitor(this)
    if (outerClass != null) visitor(outerClass!!)
}

val ClassApi.Method.isVoid get() = returnType.type == GenericReturnType.Void
fun ClassApi.asType(): JavaClassType = name.toClassGenericType(
    if (isStatic) {
        // Only put type arguments at the end
        (0 until outerClassCount()).map { null } + listOf(typeArguments.toTypeArgumentsOfNames())
    } else outerClassesToThis().map { it.typeArguments.toTypeArgumentsOfNames() }
).noAnnotations()

fun ClassApi.asRawType() = asJvmType().toRawJavaType()
fun ClassApi.asJvmType() = ObjectType(name)

fun ClassApi.isSamInterface() = isInterface && methods.filter { it.isAbstract }.size == 1

fun ClassApi.getSignature(): ClassSignature = ClassSignature(
    typeArguments.applyIf<List<TypeArgumentDeclaration>?>(typeArguments.isEmpty()) { null },
    superClass?.type ?: JavaLangObjectGenericType,
    superInterfaces.map { it.type }
)



fun ClassApi.visitThisAndInnerClasses(visitor: (ClassApi) -> Unit) {
    visitor(this)
    innerClasses.forEach { it.visitThisAndInnerClasses(visitor) }
}

@OptIn(ExperimentalStdlibApi::class)
fun ClassApi.allInnerClassesAndThis() = buildList { visitThisAndInnerClasses { add(it) } }


fun ClassApi.getAllReferencedClasses(selector: TargetSelector): List<QualifiedName> {
    val list = mutableListOf<QualifiedName>()
    visitReferencedClasses(selector) { list.add(it) }
    return list
}

fun ClassApi.visitReferencedClasses(
    selector: TargetSelector,
    visitor: (QualifiedName) -> Unit
) {
    annotations.forEach { it.visitNames(visitor) }
    visitor(name)
    typeArguments.forEach { it.visitNames(visitor) }
    superClass?.visitNames(visitor)
    superInterfaces.forEach { it.visitNames(visitor) }
    val classAbstractionType = selector.classes(this)
    methods.forEach {
        if (willBeReferenced(selector.methods(it), it, classAbstractionType)) {
            it.visitReferencedClasses(visitor)
        }
    }
    fields.forEach {
        if (willBeReferenced(selector.fields(it), it, classAbstractionType)) {
            it.type.visitNames(visitor)
        }
    }
    innerClasses.forEach {
        if (willBeReferenced(selector.classes(this), it, classAbstractionType)) {
            it.visitReferencedClasses(selector, visitor)
        }
    }
}

private fun willBeReferenced(
    memberAbstractionType: IAbstractionType,
    member: Visible,
    parentClassAbstractionType: ClassAbstractionType
): Boolean = when {
    member.isPublic -> {
        memberAbstractionType.addInInterface && parentClassAbstractionType.addInInterface
    }
    member.isProtected -> {
        memberAbstractionType.addInBaseclass && parentClassAbstractionType.addInBaseclass
    }
    else -> false
}

//private fun Visible.is


fun ClassApi.Method.visitReferencedClasses(visitor: (QualifiedName) -> Unit) {
    returnType.visitNames(visitor)
    parameters.values.forEach { it.visitNames(visitor) }
    typeArguments.forEach { it.visitNames(visitor) }
    throws.forEach { it.visitNames(visitor) }
}

//fun ClassApi.Method.getAllReferencedClassesRecursively() = mutableListOf<QualifiedName>()
//    .apply { visitReferencedClasses { add(it) } }