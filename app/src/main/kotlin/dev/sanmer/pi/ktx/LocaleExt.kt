package dev.sanmer.pi.ktx

import java.util.Locale

val Locale.localizedDisplayName: String
    inline get() = getDisplayName(this)
        .replaceFirstChar {
            if (it.isLowerCase()) {
                it.titlecase(this)
            } else {
                it.toString()
            }
        }