package dev.sanmer.hidden.compat.stub;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.ArchiveInfo;

interface IPackageManagerCompat {
    ApplicationInfo getApplicationInfo(String packageName, int flags, int userId);
    PackageInfo getPackageInfo(String packageName, int flags, int userId);
    int getPackageUid(String packageName, int flags, int userId);
    ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId);
    ParceledListSlice<ApplicationInfo> getInstalledApplications(int flags, int userId);
    String[] getPackagesForUid(int uid);
    int install(in ArchiveInfo archiveInfo, String installerPackageName, int userId);
}