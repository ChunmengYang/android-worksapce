package com.ycm.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;

public class WifiDirectServerActivity extends AppCompatActivity {
    private static final String LCAT = "WifiDirectActivity";

    private TextView msgView;
    private Button startServerBtn;
    private Button stopServerBtn;

    // WiFiP2p BroadcastReceiver
    private IntentFilter intentFilter = new IntentFilter();

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_direct_server);

        // 表示Wi-Fi对等网络状态发生了改变
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // 表示可用的对等点的列表发生了改变
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // 表示Wi-Fi对等网络的连接状态发生了改变
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // 设备配置信息发生了改变
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


        mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(this, getMainLooper(), null);

        msgView = findViewById(R.id.wifi_p2p_server_msg);
        startServerBtn = findViewById(R.id.wifi_p2p_server_start);
        stopServerBtn = findViewById(R.id.wifi_p2p_server_stop);
    }

    /*
     *
     * Wi-FiP2P状态接收器
     * */
    private BroadcastReceiver wifiP2pBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Wi-Fi Direct模式是否已经启用
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    msgView.setText("点击开启服务供附近设备搜索、连接！");

                    startServerBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // 开启服务
                            openServer();
                        }
                    });
                    stopServerBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // 停止服务
                            closeServer();
                        }
                    });
                } else {
                    msgView.setText("不支持Wi-Fi Direct模式");
                }

            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

                // 连接状态已经改变! 我们可能需要对此做出处理。
                if (mWifiP2pManager == null) {
                    return;
                }

                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {
                    mWifiP2pManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(final WifiP2pInfo info) {
                            // 组群协商后，就可以确定群主。
                            if (info.groupFormed && info.isGroupOwner) {
                                // 服务器线程并接收连接请求。
                                msgView.setText("已经连接成功");

                            } else if (info.groupFormed) {
                                // 作为客户端
                            }
                        }
                    });
                }

            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(wifiP2pBroadcastReceiver, intentFilter);
    }
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(wifiP2pBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mChannel.close();
    }

    public void setMessage(String msg) {
        msgView.setText(msg);
    }

    private ServerAsyncTask asyncTask;
    private Boolean isOpened = false;
    private void openServer() {
        if (isOpened) return;

        asyncTask = new ServerAsyncTask(WifiDirectServerActivity.this);
        asyncTask.execute();

        mWifiP2pManager.createGroup(mChannel, null);

        isOpened = true;
        msgView.setText("Wi-FiP2P服务已经打开");
    }

    private void closeServer() {
        if (!isOpened) return;

        if (asyncTask != null) {
            asyncTask.cancel(true);
            asyncTask.close();
        }

        mWifiP2pManager.cancelConnect(mChannel, null);
        mWifiP2pManager.removeGroup(mChannel, null);

        isOpened = false;
        msgView.setText("Wi-FiP2P服务已经停止");
    }

    private static class ServerAsyncTask extends AsyncTask<Void, Void, Void> {

        WeakReference<WifiDirectServerActivity> weakReference;
        private StringBuffer msg;
        private ServerSocket serverSocket;

        public ServerAsyncTask(WifiDirectServerActivity activity) {
            weakReference = new WeakReference<WifiDirectServerActivity>(activity);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                serverSocket = new ServerSocket(8988);

                while (!isCancelled()) {
                    final StringBuffer msg = new StringBuffer();
                    Socket socket = serverSocket.accept();

                    // 从Socket当中得到InputStream对象
                    InputStream inputStream = socket.getInputStream();
                    byte buffer[] = new byte[1024 * 4];
                    int temp = 0;
                    // 从InputStream当中读取客户端所发送的数据
                    while ((temp = inputStream.read(buffer)) != -1) {
                        msg.append(new String(buffer, 0, temp));
                    }

                    inputStream.close();
                    socket.close();

                    if (weakReference.get() != null) {
                        final String result = msg.toString();
                        weakReference.get().mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                weakReference.get().setMessage(result);
                            }
                        });
                    }
                }

                serverSocket.close();
            } catch (Exception e) {
                Log.e(LCAT, e.getMessage());
            }
            return null;
        }

        public void close() {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.d(LCAT, "=========onPostExecute数据接收Socket已关闭=========");
            if (weakReference.get() != null) {
                weakReference.get().setMessage("Wi-FiP2P服务已经停止");
            }
        }

        @Override
        protected void onCancelled(Void aVoid) {
            Log.d(LCAT, "=========onCancelled数据接收Socket已关闭=========");
            if (weakReference.get() != null) {
                weakReference.get().setMessage("Wi-FiP2P服务已经停止");
            }
        }
    }
}
