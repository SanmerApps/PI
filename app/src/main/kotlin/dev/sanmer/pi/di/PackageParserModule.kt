package dev.sanmer.pi.di

import androidx.compose.ui.unit.dp
import dev.sanmer.pi.coil.AppIconFactory
import dev.sanmer.pi.core.res.PackageParser
import dev.sanmer.pi.core.res.ResourceParser
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val PackageParserModule = module {
    single<ResourceParser.BitmapFactory> {
        AppIconFactory(androidContext(), 48.dp, false)
    }

    single {
        PackageParser(get())
    }
}