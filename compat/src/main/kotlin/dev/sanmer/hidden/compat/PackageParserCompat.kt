package dev.sanmer.hidden.compat

import android.content.pm.PackageInfo
import android.content.pm.PackageParser
import android.content.pm.PackageUserState
import android.content.pm.pkg.FrameworkPackageUserState
import android.util.Log
import dev.sanmer.hidden.compat.content.bundle.AbiSplitConfig
import dev.sanmer.hidden.compat.content.bundle.DensitySplitConfig
import dev.sanmer.hidden.compat.content.bundle.FeatureSplitConfig
import dev.sanmer.hidden.compat.content.bundle.LanguageSplitConfig
import dev.sanmer.hidden.compat.content.bundle.SplitConfig
import dev.sanmer.hidden.compat.content.bundle.UnspecifiedSplitConfig
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object PackageParserCompat {
    private const val TAG = "PackageParserCompat"
    const val BASE_APK = "base.apk"
    const val APK_FILE_EXTENSION = ".apk"

    fun parseApkLite(file: File) =
        try {
            PackageParser.parseApkLite(file, 0)
        } catch (e: PackageParser.PackageParserException) {
            null
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse ${file.path}", e)
            null
        }

    fun parsePackage(file: File, flags: Int) =
        try {
            val pkg = PackageParser().parsePackage(file, flags, false)
            generatePackageInfo(pkg, null, flags, 0, 0, null)?.also {
                it.applicationInfo.sourceDir = file.path
                it.applicationInfo.publicSourceDir = file.path
            }
        } catch (e: PackageParser.PackageParserException) {
            null
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse ${file.path}", e)
            null
        }

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
                PackageUserState()
            )
        }
    }

    internal fun parseAppBundleInner(file: File, flags: Int, cacheDir: File): AppBundleInfo {
        file.unzip(cacheDir)

        val baseFile = File(cacheDir, BASE_APK).apply {
            if (!exists()) throw FileNotFoundException(BASE_APK)
        }
        val baseInfo = parsePackage(baseFile, flags)
            ?: throw NullPointerException("Failed to parse $BASE_APK")

        val apkFiles = cacheDir.listFiles { _, name ->
            name.endsWith(APK_FILE_EXTENSION)
        } ?: throw FileNotFoundException("*.apk")
        val splitFiles = mutableListOf<File>()
        val splitConfigs = mutableListOf<SplitConfig>()

        for (apkFile in apkFiles) {
            if (apkFile.name == BASE_APK) {
                continue
            }

            val apk = parseApkLite(apkFile)
            if (apk != null) {
                val splitConfig = parseSplitConfig(apk, apkFile)
                splitConfigs.add(splitConfig)
                splitFiles.add(apkFile)
            }
        }

        return AppBundleInfo(
            baseFile = baseFile,
            baseInfo = baseInfo,
            splitFiles = splitFiles.toList(),
            splitConfigs = splitConfigs.sortedBy { it.filename }
        )
    }

    fun parseAppBundle(file: File, flags: Int, cacheDir: File): AppBundleInfo? =
        runCatching {
            parseAppBundleInner(file, flags, cacheDir)
        }.onFailure {
            Log.w(TAG, "Failed to parse ${file.path}", it)
        }.getOrNull()

    private fun parseSplitConfig(
        apk: PackageParser.ApkLite,
        file: File
    ): SplitConfig {
        return FeatureSplitConfig.build(apk, file.name, file.length())
            ?: AbiSplitConfig.build(apk, file.name, file.length())
            ?: DensitySplitConfig.build(apk, file.name, file.length())
            ?: LanguageSplitConfig.build(apk, file.name, file.length())
            ?: UnspecifiedSplitConfig(file.name, file.length())
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