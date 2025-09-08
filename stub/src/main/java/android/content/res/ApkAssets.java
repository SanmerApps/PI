package android.content.res;

import android.content.res.loader.AssetsProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;

public class ApkAssets {
    public static @NonNull ApkAssets loadFromPath(@NonNull String path, int flags,
                                                  @Nullable AssetsProvider assets) {
        throw new RuntimeException("Stub!");
    }

    public static @NonNull ApkAssets loadFromFd(@NonNull FileDescriptor fd,
                                                @NonNull String friendlyName, long offset, long length, int flags,
                                                @Nullable AssetsProvider assets) {
        throw new RuntimeException("Stub!");
    }

    public @NonNull XmlResourceParser openXml(@NonNull String fileName) {
        throw new RuntimeException("Stub!");
    }

    public void close() {
        throw new RuntimeException("Stub!");
    }
}
