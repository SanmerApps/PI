package dev.sanmer.hidden.compat.delegate

import android.app.ActivityThread
import android.content.Context
import android.content.ContextWrapper

internal object ContextDelegate {
    fun getContext(): Context {
        var context: Context = ActivityThread.currentApplication()
        while (context is ContextWrapper) {
            context = context.baseContext
        }

        return context
    }
}