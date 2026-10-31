package dev.sanmer.pi.core.res

import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import dev.sanmer.pi.core.compat.PackageInfoCompat
import org.xmlpull.v1.XmlPullParser

object ResourceParser {
    const val NS_RESOURCES = "http://schemas.android.com/apk/res/android"
    const val TAG_MANIFEST = "manifest"
    const val TAG_USES_SDK = "uses-sdk"
    const val TAG_APPLICATION = "application"

    inline fun <reified P : XmlResourceParser> P.nextOrNull() =
        next().takeIf { it != XmlPullParser.END_DOCUMENT }

    inline fun <reified P : XmlResourceParser> P.foldAndroidManifest(
        onManifest: P.() -> Unit,
        onUsesSdk: P.() -> Unit,
        onApplication: P.() -> Unit
    ) {
        while (true) {
            val eventType = nextOrNull() ?: break
            if (eventType != XmlPullParser.START_TAG) continue
            when (name) {
                TAG_MANIFEST -> onManifest(this)
                TAG_USES_SDK -> onUsesSdk(this)
                TAG_APPLICATION -> onApplication(this)
            }
        }
    }

    inline fun <reified P : XmlResourceParser> P.getSplitConfigLite(): SplitConfigLite {
        var packageName: String? = null
        var splitName: String? = null
        var configForSplit: String? = null
        var versionCode = -1
        var isFeatureSplit = false
        val requiredSplitTypes = mutableListOf<String>()

        foldAndroidManifest(
            onManifest = {
                packageName = getAttributeValue(null, "package")
                splitName = getAttributeValue(null, "split")
                configForSplit = getAttributeValue(null, "configForSplit")
                versionCode = getAttributeIntValue(NS_RESOURCES, "versionCode", 0)
                isFeatureSplit = getAttributeBooleanValue(NS_RESOURCES, "isFeatureSplit", false)
                getAttributeValue(NS_RESOURCES, "requiredSplitTypes")?.let {
                    requiredSplitTypes.addAll(it.split(','))
                }
            },
            onUsesSdk = {},
            onApplication = {}
        )

        require(packageName != null) { "Expect packageName" }
        require(splitName != null) { "Expect splitName" }
        require(versionCode > 0) { "Expect versionCode" }
        return SplitConfigLite(
            packageName = packageName,
            splitName = splitName,
            configForSplit = configForSplit.orEmpty(),
            versionCode = versionCode,
            isFeatureSplit = isFeatureSplit,
            requiredSplitTypes = requiredSplitTypes.toList()
        )
    }

    fun interface BitmapFactory {
        fun toBitmap(drawable: Drawable): Bitmap
    }

    inline fun <reified P : XmlResourceParser> P.getPackageInfoLite(
        resources: Resources,
        factory: BitmapFactory
    ): PackageInfoLite {
        var packageName: String? = null
        var versionCode = -1
        var versionCodeMajor = -1
        var versionName: String? = null
        var compileSdkVersion = -1
        var minSdkVersion = -1
        var targetSdkVersion = -1
        var label: String? = null
        var icon: Bitmap? = null

        foldAndroidManifest(
            onManifest = {
                packageName = getAttributeValue(null, "package")
                versionCode = getAttributeIntValue(NS_RESOURCES, "versionCode", 0)
                versionCodeMajor = getAttributeIntValue(NS_RESOURCES, "versionCodeMajor", 0)
                versionName = getAttributeValue(NS_RESOURCES, "versionName")
                compileSdkVersion = getAttributeIntValue(NS_RESOURCES, "compileSdkVersion", 0)
            },
            onUsesSdk = {
                minSdkVersion = getAttributeIntValue(NS_RESOURCES, "minSdkVersion", 0)
                targetSdkVersion = getAttributeIntValue(NS_RESOURCES, "targetSdkVersion", 0)
            },
            onApplication = {
                val labelId = getAttributeResourceValue(NS_RESOURCES, "label", 0)
                if (labelId == 0) {
                    label = getAttributeValue(NS_RESOURCES, "label")
                } else runCatching {
                    label = resources.getString(labelId)
                }
                val iconId = getAttributeResourceValue(NS_RESOURCES, "icon", 0)
                if (iconId != 0) runCatching {
                    icon = factory.toBitmap(resources.getDrawable(iconId, null))
                }
            }
        )

        require(packageName != null) { "Expect packageName" }
        require(versionCode > 0) { "Expect versionCode" }
        return PackageInfoLite(
            packageName = packageName,
            versionCode = PackageInfoCompat.composeLongVersionCode(versionCodeMajor, versionCode),
            versionName = versionName.orEmpty(),
            compileSdkVersion = compileSdkVersion,
            minSdkVersion = minSdkVersion,
            targetSdkVersion = targetSdkVersion,
            label = label,
            icon = icon
        )
    }
}