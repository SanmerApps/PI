package dev.sanmer.hidden.compat.stub;

oneway interface ISessionCallback {
    void onCreated(int sessionId);
    void onBadgingChanged(int sessionId);
    void onActiveChanged(int sessionId, boolean active);
    void onProgressChanged(int sessionId, float progress);
    void onFinished(int sessionId, boolean success);
}