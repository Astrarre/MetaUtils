import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.options.Option
import java.io.File
import java.util.concurrent.TimeUnit


class MetaUtils : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("metaUtils", BuildMetaUtilsExtension::class.java, project)
    }
}







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
}

private val Project.sourceSets: SourceSetContainer
    get() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets