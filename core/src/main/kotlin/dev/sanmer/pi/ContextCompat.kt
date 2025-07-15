package dev.sanmer.pi

import android.app.ActivityThread
import android.content.Context
import android.content.ContextHidden
import android.content.ContextWrapper
import dev.rikka.tools.refine.Refine

object ContextCompat {
    val Context.userId
        get() = Refine.unsafeCast<ContextHidden>(this).userId

    fun getContext(): Context {
        var context: Context = ActivityThread.currentApplication()
        while (context is ContextWrapper) {
            context = context.baseContext
        }

        return context
    }
}