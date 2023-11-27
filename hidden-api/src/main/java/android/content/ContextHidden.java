package android.content;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(Context.class)
public class ContextHidden {
    public int getUserId() {
        throw new RuntimeException("Stub!");
    }
}
