package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class UserInfo implements Parcelable {
    public int id;
    public String name;
    public int flags;
    public int serialNumber;

    @RequiresApi(30)
    public String userType;

    public boolean isPrimary() {
        throw new RuntimeException("Stub!");
    }

    public boolean isAdmin() {
        throw new RuntimeException("Stub!");
    }

    public boolean isEnabled() {
        throw new RuntimeException("Stub!");
    }

    public UserHandle getUserHandle() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int describeContents() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        throw new RuntimeException("Stub!");
    }

    public static final Creator<UserInfo> CREATOR = new Creator<>() {
        @Override
        public UserInfo createFromParcel(Parcel in) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public UserInfo[] newArray(int size) {
            throw new RuntimeException("Stub!");
        }
    };
}