package codegeneration

import descriptor.AnyType

sealed class Code
// all implementors of Receiver MUST inherit Code
interface Receiver

class ClassReceiver(val type: AnyType) : Code(), Receiver
object SuperReceiver :   Code(), Receiver

sealed class Statement : Code() {
    class Return(val target: Expression) : Statement()
}

sealed class Expression : Statement(), Receiver {
    class Variable(val name: String) : Expression()
    class Cast(val target: Expression, val castTo: AnyType) : Expression()
    class Field(val owner: Statement, val name: String) : Expression()
    sealed class Call(val parameters: List<Expression>) : Expression() {
        class This(parameters: List<Expression>) : Call(parameters)
        class Super(parameters: List<Expression>) : Call(parameters)
        class Method(val receiver: Receiver?, val name: String, parameters: List<Expression>) : Call(parameters)
        class Constructor(val constructing: AnyType, parameters: List<Expression>) : Call(parameters)
    }

    object This : Expression()
}