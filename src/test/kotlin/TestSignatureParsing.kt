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
        testClass(classSignature, classNode.signature)

        val method1Sig = classNode.methods[1].signature
        val method2Sig = classNode.methods[2].signature
        val method1Signature = MethodSignature.readFrom(method1Sig)
        val method2Signature = MethodSignature.readFrom(method2Sig)

        testMethod(method1Signature,method1Sig)
        testMethod(method2Signature,method2Sig)

        val field1Signature = FieldSignature.readFrom(classNode.fields[0].signature)
        val field2Signature = FieldSignature.readFrom(classNode.fields[1].signature)
        val x = 2
    }

    private fun testClass(classSignature: ClassSignature, signature : String) {
        val asString = classSignature.toClassfileName()
        val backToClass = ClassSignature.readFrom(asString)
        val asStringAgain = backToClass.toClassfileName()

        assertEquals(classSignature, backToClass)
        assertEquals(signature, asString)
        assertEquals(asString, asStringAgain)
    }


    private fun testMethod(methodSignature: MethodSignature, signature : String) {
        val asString = methodSignature.toClassfileName()
        val backToClass = MethodSignature.readFrom(asString)
        val asStringAgain = backToClass.toClassfileName()

        assertEquals(methodSignature, backToClass)
        assertEquals(signature, asString)
        assertEquals(asString, asStringAgain)
    }

}