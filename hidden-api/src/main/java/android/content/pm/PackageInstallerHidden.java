package android.content.pm;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(PackageInstaller.class)
public class PackageInstallerHidden {

    public static boolean ENABLE_REVOCABLE_FD;

    @RefineAs(PackageInstaller.SessionParams.class)
    public static class SessionParamsHidden {
        public int installFlags;
    }
}