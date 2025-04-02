package dev.sanmer.pi.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.datastore.model.Preference
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.repository.PreferenceRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    var state by mutableStateOf<LoadState>(LoadState.Pending)
        private set

    val isPending inline get() = state is LoadState.Pending
    val preference inline get() = state.preference

    init {
        Timber.d("MainViewModel init")
        preferenceObserver()
    }

    private fun preferenceObserver() {
        viewModelScope.launch {
            preferenceRepository.data.collect {
                state = LoadState.Ready(it)
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
    }
}