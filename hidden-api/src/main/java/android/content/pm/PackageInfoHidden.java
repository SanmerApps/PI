package android.content.pm;

import androidx.annotation.RequiresApi;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(PackageInfo.class)
public class PackageInfoHidden {

    public int versionCodeMajor;

    public boolean isStub;

    public boolean coreApp;

    public boolean requiredForAllUsers;

    public String restrictedAccountType;

    public String requiredAccountType;

    public String overlayTarget;

    public String overlayCategory;

    public int overlayPriority;

    boolean mOverlayIsStatic;

    public int compileSdkVersion;

    public String compileSdkVersionCodename;

    @RequiresApi(34)
    public boolean isActiveApex;

    public boolean isOverlayPackage() {
        throw new RuntimeException("Stub!");
    }
}