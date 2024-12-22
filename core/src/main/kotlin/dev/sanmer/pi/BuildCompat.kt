package dev.sanmer.pi

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

internal object BuildCompat {
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    val atLeastV inline get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    val atLeastU inline get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val atLeastT inline get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val atLeastS inline get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}