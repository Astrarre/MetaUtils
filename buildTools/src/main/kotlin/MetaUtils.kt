import asm.readToClassNode
import asm.writeTo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import java.io.File


class MetaUtils : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("metaUtils", BuildMetaUtilsExtension::class.java, project)
    }
}

private fun String?.toApiPackageName() = "v1/${this ?: ""}"
private fun String.toApiClassName() = "I$this"
private fun String.remapToApiClass(): String {
    val (packageName, className) = toQualifiedName(dotQualified = false)
    checkNotNull(packageName)
    return "${packageName.toSlashQualified().toApiPackageName()}/${className.toFullString().toApiClassName()}"
}

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
        val destinationJar = project.file("build/resources/test/mcJarWithInterfaces.jar")
        tasks.create("attachInterfaces") { task ->
            task.doLast {
                val allInputs = targetClassPath.recursiveChildren().filter { !it.isDirectory() }.toList()
                val outputDir = File(targetClassDir.parentFile, targetClassDir.nameWithoutExtension + "WithInterfaces")
                    .toPath()
                val outputsToInputs = allInputs.associateBy { path ->
                    val relativePath = targetClassPath.relativize(path).toString()
                    outputDir.resolve(relativePath)
                }
                for ((output, input) in outputsToInputs) {
                    val classNode = readToClassNode(input)
                    if (classNode.name.startsWith("net/minecraft/")) {
                        val newName = classNode.name.remapToApiClass()
                        classNode.interfaces.add(newName)

                        println("Attaching interface $newName to ${classNode.name}")
                    }

                    output.parent.createDirectories()

                    classNode.writeTo(output)
                }

                outputDir.zipToJar(destination = destinationJar.toPath())
            }
        }
        files(destinationJar)
    }

}

private val Project.sourceSets: SourceSetContainer
    get() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets