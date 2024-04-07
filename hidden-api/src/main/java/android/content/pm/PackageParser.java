package android.content.pm;

import android.content.pm.pkg.FrameworkPackageUserState;
import android.content.pm.pkg.PackageUserState;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.util.Set;

public class PackageParser {
    public PackageParser() {
        throw new RuntimeException("Stub!");
    }

    public Package parsePackage(File packageFile, int flags, boolean useCaches) throws PackageParserException {
        throw new RuntimeException("Stub!");
    }

    public static ApkLite parseApkLite(File apkFile, int flags) throws PackageParserException {
        throw new RuntimeException("Stub!");
    }

    @RequiresApi(33)
    public static PackageInfo generatePackageInfo(PackageParser.Package p,
                                                  int[] gids, int flags, long firstInstallTime, long lastUpdateTime,
                                                  Set<String> grantedPermissions, FrameworkPackageUserState state) {
        throw new RuntimeException("Stub!");
    }

    public static PackageInfo generatePackageInfo(PackageParser.Package p,
                                                  int[] gids, int flags, long firstInstallTime, long lastUpdateTime,
                                                  Set<String> grantedPermissions, PackageUserState state) {
        throw new RuntimeException("Stub!");
    }

    public static class PackageParserException extends Exception {}

    public static class Package {
        public String packageName;
    }

    public static class ApkLite {
        public String packageName;
        public String splitName;
        public boolean isFeatureSplit;
        public boolean isSplitRequired;
    }
}
