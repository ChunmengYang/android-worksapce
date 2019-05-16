package com.ycm.demo.data;

import android.content.Context;
import android.graphics.Bitmap;

import com.ycm.demo.data.db.SessionDatabase;
import com.ycm.demo.data.db.UserDatabase;
import com.ycm.demo.data.model.Session;
import com.ycm.demo.data.model.User;

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

    public interface UserInfoListener {
        void onSuccess(User user);

        void onError(String error);
    }

    public interface UserIconListener {
        void onSuccess(Bitmap icon);

        void onError(String error);
    }

    private static volatile UserRepository instance;

    private Context context;
    private UserDataSource dataSource;

    private Session session = null;
    private User user = null;

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

    public Session getSession() {
        return session;
    }

    private void setSession(Session session) {
        this.session = session;

        // 保存Session
        SessionDatabase database = new SessionDatabase(context);
        Session cacheSession = database.query();
        if (cacheSession != null) {
            if (cacheSession.getId() == session.getId()) {
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
                SessionDatabase sessionDatabase = new SessionDatabase(context);
                sessionDatabase.clear();
                sessionDatabase.close();

                UserDatabase userDatabase = new UserDatabase(context);
                userDatabase.clear();
                userDatabase.close();

                session = null;
                user = null;

                listener.onSuccess();
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        });
    }

    public void getUserInfo(final UserInfoListener listener) {
        if (session == null) {
            listener.onError("用户未登录");
            return;
        }

        if (user != null) {
            listener.onSuccess(user);
            return;
        }

        final UserDatabase database = new UserDatabase(context);
        User cacheUser = database.query();
        if (cacheUser != null) {
            database.close();
            user = cacheUser;
            listener.onSuccess(user);
            return;
        }

        dataSource.getUserInfo(session.getSign(), new UserDataSource.UserInfoListener() {
            @Override
            public void onSuccess(User result) {
                user = result;

                database.insert(user);
                database.close();

                listener.onSuccess(user);
            }

            @Override
            public void onError(String error) {
                database.close();
                listener.onError(error);
            }
        });
    }

    public void getUserIcon(final UserIconListener listener) {
        if (session == null) {
            listener.onError("用户未登录");
            return;
        }

        if (user != null && user.getIcon() != null) {
            listener.onSuccess(user.getIcon());
            return;
        }


        final UserDatabase database = new UserDatabase(context);
        User cacheUser = database.query();
        if (cacheUser != null) {
            user = cacheUser;
            if (user.getIcon() != null) {
                database.close();
                listener.onSuccess(user.getIcon());
                return;
            }
        }

        dataSource.getUserIcon(session.getSign(), new UserDataSource.UserIconListener() {
            @Override
            public void onSuccess(Bitmap result) {
                if (user != null) {
                    user.setIcon(result);
                    database.update(user);
                }
                database.close();

                listener.onSuccess(user.getIcon());
            }

            @Override
            public void onError(String error) {
                database.close();
                listener.onError(error);
            }
        });
    }
}
