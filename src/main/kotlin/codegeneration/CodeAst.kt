package codegeneration

import descriptor.JvmType

sealed class Code

// all implementors of Receiver MUST inherit Code
interface Receiver

class ClassReceiver(val type: JvmType) : Code(), Receiver
object SuperReceiver : Code(), Receiver

sealed class Statement : Code() {
    class Return(val target: Expression) : Statement()
    class Assignment(val target: Expression, val assignedValue: Expression) : Statement()
}

sealed class Expression : Statement(), Receiver {
    class Variable(val name: String) : Expression()
    class Cast(val target: Expression, val castTo: JvmType) : Expression()
    class Field(val owner: Receiver, val name: String) : Expression()
    sealed class Call(val parameters: List<Expression>) : Expression() {
        class This(parameters: List<Expression>) : Call(parameters)
        class Super(parameters: List<Expression>) : Call(parameters)
        class Method(val receiver: Receiver?, val name: String, parameters: List<Expression>) : Call(parameters)
        class Constructor(val receiver: Expression?, val constructing: JvmType, parameters: List<Expression>) :
            Call(parameters)
    }

    object This : Expression()
}