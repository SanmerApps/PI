package dev.sanmer.pi.parser

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SplitConfigLite(
    val packageName: String,
    val splitName: String,
    val configForSplit: String,
    val versionCode: Int,
    val isFeatureSplit: Boolean
) : Parcelable
