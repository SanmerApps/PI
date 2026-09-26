package dev.sanmer.pi.core.res

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.UserHandle
import dev.sanmer.pi.core.compat.ContextCompat.userId
import me.zhanghai.android.appiconloader.iconloaderlib.BaseIconFactory

class AppIconFactory(
    iconSize: Int,
    context: Context
) : BaseIconFactory(
    context,
    context.resources.configuration.densityDpi,
    (iconSize * context.resources.displayMetrics.density).toInt(),
    true
) {
    private val user = UserHandle.getUserHandleForUid(context.userId)
    private val scale = floatArrayOf(0f)

    init {
        disableColorExtraction()
    }

    fun createBadgedIcon(
        icon: Drawable,
        shrinkNonAdaptiveIcons: Boolean = false
    ): Bitmap = createBadgedIconBitmap(icon, user, shrinkNonAdaptiveIcons, false, scale).icon
}