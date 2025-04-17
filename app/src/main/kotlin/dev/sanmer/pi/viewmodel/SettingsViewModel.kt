package dev.sanmer.pi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.repository.PreferenceRepository
import dev.sanmer.pi.repository.ServiceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val serviceRepository: ServiceRepository
) : ViewModel() {
    val state = serviceRepository.state

    init {
        Timber.d("SettingsViewModel init")
    }

    fun setProvider(value: Provider) {
        viewModelScope.launch {
            preferenceRepository.setProvider(value)
        }
    }

    fun restart() {
        viewModelScope.launch {
            val preference = preferenceRepository.data.first()
            serviceRepository.recreate(preference.provider)
        }
    }
}