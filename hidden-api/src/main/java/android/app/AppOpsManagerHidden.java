package android.app;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(AppOpsManager.class)
public class AppOpsManagerHidden {

    public static int strOpToOp(@NonNull String op) {
        throw new RuntimeException("Stub!");
    }

    public static String opToPermission(int op) {
        throw new RuntimeException("Stub!");
    }

    @Nullable
    public static String opToPermission(@NonNull String op) {
        throw new RuntimeException("Stub!");
    }

    public static final class OpEntry implements Parcelable {

        public int getOp() {
            throw new RuntimeException("Stub!");
        }

        public @NonNull String getOpStr() {
            throw new RuntimeException("Stub!");
        }

        public int getMode() {
            throw new RuntimeException("Stub!");
        }

        public long getTime() {
            throw new RuntimeException("Stub!");
        }

        public long getLastAccessTime(int flags) {
            throw new RuntimeException("Stub!");
        }

        public long getLastAccessForegroundTime(int flags) {
            throw new RuntimeException("Stub!");
        }

        public long getLastAccessBackgroundTime(int flags) {
            throw new RuntimeException("Stub!");
        }

        public long getLastAccessTime(int fromUidState, int toUidState,
                                      int flags) {
            throw new RuntimeException("Stub!");
        }

        public long getRejectTime() {
            throw new RuntimeException("Stub!");
        }

        public long getLastRejectTime(int flags) {
            throw new RuntimeException("Stub!");
        }

        public long getLastRejectForegroundTime(int flags) {
            throw new RuntimeException("Stub!");
        }

        public long getLastRejectBackgroundTime(int flags) {
            throw new RuntimeException("Stub!");
        }

        public long getLastRejectTime(int fromUidState, int toUidState,
                                      int flags) {
            throw new RuntimeException("Stub!");
        }

        public boolean isRunning() {
            throw new RuntimeException("Stub!");
        }

        public long getDuration() {
            throw new RuntimeException("Stub!");
        }

        public long getLastForegroundDuration(int flags) {
            throw new RuntimeException("Stub!");
        }

        public long getLastBackgroundDuration(int flags) {
            throw new RuntimeException("Stub!");
        }

        public long getLastDuration(int fromUidState, int toUidState,
                                    int flags) {
            throw new RuntimeException("Stub!");
        }

        public int getProxyUid() {
            throw new RuntimeException("Stub!");
        }

        public int getProxyUid(int uidState, int flags) {
            throw new RuntimeException("Stub!");
        }

        public @Nullable String getProxyPackageName() {
            throw new RuntimeException("Stub!");
        }

        public @Nullable String getProxyPackageName(int uidState, int flags) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public int describeContents() {
            throw new RuntimeException("Stub!");
        }

        public static final Creator<OpEntry> CREATOR = new Creator<OpEntry>() {
            @Override
            public OpEntry createFromParcel(Parcel in) {
                throw new RuntimeException("Stub!");
            }

            @Override
            public OpEntry[] newArray(int size) {
                throw new RuntimeException("Stub!");
            }
        };
    }

    public static final class PackageOps implements Parcelable {

        public @NonNull String getPackageName() {
            throw new RuntimeException("Stub!");
        }

        public int getUid() {
            throw new RuntimeException("Stub!");
        }

        public @NonNull List<OpEntry> getOps() {
            throw new RuntimeException("Stub!");
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public int describeContents() {
            throw new RuntimeException("Stub!");
        }

        public static final Creator<PackageOps> CREATOR = new Creator<PackageOps>() {
            @Override
            public PackageOps createFromParcel(Parcel in) {
                throw new RuntimeException("Stub!");
            }

            @Override
            public PackageOps[] newArray(int size) {
                throw new RuntimeException("Stub!");
            }
        };
    }
}
