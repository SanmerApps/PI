package dev.sanmer.pi.ui.di

import dev.sanmer.pi.ui.screens.install.InstallViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val ViewModels = module {
    viewModelOf(::InstallViewModel)
}