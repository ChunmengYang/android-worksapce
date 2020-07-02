package com.ycm.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.Collection;

public class WifiDirectClientManager {

    private static final String LCAT = "WifiDirectClientManager";

    public static interface ActionListener {

        // Wi-Fi P2P网络状态发生了改变
        void onWifiP2pEnabled(Boolean enabled);

        // Wi-Fi P2P 发现设备
        void onPeers(Collection<WifiP2pDevice> peerList);

        // Wi-Fi P2P 已连接
        void onConnection(WifiP2pInfo wifiP2pInfo);

        // Wi-Fi P2P 连接丢失
        void onDisconnection();

        // Wi-Fi P2P 连接后，接收收到数据
        void onDataReceive(byte[] data, int length);
    }

    private Context context;
    private WifiDirectClientManager.ActionListener actionListener;

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;

    private IntentFilter wifiP2pStateAndConnectFilter = new IntentFilter();
    private IntentFilter wifiP2pPeersFilter = new IntentFilter();

    private Handler mHandler = new Handler();

    private ClientAsyncTask asyncTask;
    private static final int port = 8988;

    public WifiDirectClientManager(Context context, WifiDirectClientManager.ActionListener actionListener) {
        this.context = context;
        this.actionListener = actionListener;

        // 表示Wi-Fi对等网络状态发生了改变
        wifiP2pStateAndConnectFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // 表示Wi-Fi对等网络的连接状态发生了改变
        wifiP2pStateAndConnectFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // 表示可用的对等点的列表发生了改变
        wifiP2pPeersFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);


        mWifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(context, context.getMainLooper(), null);

        // 注册Wi-Fi可用状态及连接状态接收器
        context.registerReceiver(wifiP2pStateAndConnectReceiver, wifiP2pStateAndConnectFilter);
    }

    private Boolean isWifiP2pEnabled = false;

    private void setIsWifiP2pEnabled(Boolean state) {
        isWifiP2pEnabled = state;
        actionListener.onWifiP2pEnabled(isWifiP2pEnabled);
    }

    private Boolean isDiscovering = false;

    public void startDiscover() {
        if (!isWifiP2pEnabled) return;

        if (!isDiscovering) {
            // 注册Wi-Fi Peers接收器
            context.registerReceiver(wifiP2pPeersReceiver, wifiP2pPeersFilter);

            mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.d(LCAT, "=========Start Discover Peers Success=========");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(LCAT, "=========Start Discover Peers Failure=========");
                }
            });
            isDiscovering = true;
        }
    }

    public void  stopDiscover() {
        if (!isWifiP2pEnabled) return;

        if (isDiscovering) {
            // 注销Wi-Fi Peers接收器
            context.unregisterReceiver(wifiP2pPeersReceiver);

            mWifiP2pManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.d(LCAT, "=========Stop Discover Peers Success=========");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(LCAT, "=========Stop Discover Peers Failure=========");
                }
            });
            isDiscovering = false;
        }
    }

    private Boolean isConnected = false;
    /*
     * 连接Wi-Fi P2P设备
     */
    public void connect(final WifiP2pDevice device) {
        if (!isWifiP2pEnabled) return;

        if (device == null) return;

        if (isConnected) {
            cancelConnect();
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        // WPS按钮认证，服务端会弹出信任按钮：config.wps.setup = WpsInfo.PBC
        // WPS的PIN码参数，服务端会弹出PIN码输入框，客户端会弹出PIN码显示框：config.wps.setup = WpsInfo.DISPLAY

        mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(LCAT, "=========Connect Device Success=========" + device.deviceName);
            }

            @Override
            public void onFailure(int reason) {
                Log.d(LCAT, "=========Connect Device Failure=========" + device.deviceName);
            }
        });
    }

    /*
     * 取消连接
     */
    public void cancelConnect() {
        if (!isWifiP2pEnabled) return;

        if (!isConnected) return;

        // 关闭数据Socket
        if (asyncTask != null) {
            asyncTask.cancel(true);
            asyncTask.close();
            asyncTask = null;
        }

        mWifiP2pManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(LCAT, "=========Cancel Connect Device Success=========");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(LCAT, "=========Cancel Connect Device Failure=========" + reason);
            }
        });
    }

    /*
     * 发送数据
     */
    public void write(byte [] data) {
        if (!isWifiP2pEnabled) return;

        if (isConnected && asyncTask != null) {
            asyncTask.write(data);
        }
    }

    public void destroy() {
        // 注销Wi-Fi可用状态及连接状态接收器
        context.unregisterReceiver(wifiP2pStateAndConnectReceiver);
        wifiP2pStateAndConnectReceiver = null;

        if (isDiscovering) {
            stopDiscover();
        }
        wifiP2pPeersReceiver = null;

        if (isConnected) {
            cancelConnect();
        }

        mChannel = null;
        mWifiP2pManager = null;

        this.context = null;
        this.actionListener = null;
    }

    private void setConnectSuccess(final WifiP2pInfo info) {
        isConnected = true;
        stopDiscover();

        // 开启数据Socket
        asyncTask = new ClientAsyncTask(WifiDirectClientManager.this, info.groupOwnerAddress.getHostAddress(), this.port);
        asyncTask.execute();

        actionListener.onConnection(info);
        new Thread() {
            @Override
            public void run() {
                Log.d(LCAT, "=========Connect Device Success=========" + info.groupOwnerAddress.getHostName());
                Log.d(LCAT, "=========Connect Device Success=========" + info.groupOwnerAddress.getAddress().toString());
            }
        }.start();
    }

    private void setConnectFailure() {
        isConnected = false;
        actionListener.onDisconnection();
        Log.d(LCAT, "=========与P2P设备已断开连接=========");
    }


    /*
     * Wi-Fi P2P可用状态接收器
     */
    private BroadcastReceiver wifiP2pStateAndConnectReceiver = new BroadcastReceiver() {
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
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

                // 连接状态已经改变! 我们可能需要对此做出处理。
                if (mWifiP2pManager == null) {
                    return;
                }

                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {
                    mWifiP2pManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo info) {
                            if (info.groupFormed && info.isGroupOwner) {
                                // 作为服务器

                            } else if (info.groupFormed) {
                                setConnectSuccess(info);
                            }
                        }
                    });
                } else {
                    setConnectFailure();
                }
            }
        }
    };

    /*
     * Wi-FiP2P接收器
     */
    private BroadcastReceiver wifiP2pPeersReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                // 对等点列表已经改变! 我们可能需要对此做出处理。
                if (mWifiP2pManager != null) {
                    mWifiP2pManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                        @Override
                        public void onPeersAvailable(WifiP2pDeviceList peerList) {
                            actionListener.onPeers(peerList.getDeviceList());
                        }
                    });
                }

            }
        }
    };

    private void setData(byte[] data, int length) {
        actionListener.onDataReceive(data, length);
    }

    private void dataCompleted() {
        // 数据发送完成
    }

    private static class ClientAsyncTask extends AsyncTask<Void, Void, Void> {

        WeakReference<WifiDirectClientManager> weakReference;

        private Socket socket;
        private String ipAddress;
        private int port;

        public ClientAsyncTask(WifiDirectClientManager clientManager, String ipAddress, int port) {
            weakReference = new WeakReference<WifiDirectClientManager>(clientManager);
            this.ipAddress = ipAddress;
            this.port = port;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                try {
                    socket = new Socket(this.ipAddress, this.port);

                    read(socket.getInputStream());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                Log.e(LCAT, e.getMessage());
            }

            Log.d(LCAT, "=========doInBackground Wi-Fi Direct数据接收Socket已关闭=========");
            return null;
        }

        private void read(InputStream inputStream) {
            try {
                byte buffer[] = new byte[1024 * 4];
                int temp = 0;
                // 从InputStream当中读取客户端所发送的数据
                while ((temp = inputStream.read(buffer)) != -1) {
                    final byte[] data = buffer;
                    final int length = temp;
                    if (weakReference.get() != null) {
                        weakReference.get().mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                weakReference.get().setData(data, length);
                            }
                        });
                    }
                }
                inputStream.close();
            } catch (IOException e) {
                Log.e(LCAT, "======read======" + e.toString());
            }
        }

        public void write(final byte[] data) {
            if (socket != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (socket.isConnected()) {
                                OutputStream out = socket.getOutputStream();
                                out.write(data);
                                out.flush();
                            }
                        } catch (Exception e) {
                            Log.e(LCAT, "======write======" + e.toString());
                        }
                    }
                }).start();
            }
        }

        public void close() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.d(LCAT, "=========onPostExecute=========");
            if (weakReference.get() != null) {
                weakReference.get().dataCompleted();
            }
        }

        @Override
        protected void onCancelled(Void aVoid) {
            Log.d(LCAT, "=========onCancelled=========");
        }
    }
}
