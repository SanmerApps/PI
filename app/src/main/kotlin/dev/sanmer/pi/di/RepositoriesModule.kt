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

    single(named("ownerPackageName")) {
        get<BinderWrapper>().ownerPackageName
    }

    single {
        AppOpsManagerDelegate { get<BinderWrapper>().wrap(this) }
    }

    single {
        PackageManagerDelegate { get<BinderWrapper>().wrap(this) }
    }

    single {
        PackageInstallerDelegate { get<BinderWrapper>().wrap(this) }
    }

    single {
        PermissionManagerDelegate { get<BinderWrapper>().wrap(this) }
    }

    single {
        UserManagerDelegate { get<BinderWrapper>().wrap(this) }
    }
}