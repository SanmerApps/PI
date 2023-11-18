package android.content.pm;

import android.os.UserHandle;

import androidx.annotation.RequiresApi;

public class UserInfo {
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
}