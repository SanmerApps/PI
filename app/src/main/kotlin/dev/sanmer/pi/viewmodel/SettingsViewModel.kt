package dev.sanmer.pi.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.datastore.Provider
import dev.sanmer.pi.repository.UserPreferencesRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    init {
        Timber.d("SettingsViewModel init")
    }

    fun setDynamicColor(value: Boolean) =
        userPreferencesRepository.setDynamicColor(value)

    fun setProvider(value: Provider) =
        userPreferencesRepository.setProvider(value)

    fun setSelfUpdate(value: Boolean) =
        userPreferencesRepository.setSelfUpdate(value)
}