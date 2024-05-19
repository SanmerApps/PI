package dev.sanmer.hidden.compat

import dev.sanmer.hidden.compat.delegate.ContextDelegate

internal object Const {
    const val TIMEOUT_MILLIS = 15_000L

    const val VERSION_CODE = 50

    val PACKAGE_NAME: String by lazy {
        val context = ContextDelegate.getContext()
        context.packageName
    }
}