package codegeneration

import api.AnyJavaType
import descriptor.JavaLangObjectJvmType
import signature.JavaLangObjectJavaType


private fun Expression.castTo(type: AnyJavaType): Expression.Cast = Expression.Cast(this, type)
fun Expression.castExpressionTo(type: AnyJavaType, doubleCast: Boolean): Expression.Cast =
    if (doubleCast) castTo(JavaLangObjectJavaType).castTo(type) else castTo(type)

//fun Expression.returnIf(condition: Boolean): Statement = if (condition) Statement.Return(this) else this
//
//// intersection type when??
//
//fun <T : Code, R : Code> T.util.applyIf(condition: Boolean, apply: (T) -> R): Code = if (condition) apply(this) else this
//fun <T : Statement, R : Statement> T.replaceIf(condition: Boolean, replacement: R): Statement =
//    if (condition) replacement else this
//
//fun <T : Expression, R : Expression> T.replaceIf(condition: Boolean, apply: (Expression) -> R): Expression =
//    if (condition) apply(this) else this

//fun <T : Expression, R : Statement> T.util.applyIf(condition: Boolean, apply: (T) -> R): Expression =
//    if (condition) apply(this) else this
