package dev.sanmer.hidden.compat.stub;

import android.content.pm.UserInfo;

interface IUserManagerCompat {
    List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated);
    UserInfo getUserInfo(int userId);
}