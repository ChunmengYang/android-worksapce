package com.ycm.demo;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WifiDirectServerActivity extends AppCompatActivity implements WifiDirectServerManager.ActionListener {
    private static final String LCAT = "WifiDirectActivity";

    private TextView wifiStateView;
    private TextView serverStateView;
    private TextView connectionStateView;
    private Button startServerBtn;
    private Button stopServerBtn;

    private WifiDirectServerManager serverManager;
    private Handler mHandler = new Handler();

    private WifiAdmin wifiAdmin;

    private Collection<ScanResult> scanWifiList;

    private String connectedWifiName;
    private String targetWifiName;
    private String targetWifiPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_direct_server);

        wifiStateView = findViewById(R.id.wifi_p2p_wifi_state_msg);
        serverStateView = findViewById(R.id.wifi_p2p_server_msg);
        connectionStateView = findViewById(R.id.wifi_p2p_connection_msg);

        startServerBtn = findViewById(R.id.wifi_p2p_server_start);
        stopServerBtn = findViewById(R.id.wifi_p2p_server_stop);

        serverManager = new WifiDirectServerManager(WifiDirectServerActivity.this, this);

        wifiAdmin = new WifiAdmin(WifiDirectServerActivity.this, new WifiAdmin.ActionListener() {
            @Override
            public void onScanResults(Collection<ScanResult> results) {
                if (results.size() > 0) {
                    Boolean isFirstTime = (scanWifiList == null);

                    scanWifiList = results;
                    if (isFirstTime) {
                        connectWifi();
                    }
                }
            }

            @Override
            public void onConnection(WifiInfo wifiInfo) {
                connectedWifiName = wifiInfo.getSSID();
                wifiStateView.setText("Wi-Fi已经连接到：" + wifiInfo.getSSID());
                serverManager.write("我是服务端，我已经连接到Wi-Fi".getBytes());
            }

            @Override
            public void onDisconnection() {
                connectedWifiName = null;
                wifiStateView.setText("Wi-Fi已丢失连接");
            }

            @Override
            public void onConnecting(int state) {
                switch (state) {
                    case 1:
                        wifiStateView.setText("正在连接Wi-Fi...");
                        break;
                    case 2:
                        wifiStateView.setText("正在验证身份信息...");
                        break;
                    case 3:
                        wifiStateView.setText("正在获取IP地址...");
                        break;
                    case 4:
                        wifiStateView.setText("连接失败");
                        break;
                    default:
                }

            }
        });
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
    public void onDataReceive(byte[] data, int length) {
        String result = new String(data);
        connectionStateView.setText("收到数据: " + result);
        serverManager.write("我是服务端，已收到数据".getBytes());


        String [] strArray = result.split(";");
        Map<String, String> map = new HashMap<String, String>();
        if (strArray.length > 0) {
            for (String item : strArray) {
                int index = item.indexOf("=");
                if (index != -1) {
                    String key = item.substring(0, index);
                    if (!"".equals(key)) {
                        String value = item.substring(index + 1, item.length());
                        map.put(key, value);
                    }
                }
            }
        }

        if (map.containsKey("name") && map.containsKey("password")) {
            targetWifiName = map.get("name");
            targetWifiPassword = map.get("password");

            if (connectedWifiName != null && connectedWifiName.equals(targetWifiName)) {
                return;
            }

            if (scanWifiList == null) {
                wifiAdmin.search();
            } else {
                connectWifi();
            }
        }
    }

    /*
     * 连接Wi-Fi
     */
    private void connectWifi() {
        if (targetWifiName != null && scanWifiList != null) {
            for (ScanResult result : scanWifiList) {
                if (targetWifiName.equals(result.SSID)) {
                    // 连接目标Wi-Fi
                    wifiAdmin.connect(result, targetWifiPassword);
                    return;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        serverManager.destroy();
        serverManager = null;

        wifiAdmin.destroy();
        wifiAdmin = null;

        super.onDestroy();
    }
}
