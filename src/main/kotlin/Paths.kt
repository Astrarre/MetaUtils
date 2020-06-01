import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.InputStream
import java.nio.file.*
import java.util.jar.JarOutputStream
import kotlin.streams.asSequence

fun Path.exists() = Files.exists(this)
fun Path.deleteIfExists() = Files.deleteIfExists(this)
fun Path.delete() = Files.delete(this)
fun Path.deleteRecursively() = toFile().deleteRecursively()
inline fun <T> Path.openJar(usage: (FileSystem) -> T): T = FileSystems.newFileSystem(this, null).use(usage)
fun Path.walk(): Sequence<Path> = Files.walk(this).asSequence()
fun <T> Path.walkJar(usage: (Sequence<Path>) -> T): T = openJar { usage(it.getPath("/").walk()) }
fun Path.createJar() = JarOutputStream(Files.newOutputStream(this)).close()
fun Path.isDirectory() = Files.isDirectory(this)
fun Path.createDirectory(): Path = Files.createDirectory(this)
fun Path.inputStream(): InputStream = Files.newInputStream(this)
fun Path.writeBytes(bytes: ByteArray): Path = Files.write(this, bytes)
inline fun openJars(jar1: Path, jar2: Path, jar3: Path, usage: (FileSystem, FileSystem, FileSystem) -> Unit) =
    jar1.openJar { j1 -> jar2.openJar { j2 -> jar3.openJar { j3 -> usage(j1, j2, j3) } } }

fun Path.isClassfile() = hasExtension(".class")
fun Path.directChildren() = Files.list(this).asSequence()
fun Path.recursiveChildren() = Files.walk(this).asSequence()
fun Path.hasExtension(extension : String) = toString().endsWith(extension)

fun Path.copyTo(target: Path, overwrite: Boolean = true): Path? {
    var args = arrayOf<CopyOption>()
    if (overwrite) args += StandardCopyOption.REPLACE_EXISTING
    return Files.copy(this, target, *args)
}

fun readToClassNode(classFile: Path): ClassNode = classFile.inputStream().use { stream ->
    ClassNode().also { ClassReader(stream).accept(it, 0) }
}

fun String.addIf(boolean: Boolean) = if (boolean) this else ""

data class FullyQualifiedName(val packageName: String?, val className: String)

fun String.splitFullyQualifiedName(dotQualified: Boolean = true): FullyQualifiedName {
    val delimiter = if (dotQualified) '.' else '/'
    val splitIndex = lastIndexOf(delimiter)
    return if (splitIndex == -1) FullyQualifiedName(packageName = null, className = this)
    else FullyQualifiedName(packageName = substring(0, splitIndex), className = substring(splitIndex + 1))
}

fun String.toDotQualified() = replace('/','.')
