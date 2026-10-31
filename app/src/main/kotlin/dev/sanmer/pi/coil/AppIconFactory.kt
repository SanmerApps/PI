package dev.sanmer.pi.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.compose.ui.unit.Dp
import dev.sanmer.pi.core.compat.ContextCompat.userId
import dev.sanmer.pi.core.res.ResourceParser
import me.zhanghai.android.appiconloader.iconloaderlib.BaseIconFactory

class AppIconFactory(
    context: Context,
    iconSize: Dp,
    private val shrinkNonAdaptiveIcons: Boolean
) : BaseIconFactory(
    context,
    context.resources.configuration.densityDpi,
    (iconSize.value * context.resources.displayMetrics.density).toInt(),
    true
), ResourceParser.BitmapFactory {
    private val user = UserHandle.getUserHandleForUid(context.userId)
    private val scale = floatArrayOf(0f)

    init {
        disableColorExtraction()
    }

    override fun toBitmap(drawable: Drawable): Bitmap = createBadgedIconBitmap(
        drawable,
        user,
        shrinkNonAdaptiveIcons,
        false,
        scale
    ).icon
}