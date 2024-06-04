package dev.sanmer.hidden.compat.stub;

import android.app.AppOpsManagerHidden;
import dev.sanmer.hidden.compat.stub.IAppOpsCallback;

interface IAppOpsServiceCompat {
    int checkOperation(int code, int uid, String packageName);
    void startWatchingMode(int op, String packageName, IAppOpsCallback callback);
    void stopWatchingMode(IAppOpsCallback callback);
    List<AppOpsManagerHidden.PackageOps> getPackagesForOps(in int[] ops);
    List<AppOpsManagerHidden.PackageOps> getOpsForPackage(int uid, String packageName, in int[] ops);
    List<AppOpsManagerHidden.PackageOps> getUidOps(int uid, in int[] ops);
    void setUidMode(int code, int uid, int mode);
    void setMode(int code, int uid, String packageName, int mode);
    void resetAllModes(int reqUserId, String reqPackageName);
}