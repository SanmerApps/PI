package dev.sanmer.hidden.compat.stub;

import android.content.pm.ParceledListSlice;
import android.content.pm.PackageInstaller;
import android.os.ParcelFileDescriptor;
import dev.sanmer.hidden.compat.stub.IPackageInstallerSessionCompat;
import dev.sanmer.hidden.compat.stub.ISessionCallback;

interface IPackageInstallerCompat {
    int createSession(in PackageInstaller.SessionParams params, String installerPackageName, String installerAttributionTag, int userId);
    IPackageInstallerSessionCompat openSession(int sessionId);
    PackageInstaller.SessionInfo getSessionInfo(int sessionId);
    ParceledListSlice<PackageInstaller.SessionInfo> getAllSessions(int userId);
    void registerCallback(ISessionCallback callback, int userId);
    void unregisterCallback(ISessionCallback callback);
}