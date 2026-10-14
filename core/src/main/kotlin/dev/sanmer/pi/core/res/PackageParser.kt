package dev.sanmer.pi.core.res

import android.content.Context
import android.content.pm.PackageInfo
import android.content.res.AssetFileDescriptor
import dev.sanmer.pi.core.compat.AssetManagerCompat
import dev.sanmer.pi.core.compat.AssetManagerCompat.resources
import dev.sanmer.pi.core.compat.AssetManagerCompat.setApkAssets
import dev.sanmer.pi.core.compat.PackageInfoCompat.compileSdkVersion
import dev.sanmer.pi.core.compat.PackageInfoCompat.loadLabel
import dev.sanmer.pi.core.compat.PackageInfoCompat.loadUnbadgedIcon
import dev.sanmer.pi.core.compat.PackageInfoCompat.minSdkVersion
import dev.sanmer.pi.core.compat.PackageInfoCompat.targetSdkVersion
import dev.sanmer.pi.core.compat.XmlBlockCompat
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.FileNotFoundException
import java.io.InputStream
import kotlin.io.readBytes
import kotlin.use

class PackageParser(
    private val context: Context,
    private val iconSize: Int
) {
    companion object Default {
        const val BASE_APK = "base.apk"
    }

    private val resourceParser by lazy {
        val factory = AppIconFactory(context, iconSize)
        ResourceParser { factory.createBadgedIcon(it) }
    }

    @Throws(FileNotFoundException::class)
    internal fun ZipArchiveInputStream.find(name: String): ZipArchiveEntry {
        while (true) {
            val entry = nextEntry ?: break
            if (entry.name == name) return entry
        }
        throw FileNotFoundException(name)
    }

    fun createPackageLite(
        packageInfo: PackageInfo,
        loadRes: Boolean = true
    ) = PackageInfoLite(
        packageName = packageInfo.packageName,
        versionCode = packageInfo.longVersionCode,
        versionName = packageInfo.versionName.orEmpty(),
        compileSdkVersion = packageInfo.compileSdkVersion,
        minSdkVersion = packageInfo.minSdkVersion,
        targetSdkVersion = packageInfo.targetSdkVersion,
        label = if (loadRes) packageInfo.loadLabel(context) else null,
        icon = if (loadRes) packageInfo.loadUnbadgedIcon(context)
            ?.let(resourceParser.toBitmap) else null
    )

    fun parseSplitLite(fd: AssetFileDescriptor) = ApkAssetsSource.Fd(fd).use { source ->
        val asset = source.get()
        asset.openXml(ResourceParser.ANDROID_MANIFEST).use {
            resourceParser.parseSplit(it)
        }
    }

    fun parseSplitLite(stream: InputStream) = ZipArchiveInputStream(stream).use { zip ->
        zip.find(ResourceParser.ANDROID_MANIFEST)
        XmlBlockCompat.newParser(zip.readBytes()).use {
            resourceParser.parseSplit(it)
        }
    }

    fun parsePackageLite(source: ApkAssetsSource): PackageInfoLite {
        val asset = source.get()
        val assets = AssetManagerCompat.new()
        assets.setApkAssets(arrayOf(asset), false)
        return asset.openXml(ResourceParser.ANDROID_MANIFEST).use {
            resourceParser.parsePackage(it, assets.resources)
        }
    }

    fun parsePackageLite(fd: AssetFileDescriptor) = ApkAssetsSource.Fd(fd)
        .use(::parsePackageLite)

    fun parsePackageLite(stream: InputStream) = ApkAssetsSource.Stream(stream, context.cacheDir)
        .use(::parsePackageLite)

    internal fun parseApks(zip: ZipFile): IPackageInfo.Apks? {
        val entry = zip.getEntry(BASE_APK) ?: return null
        val packageInfo = parsePackageLite(zip.getInputStream(entry))
        val base = IPackageInfo.Apk(
            packageInfo = packageInfo,
            sizeBytes = entry.size
        )

        val splitConfigs = mutableListOf<SplitConfig>()
        zip.entries.iterator().forEach { entry ->
            if (entry.name.endsWith(".apk") && entry.name != BASE_APK) runCatching {
                splitConfigs.add(
                    SplitConfig.from(
                        splitConfig = parseSplitLite(zip.getInputStream(entry)),
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

    fun parse(fd: AssetFileDescriptor) = ZipFile.builder()
        .setIgnoreLocalFileHeader(true)
        .setSeekableByteChannel(fd.createInputStream().channel)
        .get().use { zip ->
            val xml = zip.getEntry(ResourceParser.ANDROID_MANIFEST)
            if (xml != null) return@use IPackageInfo.Apk(
                packageInfo = parsePackageLite(fd),
                sizeBytes = fd.length
            )

            val packageInfo = parseApks(zip)
            if (packageInfo != null) return@use packageInfo

            val packageInfos = hashMapOf<String, IPackageInfo.Apk>()
            zip.entries.iterator().forEach { entry ->
                if (entry.name.endsWith(".apk")) runCatching {
                    packageInfos[entry.name] = IPackageInfo.Apk(
                        packageInfo = parsePackageLite(zip.getInputStream(entry)),
                        sizeBytes = entry.size,
                    )
                }
            }

            if (packageInfos.isEmpty()) throw FileNotFoundException("*.apk")
            IPackageInfo.Zip(packageInfos)
        }
}