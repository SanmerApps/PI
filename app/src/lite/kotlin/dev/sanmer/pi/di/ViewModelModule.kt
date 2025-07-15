package dev.sanmer.pi.di

import dev.sanmer.pi.ui.screens.install.InstallViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val ViewModel = module {
    viewModelOf(::InstallViewModel)
}