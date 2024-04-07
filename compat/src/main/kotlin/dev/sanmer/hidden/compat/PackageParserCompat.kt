package dev.sanmer.hidden.compat

import android.content.pm.PackageInfo
import android.content.pm.PackageParser
import android.content.pm.pkg.FrameworkPackageUserState
import android.content.pm.pkg.PackageUserState
import android.util.Log
import dev.sanmer.hidden.compat.content.bundle.AbiSplitConfig
import dev.sanmer.hidden.compat.content.bundle.DensitySplitConfig
import dev.sanmer.hidden.compat.content.bundle.FeatureSplitConfig
import dev.sanmer.hidden.compat.content.bundle.LanguageSplitConfig
import dev.sanmer.hidden.compat.content.bundle.SplitConfig
import dev.sanmer.hidden.compat.content.bundle.UnspecifiedSplitConfig
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object PackageParserCompat {
    private const val TAG = "PackageParserCompat"
    const val BASE_APK = "base.apk"
    const val APK_FILE_EXTENSION = ".apk"

    fun isApkFile(file: File) = isApkPath(file.name)

    fun isApkPath(path: String) = path.endsWith(APK_FILE_EXTENSION)

    fun parseApkLite(file: File) =
        runCatching {
            PackageParser.parseApkLite(file, 0)
        }.getOrNull()

    fun parsePackage(file: File, flags: Int) =
        runCatching {
            val pkg = PackageParser().parsePackage(file, flags, false)
            generatePackageInfo(pkg, null, flags, 0, 0, null)?.also {
                it.applicationInfo.sourceDir = file.path
                it.applicationInfo.publicSourceDir = file.path
            }
        }.getOrNull()

    internal fun generatePackageInfo(
        pkg: PackageParser.Package,
        gid: IntArray?,
        flags: Int,
        firstInstallTime: Long,
        lastUpdateTime: Long,
        grantedPermissions: Set<String>?,
    ): PackageInfo? {
        return if (BuildCompat.atLeastT) {
            PackageParser.generatePackageInfo(
                pkg,
                gid,
                flags,
                firstInstallTime,
                lastUpdateTime,
                grantedPermissions,
                FrameworkPackageUserState.DEFAULT
            )
        } else {
            PackageParser.generatePackageInfo(
                pkg,
                gid,
                flags,
                firstInstallTime,
                lastUpdateTime,
                grantedPermissions,
                PackageUserState.DEFAULT
            )
        }
    }

    fun parseAppBundle(file: File, flags: Int, cacheDir: File): AppBundleInfo? {
        runCatching {
            file.unzip(cacheDir)
        }.onFailure {
            Log.w(TAG, "Failed to unzip ${file.path}", it)
            return null
        }

        val apkFiles = cacheDir.listFiles { _, name ->
            name.endsWith(APK_FILE_EXTENSION)
        } ?: emptyArray()

        var baseFile = file
        var baseInfo: PackageInfo? = null
        val splitFiles = mutableListOf<File>()
        val splitConfigs = mutableListOf<SplitConfig>()
        for (apkFile in apkFiles) {
            if (apkFile.name == BASE_APK) {
                baseInfo = parsePackage(apkFile, flags)?.also {
                    it.applicationInfo.sourceDir = apkFile.path
                    it.applicationInfo.publicSourceDir = apkFile.path
                }
                baseFile = apkFile
                continue
            }

            val apk = parseApkLite(apkFile)
            if (apk != null) {
                val splitConfig = parseSplitConfig(apk, apkFile.name, apkFile.length())
                splitConfigs.add(splitConfig)
                splitFiles.add(apkFile)
            }
        }

        return if (baseInfo == null) {
            Log.w(TAG, "Missing base APK in ${file.path}")
            null
        } else {
            AppBundleInfo(
                baseFile = baseFile,
                baseInfo = baseInfo,
                splitFiles = splitFiles.toList(),
                splitConfigs = splitConfigs.sortedBy { it.filename }
            )
        }
    }

    private fun parseSplitConfig(
        apk: PackageParser.ApkLite,
        filename: String,
        size: Long
    ): SplitConfig {
        return FeatureSplitConfig.build(apk, filename, size)
            ?: AbiSplitConfig.build(apk, filename, size)
            ?: DensitySplitConfig.build(apk, filename, size)
            ?: LanguageSplitConfig.build(apk, filename, size)
            ?: UnspecifiedSplitConfig(filename, size)
    }

    @Throws(IOException::class)
    private fun File.unzip(folder: File) {
        inputStream().buffered().use {
            it.unzip(folder)
        }
    }

    @Throws(IOException::class)
    private fun InputStream.unzip(folder: File) {
        try {
            val zin = ZipInputStream(this)
            var entry: ZipEntry
            while (true) {
                entry = zin.nextEntry ?: break
                if (!entry.name.endsWith(APK_FILE_EXTENSION) || entry.isDirectory) {
                    continue
                }

                val dest = File(folder, entry.name)
                dest.parentFile!!.let {
                    if (!it.exists()) it.mkdirs()
                }
                dest.outputStream().use { out -> zin.copyTo(out) }
            }
        } catch (e: IllegalArgumentException) {
            throw IOException(e)
        }
    }

    data class AppBundleInfo(
        val baseFile: File,
        val baseInfo: PackageInfo,
        val splitFiles: List<File>,
        val splitConfigs: List<SplitConfig>
    )
}