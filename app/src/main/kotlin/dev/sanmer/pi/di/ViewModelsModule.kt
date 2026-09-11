package dev.sanmer.pi.di

import dev.sanmer.pi.ui.main.MainViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val ViewModelsModule = module {
    includes(RepositoriesModule)
    viewModel<MainViewModel>()
}