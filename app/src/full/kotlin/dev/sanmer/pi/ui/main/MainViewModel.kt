package dev.sanmer.pi.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sanmer.pi.Logger
import dev.sanmer.pi.datastore.model.Preference
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.repository.PreferenceRepository
import kotlinx.coroutines.launch

class MainViewModel(
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    var loadState by mutableStateOf<LoadState>(LoadState.Pending)
        private set

    val isPending inline get() = loadState.isPending
    val preference inline get() = loadState.preference
    val isNone inline get() = preference.provider == Provider.None

    private val logger = Logger.Android("MainViewModel")

    init {
        logger.d("init")
        preferenceObserver()
    }

    private fun preferenceObserver() {
        viewModelScope.launch {
            preferenceRepository.data.collect {
                loadState = LoadState.Ready(it)
            }
        }
    }

    fun setProvider(value: Provider) {
        viewModelScope.launch {
            preferenceRepository.setProvider(value)
        }
    }

    sealed class LoadState {
        abstract val preference: Preference

        data object Pending : LoadState() {
            override val preference = Preference()
        }

        data class Ready(
            override val preference: Preference
        ) : LoadState()

        val isPending inline get() = this is Pending
    }
}