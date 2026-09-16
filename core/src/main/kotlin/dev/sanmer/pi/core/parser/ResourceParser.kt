package dev.sanmer.pi.core.parser

import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import dev.sanmer.pi.core.ktx.dp
import dev.sanmer.pi.core.res.AppIconLoader
import org.xmlpull.v1.XmlPullParser

internal object ResourceParser {
    const val ANDROID_MANIFEST = "AndroidManifest.xml"
    const val ANDROID_RESOURCES = "http://schemas.android.com/apk/res/android"
    const val TAG_MANIFEST = "manifest"
    const val TAG_USES_SDK = "uses-sdk"
    const val TAG_APPLICATION = "application"

    private val appIconLoader by lazy { AppIconLoader(45.dp) }
    fun Drawable.toIcon() = appIconLoader.loadIcon(this)

    fun XmlResourceParser.nextOrNull(): Int? {
        return next().takeIf { it != XmlPullParser.END_DOCUMENT }
    }

    fun XmlResourceParser.getAttributeResStringValue(
        res: Resources, name: String
    ): String? {
        val resId = getAttributeResourceValue(ANDROID_RESOURCES, name, 0)
        return if (resId == 0) {
            getAttributeValue(ANDROID_RESOURCES, name)
        } else try {
            res.getString(resId)
        } catch (_: Throwable) {
            null
        }
    }

    fun XmlResourceParser.getAttributeResDrawableValue(
        res: Resources, name: String
    ): Drawable? {
        val resId = getAttributeResourceValue(ANDROID_RESOURCES, name, 0)
        return try {
            res.getDrawable(resId, null)
        } catch (_: Throwable) {
            null
        }
    }

    inline fun <reified P : XmlResourceParser> P.fold(
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
        var packageName: String? = null
        var splitName: String? = null
        var configForSplit: String? = null
        var versionCode = -1
        var isFeatureSplit = false
        val requiredSplitTypes = mutableListOf<String>()

        parser.fold(
            onManifest = {
                packageName = getAttributeValue(null, "package")
                splitName = getAttributeValue(null, "split")
                configForSplit = getAttributeValue(null, "configForSplit")
                versionCode = getAttributeIntValue(ANDROID_RESOURCES, "versionCode", 0)
                isFeatureSplit = getAttributeBooleanValue(
                    ANDROID_RESOURCES, "isFeatureSplit", false
                )
                getAttributeValue(ANDROID_RESOURCES, "requiredSplitTypes")?.let {
                    if (it.isNotEmpty()) requiredSplitTypes.addAll(it.split(','))
                }
            },
            onUsesSdk = {},
            onApplication = {}
        )

        return SplitConfigLite(
            packageName = packageName.orEmpty(),
            splitName = splitName.orEmpty(),
            configForSplit = configForSplit.orEmpty(),
            versionCode = versionCode,
            isFeatureSplit = isFeatureSplit,
            requiredSplitTypes = requiredSplitTypes.toList()
        )
    }

    fun parsePackage(parser: XmlResourceParser, res: Resources): PackageInfoLite {
        var packageName: String? = null
        var versionCode = -1
        var versionCodeMajor = -1
        var versionName: String? = null
        var compileSdkVersion = -1
        var compileSdkVersionCodename: String? = null
        var minSdkVersion = -1
        var targetSdkVersion = -1
        var label: String? = null
        var icon: Bitmap? = null

        parser.fold(
            onManifest = {
                packageName = getAttributeValue(null, "package")
                versionCode = getAttributeIntValue(ANDROID_RESOURCES, "versionCode", 0)
                versionCodeMajor = getAttributeIntValue(ANDROID_RESOURCES, "versionCodeMajor", 0)
                versionName = getAttributeValue(ANDROID_RESOURCES, "versionName")
                compileSdkVersion = getAttributeIntValue(ANDROID_RESOURCES, "compileSdkVersion", 0)
                compileSdkVersionCodename = getAttributeValue(
                    ANDROID_RESOURCES, "compileSdkVersionCodename"
                )
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
            packageName = packageName.orEmpty(),
            versionCode = versionCode,
            versionCodeMajor = versionCodeMajor,
            versionName = versionName.orEmpty(),
            compileSdkVersion = compileSdkVersion,
            compileSdkVersionCodename = compileSdkVersionCodename.orEmpty(),
            minSdkVersion = minSdkVersion,
            targetSdkVersion = targetSdkVersion,
            label = label,
            icon = icon
        )
    }
}