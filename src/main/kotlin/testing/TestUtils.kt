package testing

import java.nio.file.Path
import java.nio.file.Paths

private class DummyClass
fun getResource(path: String): Path = Paths.get(
    DummyClass::class.java
        .classLoader.getResource("dummyResource")!!.toURI()
).parent.resolve(path)