package dev.sanmer.pi.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.repository.LocalRepository
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val localRepository: LocalRepository
) : ViewModel() {
    val authorized get() = localRepository.getAuthorizedAllAsFlow().map { it.size }

    init {
        Timber.d("HomeViewModel init")
    }
}