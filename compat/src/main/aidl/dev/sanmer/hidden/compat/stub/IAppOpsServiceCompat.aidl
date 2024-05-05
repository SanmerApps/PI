package dev.sanmer.hidden.compat.stub;

import android.app.AppOpsManagerHidden;

interface IAppOpsServiceCompat {
    int checkOperation(int code, int uid, String packageName);
    List<AppOpsManagerHidden.PackageOps> getPackagesForOps(in int[] ops);
    List<AppOpsManagerHidden.PackageOps> getOpsForPackage(int uid, String packageName, in int[] ops);
    List<AppOpsManagerHidden.PackageOps> getUidOps(int uid, in int[] ops);
    void setUidMode(int code, int uid, int mode);
    void setMode(int code, int uid, String packageName, int mode);
    void resetAllModes(int reqUserId, String reqPackageName);
}