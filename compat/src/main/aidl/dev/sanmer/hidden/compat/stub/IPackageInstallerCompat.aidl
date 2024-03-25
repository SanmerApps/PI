package dev.sanmer.hidden.compat.stub;

import android.content.pm.ParceledListSlice;
import android.content.pm.PackageInstaller;
import android.os.ParcelFileDescriptor;
import dev.sanmer.hidden.compat.stub.ISessionCallback;

interface IPackageInstallerCompat {
    int createSession(in PackageInstaller.SessionParams params, String installerPackageName, String installerAttributionTag, int userId);
    void updateSessionAppIcon(int sessionId, in Bitmap appIcon);
    void updateSessionAppLabel(int sessionId, String appLabel);
    PackageInstaller.SessionInfo getSessionInfo(int sessionId);
    ParceledListSlice<PackageInstaller.SessionInfo> getAllSessions(int userId);
    void registerCallback(ISessionCallback callback, int userId);
    void unregisterCallback(ISessionCallback callback);
    ParcelFileDescriptor openWrite(int sessionId, String name, long offsetBytes, long lengthBytes);
    ParcelFileDescriptor openRead(int sessionId, String name);
    void write(int sessionId, String name, long offsetBytes, long lengthBytes, in ParcelFileDescriptor fd);
    void close(int sessionId);
    void commit(int sessionId, in IntentSender statusReceiver, boolean forTransferred);
}