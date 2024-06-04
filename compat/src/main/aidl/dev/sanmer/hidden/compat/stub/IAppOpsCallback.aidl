package dev.sanmer.hidden.compat.stub;

oneway interface IAppOpsCallback {
    void opChanged(int op, int uid, String packageName);
}