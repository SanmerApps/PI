package dev.sanmer.pi.di

import dev.sanmer.pi.repository.SuRepository
import dev.sanmer.pi.repository.SuRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val Repositories = module {
    singleOf(::SuRepositoryImpl) { bind<SuRepository>() }
}