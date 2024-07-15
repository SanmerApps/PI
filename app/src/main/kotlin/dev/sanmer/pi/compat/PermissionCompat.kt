package dev.sanmer.pi.compat

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dev.sanmer.pi.ktx.findActivity
import java.util.UUID

object PermissionCompat {
    @JvmInline
    value class PermissionState(
        private val state: Map<String, Boolean>
    ) {
        val allGranted get() = state.all { it.value }
    }

    fun checkPermissions(
        context: Context,
        permissions: List<String>
    ) = PermissionState(
        permissions.associateWith {
            ContextCompat.checkSelfPermission(
                context, it
            ) == PackageManager.PERMISSION_GRANTED
        }
    )

    fun checkPermission(
        context: Context,
        permission: String
    ) = checkPermissions(
        context = context,
        permissions = listOf(permission)
    ).allGranted

    fun requestPermissions(
        context: Context,
        permissions: List<String>,
        callback: (PermissionState) -> Unit = {}
    ) {
        val activity = context.findActivity()
        if (activity !is ActivityResultRegistryOwner) return

        val activityResultRegistry = activity.activityResultRegistry
        val launcher = activityResultRegistry.register(
            key = UUID.randomUUID().toString(),
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            callback = { callback(PermissionState(it)) }
        )

        launcher.launch(permissions.toTypedArray())
    }

    fun requestPermission(
        context: Context,
        permission: String,
        callback: (Boolean) -> Unit = {}
    ) = requestPermissions(
        context = context,
        permissions = listOf(permission),
        callback = { callback(it.allGranted) }
    )
}