package dev.sanmer.hidden.compat.stub;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import dev.sanmer.hidden.compat.stub.IPackageInstallerCompat;

interface IPackageManagerCompat {
    IPackageInstallerCompat getPackageInstaller();
    ApplicationInfo getApplicationInfo(String packageName, int flags, int userId);
    PackageInfo getPackageInfo(String packageName, int flags, int userId);
    int getPackageUid(String packageName, int flags, int userId);
    ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId);
    ParceledListSlice<ApplicationInfo> getInstalledApplications(int flags, int userId);
    ParceledListSlice<ResolveInfo> queryIntentActivities(in Intent intent, String resolvedType, int flags, int userId);
    String[] getPackagesForUid(int uid);
    Intent getLaunchIntentForPackage(String packageName, int userId);
}