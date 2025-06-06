package dev.sanmer.pi.repository

import dev.sanmer.pi.datastore.model.Preference
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceRepository @Inject constructor() {
    val data = flowOf(Preference())
}