package dev.sanmer.su

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
import android.os.SELinux
import android.system.Os
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object LibSu {
    internal const val BINDER_TRANSACTION = 84398154

    private class ShellInitializer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell) = shell.isRoot
    }

    init {
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setInitializers(ShellInitializer::class.java)
                .setTimeout(10)
        )
    }

    private class ServiceImpl : IService.Stub() {
        override fun getUid() = Os.getuid()

        override fun getPid() = Os.getpid()

        override fun getSELinuxContext(): String = SELinux.getContext()

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int) =
            if (code == BINDER_TRANSACTION) {
                data.enforceInterface(DESCRIPTOR)
                val targetBinder = data.readStrongBinder()
                val targetCode = data.readInt()
                val targetFlags = data.readInt()
                val newData = Parcel.obtain()
                try {
                    newData.appendFrom(data, data.dataPosition(), data.dataAvail())
                    val id = clearCallingIdentity()
                    targetBinder.transact(targetCode, newData, reply, targetFlags)
                    restoreCallingIdentity(id)
                } finally {
                    newData.recycle()
                }
                true
            } else {
                super.onTransact(code, data, reply, flags)
            }
    }

    private class Service : RootService() {
        override fun onBind(intent: Intent): IBinder {
            return ServiceImpl()
        }
    }

    private class BinderImpl(
        private val bearer: IBinder,
        private val original: IBinder
    ) : IBinder {
        override fun getInterfaceDescriptor() = original.interfaceDescriptor

        override fun pingBinder() = original.pingBinder()

        override fun isBinderAlive() = original.isBinderAlive

        override fun queryLocalInterface(descriptor: String) = null

        override fun dump(fd: FileDescriptor, args: Array<out String>?) =
            original.dump(fd, args)

        override fun dumpAsync(fd: FileDescriptor, args: Array<out String>?) =
            original.dumpAsync(fd, args)

        override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) =
            original.linkToDeath(recipient, flags)

        override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int) =
            original.unlinkToDeath(recipient, flags)

        override fun transact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            val newData = Parcel.obtain()
            try {
                newData.apply {
                    writeInterfaceToken(IService.DESCRIPTOR)
                    writeStrongBinder(original)
                    writeInt(code)
                    writeInt(flags)
                    appendFrom(data, 0, data.dataSize())
                }
                bearer.transact(BINDER_TRANSACTION, newData, reply, 0)
            } finally {
                newData.recycle()
            }
            return true
        }
    }

    class Wrapper internal constructor(
        private val service: IService
    ) : BinderWrapper {
        override fun getUid(): Int {
            return service.uid
        }

        override fun getSELinuxContext(): String {
            return service.seLinuxContext
        }

        override fun wrap(original: IBinder): IBinder {
            return BinderImpl(
                bearer = service.asBinder(),
                original = original
            )
        }
    }

    suspend fun launch(context: Context) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val intent = Intent(context, Service::class.java)
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val service = IService.Stub.asInterface(binder)
                    continuation.resume(Wrapper(service))
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    continuation.resumeWithException(
                        IllegalStateException("IService destroyed")
                    )
                }

                override fun onBindingDied(name: ComponentName?) {
                    continuation.resumeWithException(
                        IllegalStateException("IService destroyed")
                    )
                }
            }
            RootService.bind(intent, connection)
            continuation.invokeOnCancellation {
                RootService.unbind(connection)
            }
        }
    }
}