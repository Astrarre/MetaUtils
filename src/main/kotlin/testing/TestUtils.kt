package testing

import util.directChildren
import util.exists
import java.lang.reflect.Member
import java.lang.reflect.Modifier
import java.nio.file.Path
import java.nio.file.Paths

private class DummyClass

fun getResource(path: String): Path = Paths.get(
    DummyClass::class.java
        .classLoader.getResource("dummyResource")!!.toURI()
).parent.resolve(path)
    .also { check(it.exists()) {
            "Resource '$path' at $it does not exist. Other resources in resources directory ${it.parent}: " + it.parent.directChildren().toList()
    } }


typealias Something = ArrayList<*>

//@DslMarker
//annotation class JarDsl

//@JarDsl
//fun testJars(vararg jars:  Path, init: TestJar.() -> Unit): TestJar {
//    return TestJar(
//        URLClassLoader(
//            jars.map { it.toUri().toURL() }.toTypedArray(),
//            DummyClass::class.java.classLoader
//        )
//    ).apply(init)
//}

//@JarDsl
//class TestJar(private val classLoader: ClassLoader) {
//    fun inClass(className: String, init: TestClass.() -> Unit = {}) =
//        TestClass(Class.forName(className, true, classLoader), classLoader).apply(init)
//
////    fun instance()
//}

//val Member.isPublic get() = Modifier.isPublic(this.modifiers)
//val Member.isPrivate get() = Modifier.isPrivate(this.modifiers)

//interface ClassContext {
//    val clazz: Class<*>
//    val instance: Any?
//
//    fun field(name: String) = clazz.getDeclaredField(name)
//    fun method(name: String, argTypes: List<Class<*>> = listOf()) = try {
//        clazz.getDeclaredMethod(name, *argTypes.toTypedArray())
//    } catch (e: NoSuchMethodException) {
//        clazz.getMethod(name, *argTypes.toTypedArray())
//    }
//
//    operator fun <T> String.invoke(argTypes: List<Class<*>> = listOf(), vararg args: Any?) =
//        method(this, argTypes).let {
//            it.isAccessible = true
//            it.invoke(instance, *args)
//        } as T
//
//    operator fun <T> get(name: String) = field(name).let {
//        it.isAccessible = true
//        it.get(instance)
//    } as T
//
//    operator fun set(name: String, value: Any) = field(name).let {
//        it.isAccessible = true
//        it.set(instance, value)
//    }
//}
//
//@JarDsl
//class TestClass(override val clazz: Class<*>, val classLoader: ClassLoader) : ClassContext {
//    override val instance: Any? = null
//    inline fun withInstance(className: String? = null, init: TestInstance.() -> Unit = {}) = TestInstance(clazz,
//        className?.let { Class.forName(className, true, classLoader) } ?: clazz.newInstance()
//    ).apply(init)
//}
//
//@JarDsl
//class TestInstance(override val clazz: Class<*>, override val instance: Any) : ClassContext