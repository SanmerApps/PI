package dev.sanmer.pi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.Compat
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val isProviderAlive get() = Compat.isAlive
    val providerPlatform get() = Compat.get("") { platform }

    init {
        Timber.d("SettingsViewModel init")
    }

    fun setProvider(value: Provider) {
        viewModelScope.launch {
            userPreferencesRepository.setProvider(value)
        }
    }

    fun tryStartProvider() {
        viewModelScope.launch {
            val userPreferences = userPreferencesRepository.data.first()
            Compat.init(userPreferences.provider)
        }
    }
}