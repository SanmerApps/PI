package dev.sanmer.pi.appiconloader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.annotation.Px
import dev.sanmer.pi.ContextCompat
import dev.sanmer.pi.ContextCompat.userId
import me.zhanghai.android.appiconloader.iconloaderlib.BaseIconFactory
import me.zhanghai.android.appiconloader.iconloaderlib.BitmapInfo

class AppIconLoader(
    @field:Px private val iconSize: Int,
    private val shrinkNonAdaptiveIcons: Boolean,
    context: Context = ContextCompat.getContext()
) {
    private val iconFactory = IconFactory(iconSize, context)
    private val user = UserHandle.getUserHandleForUid(context.userId)

    fun loadIcon(unbadgedIcon: Drawable): Bitmap {
        return iconFactory.createBadgedIconBitmap(
            unbadgedIcon, user, shrinkNonAdaptiveIcons, false
        ).icon
    }

    private class IconFactory(
        @Px iconBitmapSize: Int,
        context: Context
    ) : BaseIconFactory(
        context,
        context.resources.configuration.densityDpi,
        iconBitmapSize,
        true
    ) {
        private val mTempScale = FloatArray(1)

        init {
            disableColorExtraction()
        }

        fun createBadgedIconBitmap(
            icon: Drawable, user: UserHandle?,
            shrinkNonAdaptiveIcons: Boolean,
            isInstantApp: Boolean
        ): BitmapInfo {
            return super.createBadgedIconBitmap(
                icon, user, shrinkNonAdaptiveIcons, isInstantApp, mTempScale
            )
        }
    }
}