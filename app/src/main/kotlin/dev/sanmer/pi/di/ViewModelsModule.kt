package dev.sanmer.pi.di

import dev.sanmer.pi.ui.main.HomeViewModel
import dev.sanmer.pi.ui.main.PackagesViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val ViewModelsModule = module {
    includes(RepositoriesModule)
    includes(PackageParserModule)
    viewModel<HomeViewModel>()
    viewModel<PackagesViewModel>()
}