package com.android.internal.app;

import android.app.AppOpsManagerHidden;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import java.util.List;

public interface IAppOpsService extends IInterface {

    int checkOperation(int code, int uid, String packageName) throws RemoteException;

    List<AppOpsManagerHidden.PackageOps> getPackagesForOps(int[] ops) throws RemoteException;

    List<AppOpsManagerHidden.PackageOps> getOpsForPackage(int uid, String packageName, int[] ops)  throws RemoteException;

    List<AppOpsManagerHidden.PackageOps> getUidOps(int uid, int[] ops) throws RemoteException;

    void setUidMode(int code, int uid, int mode) throws RemoteException;

    void setMode(int code, int uid, String packageName, int mode) throws RemoteException;

    void resetAllModes(int reqUserId, String reqPackageName) throws RemoteException;

    abstract class Stub extends Binder implements IAppOpsService {

        public static IAppOpsService asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}