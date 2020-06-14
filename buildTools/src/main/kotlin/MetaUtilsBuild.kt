import abstractor.VersionPackage
import api.ClassApi
import api.listInnerClassChain
import api.readFromList
import api.visitClasses
import asm.readToClassNode
import asm.writeTo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import signature.*
import util.*
import java.io.File
import java.nio.file.Path


class MetaUtils : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("metaUtils", BuildMetaUtilsExtension::class.java, project)
    }
}

//private fun String?.toApiPackageName() = "v1/${this ?: ""}"
//private fun String.toApiClassName() = "I$this"
//private fun String.remapToApiClass(): String {
//    val (packageName, className) = toQualifiedName(dotQualified = false)
//    checkNotNull(packageName)
//    return "${packageName.toSlashQualified().toApiPackageName()}/${className.toDollarQualifiedString()
//        .toApiClassName()}"
//}

//class X : ArrayList<String>()

open class BuildMetaUtilsExtension(private val project: Project) {
    fun createJarTest(name: String): SourceSet = with(project) {
        val sourceSet = sourceSets.create(name)
        val jarTask = tasks.create(name, Jar::class.java) { task ->
            group = "testing"
            task.from(sourceSet.output)

            task.destinationDirectory.set(sourceSets.getByName("test").resources.srcDirs.first())
            task.archiveFileName.set("$name.jar")
        }

        tasks.named("processTestResources") { task ->
            task.dependsOn(jarTask)
        }

        return@with sourceSet
    }

    fun createAttachInterfacesTask(targetClassDirs: Set<File>): FileCollection = with(project) {
        val targetClassDir = targetClassDirs.first { it.parentFile.name == "java" }
        val targetClassPath = targetClassDir.toPath()
        val destinationJar = project.file("testdata/mcJarWithInterfaces.jar")
        tasks.create("attachInterfaces") { task ->
            task.doLast {
                val inputChildren = targetClassPath.recursiveChildren()
                val allInputs = inputChildren.filter { !it.isDirectory() }.toList()
                val inputsParsed = ClassApi.readFromList(allInputs, rootPath = targetClassPath)
                    .matchToPaths(rootPath = targetClassPath)
                val outputDir = File(targetClassDir.parentFile, targetClassDir.nameWithoutExtension + "WithInterfaces")
                    .toPath()
                val outputsToInputs = allInputs.associateBy { path ->
                    val relativePath = targetClassPath.relativize(path).toString()
                    outputDir.resolve(relativePath)
                }
                with(VersionPackage("v1")) {
                    for ((output, input) in outputsToInputs) {
                        val classNode = readToClassNode(input)
                        check(classNode.name.startsWith("net/minecraft/"))
                        val parsedClass = inputsParsed[input]

                        if (parsedClass != null) {
                            val newName = classNode.name.remapToApiClass()
                            println("Attaching interface $newName to ${classNode.name}")
                            classNode.interfaces.add(newName)

                            if (classNode.signature != null) {
                                val signature = ClassSignature.readFrom(classNode.signature)
                                val insertedInterface = ClassGenericType.fromNameAndTypeArgs(
                                    name = parsedClass.name.toApiClass(),
                                    typeArgs = allApiClassTypeArguments(parsedClass).toTypeArgumentsOfNames()
                                )
                                val newSignature = signature.copy(
                                    superInterfaces = signature.superInterfaces + insertedInterface
                                )
                                classNode.signature = newSignature.toClassfileName()
                            }
                        }

                        output.parent.createDirectories()

                        classNode.writeTo(output)
                    }
                }




                destinationJar.toPath().parent.createDirectories()
                outputDir.convertDirToJar(destination = destinationJar.toPath())
            }
        }
        files(destinationJar)
    }

}

private fun Collection<ClassApi>.matchToPaths(rootPath: Path): Map<Path, ClassApi> =
    mutableMapOf<Path, ClassApi>().apply {
        for (topLevelClass in this@matchToPaths) {
            topLevelClass.visitClasses { classApi ->
                val path = rootPath.resolve(classApi.name.packageName.toPath())
                    .resolve(classApi.name.shortName.toDollarQualifiedString() + ".class")
                put(path, classApi)
            }
        }
    }


private fun VersionPackage.allApiClassTypeArguments(classApi: ClassApi): List<TypeArgumentDeclaration> = when {
    classApi.isStatic -> classApi.typeArguments.remapDeclToApiClasses()
    else -> classApi.listInnerClassChain().flatMap { it.typeArguments.remapDeclToApiClasses() }
}

private val Project.sourceSets: SourceSetContainer
    get() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets