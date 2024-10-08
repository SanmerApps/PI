package dev.sanmer.pi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.Compat
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.repository.PreferenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preference: PreferenceRepository
) : ViewModel() {
    val isAlive get() = Compat.isAlive
    val platform get() = Compat.get("") { platform }

    init {
        Timber.d("SettingsViewModel init")
    }

    fun setProvider(value: Provider) {
        viewModelScope.launch {
            preference.setProvider(value)
        }
    }

    fun tryStart() {
        viewModelScope.launch {
            val preference = preference.data.first()
            Compat.init(preference.provider)
        }
    }
}