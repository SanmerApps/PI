package dev.sanmer.pi.core.delegate

import android.companion.virtual.VirtualDeviceManagerHidden
import android.content.pm.IPackageManager
import android.os.IBinder
import android.os.ServiceManager
import android.permission.IPermissionManager
import dev.sanmer.pi.core.compat.BuildCompat

class PermissionManagerDelegate(
    private val proxy: IBinder.() -> IBinder = { this }
) {
    private val packageManager by lazy {
        IPackageManager.Stub.asInterface(
            ServiceManager.getService("package").proxy()
        )
    }

    private val permissionManager by lazy {
        IPermissionManager.Stub.asInterface(
            ServiceManager.getService("permissionmgr").proxy()
        )
    }

    fun grantRuntimePermission(packageName: String, permissionName: String, userId: Int) {
        when {
            BuildCompat.atLeastV -> permissionManager.grantRuntimePermission(
                packageName,
                permissionName,
                VirtualDeviceManagerHidden.PERSISTENT_DEVICE_ID_DEFAULT,
                userId
            )

            BuildCompat.atLeastR -> permissionManager.grantRuntimePermission(
                packageName,
                permissionName,
                userId
            )

            else -> packageManager.grantRuntimePermission(
                packageName,
                permissionName,
                userId
            )
        }
    }

    fun revokeRuntimePermission(packageName: String, permissionName: String, userId: Int) {
        when {
            BuildCompat.atLeastV -> permissionManager.revokeRuntimePermission(
                packageName,
                permissionName,
                VirtualDeviceManagerHidden.PERSISTENT_DEVICE_ID_DEFAULT,
                userId,
                null
            )

            BuildCompat.atLeastR -> permissionManager.revokeRuntimePermission(
                packageName,
                permissionName,
                userId,
                null
            )

            else -> packageManager.revokeRuntimePermission(
                packageName,
                permissionName,
                userId,
                null
            )
        }
    }

    fun checkPermission(packageName: String, permissionName: String, userId: Int): Int {
        return when {
            BuildCompat.atLeastV -> permissionManager.checkPermission(
                packageName,
                permissionName,
                VirtualDeviceManagerHidden.PERSISTENT_DEVICE_ID_DEFAULT,
                userId
            )

            else -> packageManager.checkPermission(
                packageName,
                permissionName,
                userId
            )
        }
    }
}