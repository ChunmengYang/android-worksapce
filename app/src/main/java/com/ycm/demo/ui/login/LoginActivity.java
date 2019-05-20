package com.ycm.demo.ui.login;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ycm.demo.R;
import com.ycm.demo.data.model.Session;
import com.ycm.demo.data.model.User;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginViewModel = ViewModelProviders.of(this).get(LoginViewModel.class);

        final EditText usernameEditText = findViewById(R.id.login_username);
        final EditText passwordEditText = findViewById(R.id.login_password);
        final Button loginButton = findViewById(R.id.login_btn);
        final ImageView iconView = findViewById(R.id.login_user_icon);
        final Button logoutButton = findViewById(R.id.logout_btn);
        final ProgressBar loadingProgressBar = findViewById(R.id.login_loading);

        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

        loginViewModel.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                if (loginResult.getError() != null) {
                    showFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    showLoginSuccess(loginResult.getSuccess());
                }
                setResult(Activity.RESULT_OK);

                //Complete and destroy login activity once successful
                finish();
            }
        });

        loginViewModel.getLogoutResult().observe(this, new Observer<LogoutResult>() {
            @Override
            public void onChanged(@Nullable LogoutResult logoutResult) {
                if (logoutResult == null) {
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                if (logoutResult.getError() != null) {
                    showFailed(logoutResult.getError());
                }
                if (logoutResult.getSuccess()) {
                    showLogoutSuccess();
                }
                setResult(Activity.RESULT_OK);

                //Complete and destroy login activity once successful
                finish();
            }
        });

        loginViewModel.getLoginUser().observe(this, new Observer<User>() {
            @Override
            public void onChanged(@Nullable User user) {
                Toast.makeText(getApplicationContext(), user.getUserName(), Toast.LENGTH_LONG).show();
            }
        });

        loginViewModel.getLoginUserIcon().observe(this, new Observer<Bitmap>() {
            @Override
            public void onChanged(@Nullable Bitmap bitmap) {
                if (bitmap != null) {
                    iconView.setImageBitmap(bitmap);
                }
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                }
                return false;
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        });
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.logout();
            }
        });

        if (loginViewModel.isLoggedIn()) {
            showLoginSuccess(loginViewModel.getSession());

            usernameEditText.setVisibility(View.GONE);
            passwordEditText.setVisibility(View.GONE);
            loginButton.setVisibility(View.GONE);

            iconView.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
            loginViewModel.queryUserInfo();
            loginViewModel.queryUserIcon();
        } else {
            iconView.setVisibility(View.GONE);
            logoutButton.setVisibility(View.GONE);

            usernameEditText.setVisibility(View.VISIBLE);
            passwordEditText.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.VISIBLE);
        }
    }

    private void showLoginSuccess(Session session) {
        String welcome = getString(R.string.welcome) + "===AccountId: " + String.valueOf(session.getId()) + "===Sign: " + session.getSign();
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLogoutSuccess() {
        Toast.makeText(getApplicationContext(), "Logout Success", Toast.LENGTH_LONG).show();
    }

    private void showFailed(String error) {
        Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT).show();
    }
}