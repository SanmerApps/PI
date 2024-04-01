package android.content.pm;

import android.content.IntentSender;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public interface IPackageInstallerSession extends IInterface {

    ParcelFileDescriptor openWrite(String name, long offsetBytes, long lengthBytes) throws RemoteException;

    ParcelFileDescriptor openRead(String name) throws RemoteException;

    void write(String name, long offsetBytes, long lengthBytes, ParcelFileDescriptor fd) throws RemoteException;

    void close() throws RemoteException;

    void commit(IntentSender statusReceiver, boolean forTransferred) throws RemoteException;

    void abandon() throws RemoteException;

    abstract class Stub extends Binder implements IPackageInstallerSession {

        public static IPackageInstallerSession asInterface(IBinder binder) {
            throw new RuntimeException("Stub!");
        }
    }
}