import asm.readToClassNode
import org.junit.jupiter.api.Test
import signature.*
import testing.getResource
import kotlin.test.assertEquals

class TestSignatureParsing {
    @Test
    fun testSignatures() {
        val classNode = readToClassNode(getResource("GenericsTest.class"))
        val classSignature = ClassSignature.readFrom(classNode.signature)
        val asString = classSignature.toClassfileName()
        val backToClass = ClassSignature.readFrom(asString)
        val asStringAgain = backToClass.toClassfileName()

        assertEquals(classSignature,backToClass)
        assertEquals(classNode.signature, asString)
        assertEquals(asString, asStringAgain)

        val method1Signature = MethodSignature.readFrom(classNode.methods[1].signature)
        val method2Signature = MethodSignature.readFrom(classNode.methods[2].signature)

        val field1Signature = FieldSignature.readFrom(classNode.fields[0].signature)
        val field2Signature = FieldSignature.readFrom(classNode.fields[1].signature)
        val x = 2
    }
}