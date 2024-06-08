package dev.sanmer.pi

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

internal object BuildCompat {
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    val atLeastU get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val atLeastT get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val atLeastS get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    val atLeastR get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
}