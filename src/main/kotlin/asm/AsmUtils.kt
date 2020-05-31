package asm

import codegeneration.ClassVisibility
import codegeneration.Visibility
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

val AsmNode<*>.isInitializer
    get() = when (this) {
        is ClassAsmNode -> false
        is FieldAsmNode -> false
        is MethodAsmNode -> node.desc == "()V" && (node.name == "<init>" || node.name == "<clinit>")
    }

val MethodNode.isConstructor get() = name == "<init>"
val MethodNode.isVoid get() = desc == "()V"
val MethodNode.isInstanceInitializer get() = isVoid && isConstructor
val MethodNode.isStaticInitializer get() = name == "<clinit>"

fun AsmNode<*>.hasAnnotation(annotation: String) = annotations.any { it.desc == annotation }

private infix fun Int.opCode(code: Int): Boolean = (this and code) != 0

infix fun MethodNode.opCode(code: Int) = access opCode code
infix fun FieldNode.opCode(code: Int) = access opCode code
infix fun ClassNode.opCode(code: Int) = access opCode code


private val Int.static get() = opCode(Opcodes.ACC_STATIC)
private val Int.private get() = opCode(Opcodes.ACC_PRIVATE)
private val Int.protected get() = opCode(Opcodes.ACC_PROTECTED)
private val Int.public get() = opCode(Opcodes.ACC_PUBLIC)
private val Int.packagePrivate get() = !private && !protected && !public
private val Int.visibility: Visibility
    get() = when {
        private -> ClassVisibility.Private
        protected -> Visibility.Protected
        public -> ClassVisibility.Public
        packagePrivate -> ClassVisibility.Package
        else -> error("Access is unexpectedly not private, protected, public, or package private...")
    }

val MethodNode.isStatic get() = access.static
val MethodNode.isPrivate get() = access.private
val MethodNode.isProtected get() = access.protected
val MethodNode.isPublic get() = access.public
val MethodNode.isPackagePrivate get() = access.packagePrivate
val MethodNode.visibility: Visibility get() = access.visibility

val FieldNode.isStatic get() = access.static
val FieldNode.isPrivate get() = access.private
val FieldNode.isProtected get() = access.protected
val FieldNode.isPublic get() = access.public
val FieldNode.isPackagePrivate get() = access.packagePrivate
val FieldNode.visibility: Visibility get() = access.visibility


val ClassNode.isInterface get() = opCode(Opcodes.ACC_INTERFACE)
val ClassNode.isAbstract get() = opCode(Opcodes.ACC_ABSTRACT)
val ClassNode.isEnum get() = opCode(Opcodes.ACC_ENUM)
val ClassNode.isAnnotation get() = opCode(Opcodes.ACC_ANNOTATION)
val ClassNode.visibility: ClassVisibility
    get() = with(access) {
        when {
            protected -> error("Class access is unexpectedly protected")
            private -> ClassVisibility.Private
            public -> ClassVisibility.Public
            packagePrivate -> ClassVisibility.Package
            else -> error("Access is unexpectedly not private, protected, public, or package private...")
        }
    }
