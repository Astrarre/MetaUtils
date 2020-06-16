package signature

import util.PackageName

fun ClassSignature.Companion.readFrom(signature: String): ClassSignature = SignatureReader(signature).readClass()
fun MethodSignature.Companion.readFrom(signature: String): MethodSignature = SignatureReader(signature).readMethod()
fun GenericType.Companion.readFrom(signature: String): FieldSignature = SignatureReader(signature).readField()

private const val doChecks = true

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalStdlibApi::class)
private class SignatureReader(private val signature: String) {
    var progressPointer = 0


    fun readClass(): ClassSignature {
        check { signature.isNotEmpty() }

        val typeParamsMarker = signature[0]
        val formalTypeParameters = if (typeParamsMarker == '<') {
            readFormalTypeParameters()
        } else null
        val superClassSignature = readClassTypeSignature()
        val superInterfaceSignatures = readRepeatedly(
            until = { progressPointer == signature.length },
            reader = { readClassTypeSignature() },
            skip = false
        )
        return ClassSignature(formalTypeParameters, superClassSignature, superInterfaceSignatures)
    }

    fun readMethod(): MethodSignature {
        val formalTypeParameters = if (current() == '<') readFormalTypeParameters() else null
        advance('(')
        val parameterTypes = readRepeatedly(until = { current() == ')' }, reader = { readTypeSignature() }, skip = true)
        val returnType = if (current() == 'V') GenericReturnType.Void.also { advance() } else readTypeSignature()
        val throws = readRepeatedly(
            until = { progressPointer == signature.length },
            reader = { readThrowsSignature() },
            skip = false
        )
        return MethodSignature(formalTypeParameters, parameterTypes, returnType, throws)
    }

    fun readField(): FieldSignature = readFieldTypeSignature()

    private fun readThrowsSignature(): ThrowableType {
        advance('^')
        return when (current()) {
            'L' -> readClassTypeSignature()
            'T' -> readTypeVariableSignature()
            else -> error("Unrecognized throwable type prefix: ${current()}")
        }
    }

    private fun readFormalTypeParameters(): List<TypeArgumentDeclaration> {
        advance('<')
        val args = readRepeatedly(until = { current() == '>' }, reader = { readFormalTypeParameter() }, skip = true)
        check { args.isNotEmpty() }
        return args
    }

    private fun readFormalTypeParameter(): TypeArgumentDeclaration {
        val identifier = readUntil(':', skip = true)
        val current = current()
        val classBound = if (current.let { it == 'L' || it == '[' || it == 'T' }) {
            readFieldTypeSignature()
        } else null
        val interfaceBounds = readRepeatedly(
            until = { current() != ':' },
            reader = {
                advance(':')
                readFieldTypeSignature()
            }, skip = false
        )
        return TypeArgumentDeclaration(identifier, classBound, interfaceBounds)
    }


    private fun readClassTypeSignature(): ClassGenericType {
        advance('L')
        val packageSpecifier = readPackageSpecifier()
        val classNameChain =
            readRepeatedly(until = { current() == ';' }, reader = { readSimpleClassTypeSignature() }, skip = true)

        check { classNameChain.isNotEmpty() }
        return ClassGenericType(packageSpecifier, classNameChain)
    }

    private fun readSimpleClassTypeSignature(): SimpleClassGenericType {
        val identifier = readUntil(until = { it == '<' || it == '$' || it == ';' }, skip = false)
        val typeArguments = if (current() == '<') readTypeArguments()
        else {
            if (current() == '$') advance()
            null
        }
        return SimpleClassGenericType(identifier, typeArguments)
    }

    private fun readTypeArguments(): List<TypeArgument> {
        advance('<')
        val args = readRepeatedly(until = { current() == '>' }, reader = { readTypeArgument() }, skip = true)
        check { args.isNotEmpty() }
        return args
    }

    private fun readTypeArgument(): TypeArgument {
        if (current() == '*') {
            advance()
            return TypeArgument.AnyType
        }
        val wildcardIndicator = readWildcardIndicator()
        val fieldTypeSignature = readFieldTypeSignature()
        return TypeArgument.SpecificType(fieldTypeSignature, wildcardIndicator)
    }

    private fun readWildcardIndicator(): WildcardType? = when (current()) {
        '+' -> WildcardType.Extends
        '-' -> WildcardType.Super
        else -> null
    }.also { if (it != null) advance() }

    private fun readFieldTypeSignature(): GenericType = when (current()) {
        'L' -> readClassTypeSignature()
        '[' -> readArrayTypeSignature()
        'T' -> readTypeVariableSignature()
        else -> error("Unrecognized field type signature prefix: ${current()}")
    }

    private fun readArrayTypeSignature(): ArrayGenericType {
        advance('[')
        return ArrayGenericType(readTypeSignature())
    }

    private fun readTypeSignature(): GenericTypeOrPrimitive {
        readPrimitiveSignature()?.let { return it }
        return readFieldTypeSignature()
    }

    private fun readPrimitiveSignature(): GenericsPrimitiveType? = baseTypesGenericsMap[current()]?.also { advance() }

    private fun readTypeVariableSignature(): TypeVariable {
        advance('T')
        val identifier = readUntil(';', skip = true)
        return TypeVariable(identifier)
    }

    private fun readPackageSpecifier(): PackageName? {
        val packageEnd = findLastPackageSeparator()
        if (packageEnd == -1) return null
        return PackageName(
            buildList {
                readRepeatedly(
                    until = { progressPointer > packageEnd },
                    reader = { add(readUntil('/', skip = true)) },
                    skip = false
                )
            }
        )
    }

    //    // signature <T:LInnerClassGenericTest.InnerClass<Ljava/util/ArrayList<Ljava/lang/String;>;>;>Ljava/lang/Object;
//// declaration: InnerClassGenericTest<T extends InnerClassGenericTest.InnerClass<java.util.ArrayList<java.lang.String>>>
    private fun findLastPackageSeparator(): Int {
        var currentIndex = progressPointer
        var lastSeparator = -1
        do {
            val current = signature[currentIndex]
            if (current == '/') lastSeparator = currentIndex
            currentIndex++
        } while (
        // Got to start of some generic parameter, means package specifier is over, e.g. foo/bar/List<String>
            current != '<' &&
            // Got to some inner class, e.g. foo/bar/Baz$Inner
            current != '$' &&
            // Got to the end of a class, e.g. foo/bar/Baz;
            current != ';'
        )
        return lastSeparator
    }

    private inline fun current() = signature[progressPointer]
    private inline fun advance() = progressPointer++
    private inline fun advance(checkChar: Char) {
        check(message = {
            "Expected $checkChar at position $progressPointer, but was ${current()} instead"
        }) { current() == checkChar }
        advance()
    }

    private inline fun check(message: () -> String = { "" }, check: () -> Boolean) {
        if (doChecks) {
            if (message().isNotEmpty()) check(check(), message)
            else check(check())
        }
    }

    private inline fun readUntil(until: (Char) -> Boolean, skip: Boolean): String {
        val start = progressPointer
        while (!until(current())) {
            advance()
        }
        val result = signature.substring(start, progressPointer)
        if (skip) advance()
        return result
    }

    private fun readUntil(symbol: Char, skip: Boolean): String = readUntil({ it == symbol }, skip)

    private inline fun <T> readRepeatedly(until: () -> Boolean, reader: () -> T, skip: Boolean): List<T> = buildList {
        while (!until()) {
            add(reader())
        }
        if (skip) advance()
    }
}



