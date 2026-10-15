package dev.sanmer.pi.core.res

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SplitConfigLite(
    val packageName: String,
    val splitName: String,
    val configForSplit: String,
    val versionCode: Int,
    val isFeatureSplit: Boolean,
    val requiredSplitTypes: List<String>
) : Parcelable
