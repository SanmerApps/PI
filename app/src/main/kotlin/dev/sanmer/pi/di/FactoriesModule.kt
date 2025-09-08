package dev.sanmer.pi.di

import dev.sanmer.pi.factory.BundleFactory
import dev.sanmer.pi.factory.VersionFactory
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val Factories = module {
    factoryOf(::VersionFactory)
    factoryOf(::BundleFactory)
}