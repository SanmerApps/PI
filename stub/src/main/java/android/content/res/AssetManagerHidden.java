package android.content.res;

import androidx.annotation.NonNull;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(AssetManager.class)
public class AssetManagerHidden {
    public AssetManagerHidden() {
        throw new RuntimeException("Stub!");
    }

    public void setApkAssets(@NonNull ApkAssets[] apkAssets, boolean invalidateCaches) {
        throw new RuntimeException("Stub!");
    }
}
