package android.os;

import android.content.pm.UserInfo;

import java.util.List;

public interface IUserManager extends IInterface {

    List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated) throws RemoteException;

    UserInfo getUserInfo(int userId);

    abstract class Stub extends Binder implements IUserManager {

        public static IUserManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}