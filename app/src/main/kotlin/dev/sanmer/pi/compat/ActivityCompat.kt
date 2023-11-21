package dev.sanmer.pi.compat

import android.annotation.SuppressLint
import android.app.Activity

object ActivityCompat {
    @SuppressLint("DiscouragedPrivateApi", "PrivateApi")
    fun getReferrer(activity: Activity): String? {
        return Activity::class.java
            .getDeclaredField("mReferrer")
            .apply { isAccessible = true }
            .get(activity) as? String
    }
}