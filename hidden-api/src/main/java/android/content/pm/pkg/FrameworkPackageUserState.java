package android.content.pm.pkg;

import androidx.annotation.RequiresApi;

@RequiresApi(33)
public interface FrameworkPackageUserState {
    FrameworkPackageUserState DEFAULT = new FrameworkPackageUserStateDefault();
}
