package dev.sanmer.pi

import android.content.pm.PackageInfo
import android.content.pm.PackageParser
import android.content.pm.PackageUserState
import android.content.pm.pkg.FrameworkPackageUserState
import android.util.Log
import dev.sanmer.pi.bundle.BundleInfo
import dev.sanmer.pi.bundle.SplitConfig
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object PackageParserCompat {
    private const val TAG = "PackageParserCompat"
    const val BASE_APK = "base.apk"

    private fun parseApkLite(file: File) =
        try {
            PackageParser.parseApkLite(file, 0)
        } catch (e: PackageParser.PackageParserException) {
            null
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse ${file.path}", e)
            null
        }

    private fun parsePackageInner(file: File, flags: Int): PackageInfo? {
        val pkg = PackageParser().parsePackage(file, flags, false)
        return generatePackageInfo(pkg, flags)?.also {
            it.applicationInfo?.sourceDir = file.path
            it.applicationInfo?.publicSourceDir = file.path
        }
    }

    fun parsePackage(file: File, flags: Int) =
        try {
            parsePackageInner(file, flags)
        } catch (e: PackageParser.PackageParserException) {
            null
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse ${file.path}", e)
            null
        }

    private fun generatePackageInfo(
        pkg: PackageParser.Package,
        flags: Int,
    ): PackageInfo? {
        return if (BuildCompat.atLeastT) {
            PackageParser.generatePackageInfo(
                pkg,
                null,
                flags,
                0,
                0,
                null,
                FrameworkPackageUserState.DEFAULT
            )
        } else {
            PackageParser.generatePackageInfo(
                pkg,
                null,
                flags,
                0,
                0,
                null,
                PackageUserState()
            )
        }
    }

    private fun parseAppBundleInner(file: File, flags: Int, cacheDir: File): BundleInfo {
        file.unzip(cacheDir)

        val baseFile = File(cacheDir, BASE_APK).apply {
            if (!exists()) throw FileNotFoundException(BASE_APK)
        }

        val baseInfo = parsePackageInner(baseFile, flags)
            ?: throw NullPointerException("Failed to parse $BASE_APK")

        val apkFiles = cacheDir.listFiles { f -> f.extension == "apk" }
            ?: throw FileNotFoundException("*.apk")

        val splitConfigs = mutableListOf<SplitConfig>()
        for (apkFile in apkFiles) {
            if (apkFile.name == BASE_APK) continue

            val apk = parseApkLite(apkFile)
            if (apk != null) {
                splitConfigs.add(
                    SplitConfig.parse(apk, apkFile)
                )
            }
        }

        return BundleInfo(
            baseFile = baseFile,
            baseInfo = baseInfo,
            splitConfigs = splitConfigs.sortedBy { it.file.name }
        )
    }

    fun parseAppBundle(file: File, flags: Int, cacheDir: File): BundleInfo? =
        try {
            parseAppBundleInner(file, flags, cacheDir)
        } catch (e: PackageParser.PackageParserException) {
            null
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse ${file.path}", e)
            null
        }

    private fun File.unzip(folder: File) {
        inputStream().buffered().use {
            it.unzip(folder)
        }
    }

    private fun InputStream.unzip(folder: File) {
        try {
            val zin = ZipInputStream(this)
            var entry: ZipEntry
            while (true) {
                entry = zin.nextEntry ?: break
                if (!entry.name.endsWith(".apk") || entry.isDirectory) continue
                val dest = File(folder, entry.name)
                dest.outputStream().use(zin::copyTo)
            }
        } catch (e: IllegalArgumentException) {
            throw IOException(e)
        }
    }
}