package dev.sanmer.pi.core.parser

import android.content.res.AssetFileDescriptor
import dev.sanmer.pi.core.compat.AssetManagerCompat
import dev.sanmer.pi.core.compat.AssetManagerCompat.resources
import dev.sanmer.pi.core.compat.AssetManagerCompat.setApkAssets
import dev.sanmer.pi.core.compat.ContextCompat
import dev.sanmer.pi.core.compat.XmlBlockCompat
import dev.sanmer.pi.core.ktx.find
import dev.sanmer.pi.core.res.ApkAssetsSource
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.FileNotFoundException
import java.io.InputStream
import kotlin.io.readBytes
import kotlin.use

object PackageParser {
    const val BASE_APK = "base.apk"

    private val cacheDir by lazy { ContextCompat.getContext().cacheDir }

    fun loadSplitLite(fd: AssetFileDescriptor) = ApkAssetsSource.Fd(fd).use { source ->
        val asset = source.get()
        asset.openXml(ResourceParser.ANDROID_MANIFEST).use {
            ResourceParser.parseSplit(it)
        }.also {
            require(it.versionCode > 0) { "Expect versionCode" }
            require(it.splitName.isNotEmpty()) { "Expect splitName" }
        }
    }

    fun loadSplitLite(stream: InputStream) = ZipArchiveInputStream(stream).use { zip ->
        zip.find(ResourceParser.ANDROID_MANIFEST)
        XmlBlockCompat.newParser(zip.readBytes()).use {
            ResourceParser.parseSplit(it)
        }.also {
            require(it.versionCode > 0) { "Expect versionCode" }
            require(it.splitName.isNotEmpty()) { "Expect splitName" }
        }
    }

    fun loadPackageLite(source: ApkAssetsSource): PackageInfoLite {
        val asset = source.get()
        val assets = AssetManagerCompat.new()
        assets.setApkAssets(arrayOf(asset), false)
        return asset.openXml(ResourceParser.ANDROID_MANIFEST).use {
            ResourceParser.parsePackage(it, assets.resources)
        }.also {
            require(it.versionCode > 0) { "Expect versionCode" }
            require(it.packageName.isNotEmpty()) { "Expect packageName" }
        }
    }

    fun loadPackageLite(fd: AssetFileDescriptor) = ApkAssetsSource.Fd(fd)
        .use(::loadPackageLite)

    fun loadPackageLite(stream: InputStream) = ApkAssetsSource.Stream(stream, cacheDir)
        .use(::loadPackageLite)

    fun loadApks(zip: ZipFile): IPackageInfo.Apks? {
        val entry = zip.getEntry(BASE_APK) ?: return null
        val packageInfo = loadPackageLite(zip.getInputStream(entry))
        val base = IPackageInfo.Apk(
            packageInfo = packageInfo,
            sizeBytes = entry.size
        )

        val splitConfigs = mutableListOf<SplitConfig>()
        zip.entries.iterator().forEach { entry ->
            if (entry.name.endsWith(".apk") && entry.name != BASE_APK) runCatching {
                splitConfigs.add(
                    SplitConfig.from(
                        splitConfig = loadSplitLite(zip.getInputStream(entry)),
                        fileName = entry.name,
                        sizeBytes = entry.size
                    )
                )
            }
        }

        return IPackageInfo.Apks(
            base = base,
            splitConfigs = splitConfigs
        )
    }

    fun loadPackage(fd: AssetFileDescriptor) = ZipFile.builder()
        .setIgnoreLocalFileHeader(true)
        .setSeekableByteChannel(fd.createInputStream().channel)
        .get().use { zip ->
            val xml = zip.getEntry(ResourceParser.ANDROID_MANIFEST)
            if (xml != null) return@use IPackageInfo.Apk(
                packageInfo = loadPackageLite(fd),
                sizeBytes = fd.length
            )

            val packageInfo = loadApks(zip)
            if (packageInfo != null) return@use packageInfo

            val packageInfos = hashMapOf<String, IPackageInfo.Apk>()
            zip.entries.iterator().forEach { entry ->
                if (entry.name.endsWith(".apk")) runCatching {
                    packageInfos[entry.name] = IPackageInfo.Apk(
                        packageInfo = loadPackageLite(zip.getInputStream(entry)),
                        sizeBytes = entry.size,
                    )
                }
            }

            if (packageInfos.isEmpty()) throw FileNotFoundException("*.apk")
            IPackageInfo.Zip(packageInfos)
        }
}