package dev.sanmer.pi.core.res

import android.content.Context
import android.content.res.ApkAssets
import android.os.ProxyFileDescriptorCallback
import android.util.Log
import dev.sanmer.pi.core.compat.BuildCompat
import dev.sanmer.pi.core.res.ResourceParser.ANDROID_MANIFEST_XML
import dev.sanmer.pi.core.res.ResourceParser.ANDROID_MANIFEST_XML_BYTES
import dev.sanmer.pi.core.res.ResourceParser.APK_EXTENSION_BYTES
import dev.sanmer.pi.core.res.ResourceParser.BASE_APK_BYTES
import dev.sanmer.zip.DeflateDataSource
import dev.sanmer.zip.Inflater
import dev.sanmer.zip.InflaterSource
import dev.sanmer.zip.ZipFile
import dev.sanmer.zip.ZipFile.Builder.uncompress
import dev.sanmer.zip.common.CentralDirectoryRecord
import dev.sanmer.zip.common.CompressionMethod
import dev.sanmer.zip.common.LocalFileRecord.Builder.findLocalFileRecord
import dev.sanmer.zip.io.DataSource
import dev.sanmer.zip.io.asSource
import dev.sanmer.zip.io.limited
import dev.sanmer.zip.io.pfdOrNull
import kotlinx.io.buffered
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.endsWith
import kotlinx.io.readByteArray
import java.io.FileNotFoundException

class PackageParser(
    private val context: Context,
    private val builder: PackageInfoLite.Builder
) {
    enum class DecompressionStrategy {
        TempFile,
        InMemory
    }

    private class DataSourceCallback(
        private val dataSource: DataSource
    ) : ProxyFileDescriptorCallback() {
        override fun onGetSize() = dataSource.length

        override fun onRead(offset: Long, size: Int, data: ByteArray) =
            dataSource.read(data, 0, size, offset)

        override fun onRelease() {}
    }

    private inline fun <R> openStore(
        zip: ZipFile,
        record: CentralDirectoryRecord,
        block: (ApkAssets) -> R
    ): R {
        val pfd = zip.asDataSource().pfdOrNull
        if (pfd != null && BuildCompat.atLeastR) return ApkAssetsDelegate.Builder.openFd(
            fd = pfd.fileDescriptor,
            offset = zip.dataOffset(record),
            length = record.compressedSize.toLong(),
            autoClose = false,
            friendlyName = record.name.decodeToString(Charsets.UTF_8),
            block = block
        )
        return ApkAssetsDelegate.Builder.openFd(
            context = context,
            callback = DataSourceCallback(zip.getRawDataSource(record)),
            friendlyName = record.name.decodeToString(Charsets.UTF_8),
            block = block
        )
    }

    private inline fun <R> openDeflate(
        zip: ZipFile,
        record: CentralDirectoryRecord,
        strategy: DecompressionStrategy,
        block: (ApkAssets) -> R
    ) = when (strategy) {
        DecompressionStrategy.TempFile -> ApkAssetsDelegate.Builder.createTemp(
            source = InflaterSource(
                source = zip.getRawSource(record),
                inflater = Inflater(-Inflater.MAX_WINDOW_BITS)
            ).buffered(),
            cacheDir = context.externalCacheDir,
            block = block
        )

        DecompressionStrategy.InMemory -> ApkAssetsDelegate.Builder.openFd(
            context = context,
            callback = DataSourceCallback(
                DeflateDataSource.of(
                    dataSource = zip.getRawDataSource(record),
                    uncompressedSize = record.uncompressedSize.toLong()
                )
            ),
            friendlyName = record.name.decodeToString(Charsets.UTF_8),
            block = block
        )
    }

    private inline fun <R> open(
        zip: ZipFile,
        record: CentralDirectoryRecord,
        strategy: DecompressionStrategy,
        block: (ApkAssets) -> R
    ) = when (record.compressionMethod) {
        CompressionMethod.Store -> openStore(
            zip = zip,
            record = record,
            block = block
        )

        CompressionMethod.Deflate -> openDeflate(
            zip = zip,
            record = record,
            strategy = strategy,
            block = block
        )

        else -> throw IllegalArgumentException("Unsupported compressionMethod: ${record.compressionMethod}")
    }

    private inline fun <R> readStore(
        zip: ZipFile,
        record: CentralDirectoryRecord,
        block: (ByteArray) -> R
    ): R {
        val zip = ZipFile.of(zip.getRawDataSource(record))
        val record = zip[ANDROID_MANIFEST_XML_BYTES]
            ?: throw FileNotFoundException(ANDROID_MANIFEST_XML)
        val data = zip.getSource(record).asSource().readByteArray()
        return block(data)
    }

    private inline fun <R> readDeflate(
        zip: ZipFile,
        record: CentralDirectoryRecord,
        block: (ByteArray) -> R
    ): R {
        val source = InflaterSource(
            source = zip.getRawSource(record),
            inflater = Inflater(-Inflater.MAX_WINDOW_BITS)
        ).buffered()
        val record = source.findLocalFileRecord { it.name == ANDROID_MANIFEST_XML_BYTES }
            ?: throw FileNotFoundException(ANDROID_MANIFEST_XML)
        val data = source.limited(record.compressedSize.toLong())
            .uncompress(record.compressionMethod)
            .buffered()
            .readByteArray()
        return block(data)
    }

    private inline fun <R> read(
        zip: ZipFile,
        record: CentralDirectoryRecord,
        block: (ByteArray) -> R
    ) = when (record.compressionMethod) {
        CompressionMethod.Store -> readStore(
            zip = zip,
            record = record,
            block = block
        )

        CompressionMethod.Deflate -> readDeflate(
            zip = zip,
            record = record,
            block = block
        )

        else -> throw IllegalArgumentException("Unsupported compressionMethod: ${record.compressionMethod}")
    }

    fun parse(dataSource: DataSource, strategy: DecompressionStrategy): IPackageInfo {
        val zip = ZipFile.of(dataSource)

        if (ANDROID_MANIFEST_XML_BYTES in zip) {
            val packageInfo = dataSource.pfdOrNull?.let { pfd ->
                ApkAssetsDelegate.Builder.openFd(
                    fd = pfd.fileDescriptor,
                    autoClose = false,
                    block = builder::fromApkAssets
                )
            } ?: ApkAssetsDelegate.Builder.openFd(
                context = context,
                callback = DataSourceCallback(zip.asDataSource()),
                block = builder::fromApkAssets
            )

            return IPackageInfo.Apk(
                packageInfo = packageInfo,
                sizeBytes = zip.length
            )
        }

        zip[BASE_APK_BYTES]?.let { record ->
            val packageInfo = open(
                zip = zip,
                record = record,
                strategy = strategy,
                block = builder::fromApkAssets
            )

            var error: Throwable? = null
            val splitConfigs = mutableListOf<SplitConfig>()
            for (record in zip.records) if (
                record.name != BASE_APK_BYTES && record.name.endsWith(APK_EXTENSION_BYTES)
            ) try {
                val splitConfig = read(
                    zip = zip,
                    record = record,
                    block = SplitConfigLite.Builder::fromBytes
                )
                splitConfigs += SplitConfig.from(
                    splitConfig = splitConfig,
                    fileName = record.name.decodeToString(Charsets.UTF_8),
                    sizeBytes = record.uncompressedSize.toLong()
                )
            } catch (e: Throwable) {
                val e = IllegalArgumentException(record.name.decodeToString(Charsets.UTF_8), e)
                if (error == null) error = e else error.addSuppressed(e)
            }

            if (error != null) Log.w("PackageParser", "splitConfigs", error)

            return IPackageInfo.Apks(
                base = IPackageInfo.Apk(
                    packageInfo = packageInfo,
                    sizeBytes = record.uncompressedSize.toLong()
                ),
                splitConfigs = splitConfigs
            )
        }

        var error: Throwable? = null
        val packageInfos = hashMapOf<String, IPackageInfo.Apk>()
        for (record in zip.records) if (record.name.endsWith(APK_EXTENSION_BYTES)) try {
            val packageInfo = open(
                zip = zip,
                record = record,
                strategy = strategy,
                block = builder::fromApkAssets
            )

            packageInfos[record.name.decodeToString(Charsets.UTF_8)] = IPackageInfo.Apk(
                packageInfo = packageInfo,
                sizeBytes = record.uncompressedSize.toLong()
            )
        } catch (e: Throwable) {
            val e = IllegalArgumentException(record.name.decodeToString(Charsets.UTF_8), e)
            if (error == null) error = e else error.addSuppressed(e)
        }

        if (error != null) Log.w("PackageParser", "packageInfos", error)
        if (packageInfos.isEmpty()) throw FileNotFoundException("*.apk")

        return IPackageInfo.Zip(packageInfos)
    }
}