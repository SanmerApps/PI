package android.os;

import android.content.pm.UserInfo;

import androidx.annotation.RequiresApi;

import java.util.List;

public interface IUserManager extends IInterface {

    List<UserInfo> getUsers(boolean excludeDying) throws RemoteException;

    @RequiresApi(30)
    List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated) throws RemoteException;

    UserInfo getUserInfo(int userId) throws RemoteException;

    abstract class Stub extends Binder implements IUserManager {

        public static IUserManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}