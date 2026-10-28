package dev.sanmer.pi.ktx

inline fun <T> MutableList<T>.removeLastIf(predicate: (T) -> Boolean) =
    isNotEmpty() && if (predicate(get(lastIndex))) {
        removeAt(lastIndex)
        true
    } else {
        false
    }