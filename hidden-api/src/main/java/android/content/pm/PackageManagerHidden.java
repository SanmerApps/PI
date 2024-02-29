package android.content.pm;

import androidx.annotation.RequiresApi;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(PackageManager.class)
public class PackageManagerHidden {

    public static int INSTALL_REPLACE_EXISTING;
    public static int INSTALL_ALLOW_TEST;

    @RequiresApi(34)
    public static int INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK;
}