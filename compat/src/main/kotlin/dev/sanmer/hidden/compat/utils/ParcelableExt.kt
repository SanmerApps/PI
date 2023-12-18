package dev.sanmer.hidden.compat.utils

import android.os.Parcel
import android.os.Parcelable
import dev.sanmer.hidden.compat.BuildCompat

internal fun <T : Parcelable> Parcel.readParcelable(
    clazz: Class<T>
): T? = if (BuildCompat.atLeastT) {
    readParcelable(clazz.classLoader, clazz)
} else {
    @Suppress("DEPRECATION")
    readParcelable(clazz.classLoader)
}