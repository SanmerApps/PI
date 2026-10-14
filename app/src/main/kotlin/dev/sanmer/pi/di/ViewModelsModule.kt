package dev.sanmer.pi.di

import dev.sanmer.pi.core.res.PackageParser
import dev.sanmer.pi.ui.main.HomeViewModel
import dev.sanmer.pi.ui.main.PackagesViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val ViewModelsModule = module {
    includes(RepositoriesModule)

    single {
        PackageParser(androidContext(), 48)
    }

    viewModel<HomeViewModel>()
    viewModel<PackagesViewModel>()
}