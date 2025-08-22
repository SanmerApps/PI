package dev.sanmer.pi.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sanmer.pi.Logger
import dev.sanmer.pi.datastore.model.DarkMode
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.repository.PreferenceRepository
import dev.sanmer.pi.repository.ServiceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferenceRepository: PreferenceRepository,
    private val serviceRepository: ServiceRepository
) : ViewModel() {
    val state = serviceRepository.state

    private val logger = Logger.Android("SettingsViewModel")

    init {
        logger.d("init")
    }

    fun setProvider(value: Provider) {
        viewModelScope.launch {
            preferenceRepository.setProvider(value)
        }
    }

    fun setAutomatic(value: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setAutomatic(value)
        }
    }

    fun setDarkMode(value: DarkMode) {
        viewModelScope.launch {
            preferenceRepository.setDarkMode(value)
        }
    }

    fun restart() {
        viewModelScope.launch {
            val preference = preferenceRepository.data.first()
            serviceRepository.recreate(preference.provider)
        }
    }
}