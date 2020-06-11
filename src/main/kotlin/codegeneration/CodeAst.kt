package codegeneration

import api.AnyJavaType
import api.JavaClassType
import descriptor.JvmType

sealed class Code {
    override fun toString(): String  = JavaCodeWriter().write(this).string
}

// all implementors of Receiver MUST inherit Code
interface Receiver

class ClassReceiver(val type: JavaClassType) : Code(), Receiver
object SuperReceiver : Code(), Receiver

sealed class Statement : Code() {
    class Return(val target: Expression) : Statement()
    class Assignment(val target: Expression, val assignedValue: Expression) : Statement()
}

sealed class Expression : Statement(), Receiver {
    class Variable(val name: String) : Expression()
    class Cast(val target: Expression, val castTo: AnyJavaType) : Expression()
    class Field(val owner: Receiver, val name: String) : Expression()
    sealed class Call(val parameters: List<Expression>) : Expression() {
        class This(parameters: List<Expression>) : Call(parameters)
        class Super(parameters: List<Expression>) : Call(parameters)
        class Method(val receiver: Receiver?, val name: String, parameters: List<Expression>) : Call(parameters)
        class Constructor(val receiver: Expression?, val constructing: JavaClassType, parameters: List<Expression>) :
            Call(parameters)
    }
    class ArrayConstructor(val componentClass : JavaClassType, val size: Expression) : Expression()

    object This : Expression()
}