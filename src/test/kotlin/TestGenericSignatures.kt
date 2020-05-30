import org.junit.jupiter.api.Test
import sun.reflect.generics.parser.SignatureParser
import testing.getResource

class TestGenericSignatures {
    @Test
    fun test() {
        val classFile = readToClassNode(getResource("GenericSignatureTest.class"))
        val signature = classFile.signature
        val parsed =SignatureParser.make().parseClassSig(signature)

        val x= 2
    }
}