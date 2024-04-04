package dev.sanmer.pi.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.hidden.compat.ContextCompat.userId
import dev.sanmer.hidden.compat.delegate.PackageInstallerDelegate
import dev.sanmer.hidden.compat.delegate.SessionCallbackDelegate
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.model.ISessionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SessionsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private val context: Context by lazy { getApplication() }
    private val pmCompat get() = ProviderCompat.packageManagerCompat
    private val delegate by lazy {
        PackageInstallerDelegate(
            pmCompat.packageInstallerCompat
        )
    }

    private val isProviderAlive get() = ProviderCompat.isAlive

    private val sessionsFlow = MutableStateFlow(listOf<ISessionInfo>())
    val sessions get() = sessionsFlow.asStateFlow()

    var isLoading by mutableStateOf(true)
        private set

    private val mCallback = object : SessionCallbackDelegate() {
        override fun onCreated(sessionId: Int) {
            loadData()
        }

        override fun onFinished(sessionId: Int, success: Boolean) {
            loadData()
        }
    }

    init {
        Timber.d("SessionsViewModel init")
        loadData()
    }

    fun registerCallback() {
        if (!isProviderAlive) return

        delegate.registerCallback(mCallback)
    }

    fun unregisterCallback() {
        if (!isProviderAlive) return

        delegate.unregisterCallback(mCallback)
    }

    private fun loadData() {
        if (!isProviderAlive) return

        viewModelScope.launch {
            sessionsFlow.value = getAllSessions()
            isLoading = false
        }
    }

    private suspend fun getAllSessions() = withContext(Dispatchers.IO) {
        delegate.getAllSessions()
            .map {
                ISessionInfo(
                    sessionInfo = it,
                    installer = it.installerPackageName?.let(::getPackageInfo),
                    app = it.appPackageName?.let(::getPackageInfo)
                )
            }
    }

    private fun getPackageInfo(packageName: String): PackageInfo? =
        runCatching {
            pmCompat.getPackageInfo(packageName, 0, context.userId)
        }.getOrNull()

    fun abandonAll() {
        viewModelScope.launch(Dispatchers.IO) {
            delegate.getMySessions().forEach {
                val session = delegate.openSession(it.sessionId)
                runCatching {
                    session.abandon()
                }
            }
        }
    }
}