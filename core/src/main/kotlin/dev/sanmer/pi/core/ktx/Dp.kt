package dev.sanmer.pi.core.ktx

import android.content.res.Resources

internal val Int.dp get() = times(Resources.getSystem().displayMetrics.density).toInt()