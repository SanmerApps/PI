package dev.sanmer.pi.ktx

val Throwable.messageOrName: String
    inline get(): String = message ?: javaClass.name