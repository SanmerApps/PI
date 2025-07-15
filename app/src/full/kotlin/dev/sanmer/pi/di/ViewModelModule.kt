package dev.sanmer.pi.di

import dev.sanmer.pi.ui.main.MainViewModel
import dev.sanmer.pi.ui.screens.apps.AppsViewModel
import dev.sanmer.pi.ui.screens.install.InstallViewModel
import dev.sanmer.pi.ui.screens.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val ViewModel = module {
    viewModelOf(::InstallViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::AppsViewModel)
    viewModelOf(::SettingsViewModel)
}