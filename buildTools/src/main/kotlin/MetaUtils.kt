import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the


class MetaUtils : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("metaUtils", BuildMetaUtilsExtension::class.java, project)
    }
}


open class BuildMetaUtilsExtension(private val project: Project) {
    fun createJarTest(name: String): SourceSet = with(project) {
        val sourceSet = sourceSets.create(name)
        tasks.create(name, Jar::class.java) {
            group = "testing"
            from(sourceSet.output)

            destinationDirectory.set(sourceSets["test"].resources.srcDirs.first())
            archiveFileName.set("$name.jar")
        }

        tasks.named("processTestResources") {
            dependsOn(sourceSet)
        }

        return@with sourceSet
    }
}

private val Project.sourceSets: SourceSetContainer
    get() = the<JavaPluginConvention>().sourceSets