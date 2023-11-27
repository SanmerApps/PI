package dev.sanmer.pi.compat

import android.content.Context
import android.content.ContextHidden
import dev.rikka.tools.refine.Refine

object ContextCompat {
    val Context.userId get() =
        Refine.unsafeCast<ContextHidden>(this)
            .userId
}