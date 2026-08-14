package dev.sanmer.pi.repository

import android.content.Context
import android.os.IBinder
import android.util.Log
import dev.sanmer.pi.core.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.core.delegate.PackageInstallerDelegate
import dev.sanmer.pi.core.delegate.PackageManagerDelegate
import dev.sanmer.pi.core.delegate.PermissionManagerDelegate
import dev.sanmer.pi.core.delegate.UserManagerDelegate
import dev.sanmer.pi.model.LoadData
import dev.sanmer.pi.model.LoadData.Default.loadData
import dev.sanmer.su.AnySu
import dev.sanmer.su.BinderWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SuRepositoryImpl(
    private val context: Context
) : SuRepository {
    private val _state = MutableStateFlow<LoadData<BinderWrapper>>(LoadData.Pending)
    override val state = _state.asStateFlow()

    override suspend fun launch() {
        _state.update { LoadData.Loading }
        _state.update {
            loadData {
                AnySu.launch(context)
            }.onFailure {
                Log.e("SU", it.stackTraceToString())
            }
        }
    }

    private fun IBinder.proxy() = state.value.getOrElse({ it.wrap(this) }) { this }

    override fun getAppOpsManager() = AppOpsManagerDelegate { proxy() }

    override fun getPackageManager() = PackageManagerDelegate { proxy() }

    override fun getPackageInstaller() = PackageInstallerDelegate { proxy() }

    override fun getPermissionManager() = PermissionManagerDelegate { proxy() }

    override fun getUserManager() = UserManagerDelegate { proxy() }
}