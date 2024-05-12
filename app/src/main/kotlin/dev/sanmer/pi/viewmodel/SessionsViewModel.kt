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
import dev.sanmer.pi.repository.LocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val localRepository: LocalRepository
) : ViewModel(), PackageInstallerDelegate.SessionCallback {
    private val pmCompat get() = ProviderCompat.packageManager
    private val delegate by lazy {
        PackageInstallerDelegate(
            ProviderCompat::packageInstaller
        )
    }

    val isProviderAlive get() = ProviderCompat.isAlive

    private val sessionsFlow = MutableStateFlow(listOf<ISessionInfo>())
    val sessions get() = sessionsFlow.asStateFlow()

    var isLoading by mutableStateOf(true)
        private set

    init {
        Timber.d("SessionsViewModel init")
        providerObserver()
        dbObserver()
    }

    override fun onCreated(sessionId: Int) {
        Timber.d("onCreated")
        loadData()
    }

    override fun onFinished(sessionId: Int, success: Boolean) {
        Timber.d("onFinished")
        loadData()
    }

    private fun providerObserver() {
        ProviderCompat.isAliveFlow
            .onEach {
                if (it) loadData()

            }.launchIn(viewModelScope)
    }

    private fun dbObserver() {
        localRepository.getSessionAllAsFlow()
            .onEach {
                loadData()

            }.launchIn(viewModelScope)
    }

    private suspend fun getAllSessions() = withContext(Dispatchers.IO) {
        if (!isProviderAlive) return@withContext emptyList()

        val records = localRepository.getSessionAll().toMutableList()
        val currents = delegate.getAllSessions()
            .map {
                ISessionInfo(
                    session = it,
                    installer = it.installerPackageName?.let(::getPackageInfo),
                    app = it.appPackageName?.let(::getPackageInfo)
                )
            }.toMutableList()

        val currentIds = currents.map { it.sessionId }
        records.removeIf { currentIds.contains(it.sessionId) }

        currents.addAll(
            records.map {
                it.copy(
                    installer = it.installerPackageName?.let(::getPackageInfo),
                    app = it.appPackageName?.let(::getPackageInfo)
                )
            }
        )

        currents.sortedBy { it.sessionId }
    }

    private fun getPackageInfo(packageName: String): PackageInfo? =
        runCatching {
            pmCompat.getPackageInfo(
                packageName, 0, UserHandleCompat.myUserId()
            )
        }.getOrNull()

    private fun loadData() {
        viewModelScope.launch {
            sessionsFlow.value = getAllSessions()
            isLoading = false
        }
    }

    fun registerCallback() {
        if (isProviderAlive) {
            delegate.registerCallback(this)
        }
    }

    fun unregisterCallback() {
        if (isProviderAlive) {
            delegate.unregisterCallback(this)
        }
    }

    fun deleteAll() {
        viewModelScope.launch(Dispatchers.IO) {
            delegate.getMySessions().forEach {
                runCatching {
                    val session = delegate.openSession(it.sessionId)
                    session.abandon()
                }
            }

            localRepository.deleteSessionAll()
        }
    }
}