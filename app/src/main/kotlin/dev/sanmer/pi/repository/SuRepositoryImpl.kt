package dev.sanmer.pi.repository

import android.content.Context
import android.os.IBinder
import android.util.Log
import dev.sanmer.pi.model.LoadData
import dev.sanmer.pi.model.LoadData.Default.loadData
import dev.sanmer.su.AnySu
import dev.sanmer.su.BinderWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SuRepositoryImpl(
    private val context: Context
) : SuRepository {
    private val _state = MutableStateFlow<LoadData<BinderWrapper>>(LoadData.Loading)
    override val state = _state.asStateFlow().onSubscription { launch() }

    private val mutex = Mutex()
    private suspend fun launch() = mutex.withLock {
        _state.update {
            if (it.isSuccess) it else loadData {
                Log.d(TAG, "launch")
                AnySu.launch(context)
            }.onFailure { e ->
                Log.e(TAG, "launch", e)
            }
        }
    }

    override val ownerPackageName: String
        get() = state.value.getOrElse({ it.ownerPackageName }) { super.ownerPackageName }

    override fun wrap(original: IBinder) = state.value.getOrElse({ it.wrap(original) }) { original }

    private companion object Default {
        const val TAG = "SuRepositoryImpl"
    }
}