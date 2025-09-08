package dev.sanmer.pi.parser

import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import dev.sanmer.pi.appiconloader.AppIconLoader
import dev.sanmer.pi.ktx.dp
import dev.sanmer.pi.ktx.toBitmap
import org.xmlpull.v1.XmlPullParser

internal object ResourceParser {
    const val ANDROID_MANIFEST = "AndroidManifest.xml"
    const val ANDROID_RESOURCES = "http://schemas.android.com/apk/res/android"
    const val TAG_MANIFEST = "manifest"
    const val TAG_USES_SDK = "uses-sdk"
    const val TAG_APPLICATION = "application"

    val appIconLoader by lazy { AppIconLoader(40.dp, true) }
    var enableAdaptiveIcons = true

    fun Drawable.toIcon(): Bitmap {
        return if (enableAdaptiveIcons) {
            appIconLoader.loadIcon(this)
        } else {
            toBitmap()
        }
    }

    fun XmlResourceParser.nextOrNull(): Int? {
        return next().takeIf { it != XmlPullParser.END_DOCUMENT }
    }

    fun XmlResourceParser.getAttributeValue(
        namespace: String?, name: String, defaultValue: String
    ): String {
        return getAttributeValue(namespace, name) ?: defaultValue
    }

    fun XmlResourceParser.getAttributeResStringValue(
        res: Resources, name: String
    ): String? {
        val resId = getAttributeResourceValue(ANDROID_RESOURCES, name, 0)
        return runCatching { res.getString(resId) }.getOrNull()
    }

    fun XmlResourceParser.getAttributeResDrawableValue(
        res: Resources, name: String
    ): Drawable? {
        val resId = getAttributeResourceValue(ANDROID_RESOURCES, name, 0)
        return runCatching { res.getDrawable(resId, null) }.getOrNull()
    }

    inline fun <reified P : XmlResourceParser> P.split(
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

    fun parseSplit(parser: XmlResourceParser): SplitConfigLite {
        var packageName = ""
        var splitName = ""
        var configForSplit = ""
        var versionCode = -1
        var isFeatureSplit = false

        parser.split(
            onManifest = {
                packageName = getAttributeValue(null, "package", "")
                splitName = getAttributeValue(null, "split", "")
                configForSplit = getAttributeValue(null, "configForSplit", "")
                versionCode = getAttributeIntValue(ANDROID_RESOURCES, "versionCode", 0)
                isFeatureSplit = getAttributeBooleanValue(ANDROID_RESOURCES, "isFeatureSplit", false)
            },
            onUsesSdk = {},
            onApplication = {}
        )

        return SplitConfigLite(
            packageName = packageName,
            splitName = splitName,
            configForSplit = configForSplit,
            versionCode = versionCode,
            isFeatureSplit = isFeatureSplit
        )
    }

    fun parseBase(parser: XmlResourceParser, res: Resources): PackageInfoLite {
        var packageName = ""
        var versionCode = -1
        var versionCodeMajor = -1
        var versionName = ""
        var compileSdkVersion = -1
        var compileSdkVersionCodename = ""
        var minSdkVersion = -1
        var targetSdkVersion = -1
        var label: String? = null
        var icon: Bitmap? = null

        parser.split(
            onManifest = {
                packageName = getAttributeValue(null, "package", "")
                versionCode = getAttributeIntValue(ANDROID_RESOURCES, "versionCode", 0)
                versionCodeMajor = getAttributeIntValue(ANDROID_RESOURCES, "versionCodeMajor", 0)
                versionName = getAttributeValue(ANDROID_RESOURCES, "versionName", "")
                compileSdkVersion = getAttributeIntValue(ANDROID_RESOURCES, "compileSdkVersion", 0)
                compileSdkVersionCodename = getAttributeValue(ANDROID_RESOURCES, "compileSdkVersionCodename", "")
            },
            onUsesSdk = {
                minSdkVersion = getAttributeIntValue(ANDROID_RESOURCES, "minSdkVersion", 0)
                targetSdkVersion = getAttributeIntValue(ANDROID_RESOURCES, "targetSdkVersion", 0)
            },
            onApplication = {
                label = getAttributeResStringValue(res, "label")
                icon = getAttributeResDrawableValue(res, "icon")?.toIcon()
            }
        )

        return PackageInfoLite(
            packageName = packageName,
            versionCode = versionCode,
            versionCodeMajor = versionCodeMajor,
            versionName = versionName,
            compileSdkVersion = compileSdkVersion,
            compileSdkVersionCodename = compileSdkVersionCodename,
            minSdkVersion = minSdkVersion,
            targetSdkVersion = targetSdkVersion,
            label = label,
            icon = icon
        )
    }
}