package dev.sanmer.pi.res

interface Wrapper<T> : AutoCloseable {
    fun get(): T
}