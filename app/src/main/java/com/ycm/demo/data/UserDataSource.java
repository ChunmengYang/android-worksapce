package com.ycm.demo.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import com.ycm.demo.data.model.Session;
import com.ycm.demo.data.model.User;
import com.ycm.demo.security.Base64Utils;
import com.ycm.demo.security.RSAUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class UserDataSource {
    private static final String TAG = "UserDataSource";

    private static final String SERVICE_URL = "http://192.168.3.112:9090/mms";

    private static final String ERROR_CONNECT_TIMEOUT = "Connection timeout";
    private static final String ERROR_REQUEST_FAILURE = "Request failure";
    private static final String ERROR_RESPONSE_DATA = "Response data error";

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

    public interface UserIconUploadListener {
        void onSuccess();

        void onError(String error);
    }

    private Handler mHandler = new Handler();

    public void login(String username, String password, final LoginListener listener) {
        String url = SERVICE_URL + "/account/login";
        String params = null;

        try {
            String data = "account=" + username + "&password=" + password;
            byte[] bytes = RSAUtil.encryptByPublicKey(data.getBytes(), RSAUtil.API_PUBLICKEY);
            params = Base64Utils.encode(bytes);

            Log.d(TAG, params);
        } catch (final Exception e) {
            Log.d(TAG, e.getMessage());

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onError(e.getMessage());
                }
            });
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3000, TimeUnit.MILLISECONDS)
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .build();

        RequestBody requestBody = new FormBody.Builder()
                .addEncoded("encryp_data", params)
                .build();


        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(ERROR_CONNECT_TIMEOUT);
                    }
                });
                Log.d(TAG, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject result = new JSONObject(response.body().string());
                        boolean  success = result.optBoolean("success");
                        if (success) {
                            JSONObject obj = result.optJSONObject("session");

                            final  Session session = new Session();
                            session.setId(obj.optInt("id"));
                            session.setAccountId(obj.optInt("accountId"));
                            session.setCreateTime(obj.optLong("createTime"));
                            session.setSign(obj.optString("sign"));

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onSuccess(session);
                                }
                            });

                        } else {
                            final String error = result.optString("error");

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onError(error);
                                }
                            });

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onError(ERROR_RESPONSE_DATA);
                            }
                        });
                    }
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onError(ERROR_REQUEST_FAILURE);
                        }
                    });
                }
            }
        });
    }

    public void logout(String sign, final LogoutListener listener) {
        String url = SERVICE_URL + "/account/logout";

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3000, TimeUnit.MILLISECONDS)
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .build();

        RequestBody requestBody = new FormBody.Builder()
                .addEncoded("session", sign)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(ERROR_CONNECT_TIMEOUT);
                    }
                });
                Log.d(TAG, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject result = new JSONObject(response.body().string());
                        boolean  success = result.optBoolean("success");
                        if (success) {

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onSuccess();
                                }
                            });

                        } else {
                            final String error = result.optString("error");

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onError(error);
                                }
                            });

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onError(ERROR_RESPONSE_DATA);
                            }
                        });
                    }
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onError(ERROR_REQUEST_FAILURE);
                        }
                    });
                }
            }
        });
    }

    public void getUserInfo(String sign, final UserInfoListener listener) {
        String url = SERVICE_URL + "/user?session=" + sign;

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3000, TimeUnit.MILLISECONDS)
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(ERROR_CONNECT_TIMEOUT);
                    }
                });
                Log.d(TAG, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject result = new JSONObject(response.body().string());
                        boolean  success = result.optBoolean("success");
                        if (success) {
                            JSONObject obj = result.optJSONObject("user");

                            final User user = new User();
                            user.setId(obj.optInt("id"));
                            user.setAccountId(obj.optInt("accountId"));
                            user.setNickName(obj.getString("nickName"));
                            user.setUserName(obj.optString("userName"));
                            user.setSex(obj.optInt("sex"));
                            user.setCreateTime(obj.optLong("createTime"));
                            user.setModifyTime(obj.optLong("modifyTime"));

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onSuccess(user);
                                }
                            });

                        } else {
                            final String error = result.optString("error");

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onError(error);
                                }
                            });

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onError(ERROR_RESPONSE_DATA);
                            }
                        });
                    }
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onError(ERROR_REQUEST_FAILURE);
                        }
                    });
                }
            }
        });
    }

    public void getUserIcon(String sign, final UserIconListener listener) {
        String url = SERVICE_URL + "/user/icon/downloadbyim?session=" + sign;

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3000, TimeUnit.MILLISECONDS)
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(ERROR_CONNECT_TIMEOUT);
                    }
                });
                Log.d(TAG, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    final Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (bitmap.getByteCount() > 0) {
                                listener.onSuccess(bitmap);
                            } else {
                                listener.onSuccess(null);
                            }
                        }
                    });
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onError(ERROR_REQUEST_FAILURE);
                        }
                    });
                }
            }
        });
    }

    public void uploadUserIcon(String sign, File file, final UserIconUploadListener listener) {
        String url = SERVICE_URL + "/user/icon/uploadbyim?session=" + sign;

        OkHttpClient client =  new OkHttpClient.Builder()
                .connectTimeout(3000,  TimeUnit.MILLISECONDS)
                .writeTimeout(10000,  TimeUnit.MILLISECONDS)
                .build();

        MediaType mediaType = MediaType.parse("image/png; charset=utf-8");

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(mediaType, file))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(ERROR_CONNECT_TIMEOUT);
                    }
                });
                Log.d(TAG, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    ResponseBody responseBody = response.body();
                    try {
                        JSONObject result = new JSONObject(responseBody.string());
                        boolean  success = result.optBoolean("success");
                        if (success) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onSuccess();
                                }
                            });
                        } else {
                            final String error = result.optString("error");

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onError(error);
                                }
                            });
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onError(ERROR_RESPONSE_DATA);
                            }
                        });
                    }
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onError(ERROR_REQUEST_FAILURE);
                        }
                    });
                }
            }
        });
    }
}
