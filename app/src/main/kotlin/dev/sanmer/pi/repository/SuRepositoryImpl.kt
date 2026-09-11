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

    override val ownerPackageName: String
        get() = state.value.getOrElse({ it.ownerPackageName }) { super.ownerPackageName }

    override fun wrap(original: IBinder) = state.value.getOrElse({ it.wrap(original) }) { original }
}