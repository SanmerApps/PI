package dev.sanmer.pi.repository

import dev.sanmer.pi.model.LoadData
import dev.sanmer.su.BinderWrapper
import kotlinx.coroutines.flow.StateFlow

interface SuRepository : BinderWrapper {
    val state: StateFlow<LoadData<BinderWrapper>>
    suspend fun launch()
}