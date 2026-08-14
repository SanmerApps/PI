package dev.sanmer.pi.di

import dev.sanmer.pi.ui.main.MainViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val ViewModels = module {
    viewModelOf(::MainViewModel)
}