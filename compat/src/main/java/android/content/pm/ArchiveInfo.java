package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.File;

public class ArchiveInfo implements Parcelable {
    String path;
    String packageName;
    String originating;

    public File getPackageFile() {
        return new File(path);
    }

    public String getPackageName() {
        return packageName;
    }

    public String getOriginating() {
        return originating;
    }

    public ArchiveInfo(@NonNull String path, @NonNull String packageName, String originating) {
        this.path = path;
        this.packageName = packageName;
        this.originating = originating;
    }

    public ArchiveInfo(@NonNull File path, @NonNull String packageName, String originating) {
        this.path = path.getPath();
        this.packageName = packageName;
        this.originating = originating;
    }

    protected ArchiveInfo(Parcel in) {
        path = in.readString();
        packageName = in.readString();
        originating = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(packageName);
        dest.writeString(originating);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ArchiveInfo> CREATOR = new Creator<>() {
        @Override
        public ArchiveInfo createFromParcel(Parcel in) {
            return new ArchiveInfo(in);
        }

        @Override
        public ArchiveInfo[] newArray(int size) {
            return new ArchiveInfo[size];
        }
    };
}
