package dev.sanmer.pi.parser

import android.content.pm.PackageInfo
import dev.sanmer.pi.AssetManagerCompat
import dev.sanmer.pi.AssetManagerCompat.resources
import dev.sanmer.pi.AssetManagerCompat.setApkAssets
import dev.sanmer.pi.ContextCompat
import dev.sanmer.pi.PackageInfoCompat.compileSdkVersion
import dev.sanmer.pi.PackageInfoCompat.compileSdkVersionCodename
import dev.sanmer.pi.PackageInfoCompat.loadLabel
import dev.sanmer.pi.PackageInfoCompat.loadUnbadgedIcon
import dev.sanmer.pi.PackageInfoCompat.minSdkVersion
import dev.sanmer.pi.PackageInfoCompat.targetSdkVersion
import dev.sanmer.pi.PackageInfoCompat.versionCodeMajor
import dev.sanmer.pi.XmlBlockCompat
import dev.sanmer.pi.ktx.asZipFile
import dev.sanmer.pi.ktx.find
import dev.sanmer.pi.ktx.statSize
import dev.sanmer.pi.parser.ResourceParser.toIcon
import dev.sanmer.pi.res.ApkAssetsWrapper
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.FileDescriptor
import java.io.InputStream
import kotlin.io.readBytes
import kotlin.use

object PackageParser {
    const val BASE_APK = "base.apk"

    fun setEnableAdaptiveIcons(value: Boolean) {
        ResourceParser.enableAdaptiveIcons = value
    }

    @Suppress("DEPRECATION")
    fun PackageInfo.toLite() = PackageInfoLite(
        packageName = packageName,
        versionCode = versionCode,
        versionCodeMajor = versionCodeMajor,
        versionName = versionName.orEmpty(),
        compileSdkVersion = compileSdkVersion,
        compileSdkVersionCodename = compileSdkVersionCodename.orEmpty(),
        minSdkVersion = minSdkVersion,
        targetSdkVersion = targetSdkVersion,
        label = loadLabel(ContextCompat.context),
        icon = loadUnbadgedIcon(ContextCompat.context)?.toIcon()
    )

    fun loadSplitFromFd(fd: FileDescriptor): SplitConfigLite {
        val lite = ApkAssetsWrapper.Fd(fd).use { wrapper ->
            val asset = wrapper.get()
            asset.openXml(ResourceParser.ANDROID_MANIFEST).use {
                ResourceParser.parseSplit(it)
            }
        }
        require(lite.versionCode > 0) { "VersionCode not set" }
        return lite
    }

    fun loadSplitFromStream(stream: InputStream): SplitConfigLite {
        val lite = ZipArchiveInputStream(stream).use { zip ->
            zip.find(ResourceParser.ANDROID_MANIFEST)
            XmlBlockCompat.newParser(zip.readBytes()).use {
                ResourceParser.parseSplit(it)
            }
        }
        require(lite.versionCode > 0) { "VersionCode not set" }
        return lite
    }

    fun loadBaseFromFd(fd: FileDescriptor): PackageInfoLite {
        val assets = AssetManagerCompat.new()
        val base = ApkAssetsWrapper.Fd(fd).use { wrapper ->
            val asset = wrapper.get()
            assets.setApkAssets(arrayOf(asset), false)
            asset.openXml(ResourceParser.ANDROID_MANIFEST).use {
                ResourceParser.parseBase(it, assets.resources)
            }
        }
        require(base.versionCode > 0) { "VersionCode not set" }
        return base
    }

    fun loadBaseFromStream(stream: InputStream): PackageInfoLite {
        val base = ApkAssetsWrapper.Stream(stream).use { wrapper ->
            val asset = wrapper.get()
            val assets = AssetManagerCompat.new()
            assets.setApkAssets(arrayOf(asset), false)
            asset.openXml(ResourceParser.ANDROID_MANIFEST).use {
                ResourceParser.parseBase(it, assets.resources)
            }
        }
        require(base.versionCode > 0) { "VersionCode not set" }
        return base
    }

    fun loadBundleFromFd(fd: FileDescriptor): BundleInfo {
        return fd.asZipFile().use { zip ->
            val xml = zip.getEntry(ResourceParser.ANDROID_MANIFEST)
            if (xml != null) {
                val packageInfo = loadBaseFromFd(fd)
                return BundleInfo(
                    packageInfo = packageInfo,
                    fileName = "",
                    sizeBytes = fd.statSize,
                    splitConfigs = emptyList()
                )
            }

            val base = zip.getEntry(BASE_APK)
            if (base != null) {
                val stream = zip.getInputStream(base)
                val packageInfo = loadBaseFromStream(stream)

                val splitConfigs = mutableListOf<SplitConfig>()
                zip.entries.iterator().forEach { entry ->
                    if (entry == base || !entry.name.endsWith(".apk")) return@forEach
                    runCatching {
                        val stream = zip.getInputStream(entry)
                        splitConfigs.add(
                            SplitConfig(
                                lite = loadSplitFromStream(stream),
                                fileName = entry.name,
                                sizeBytes = entry.size
                            )
                        )
                    }
                }

                BundleInfo(
                    packageInfo = packageInfo,
                    fileName = BASE_APK,
                    sizeBytes = base.size,
                    splitConfigs = splitConfigs
                )
            } else {
                val entries = zip.entries.toList().filter { it.name.endsWith(".apk") }
                require(entries.size == 1) { "Unsupported ZIP" }
                val entry = entries[0]
                val stream = zip.getInputStream(entry)

                BundleInfo(
                    packageInfo = loadBaseFromStream(stream),
                    fileName = entry.name,
                    sizeBytes = entry.size,
                    splitConfigs = emptyList()
                )
            }
        }
    }
}