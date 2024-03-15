package dev.sanmer.hidden.compat.stub;

import android.content.Intent;

interface IInstallCallback {
    void onSuccess(in Intent intent);
    void onFailure(in Intent intent);
}