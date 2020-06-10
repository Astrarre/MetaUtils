import asm.readToClassNode
import org.junit.jupiter.api.Test
import signature.ClassSignature
import signature.FieldSignature
import signature.MethodSignature
import signature.readFrom
import testing.getResource

class TestSignatureParsing {
    @Test
    fun testSignatures() {
        val classNode = readToClassNode(getResource("GenericsTest.class"))
        val classSignature = ClassSignature.readFrom(classNode.signature)
        val method1Signature = MethodSignature.readFrom(classNode.methods[1].signature)
        val method2Signature = MethodSignature.readFrom(classNode.methods[2].signature)

        val field1Signature = FieldSignature.readFrom(classNode.fields[0].signature)
        val field2Signature = FieldSignature.readFrom(classNode.fields[1].signature)
        val x = 2
    }
}