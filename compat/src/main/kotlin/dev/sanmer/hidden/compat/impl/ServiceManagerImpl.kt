package dev.sanmer.hidden.compat.impl

import android.content.pm.IPackageManager
import android.os.IUserManager
import android.os.SELinux
import android.os.ServiceManager
import android.system.Os
import dev.sanmer.hidden.compat.BuildConfig
import dev.sanmer.hidden.compat.stub.IPackageManagerCompat
import dev.sanmer.hidden.compat.stub.IServiceManager
import dev.sanmer.hidden.compat.stub.IUserManagerCompat
import kotlin.system.exitProcess

internal class ServiceManagerImpl : IServiceManager.Stub() {
    private val packageManager by lazy {
        PackageManagerCompatImpl(
            IPackageManager.Stub.asInterface(
                ServiceManager.getService("package")
            )
        )
    }

    private val userManager by lazy {
        UserManagerCompatImpl(
            IUserManager.Stub.asInterface(
                ServiceManager.getService("user")
            )
        )
    }

    override fun getUid(): Int {
        return Os.getuid()
    }

    override fun getPid(): Int {
        return Os.getpid()
    }

    override fun getVersion(): Int {
        return BuildConfig.VERSION_CODE
    }

    override fun getSELinuxContext(): String {
        return SELinux.getContext()
    }

    override fun getPackageManagerCompat(): IPackageManagerCompat {
        return packageManager
    }

    override fun getUserManagerCompat(): IUserManagerCompat {
        return userManager
    }

    override fun destroy() {
        exitProcess(0)
    }
}