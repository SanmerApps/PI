package dev.sanmer.pi.viewmodel

import android.content.pm.PackageInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.hidden.compat.UserHandleCompat
import dev.sanmer.hidden.compat.delegate.PackageInstallerDelegate
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
) : ViewModel(), PackageInstallerDelegate.SessionCallback {
    private val pmCompat get() = ProviderCompat.packageManager
    private val delegate by lazy {
        PackageInstallerDelegate(
            pmCompat.packageInstallerCompat
        )
    }

    val isProviderAlive get() = ProviderCompat.isAlive

    private val sessionsFlow = MutableStateFlow(listOf<ISessionInfo>())
    val sessions get() = sessionsFlow.asStateFlow()

    var isLoading by mutableStateOf(true)
        private set

    init {
        Timber.d("SessionsViewModel init")
    }

    override fun onCreated(sessionId: Int) {
        loadData()
    }

    override fun onFinished(sessionId: Int, success: Boolean) {
        loadData()
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
            pmCompat.getPackageInfo(
                packageName, 0, UserHandleCompat.myUserId()
            )
        }.getOrNull()

    private fun loadData() {
        if (!isProviderAlive) return

        viewModelScope.launch {
            sessionsFlow.value = getAllSessions()
            isLoading = false
        }
    }

    fun registerCallback() {
        if (!isProviderAlive) return

        delegate.registerCallback(this)
        loadData()
    }

    fun unregisterCallback() {
        if (!isProviderAlive) return

        delegate.unregisterCallback(this)
    }

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