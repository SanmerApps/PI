package android.os;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(UserHandle.class)
public class UserHandleHidden {
    public static int myUserId() {
        throw new RuntimeException("Stub!");
    }
}
