package dev.sanmer.pi.core.res

import android.content.Context
import android.content.pm.PackageInfo
import android.content.res.AssetFileDescriptor
import android.util.Log
import dev.sanmer.pi.core.compat.AssetManagerCompat
import dev.sanmer.pi.core.compat.AssetManagerCompat.resources
import dev.sanmer.pi.core.compat.PackageInfoCompat.compileSdkVersion
import dev.sanmer.pi.core.compat.PackageInfoCompat.loadLabel
import dev.sanmer.pi.core.compat.PackageInfoCompat.loadUnbadgedIcon
import dev.sanmer.pi.core.compat.PackageInfoCompat.minSdkVersion
import dev.sanmer.pi.core.compat.PackageInfoCompat.targetSdkVersion
import dev.sanmer.pi.core.compat.XmlBlockCompat
import dev.sanmer.pi.core.res.ResourceParser.getPackageInfoLite
import dev.sanmer.pi.core.res.ResourceParser.getSplitConfigLite
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

class PackageParser(
    private val factory: ResourceParser.BitmapFactory
) {
    companion object Default {
        const val ANDROID_MANIFEST_XML = "AndroidManifest.xml"
        const val BASE_APK = "base.apk"

        fun PackageInfo.toPackageLite() = PackageInfoLite(
            packageName = packageName,
            versionCode = longVersionCode,
            versionName = versionName.orEmpty(),
            compileSdkVersion = compileSdkVersion,
            minSdkVersion = minSdkVersion,
            targetSdkVersion = targetSdkVersion,
            label = null,
            icon = null
        )

        internal fun parseSplit(stream: InputStream): SplitConfigLite {
            ZipArchiveInputStream(stream).use { zip ->
                for (entry in zip) {
                    if (entry.name == ANDROID_MANIFEST_XML) {
                        return XmlBlockCompat.newParser(zip.readBytes()) { parser ->
                            parser.getSplitConfigLite()
                        }
                    }
                }
                throw FileNotFoundException(ANDROID_MANIFEST_XML)
            }
        }
    }

    fun PackageInfo.toPackageLite(context: Context) = PackageInfoLite(
        packageName = packageName,
        versionCode = longVersionCode,
        versionName = versionName.orEmpty(),
        compileSdkVersion = compileSdkVersion,
        minSdkVersion = minSdkVersion,
        targetSdkVersion = targetSdkVersion,
        label = loadLabel(context),
        icon = loadUnbadgedIcon(context)?.let { factory.toBitmap(it) }
    )

    fun parseApk(fd: AssetFileDescriptor): PackageInfoLite {
        return ApkAssetsDelegate.Fd(fd.length, fd.fileDescriptor, false).use { apkAssets ->
            val assetManager = AssetManagerCompat.new(arrayOf(apkAssets.get()), false)
            assetManager.openXmlResourceParser(ANDROID_MANIFEST_XML).use { parser ->
                parser.getPackageInfoLite(assetManager.resources, factory)
            }
        }
    }

    fun parseApk(context: Context, stream: InputStream): PackageInfoLite {
        val temp = File.createTempFile("stream", "", context.externalCacheDir)
        return try {
            temp.outputStream().use { stream.copyTo(it) }
            ApkAssetsDelegate.Path(temp.absolutePath, false).use { apkAssets ->
                val assetManager = AssetManagerCompat.new(arrayOf(apkAssets.get()), false)
                assetManager.openXmlResourceParser(ANDROID_MANIFEST_XML).use { parser ->
                    parser.getPackageInfoLite(assetManager.resources, factory)
                }
            }
        } finally {
            if (temp.delete()) {
                Log.d("PackageParser", "Deleted $temp")
            }
        }
    }

    internal fun parseApks(context: Context, zip: ZipFile): IPackageInfo.Apks? {
        val entry = zip.getEntry(BASE_APK) ?: return null
        val base = IPackageInfo.Apk(
            packageInfo = parseApk(context, zip.getInputStream(entry)),
            sizeBytes = entry.size
        )

        val splitConfigs = mutableListOf<SplitConfig>()
        for (entry in zip.entries) {
            if (entry.name != BASE_APK && entry.name.endsWith(".apk")) runCatching {
                splitConfigs += SplitConfig.from(
                    splitConfig = parseSplit(zip.getInputStream(entry)),
                    fileName = entry.name,
                    sizeBytes = entry.size
                )
            }.onFailure {
                Log.d("PackageParser", "parseSplit(${entry.name})", it)
            }
        }

        return IPackageInfo.Apks(
            base = base,
            splitConfigs = splitConfigs
        )
    }

    fun parse(context: Context, fd: AssetFileDescriptor) = ZipFile.Builder()
        .setIgnoreLocalFileHeader(true)
        .setSeekableByteChannel(fd.createInputStream().channel)
        .get().use { zip ->
            if (zip.getEntry(ANDROID_MANIFEST_XML) != null) {
                return@use IPackageInfo.Apk(
                    packageInfo = parseApk(fd),
                    sizeBytes = fd.length
                )
            }

            val packageInfo = parseApks(context, zip)
            if (packageInfo != null) return@use packageInfo

            val packageInfos = hashMapOf<String, IPackageInfo.Apk>()
            for (entry in zip.entries) {
                if (entry.name.endsWith(".apk")) runCatching {
                    packageInfos[entry.name] = IPackageInfo.Apk(
                        packageInfo = parseApk(context, zip.getInputStream(entry)),
                        sizeBytes = entry.size
                    )
                }.onFailure {
                    Log.d("PackageParser", "parseApk(${entry.name})", it)
                }
            }
            if (packageInfos.isEmpty()) throw FileNotFoundException("*.apk")
            IPackageInfo.Zip(packageInfos)
        }
}