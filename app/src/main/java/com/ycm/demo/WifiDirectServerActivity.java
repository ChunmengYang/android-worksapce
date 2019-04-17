package com.ycm.demo;

import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class WifiDirectServerActivity extends AppCompatActivity implements WifiDirectServerManager.ActionListener {
    private static final String LCAT = "WifiDirectActivity";

    private TextView serverStateView;
    private TextView connectionStateView;
    private Button startServerBtn;
    private Button stopServerBtn;

    private WifiDirectServerManager serverManager;
    private Handler mHandler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_direct_server);

        serverStateView = findViewById(R.id.wifi_p2p_server_msg);
        connectionStateView = findViewById(R.id.wifi_p2p_connection_msg);

        startServerBtn = findViewById(R.id.wifi_p2p_server_start);
        stopServerBtn = findViewById(R.id.wifi_p2p_server_stop);

        serverManager = new WifiDirectServerManager(WifiDirectServerActivity.this, this);
    }

    @Override
    public void onWifiP2pEnabled(Boolean enabled) {
        if (enabled) {
            serverStateView.setText("点击开启服务打开Wi-Fi P2P服务！");

            startServerBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 开启服务
                    serverManager.openServer(8988);
                }
            });
            stopServerBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 停止服务
                    serverManager.closeServer();
                }
            });
        } else {
            serverStateView.setText("该设备不支持Wi-Fi Direct模式");
        }
    }

    @Override
    public void onServerOpen(Boolean isOpened) {
        if (isOpened) {
            serverStateView.setText("Wi-Fi P2P服务已开启");
        } else {
            serverStateView.setText("Wi-Fi P2P服务已关闭");
        }
    }

    @Override
    public void onConnection(final WifiP2pInfo wifiP2pInfo) {
        if (wifiP2pInfo != null) {
            connectionStateView.setText("已经与P2P设备连接成功");
        }
    }

    @Override
    public void onDisconnection() {
        connectionStateView.setText("已经与P2P设备断开连接");
    }

    @Override
    public void onDataReceive(String result) {
        connectionStateView.setText("收到数据: " + result);
    }

    @Override
    protected void onDestroy() {
        serverManager.destroy();
        serverManager = null;

        super.onDestroy();
    }
}
