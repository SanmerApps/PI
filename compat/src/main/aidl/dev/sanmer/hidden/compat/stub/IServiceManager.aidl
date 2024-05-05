package dev.sanmer.hidden.compat.stub;

import dev.sanmer.hidden.compat.stub.IAppOpsServiceCompat;
import dev.sanmer.hidden.compat.stub.IPackageManagerCompat;
import dev.sanmer.hidden.compat.stub.IUserManagerCompat;

interface IServiceManager {
    int getUid() = 0;
    int getPid() = 1;
    int getVersion() = 2;
    String getSELinuxContext() = 3;
    IPackageManagerCompat getPackageManager() = 4;
    IUserManagerCompat getUserManager() = 5;
    IAppOpsServiceCompat getAppOpsService() = 6;

    void destroy() = 16777114; // Only for Shizuku
}