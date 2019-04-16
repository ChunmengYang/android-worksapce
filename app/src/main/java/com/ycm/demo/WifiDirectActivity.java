package com.ycm.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WifiDirectActivity extends AppCompatActivity {
    private static final String LCAT = "WifiDirectActivity";

    private IntentFilter intentFilter = new IntentFilter();

    private TextView msgView;
    private Button startSearchBtn;
    private Button stopSearchBtn;
    private Button postDataBtn;
    private ListView wifiListView;
    private ScanResultAdapter mScanResultAdapter;

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_direct);

        msgView = findViewById(R.id.wifi_p2p_msg);
        startSearchBtn = findViewById(R.id.wifi_p2p_search_start);
        stopSearchBtn = findViewById(R.id.wifi_p2p_search_stop);
        postDataBtn = findViewById(R.id.wifi_p2p_post_data);

        wifiListView = findViewById(R.id.wifi_p2p_items);
        mScanResultAdapter = new ScanResultAdapter(WifiDirectActivity.this, R.layout.wifi_item, new ArrayList<WifiP2pDevice>());
        wifiListView.setAdapter(mScanResultAdapter);
        wifiListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                WifiP2pDevice result = mScanResultAdapter.getItem(position);
                connect(result);
            }
        });


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
    }

    /*
     * Wi-FiP2P接收器
     */
    private BroadcastReceiver wifiP2pBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

                // Wi-Fi Direct模式是否已经启用
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    msgView.setText("点击开始搜索来查找附近设备！");

                    startSearchBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startDiscoverPeers();
                        }
                    });
                    stopSearchBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            stopDiscoverPeers();
                        }
                    });
                } else {
                    msgView.setText("不支持Wi-Fi Direct模式");
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                // 对等点列表已经改变! 我们可能需要对此做出处理。
                if (mWifiP2pManager != null) {
                    mWifiP2pManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                        @Override
                        public void onPeersAvailable(WifiP2pDeviceList peerList) {
                            mScanResultAdapter.clear();
                            mScanResultAdapter.addAll(peerList.getDeviceList());

                            if (mScanResultAdapter.getCount() == 0) {
                                msgView.setText("没有搜索到设备");
                            }
                        }
                    });
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

                            if (info.groupFormed && info.isGroupOwner) {
                                // 服务器线程并接收连接请求。

                            } else if (info.groupFormed) {
                                // 作为客户端
                                msgView.setText("已经连接成功");
                                postDataBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        new ClientAsyncTask(WifiDirectActivity.this, info.groupOwnerAddress.getHostAddress()).execute();
                                    }
                                });

                            }
                        }
                    });
                } else {
                    msgView.setText("连接失败");
                }

            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

                // 设备的配置信息发生了改变，我们可能需要对此做出处理。
                // WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

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


    private Boolean isDiscovering = false;
    /*
     * 发现Wi-FiP2P设备的回调
     */
    private WifiP2pManager.ActionListener wifiP2pActionListener = new WifiP2pManager.ActionListener() {

        @Override
        public void onSuccess() {
            // 查找初始化成功时的处理写在这里。

            // 实际上并没有发现任何服务，所以该方法可以置空。
            // 对等点搜索的代码在onReceive方法中。
        }

        @Override
        public void onFailure(int reasonCode) {
            // 查找初始化失败时的处理写在这里。
            // 警告用户出错了。
            msgView.setText("搜索初始化失败！");
        }
    };

    /*
     * 开始搜索Wi-FiP2P设备
     */
    public void startDiscoverPeers() {
        if (!isDiscovering) {
            mWifiP2pManager.discoverPeers(mChannel, wifiP2pActionListener);
            isDiscovering = true;
            msgView.setText("正在搜索Wi-FiP2P设备...");
        }

    }
    /*
     * 停止搜索Wi-FiP2P设备
     */
    public void stopDiscoverPeers() {
        if (isDiscovering) {
            mWifiP2pManager.stopPeerDiscovery(mChannel, wifiP2pActionListener);
            isDiscovering = false;
            msgView.setText("已停止搜索Wi-FiP2P设备");
        }
    }

    /*
     * 连接Wi-FiP2P对等点
     */
    private void connect(WifiP2pDevice device) {

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver将会通知我们, 可以先忽略。
            }

            @Override
            public void onFailure(int reason) {
                msgView.setText("连接失败");
            }
        });
    }

    /*
     * Wi-FiP2P设备列表Adapter
     */
    private class ScanResultAdapter extends ArrayAdapter<WifiP2pDevice> {
        private int resource;
        private List<WifiP2pDevice> objects;
        public ScanResultAdapter(Context context, int resource, List<WifiP2pDevice> objects) {
            super(context, resource, objects);
            this.resource = resource;
            this.objects = objects;
        }

        @Override
        public View getView(int position, View convertView,ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(this.resource, null);

            TextView nameView = (TextView) view.findViewById(R.id.wifi_item_name);
            TextView signlView = (TextView) view.findViewById(R.id.wifi_item_signl);

            WifiP2pDevice scanResult = getItem(position);
            nameView.setText(scanResult.deviceName);
            signlView.setText(scanResult.deviceAddress);

            return view;
        }
    }

    public void setMessage(String msg) {
        msgView.setText(msg);
    }

    private static class ClientAsyncTask extends AsyncTask<Void, Void, Void> {

        WeakReference<WifiDirectActivity> weakReference;
        private String ipAddress;

        public ClientAsyncTask(WifiDirectActivity activity, String ipAddress) {
            weakReference = new WeakReference<WifiDirectActivity>(activity);
            this.ipAddress = ipAddress;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                try {
                    Socket socket = new Socket(this.ipAddress, 8988);

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                            socket.getOutputStream()));

                    writer.write("how are you?" + new Date().toString());
                    writer.flush();
                    writer.close();

                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                Log.e(LCAT, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (weakReference.get() != null) {
                weakReference.get().setMessage("数据发送完毕");
            }
        }
    }

}
