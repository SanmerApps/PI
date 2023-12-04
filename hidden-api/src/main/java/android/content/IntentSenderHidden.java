package android.content;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(IntentSender.class)
public class IntentSenderHidden {
    public IntentSenderHidden(IIntentSender target) {
        throw new RuntimeException("Stub!");
    }
}
