package api

import descriptor.Descriptor

fun <T : GenericJavaType> T.remap(mapper: (className: String) -> String?): T = TODO()
typealias SuperType = JavaType.Class.SuperType
