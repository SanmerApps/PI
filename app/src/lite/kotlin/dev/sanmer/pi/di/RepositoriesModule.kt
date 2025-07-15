package dev.sanmer.pi.di

import dev.sanmer.pi.repository.PreferenceRepository
import dev.sanmer.pi.repository.PreferenceRepositoryImpl
import dev.sanmer.pi.repository.ServiceRepository
import dev.sanmer.pi.repository.ServiceRepositoryImpl
import org.koin.dsl.module

val Repositories = module {
    single<PreferenceRepository> {
        PreferenceRepositoryImpl()
    }

    single<ServiceRepository> {
        ServiceRepositoryImpl()
    }
}