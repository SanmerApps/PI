package dev.sanmer.pi.model

import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Parcelable
import dev.sanmer.pi.PackageParserCompat
import dev.sanmer.pi.bundle.SplitConfig
import dev.sanmer.pi.ktx.parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

sealed class Task : Parcelable {
    abstract val archivePath: File
    abstract val archiveInfo: PackageInfo
    abstract val userId: Int
    abstract val sourceInfo: PackageInfo

    @Parcelize
    data class Apk(
        override val archivePath: File,
        override val archiveInfo: PackageInfo,
        override val userId: Int,
        override val sourceInfo: PackageInfo
    ) : Task()

    @Parcelize
    data class AppBundle(
        override val archivePath: File,
        override val archiveInfo: PackageInfo,
        override val userId: Int,
        override val sourceInfo: PackageInfo,
        val splitConfigs: List<SplitConfig>
    ) : Task() {
        val baseFile get() = File(archivePath, PackageParserCompat.BASE_APK)

        val archiveFiles
            get() = splitConfigs.map { it.file }
                .toMutableList().apply {
                    add(0, baseFile)
                }
    }

    companion object Default {
        const val EXTRA_TASK = "dev.sanmer.pi.extra.TASK"

        fun Intent.putTask(value: Task) =
            putExtra(EXTRA_TASK, value)

        inline val Intent.taskOrNull: Task?
            get() = parcelable(EXTRA_TASK)
    }
}