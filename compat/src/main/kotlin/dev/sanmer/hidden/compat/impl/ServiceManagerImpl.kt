package dev.sanmer.hidden.compat.impl

import android.content.Context
import android.content.pm.IPackageManager
import android.os.IUserManager
import android.os.SELinux
import android.os.ServiceManager
import android.system.Os
import com.android.internal.app.IAppOpsService
import dev.sanmer.hidden.compat.ServiceManagerCompat
import dev.sanmer.hidden.compat.stub.IAppOpsServiceCompat
import dev.sanmer.hidden.compat.stub.IPackageManagerCompat
import dev.sanmer.hidden.compat.stub.IServiceManager
import dev.sanmer.hidden.compat.stub.IUserManagerCompat
import kotlin.system.exitProcess

internal class ServiceManagerImpl : IServiceManager.Stub() {
    private val appOpsService by lazy {
        AppOpsServiceCompatImpl(
            IAppOpsService.Stub.asInterface(
                ServiceManager.getService(Context.APP_OPS_SERVICE)
            )
        )
    }

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
                ServiceManager.getService(Context.USER_SERVICE)
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
        return ServiceManagerCompat.VERSION_CODE
    }

    override fun getSELinuxContext(): String {
        return SELinux.getContext()
    }

    override fun getPackageManager(): IPackageManagerCompat {
        return packageManager
    }

    override fun getUserManager(): IUserManagerCompat {
        return userManager
    }

    override fun getAppOpsService(): IAppOpsServiceCompat {
        return appOpsService
    }

    override fun destroy() {
        exitProcess(0)
    }
}