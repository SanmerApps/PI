package com.android.internal.app;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

public interface IAppOpsCallback extends IInterface {

    void opChanged(int op, int uid, String packageName) throws RemoteException;

    @RequiresApi(35)
    void opChanged(int op, int uid, String packageName, String persistentDeviceId) throws RemoteException;

    abstract class Stub extends Binder implements IAppOpsCallback {

        public static IAppOpsCallback asInterface(IBinder binder) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public IBinder asBinder() {
            throw new RuntimeException("Stub!");
        }
    }
}
