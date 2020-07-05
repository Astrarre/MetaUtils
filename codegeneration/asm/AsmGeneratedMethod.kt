package metautils.codegeneration.asm

import codegeneration.*
import codegeneration.asm.asmType
import descriptor.*
import metautils.codegeneration.GeneratedMethod
import metautils.descriptor.JvmType
import metautils.descriptor.MethodDescriptor
import metautils.descriptor.ObjectType
import metautils.descriptor.ReturnDescriptor
import metautils.util.*
import org.objectweb.asm.Opcodes
import metautils.signature.outerClass
import metautils.signature.toJvmType

private const val printStackOps = false

private class JvmState(
    private val methodName: String,
    private val methodStatic: Boolean,
    parameters: List<Pair<String,JvmType>>
) {
    fun maxStackSize(): Int {
        if (currentStack != 0) error("$currentStack element(s) on the stack were not used yet")
        if (printStackOps) {
            println("Method complete")
        }
        return maxStack
    }

    fun getVariable(variable: VariableExpression): JvmLocalVariable {
        val type = lvTable[variable.name]
        requireNotNull(type) { "Attempt to reference variable ${variable.name} when no such variable is declared" }
        return type
    }

    private val lvTable: MutableMap<String, JvmLocalVariable> = parameters.mapIndexed { i, (name, type) ->
        name to JvmLocalVariable(i.applyIf(!methodStatic) { it + 1 }, type)
    }.toMap().toMutableMap()


    // Max local variables is not tracked yet, assuming no local variables are created
    private var maxStack = 0
    private var currentStack = 0

    inline fun trackPush(amount: Int = 1, message: () -> String = { "" }) {
        val oldCurrent = currentStack
        currentStack += amount

        if (maxStack < currentStack) maxStack = currentStack

        if (printStackOps) {
            val str = message().let { if (it.isEmpty()) "operation" else it }
            println("$str: $oldCurrent -> $currentStack")
        }

    }

    inline fun trackPop(amount: Int = 1, message: () -> String = { "" }) {
        if (printStackOps) {
            val str = message().let { if (it.isEmpty()) "operation" else it }
            print("$str: ")
        }
        if (currentStack < amount) error("Attempt to pop $amount element(s) from stack when it only contains $currentStack in method $methodName")
        val oldCurrent = currentStack
        currentStack -= amount
        if (printStackOps) {
            println("$oldCurrent -> $currentStack")
        }
    }
}

private data class JvmLocalVariable(val index: Int, val type: JvmType)


const val ConstructorsName = "<init>"

internal class AsmGeneratedMethod(
    private val methodWriter: AsmClassWriter.MethodBody,
    name: String,
    isStatic: Boolean,
    parameters: List<Pair<String,JvmType>>
) : GeneratedMethod {
    fun maxStackSize(): Int = state.maxStackSize()

    private val state = JvmState(name, isStatic, parameters)


    private fun Receiver.addOpcodes() = code.addOpcodes()
    private fun Statement.addOpcodes() = code.addOpcodes()


//    private fun Expression.getVariable(): JvmLocalVariable = when(this){
//        is ArrayConstructor
//    }

    // Everything assumes we just pass the parameters in order, very simplistic, a real solution would have a variableIndexTable
    private fun Code.addOpcodes(): Unit = with(state) {
        when (this@addOpcodes) {
            is ClassReceiver -> {
            }
            SuperReceiver -> ThisExpression.code.addOpcodes()
            is ReturnStatement -> {
                // the RETURN is added by addMethod()
                target.addOpcodes()
                trackPop { "return" }
            }
            is AssignmentStatement -> {
                if (target is FieldExpression) target.receiver.addOpcodes()
                assignedValue.addOpcodes()
                target.assign()
            }
            is ConstructorCall.Super -> {
                invoke(
                    opcode = Opcodes.INVOKESPECIAL,
                    methodName = ConstructorsName,
                    isInterface = false,
                    returnType = ReturnDescriptor.Void,
                    owner = superType,
                    parameterTypes = parameters.keys,
                    parametersToLoad = ThisExpression.prependTo(parameters.values)
                )
            }
            is VariableExpression -> {
                val (index, type) = state.getVariable(this@addOpcodes)
                methodWriter.writeLvArgInstruction(type.asmType().getOpcode(Opcodes.ILOAD), index)
                trackPush { "variable get" }
            }
            is CastExpression -> {
                target.addOpcodes()
                methodWriter.writeTypeArgInstruction(Opcodes.CHECKCAST, castTo.toJvmType())
            }
            is FieldExpression -> {
                receiver.addOpcodes()
                val opcode = if (receiver is ClassReceiver) Opcodes.GETSTATIC else Opcodes.GETFIELD
                methodWriter.writeFieldArgInstruction(opcode, owner, name, type)
                if (receiver !is ClassReceiver) trackPop { "field receiver consume" }
                trackPush { "field get" }
            }
            is MethodCall -> addMethodCall()

            is ArrayConstructor -> {
                trackPush { "push array size" }
                methodWriter.writeLvArgInstruction(Opcodes.ILOAD, 0)
                // Assumes reference type array
                trackPop { "pass array size" }
                methodWriter.writeTypeArgInstruction(Opcodes.ANEWARRAY, componentClass.type.toJvmType())
                trackPush { "push array result" }
            }
            ThisExpression -> {
                trackPush { "push this" }
                methodWriter.writeLvArgInstruction(Opcodes.ALOAD, 0)
            }
        }
    }

    private fun Assignable.assign() = when (this) {
        is VariableExpression -> error("Variable expressions are not supported by the ASM generator yet") // soft to do
        is FieldExpression -> {
//            receiver.addOpcodes()
            val isStatic = receiver is ClassReceiver
            val opcode = if (isStatic) Opcodes.PUTSTATIC else Opcodes.PUTFIELD
            if (!isStatic) state.trackPop { "pass field receiver" }
            state.trackPop { "pass field value" }
            methodWriter.writeFieldArgInstruction(opcode, owner, name, type)
        }
        else -> error("Impossible")
    }

    private fun MethodAccess.invokeOpcode(isInterface: Boolean, isSuperCall: Boolean) = when {
        isStatic -> Opcodes.INVOKESTATIC
        // Final methods sometimes use INVOKESPECIAL but analyzing that is difficult, INVOKEVIRTUAL will work for our purposes.
        !isSuperCall && !visibility.isPrivate -> {
            if (isInterface) Opcodes.INVOKEINTERFACE else Opcodes.INVOKEVIRTUAL
        }
        else -> Opcodes.INVOKESPECIAL
    }

    private fun invoke(
        opcode: Int, methodName: String,
        parameterTypes: List<JvmType>,
        parametersToLoad: List<Receiver>,
        returnType: ReturnDescriptor, owner: ObjectType, isInterface: Boolean, parametersAlreadyLoaded: Int = 0
    ) {
        parametersToLoad.forEach { it.addOpcodes() }

        val descriptor = MethodDescriptor(parameterTypes, returnType)

        state.trackPop(parametersToLoad.size + parametersAlreadyLoaded) { "pass parameters" }
        if (returnType != ReturnDescriptor.Void) state.trackPush { "push method result" }
        methodWriter.writeMethodCall(opcode, owner, methodName, descriptor, isInterface)
    }

    private fun MethodCall.addMethodCall() = when (this) {
        is MethodCall.Method -> {
            val isInterface = receiverAccess.variant.isInterface
            val receiver = if (methodAccess.isStatic) null else receiver ?: ThisExpression
            invoke(
                opcode = methodAccess.invokeOpcode(isInterface, isSuperCall = receiver == SuperReceiver),
                methodName = name,
                returnType = returnType,
                owner = owner,
                isInterface = isInterface,
                parametersToLoad = parameters.values.prependIfNotNull(receiver),
                parameterTypes = parameters.keys
            )
        }
        is MethodCall.Constructor -> {
            methodWriter.writeTypeArgInstruction(Opcodes.NEW, constructing.toJvmType())
            methodWriter.writeZeroOperandInstruction(Opcodes.DUP)
            state.trackPush(2) { "NEW operation on ${constructing.toJvmType().toJvmString()}" }

            invoke(
                opcode = Opcodes.INVOKESPECIAL,
                methodName = ConstructorsName,
                // outer class is implicitly the first parameter in java, but explicitly the first parameter in bytecode
                parameterTypes = parameters.keys.applyIf(receiver != null) {
                    // Add outer class as first param to inner class
                    constructing.outerClass().toJvmType().prependTo(it)
                },
                parametersToLoad = parameters.values.prependIfNotNull(receiver), // inner class
                returnType = ReturnDescriptor.Void,
                owner = constructing.toJvmType(),
                isInterface = false,
                parametersAlreadyLoaded = 1
            )
        }
    }

    override fun addStatement(statement: Statement) {
        if (printStackOps) println("Adding statement $statement")
        statement.addOpcodes()
    }

    override fun addJavadoc(comment: String) {
    }

}