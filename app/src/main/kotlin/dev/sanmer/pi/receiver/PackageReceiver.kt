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
        val actionStr = intent?.action.orEmpty()
        val packageName = intent?.data?.encodedSchemeSpecificPart.orEmpty()

        if (filter.hasAction(actionStr)) {
            val action = Action.from(actionStr)
            _eventFlow.value = Event(action, packageName)

            Timber.d("packageChanged<$action>: $packageName")
        }
    }

    data class Event(
        val action: Action = Action.None,
        val packageName: String = ""
    )

    enum class Action(val str: String) {
        None("None"),
        Added(Intent.ACTION_PACKAGE_ADDED),
        Replaced(Intent.ACTION_PACKAGE_REPLACED),
        Removed(Intent.ACTION_PACKAGE_REMOVED);

        companion object {
            fun from(str: String) = entries.first { it.str == str }
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

        private val _eventFlow = MutableStateFlow(Event())
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