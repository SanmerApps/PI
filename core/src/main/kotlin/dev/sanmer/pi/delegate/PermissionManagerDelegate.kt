package dev.sanmer.pi.delegate

import android.companion.virtual.VirtualDeviceManagerHidden
import android.content.Context
import android.content.pm.IPackageManager
import android.permission.IPermissionManager
import dev.sanmer.pi.BuildCompat
import dev.sanmer.su.IServiceManager
import dev.sanmer.su.ServiceManagerCompat.getSystemService

class PermissionManagerDelegate(
    private val service: IServiceManager
) {
    private val packageManager by lazy {
        IPackageManager.Stub.asInterface(
            service.getSystemService("package")
        )
    }

    private val permissionManager by lazy {
        IPermissionManager.Stub.asInterface(
            service.getSystemService("permissionmgr")
        )
    }

    fun grantRuntimePermission(packageName: String, permissionName: String, userId: Int) {
        when {
            BuildCompat.atLeastU ->
                rollback {
                    permissionManager.grantRuntimePermission(
                        packageName,
                        permissionName,
                        VirtualDeviceManagerHidden.PERSISTENT_DEVICE_ID_DEFAULT,
                        userId
                    )
                } ?: rollback {
                    permissionManager.grantRuntimePermission(
                        packageName,
                        permissionName,
                        Context.DEVICE_ID_DEFAULT,
                        userId
                    )
                } ?: permissionManager.grantRuntimePermission(
                    packageName,
                    permissionName,
                    userId
                )

            else -> permissionManager.grantRuntimePermission(
                packageName,
                permissionName,
                userId
            )
        }
    }

    fun revokeRuntimePermission(packageName: String, permissionName: String, userId: Int) {
        when {
            BuildCompat.atLeastU ->
                rollback {
                    permissionManager.revokeRuntimePermission(
                        packageName,
                        permissionName,
                        VirtualDeviceManagerHidden.PERSISTENT_DEVICE_ID_DEFAULT,
                        userId,
                        null
                    )
                } ?: rollback {
                    permissionManager.revokeRuntimePermission(
                        packageName,
                        permissionName,
                        Context.DEVICE_ID_DEFAULT,
                        userId,
                        null
                    )
                } ?: permissionManager.revokeRuntimePermission(
                    packageName,
                    permissionName,
                    userId,
                    null
                )

            else -> permissionManager.revokeRuntimePermission(
                packageName,
                permissionName,
                userId,
                null
            )
        }
    }

    fun checkPermission(packageName: String, permissionName: String, userId: Int): Int {
        return when {
            BuildCompat.atLeastU ->
                rollback {
                    permissionManager.checkPermission(
                        packageName,
                        permissionName,
                        VirtualDeviceManagerHidden.PERSISTENT_DEVICE_ID_DEFAULT,
                        userId
                    )
                } ?: rollback {
                    permissionManager.checkPermission(
                        packageName,
                        permissionName,
                        Context.DEVICE_ID_DEFAULT,
                        userId
                    )
                } ?: packageManager.checkPermission(
                    packageName,
                    permissionName,
                    userId
                )

            else -> packageManager.checkPermission(
                permissionName,
                packageName,
                userId
            )
        }
    }

    private inline fun <T> rollback(block: () -> T): T? {
        return try {
            block()
        } catch (_: NoSuchMethodError) {
            null
        } catch (_: NoSuchFieldError) {
            null
        }
    }
}