package dev.sanmer.pi.factory

import android.content.Context
import android.text.format.Formatter
import dev.sanmer.pi.R

class VersionFactory(
    private val context: Context
) {
    private val Pair<Any, String?>.value inline get() = second ?: first.toString()

    private infix fun <T : Comparable<T>> Pair<T, String?>.compare(
        other: Pair<T, String?>
    ) = when {
        first == other.first -> value
        else -> context.getString(R.string.comparator, value, other.value)
    }

    private infix fun <T : Comparable<T>> T.compare(
        other: T
    ) = when {
        this == other -> toString()
        else -> context.getString(R.string.comparator, toString(), other.toString())
    }

    fun sdkVersions(target: Int, min: Int, compile: Int): String {
        return context.getString(
            R.string.sdk_versions,
            target.orUnknown, min.orUnknown, compile.orUnknown
        )
    }

    fun versionDiff(that: Pair<Long, String>, other: Pair<Long, String>): String {
        if (that.first <= 0) return other.version
        if (other.first <= 0) return that.version
        return (that.first to that.version) compare (other.first to other.version)
    }

    fun sdkVersionsDiff(
        target: Pair<Int, Int>,
        min: Pair<Int, Int>,
        compile: Pair<Int, Int>
    ): String {
        if (min.first <= 0) return sdkVersions(target.second, min.second, compile.second)
        if (min.second <= 0) return sdkVersions(target.first, min.first, compile.first)
        return context.getString(
            R.string.sdk_versions,
            target.first compare target.second,
            min.first compare min.second,
            compile.first compare compile.second
        )
    }

    fun fileSize(sizeBytes: Long): String {
        val value = Formatter.formatFileSize(context, sizeBytes)
        return context.getString(R.string.file_size, value)
    }

    companion object Default {
        val Pair<Long, String>.version inline get() = "$second ($first)"

        val Int.orUnknown inline get() = if (this > 0) "$this" else "?"
    }
}