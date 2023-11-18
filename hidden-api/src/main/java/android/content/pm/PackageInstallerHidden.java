package android.content.pm;

import androidx.annotation.RequiresApi;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(PackageInstaller.class)
public class PackageInstallerHidden {
    public PackageInstallerHidden(IPackageInstaller installer, String installerPackageName, int userId) {
        throw new RuntimeException();
    }

    @RequiresApi(31)
    public PackageInstallerHidden(IPackageInstaller installer, String installerPackageName, String installerAttributionTag, int userId) {
        throw new RuntimeException();
    }

    @RefineAs(PackageInstaller.Session.class)
    public static class SessionHidden {
        public SessionHidden(IPackageInstallerSession session) {
            throw new RuntimeException();
        }
    }

    @RefineAs(PackageInstaller.SessionParams.class)
    public static class SessionParamsHidden {
        public int installFlags;
    }
}