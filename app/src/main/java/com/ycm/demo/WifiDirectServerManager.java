package com.ycm.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;

public class WifiDirectServerManager {
    private static final String LCAT = "WifiDirectServerManager";

    public static interface ActionListener {

        // Wi-Fi P2P网络状态发生了改变
        void onWifiP2pEnabled(Boolean enabled);

        // Wi-Fi P2P Server态发生了改变
        void onServerOpen(Boolean isOpened);

        // Wi-Fi P2P 已连接
        void onConnection(WifiP2pInfo wifiP2pInfo);

        // Wi-Fi P2P 连接丢失
        void onDisconnection();

        // Wi-Fi P2P 连接后，接收收到数据
        void onDataReceive(String result);
    }

    private Context context;
    private WifiDirectServerManager.ActionListener actionListener;

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;

    private IntentFilter wifiP2pStateFilter = new IntentFilter();
    private IntentFilter wifiP2pConnFilter = new IntentFilter();

    private Handler mHandler = new Handler();

    public WifiDirectServerManager(Context context, WifiDirectServerManager.ActionListener actionListener) {
        this.context = context;
        this.actionListener = actionListener;

        // 表示Wi-Fi对等网络状态发生了改变
        wifiP2pStateFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // 表示Wi-Fi对等网络的连接状态发生了改变
        wifiP2pConnFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);


        mWifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(context, context.getMainLooper(), null);

        // 注册Wi-Fi可用状态接收器
        context.registerReceiver(wifiP2pStateReceiver, wifiP2pStateFilter);
    }

    private Boolean isWifiP2pEnabled = false;
    private ServerAsyncTask asyncTask;
    private Boolean isOpened = false;

    public void openServer(int port) {
        if (!isWifiP2pEnabled) return;
        if (isOpened) return;

        // 开启数据接收Socket
        asyncTask = new ServerAsyncTask(WifiDirectServerManager.this);
        asyncTask.execute(port);

        mWifiP2pManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(LCAT, "=========Create Group Success=========");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(LCAT, "=========Create Group Failure=========");
            }
        });

        // 注册Wi-Fi连接状态接收器
        context.registerReceiver(wifiP2pConnectReceiver, wifiP2pConnFilter);

        isOpened = true;
        actionListener.onServerOpen(isOpened);
    }


    public void closeServer() {
        if (!isWifiP2pEnabled) return;
        if (!isOpened) return;

        // 注销Wi-Fi连接状态接收器
        context.unregisterReceiver(wifiP2pConnectReceiver);

        // 关闭数据接收Socket
        if (asyncTask != null) {
            asyncTask.cancel(true);
            asyncTask.close();
        }

        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(LCAT, "=========Remove Group Success=========");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(LCAT, "=========Remove Group Failure=========");
            }
        });

        isOpened = false;
        actionListener.onServerOpen(isOpened);
    }

    public void destroy() {
        // 注销Wi-Fi可用状态接收器
        context.unregisterReceiver(wifiP2pStateReceiver);
        wifiP2pStateReceiver = null;

        if (isOpened) {
            closeServer();
        }
        wifiP2pConnectReceiver = null;
        mHandler = null;

        mChannel = null;
        mWifiP2pManager = null;

        this.context = null;
        this.actionListener = null;
    }

    private void setIsWifiP2pEnabled(Boolean state) {
        isWifiP2pEnabled = state;
        actionListener.onWifiP2pEnabled(isWifiP2pEnabled);
    }

    /*
     * Wi-Fi P2P可用状态接收器
     */
    private BroadcastReceiver wifiP2pStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Wi-Fi Direct模式是否已经启用
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    setIsWifiP2pEnabled(true);
                } else {
                    setIsWifiP2pEnabled(false);
                }
            }
        }
    };

    /*
     * Wi-Fi P2P连接状态接收器
     */
    private BroadcastReceiver wifiP2pConnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                // 连接状态已经改变! 我们可能需要对此做出处理。
                if (mWifiP2pManager == null) {
                    return;
                }

                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    mWifiP2pManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo info) {
                            if (info != null) {
                                // ！！！这里很关键，只有真正的走到这里，才是真正的建立了P2P连接。拿到了准备建立Socket通道的必要信息。
                                if (info.groupFormed && info.isGroupOwner) {
                                    // 作为服务器
                                    actionListener.onConnection(info);
                                } else if (info.groupFormed) {
                                    // 作为客户端
                                }
                            }
                        }
                    });
                } else {
                    actionListener.onDisconnection();
                    Log.d(LCAT, "=========与P2P设备已断开连接=========");
                }

            }
        }
    };

    private void setData(String result) {
        actionListener.onDataReceive(result);
    }

    private static class ServerAsyncTask extends AsyncTask<Integer, Void, Void> {

        WeakReference<WifiDirectServerManager> weakReference;
        private ServerSocket serverSocket;

        public ServerAsyncTask(WifiDirectServerManager manager) {
            weakReference = new WeakReference<WifiDirectServerManager>(manager);
        }

        @Override
        protected Void doInBackground(Integer...port) {
            try {
                serverSocket = new ServerSocket(port[0]);

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
                                weakReference.get().setData(result);
                            }
                        });
                    }
                }

                serverSocket.close();
            } catch (Exception e) {
                Log.e(LCAT, e.getMessage());
            }

            Log.d(LCAT, "=========doInBackground Wi-Fi Direct数据接收Socket已关闭=========");
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
            Log.d(LCAT, "=========onPostExecute=========");
        }

        @Override
        protected void onCancelled(Void aVoid) {
            Log.d(LCAT, "=========onCancelled=========");
        }
    }
}
