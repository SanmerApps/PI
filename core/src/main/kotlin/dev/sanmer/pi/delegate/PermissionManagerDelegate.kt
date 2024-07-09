package dev.sanmer.pi.delegate

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
            BuildCompat.atLeastU -> try {
                permissionManager.grantRuntimePermission(
                    packageName,
                    permissionName,
                    "default:0",
                    userId
                )
            } catch (e: NoSuchMethodError) {
                try {
                    permissionManager.grantRuntimePermission(
                        packageName,
                        permissionName,
                        0,
                        userId)
                } catch (e: NoSuchMethodError) {
                    permissionManager.grantRuntimePermission(
                        packageName,
                        permissionName,
                        userId
                    )
                }
            }

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
            BuildCompat.atLeastU -> try {
                permissionManager.revokeRuntimePermission(
                    packageName,
                    permissionName,
                    "default:0",
                    userId,
                    null
                )
            } catch (e: NoSuchMethodError) {
                try {
                    permissionManager.revokeRuntimePermission(
                        packageName,
                        permissionName,
                        0,
                        userId,
                        null
                    )
                } catch (e: NoSuchMethodError) {
                    permissionManager.revokeRuntimePermission(
                        packageName,
                        permissionName,
                        userId
                    )
                }
            }

            BuildCompat.atLeastR -> permissionManager.revokeRuntimePermission(
                packageName,
                permissionName,
                userId
            )

            else -> packageManager.revokeRuntimePermission(
                packageName,
                permissionName,
                userId
            )
        }
    }

    fun checkPermission(packageName: String, permissionName: String, userId: Int): Int {
        return when {
            BuildCompat.atLeastS -> packageManager.checkPermission(
                permissionName,
                packageName,
                userId
            )

            BuildCompat.atLeastR -> permissionManager.checkPermission(
                permissionName,
                packageName,
                userId
            )

            else -> packageManager.checkPermission(
                permissionName,
                packageName,
                userId
            )
        }
    }
}