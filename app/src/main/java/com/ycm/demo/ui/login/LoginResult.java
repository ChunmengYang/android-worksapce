package com.ycm.demo.ui.login;

import android.support.annotation.Nullable;

import com.ycm.demo.data.model.Session;

/**
 * Authentication result : success (user details) or error message.
 */
class LoginResult {
    @Nullable
    private Session session;
    @Nullable
    private String error;

    LoginResult(@Nullable String error) {
        this.error = error;
    }

    LoginResult(@Nullable Session session) {
        this.session = session;
    }

    @Nullable
    Session getSuccess() {
        return session;
    }

    @Nullable
    String getError() {
        return error;
    }
}
