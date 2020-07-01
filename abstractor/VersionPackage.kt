package abstractor

import metautils.api.JavaType
import metautils.api.remap
import descriptor.*
import metautils.descriptor.ArrayType
import metautils.descriptor.JvmType
import metautils.descriptor.ObjectType
import metautils.descriptor.ReturnDescriptor
import metautils.signature.*
import metautils.util.*
import metautils.signature.remap
import metautils.signature.toJvmType

class VersionPackage(private val versionPackage: String) {
    private fun PackageName?.toApiPackageName() = versionPackage.prependToQualified(this ?: PackageName.Empty)
    private fun ShortClassName.toApiShortClassName() =
        ShortClassName(("I" + outerMostClass()).prependTo(innerClasses()))


//    fun String.remapToApiClass(dotQualified : Boolean = false, dollarQualified : Boolean = true) =
//        toQualifiedName(dotQualified, dollarQualified).toApiClass().toString(dotQualified, dollarQualified)

    fun QualifiedName.toApiClass(): QualifiedName = if (isMcClassName()) {
        QualifiedName(
            packageName = packageName.toApiPackageName(),
            shortName = shortName.toApiShortClassName()
        )
    } else this

    private fun ShortClassName.toBaseShortClassName() =
        ShortClassName(("Base" + outerMostClass()).prependTo(innerClasses()))

//    private fun ShortClassName.toBaseApiShortClassName() =
//        ShortClassName(("IBase" + outerMostClass()).prependTo(innerClasses()))

    fun QualifiedName.toBaseClass(): QualifiedName =
        QualifiedName(
            packageName = packageName.toApiPackageName(),
            shortName = shortName.toBaseShortClassName()
        )

//
//    private fun ShortClassName.toExceptionSuperShortClassName() =
//        ShortClassName(("E" + outerMostClass()).prependTo(innerClasses()))
//
//
//    fun QualifiedName.toExceptionSuperClass(): QualifiedName = QualifiedName(
//        packageName = packageName.toApiPackageName(),
//        shortName = shortName.toExceptionSuperShortClassName()
//    )


//    fun QualifiedName.toBaseApiInterface(): QualifiedName = if (isMcClassName()) {
//        QualifiedName(
//            packageName = packageName.toApiPackageName(),
//            shortName = shortName.toBaseApiShortClassName()
//        )
//    } else this

    fun <T : ReturnDescriptor> T.remapToApiClass(): T = remap { it.toApiClass() }
    fun <T : GenericReturnType> JavaType<T>.remapToApiClass(): JavaType<T> = remap { it.toApiClass() }
//    fun <T : GenericReturnType> JavaType<T>.remapToBaseClass(): JavaType<T> = if(this is ClassGenericType){
//        remap { it.toBaseClass() }
//    }

//    fun JavaClassType.remapToBaseClass() : JavaClassType = copy(
//        type = type.remapTopLevel { it.toBaseClass() }.remapTypeArguments { it.toApiClass() }
//    )

    fun <T : GenericReturnType> T.remapToApiClass(): T = remap { it.toApiClass() }
    fun List<TypeArgumentDeclaration>.remapDeclToApiClasses() = map { typeArg ->
        typeArg.copy(
            classBound = typeArg.classBound?.remapToApiClass(),
            interfaceBounds = typeArg.interfaceBounds.map { it.remapToApiClass() })
    }


    fun <T : GenericReturnType> List<JavaType<T>>.remapToApiClasses(): List<JavaType<T>> =
        map { it.remapToApiClass() }
}

fun PackageName?.isMcPackage(): Boolean = this?.startsWith("net", "minecraft") == true
fun QualifiedName.isMcClassName(): Boolean = packageName.isMcPackage()
fun GenericReturnType.isMcClass(): Boolean = this is ArrayGenericType && componentType.isMcClass() ||
        this is ClassGenericType && packageName.isMcPackage()
        || this is TypeVariable && toJvmType().isMcClass()

private fun JvmType.isMcClass(): Boolean = this is ObjectType && fullClassName.isMcClassName()
        || this is ArrayType && componentType.isMcClass()

fun JavaType<*>.isMcClass(): Boolean = type.isMcClass()