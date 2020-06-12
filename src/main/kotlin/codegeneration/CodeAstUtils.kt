package codegeneration

import api.AnyJavaType
import descriptor.JavaLangObjectJvmType
import signature.JavaLangObjectJavaType


private fun Expression.castTo(type: AnyJavaType): Expression.Cast = Expression.Cast(this, type)
fun Expression.castExpressionTo(type: AnyJavaType, doubleCast: Boolean): Expression.Cast =
    if (doubleCast) castTo(JavaLangObjectJavaType).castTo(type) else castTo(type)

