package dev.sanmer.hidden.compat.impl

import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import dev.sanmer.hidden.compat.stub.IPackageInstallerSessionCompat

internal class PackageInstallerSessionCompatImpl(
    private val original: IPackageInstallerSession
) : IPackageInstallerSessionCompat.Stub() {
    override fun openWrite(
        name: String,
        offsetBytes: Long,
        lengthBytes: Long
    ): ParcelFileDescriptor {
        return original.openWrite(name, offsetBytes, lengthBytes)
    }

    override fun openRead(name: String): ParcelFileDescriptor {
        return original.openRead(name)
    }

    override fun write(
        name: String,
        offsetBytes: Long,
        lengthBytes: Long,
        fd: ParcelFileDescriptor
    ) {
        original.write(name, offsetBytes, lengthBytes, fd)
    }

    override fun close() {
        original.close()
    }

    override fun commit(statusReceiver: IntentSender, forTransferred: Boolean) {
        original.commit(statusReceiver, forTransferred)
    }

    override fun abandon() {
        original.abandon()
    }
}