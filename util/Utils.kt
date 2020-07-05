package metautils.util

import kotlin.contracts.ExperimentalContracts


fun String.includeIf(boolean: Boolean) = if (boolean) this else ""

fun <T> T.applyIf(boolean: Boolean, apply: (T) -> T): T {
    return if (boolean) apply(this) else this
}

fun <T, U> T.ifNotNull(obj: U?, apply: (T, U) -> T): T {
    return if (obj != null) apply(this, obj) else this
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

fun <T, R> Iterable<T>.flatMapNotNull(mapping: (T) -> Iterable<R?>): List<R> {
    val list = mutableListOf<R>()
    for (element in this) {
        mapping(element).forEach { if (it != null) list.add(it) }
    }
    return list
}

val <K, V> List<Pair<K, V>>.keys get() = map { it.first }
val <K, V> List<Pair<K, V>>.values get() = map { it.second }
fun <K, V> List<Pair<K, V>>.mapValues(mapper: (V) -> V) = map { (k, v) -> k to mapper(v) }

inline fun <T> recursiveList(seed: T?, getter: (T) -> T?): List<T> {
    val list = mutableListOf<T>()
    var current: T? = seed
    while (current != null) {
        list.add(current)
        current = getter(current)
    }

    return list
}