package dev.sanmer.pi.ktx

import java.util.Locale

fun Long.formatFileSize() = when {
    this >= 1073741824 -> String.format(
        locale = Locale.getDefault(),
        format = "%.2f GiB",
        this / 1073741824.0
    )

    this >= 1048576 -> String.format(
        locale = Locale.getDefault(),
        format = "%.2f MiB",
        this / 1048576.0
    )

    this >= 1024 -> String.format(
        locale = Locale.getDefault(),
        format = "%.2f KiB",
        this / 1024.0
    )

    else -> "$this B"
}

fun Int.formatFileSize() = toLong().formatFileSize()