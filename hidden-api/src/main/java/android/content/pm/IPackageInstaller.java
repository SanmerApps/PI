package android.content.pm;

import android.content.IntentSender;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

public interface IPackageInstaller extends IInterface {

    int createSession(PackageInstaller.SessionParams params, String installerPackageName, int userId) throws RemoteException;

    @RequiresApi(31)
    int createSession(PackageInstaller.SessionParams params, String installerPackageName, String installerAttributionTag, int userId);

    IPackageInstallerSession openSession(int sessionId) throws RemoteException;

    PackageInstaller.SessionInfo getSessionInfo(int sessionId);

    ParceledListSlice<PackageInstaller.SessionInfo> getAllSessions(int userId);

    void registerCallback(IPackageInstallerCallback callback, int userId) throws RemoteException;

    void unregisterCallback(IPackageInstallerCallback callback) throws RemoteException;

    void uninstall(VersionedPackage versionedPackage, String callerPackageName, int flags, IntentSender statusReceiver, int userId) throws RemoteException;
}