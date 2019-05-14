package com.ycm.demo.ui.login;

import android.support.annotation.Nullable;

public class LogoutResult {

    @Nullable
    private boolean success;
    @Nullable
    private String error;

    LogoutResult(@Nullable String error) {
        this.error = error;
    }

    LogoutResult(@Nullable boolean success) {
        this.success = success;
    }

    @Nullable
    boolean getSuccess() {
        return success;
    }

    @Nullable
    String getError() {
        return error;
    }
}