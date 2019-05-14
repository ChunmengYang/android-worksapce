package com.ycm.demo.data;

import android.content.Context;

import com.ycm.demo.data.db.SessionDatabase;
import com.ycm.demo.data.model.Session;

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
public class UserRepository {

    public interface LoginListener {

        void onSuccess(Session result);

        void onError(String error);
    }

    public interface LogoutListener {
        void onSuccess();

        void onError(String error);
    }

    private static volatile UserRepository instance;

    private Context context;
    private UserDataSource dataSource;

    private Session session = null;

    // private constructor : singleton access
    private UserRepository(Context context) {
        this.context = context;
        this.dataSource = new UserDataSource();

        // 查询已登录Session
        SessionDatabase database = new SessionDatabase(context);
        session = database.query();
        database.close();
    }

    public static UserRepository getInstance(Context context) {
        if (instance == null) {
            instance = new UserRepository(context);
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return session != null;
    }

    public void logout(final LogoutListener listener) {
        if (session == null) {
            listener.onSuccess();
            return;
        }

        // 登出
        dataSource.logout(session.getSign(), new UserDataSource.LogoutListener() {
            @Override
            public void onSuccess() {
                // 清理登录Session
                SessionDatabase database = new SessionDatabase(context);
                database.clear();
                database.close();

                session = null;

                listener.onSuccess();
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        });
    }

    public Session getSession() {
        return session;
    }

    private void setSession(Session session) {
        this.session = session;

        // 保存Session
        SessionDatabase database = new SessionDatabase(context);
        Session oldSession = database.query();
        if (oldSession != null) {
            if (oldSession.getId() == session.getId()) {
                database.update(session);
            } else {
                database.clear();
                database.insert(session);
            }
        } else {
            database.insert(session);
        }
        database.close();
    }

    public void login(String username, String password, final LoginListener listener) {
        // handle login
        dataSource.login(username, password, new UserDataSource.LoginListener() {
            @Override
            public void onSuccess(Session result) {
                setSession(result);
                listener.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        });
    }
}
