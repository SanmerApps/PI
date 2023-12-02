package dev.sanmer.hidden.compat.stub;

import dev.sanmer.hidden.compat.stub.IPackageManagerCompat;
import dev.sanmer.hidden.compat.stub.IUserManagerCompat;

interface IServiceManager {
    int getUid();
    int getPid();
    int getVersion();
    String getSELinuxContext();
    IPackageManagerCompat getPackageManagerCompat();
    IUserManagerCompat getUserManagerCompat();
}