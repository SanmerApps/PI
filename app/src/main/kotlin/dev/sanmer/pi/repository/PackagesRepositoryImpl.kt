package dev.sanmer.pi.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.util.Log
import androidx.core.content.ContextCompat
import dev.sanmer.pi.core.compat.ContextCompat.userId
import dev.sanmer.pi.core.delegate.PackageManagerDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PackagesRepositoryImpl(
    private val context: Context,
    private val packageManager: PackageManagerDelegate
) : PackagesRepository {
    private val packageStateFlow = callbackFlow {
        val packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val packageName = intent.data?.schemeSpecificPart ?: return
                val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> if (!isReplacing) trySend(
                        PackageState.Update(packageName)
                    )

                    Intent.ACTION_PACKAGE_REMOVED -> if (!isReplacing) trySend(
                        PackageState.Remove(packageName)
                    )

                    Intent.ACTION_PACKAGE_REPLACED -> trySend(
                        PackageState.Update(packageName)
                    )
                }
            }
        }

        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        ContextCompat.registerReceiver(
            context,
            packageReceiver,
            packageFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        awaitClose {
            context.unregisterReceiver(packageReceiver)
        }
    }

    private val mutex = Mutex()
    private val installedPackages = hashMapOf<String, PackageInfo>()
    private val packageStateResolved = mutableListOf<PackageState>()

    private suspend fun getInstalledPackages(flags: Int) =
        withContext(Dispatchers.IO) {
            packageManager.getInstalledPackages(flags, context.userId)
                .associateBy { it.packageName }
        }

    private suspend fun getPackageInfo(packageName: String, flags: Int) =
        withContext(Dispatchers.IO) {
            packageManager.getPackageInfo(packageName, flags, context.userId)
        }

    private suspend inline fun <R> updatePackages(
        state: PackageState,
        flags: Int,
        block: Map<String, PackageInfo>.() -> R
    ) = mutex.withLock {
        if (packageStateResolved.lastOrNull() != state) {
            when (state) {
                PackageState.Start -> {
                    Log.d(TAG, "Start")
                    runCatching {
                        installedPackages.putAll(getInstalledPackages(flags))
                    }.onFailure {
                        Log.e(TAG, "Start", it)
                    }
                }

                is PackageState.Update -> {
                    Log.d(TAG, "Update(${state.packageName})")
                    runCatching {
                        installedPackages.put(
                            state.packageName,
                            getPackageInfo(state.packageName, flags)
                        )
                    }.onFailure {
                        Log.e(TAG, "Update(${state.packageName})", it)
                    }
                }

                is PackageState.Remove -> {
                    Log.d(TAG, "Remove(${state.packageName})")
                    installedPackages.remove(state.packageName)
                }
            }
            packageStateResolved.add(state)
        }
        block(installedPackages)
    }

    override fun getInstalledPackagesAsFlow(flags: Int) = packageStateFlow.onStart {
        emit(PackageState.Start)
    }.map {
        updatePackages(it, flags) { values.toList() }
    }

    override fun getPackageInfoAsFlow(packageName: String, flags: Int) = packageStateFlow.onStart {
        emit(PackageState.Update(packageName))
    }.mapNotNull {
        updatePackages(it, flags) { get(packageName) }
    }

    private val pm = context.packageManager
    override fun loadLabel(packageInfo: PackageInfo) =
        packageInfo.applicationInfo?.loadLabel(pm) ?: ""

    private sealed interface PackageState {
        data object Start : PackageState

        @JvmInline
        value class Update(val packageName: String) : PackageState

        @JvmInline
        value class Remove(val packageName: String) : PackageState
    }

    private companion object Default {
        const val TAG = "PackagesRepositoryImpl"
    }
}