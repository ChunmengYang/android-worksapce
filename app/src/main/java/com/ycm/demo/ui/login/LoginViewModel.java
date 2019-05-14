package com.ycm.demo.ui.login;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Patterns;

import com.ycm.demo.R;
import com.ycm.demo.data.UserRepository;
import com.ycm.demo.data.model.Session;

public class LoginViewModel extends AndroidViewModel {

    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private MutableLiveData<LogoutResult> logoutResult = new MutableLiveData<>();

    private UserRepository userRepository;

    LoginViewModel(Application application) {
        super(application);
        this.userRepository = UserRepository.getInstance(application.getApplicationContext());
    }

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    LiveData<LogoutResult> getLogoutResult() {
        return logoutResult;
    }

    public boolean isLoggedIn() {
        return userRepository.isLoggedIn();
    }

    public Session getSession() {
        return userRepository.getSession();
    }

    public void login(String username, String password) {

        userRepository.login(username, password, new UserRepository.LoginListener() {
            @Override
            public void onSuccess(Session result) {
                loginResult.setValue(new LoginResult(result));
            }

            @Override
            public void onError(String error) {
                loginResult.setValue(new LoginResult(error));
            }
        });
    }

    public void logout() {
        userRepository.logout(new UserRepository.LogoutListener() {
            @Override
            public void onSuccess() {
                logoutResult.setValue(new LogoutResult(true));
            }

            @Override
            public void onError(String error) {
                logoutResult.setValue(new LogoutResult(error));
            }
        });
    }

    public void loginDataChanged(String username, String password) {
        if (!isUserNameValid(username)) {
            loginFormState.setValue(new LoginFormState(R.string.invalid_username, null));
        } else if (!isPasswordValid(password)) {
            loginFormState.setValue(new LoginFormState(null, R.string.invalid_password));
        } else {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        if (username == null) {
            return false;
        }
        if (username.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(username).matches();
        } else {
            return !username.trim().isEmpty();
        }
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        return password != null && password.trim().length() > 5;
    }
}
