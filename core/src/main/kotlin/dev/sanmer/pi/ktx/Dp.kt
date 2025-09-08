package dev.sanmer.pi.ktx

import android.content.res.Resources

val Int.dp get() = times(Resources.getSystem().displayMetrics.density).toInt()