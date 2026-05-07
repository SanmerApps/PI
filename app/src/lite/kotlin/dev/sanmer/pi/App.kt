package dev.sanmer.pi

import dev.sanmer.pi.di.Factories
import dev.sanmer.pi.di.Repositories
import dev.sanmer.pi.ui.di.ViewModels

class App : BaseApp(Factories, Repositories, ViewModels)