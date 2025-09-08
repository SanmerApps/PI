package android.content.pm;

import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(PackageInstaller.class)
public class PackageInstallerHidden {

    @RefineAs(PackageInstaller.SessionParams.class)
    public static class SessionParamsHidden {
        public int installFlags;
    }

    @RefineAs(PackageInstaller.SessionInfo.class)
    public static class SessionInfoHidden {
        public int userId;
    }

    @RefineAs(PackageInstaller.Session.class)
    public static class SessionHidden {
        public SessionHidden(IPackageInstallerSession session) {
            throw new RuntimeException("Stub!");
        }

        public void write(@NonNull String name, long offsetBytes, long lengthBytes,
                          @NonNull ParcelFileDescriptor fd) {
            throw new RuntimeException("Stub!");
        }
    }
}