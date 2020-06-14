package util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


fun String.includeIf(boolean: Boolean) = if (boolean) this else ""
@OptIn(ExperimentalContracts::class)
fun <T> T.applyIf(boolean: Boolean, apply: (T) -> T): T {
    return if (boolean) apply(this) else this
}
fun <T> List<T>.appendIfNotNull(value: T?) = if (value == null) this else this + value
fun <T> List<T>.prependIfNotNull(value: T?) = value?.prependTo(this) ?: this
fun <T> T.singletonList() = listOf(this)

fun <T : Any?> T.prependTo(list: List<T>): List<T> {
    val appendedList = ArrayList<T>(list.size + 1)
    appendedList.add(this)
    appendedList.addAll(list)
    return appendedList
}