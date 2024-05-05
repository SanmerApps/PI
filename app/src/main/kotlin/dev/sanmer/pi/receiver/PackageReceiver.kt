package dev.sanmer.pi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class PackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val packageName = intent?.data?.encodedSchemeSpecificPart.orEmpty()
        val action = intent?.action.orEmpty()

        if (filter.hasAction(action)) {
            Timber.d("onReceive: action = ${action}, packageName = $packageName")
            _eventFlow.value = action to packageName
        }
    }

    companion object {
        private val receiver by lazy { PackageReceiver() }
        private val filter by lazy {
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }
        }

        private val _eventFlow = MutableStateFlow("" to "")
        val eventFlow get() = _eventFlow.asStateFlow()

        fun register(context: Context) {
            // noinspection InlinedApi
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        }

        fun unregister(context: Context) {
            context.unregisterReceiver(receiver)
        }
    }
}