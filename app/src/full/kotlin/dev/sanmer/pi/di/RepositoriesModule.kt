package dev.sanmer.pi.di

import dev.sanmer.pi.repository.PreferenceRepository
import dev.sanmer.pi.repository.PreferenceRepositoryImpl
import dev.sanmer.pi.repository.ServiceRepository
import dev.sanmer.pi.repository.ServiceRepositoryImpl
import org.koin.dsl.module

val Repositories = module {
    includes(DataStore)

    single<PreferenceRepository> {
        PreferenceRepositoryImpl(get())
    }

    single<ServiceRepository> {
        ServiceRepositoryImpl(get())
    }
}