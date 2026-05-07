package dev.sanmer.pi.di

import dev.sanmer.pi.repository.PreferenceRepository
import dev.sanmer.pi.repository.PreferenceRepositoryImpl
import dev.sanmer.pi.repository.ServiceRepository
import dev.sanmer.pi.repository.ServiceRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val Repositories = module {
    singleOf(::PreferenceRepositoryImpl) { bind<PreferenceRepository>() }
    singleOf(::ServiceRepositoryImpl) { bind<ServiceRepository>() }
}