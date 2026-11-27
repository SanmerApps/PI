package dev.sanmer.pi.core.res

import android.content.res.ApkAssets
import android.os.Parcelable
import dev.sanmer.pi.core.compat.XmlBlockCompat
import dev.sanmer.pi.core.res.ResourceParser.ANDROID_MANIFEST_XML
import dev.sanmer.pi.core.res.ResourceParser.getSplitConfigLite
import kotlinx.parcelize.Parcelize

@Parcelize
data class SplitConfigLite(
    val packageName: String,
    val splitName: String,
    val configForSplit: String,
    val versionCode: Int,
    val isFeatureSplit: Boolean,
    val requiredSplitTypes: List<String>
) : Parcelable {
    object Builder {
        fun fromApkAssets(apkAssets: ApkAssets) =
            apkAssets.openXml(ANDROID_MANIFEST_XML).use { parser ->
                parser.getSplitConfigLite()
            }

        fun fromBytes(data: ByteArray) = XmlBlockCompat.newParser(data) { parser ->
            parser.getSplitConfigLite()
        }
    }
}
