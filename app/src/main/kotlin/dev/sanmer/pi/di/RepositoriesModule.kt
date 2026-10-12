package dev.sanmer.pi.di

import dev.sanmer.pi.core.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.core.delegate.PackageInstallerDelegate
import dev.sanmer.pi.core.delegate.PackageManagerDelegate
import dev.sanmer.pi.core.delegate.PermissionManagerDelegate
import dev.sanmer.pi.core.delegate.UserManagerDelegate
import dev.sanmer.pi.repository.SuRepository
import dev.sanmer.pi.repository.SuRepositoryImpl
import dev.sanmer.su.BinderWrapper
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val RepositoriesModule = module {
    single<SuRepositoryImpl>() binds arrayOf(SuRepository::class, BinderWrapper::class)

    factory(named("ownerPackageName")) {
        get<BinderWrapper>().ownerPackageName
    }

    factory {
        AppOpsManagerDelegate { get<BinderWrapper>().wrap(this) }
    }

    factory {
        PackageManagerDelegate { get<BinderWrapper>().wrap(this) }
    }

    factory {
        PackageInstallerDelegate { get<BinderWrapper>().wrap(this) }
    }

    factory {
        PermissionManagerDelegate { get<BinderWrapper>().wrap(this) }
    }

    factory {
        UserManagerDelegate { get<BinderWrapper>().wrap(this) }
    }
}