package android.content.pm;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(PackageInfo.class)
public class PackageInfoHidden {

    public boolean isOverlayPackage() {
        throw new RuntimeException("Stub!");
    }
}