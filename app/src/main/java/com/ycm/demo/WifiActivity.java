package com.ycm.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WifiActivity extends AppCompatActivity {
    private static final String LCAT = "WifiActivity";

    private TextView hotspotMsgView;
    private Button  hotspotOpenBtn;
    private Button  hotspotCloseBtn;

    private TextView msgView;
    private Button searchBtn;
    private Button postDataBtn;
    private ListView wifiListView;
    private ScanResultAdapter mScanResultAdapter;

    private WifiManager mWifiManager;

    // Wi-Fi扫描结果接收器
    private BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                } else {
                    scanFailure();
                }
            }
        }
    };
    // Wi-Fi状态改变接收器
    private BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    msgView.setText("连接已断开");
                } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                    msgView.setText("已连接到网络:" + wifiInfo.getSSID());
                } else {
                    NetworkInfo.DetailedState state = info.getDetailedState();
                    if (state == state.CONNECTING) {
                        msgView.setText("连接中...");
                    } else if (state == state.AUTHENTICATING) {
                        msgView.setText("正在验证身份信息...");
                    } else if (state == state.OBTAINING_IPADDR) {
                        msgView.setText("正在获取IP地址...");
                    } else if (state == state.FAILED) {
                        msgView.setText("连接失败");
                    }
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        // 注册Wi-Fi扫描结果接收器
        IntentFilter wifiScanFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, wifiScanFilter);

        // 注册Wi-Fi状态改变接收器
        IntentFilter wifiStateFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiStateReceiver, wifiStateFilter);

        // Wi-Fi热点开启、关闭事件绑定
        hotspotMsgView = findViewById(R.id.wifi_hotspot_msg);
        hotspotOpenBtn = findViewById(R.id.wifi_hotspot_open);
        hotspotOpenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createWifiHotspot();
            }
        });
        hotspotCloseBtn = findViewById(R.id.wifi_hotspot_close);
        hotspotCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeWifiHotspot();
            }
        });

        // Wi-Fi搜索、连接事件绑定
        msgView = findViewById(R.id.wifi_msg);
        searchBtn = findViewById(R.id.wifi_search);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
            }
        });

        wifiListView = findViewById(R.id.wifi_items);
        mScanResultAdapter = new ScanResultAdapter(WifiActivity.this, R.layout.wifi_item, new ArrayList<ScanResult>());
        wifiListView.setAdapter(mScanResultAdapter);
        wifiListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ScanResult result = mScanResultAdapter.getItem(position);
                connect(result);
            }
        });

        // 通过socket发送数据
        postDataBtn = findViewById(R.id.wifi_post_data);
        postDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postData();
            }
        });
    }


    private WifiManager.LocalOnlyHotspotReservation mReservation;
    /**
     * 开启Wi-Fi热点
     */
    private void createWifiHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!checkGPSIsOpen()) {
                Toast.makeText(WifiActivity.this, "Wi-Fi热点需要打开定位服务", Toast.LENGTH_LONG).show();
                return;
            }

            mWifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    mReservation = reservation;
                    String sid = reservation.getWifiConfiguration().SSID;
                    String pwd = reservation.getWifiConfiguration().preSharedKey;

                    hotspotMsgView.setText("热点已开启 \n SSID:" + sid + " PASSWORD:" + pwd);

                    hotspotOpenBtn.setVisibility(View.GONE);
                    hotspotCloseBtn.setVisibility(View.VISIBLE);
                }

                @Override
                public void onStopped() {
                    mReservation = null;
                    hotspotMsgView.setText("热点已关闭");
                }

                @Override
                public void onFailed(int reason) {
                    hotspotMsgView.setText("开启热点失败");
                }
            }, new Handler());

            return;
        }

        if (mWifiManager.isWifiEnabled()) {
            // 如果Wi-Fi处于打开状态，则关闭Wi-Fi
            mWifiManager.setWifiEnabled(false);
        }

        String sid = getDeviceName();
        String pwd = sid;

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = sid;
        config.preSharedKey = pwd;
        config.hiddenSSID = true;
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.status = WifiConfiguration.Status.ENABLED;

        // 通过反射调用设置热点
        try {
            Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            boolean enable = (Boolean) method.invoke(mWifiManager, config, true);
            if (enable) {
                hotspotMsgView.setText("热点已开启 \n SSID:" + sid + " PASSWORD:" + pwd);

                hotspotOpenBtn.setVisibility(View.GONE);
                hotspotCloseBtn.setVisibility(View.VISIBLE);
            } else {
                hotspotMsgView.setText("开启热点失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            hotspotMsgView.setText("开启热点失败");
        }
    }

    /**
     * 关闭WiFi热点
     */
    public void closeWifiHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mReservation != null) {
                mReservation.close();
                mReservation = null;

                hotspotMsgView.setText("热点已关闭");
                hotspotOpenBtn.setVisibility(View.VISIBLE);
                hotspotCloseBtn.setVisibility(View.GONE);
            }
            return;
        }

        try {
            Method configMethod = mWifiManager.getClass().getMethod("getWifiApConfiguration");
            configMethod.setAccessible(true);
            WifiConfiguration config = (WifiConfiguration) configMethod.invoke(mWifiManager);
            Method closeMethod = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            boolean enable = (Boolean) closeMethod.invoke(mWifiManager, config, false);
            if (enable) {
                hotspotMsgView.setText("热点已关闭");

                hotspotOpenBtn.setVisibility(View.VISIBLE);
                hotspotCloseBtn.setVisibility(View.GONE);
            } else {
                hotspotMsgView.setText("关闭热点失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            hotspotMsgView.setText("关闭热点失败");
        }
    }

    /*
    *
    * 获取设备名称，作为热点名称使用
    * */
    private String getDeviceName() {
        String deviceName = getString(R.string.app_name);
        try{
            Class<?> cls = Class.forName("android.os.SystemProperties");
            Object object = (Object) cls.newInstance();
            Method getName = cls.getDeclaredMethod("get", String.class);
            deviceName = (String) getName.invoke(object, "persist.sys.device_name");
        } catch (Exception e){
            e.printStackTrace();
        }
        return deviceName;
    }

    /*
     * GPS是否打开
     */
    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }



    /**
     * 搜索Wi-Fi热点
     */
    private void search() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
        boolean success = mWifiManager.startScan();
        if (!success) {
            scanFailure();
        }

    }


    /**
     *
     * 扫描成功回调
     */
    private void scanSuccess() {
        List<ScanResult> effectiveResults = new ArrayList<ScanResult>();
        List<ScanResult> results = mWifiManager.getScanResults();
        for (ScanResult result: results) {
            if (!"".equals(result.SSID)) {
                effectiveResults.add(result);
            }
        }
        effectiveResults.sort(new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult o1, ScanResult o2) {
                if(o1.level <= o2.level){
                    return 1;
                }
                else
                    return -1;
            }
        });
        Log.d(LCAT, "===========ScanSuccess===========" + effectiveResults.size());
        mScanResultAdapter.clear();
        mScanResultAdapter.addAll(effectiveResults);
    }

    /**
     *
     * 扫描失败回调，使用上次扫描的结果
     * */
    private void scanFailure() {
        List<ScanResult> effectiveResults = new ArrayList<ScanResult>();
        List<ScanResult> results = mWifiManager.getScanResults();
        for (ScanResult result: results) {
            if (!"".equals(result.SSID)) {
                effectiveResults.add(result);
            }
        }
        Log.d(LCAT, "===========ScanFailure===========" + effectiveResults.size());
        effectiveResults.sort(new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult o1, ScanResult o2) {
                if(o1.level <= o2.level){
                    return 1;
                }
                else
                    return -1;
            }
        });
        mScanResultAdapter.clear();
        mScanResultAdapter.addAll(effectiveResults);
    }

    /*
    *
    * Wi-Fi热点列表适配器
    * */
    private class ScanResultAdapter extends ArrayAdapter<ScanResult> {
        private int resource;
        private List<ScanResult> objects;
        public ScanResultAdapter(Context context, int resource, List<ScanResult> objects) {
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

            ScanResult scanResult = getItem(position);
            nameView.setText(scanResult.SSID);

            int level = scanResult.level;
            if (level <= 0 && level >= -50) {
                signlView.setText("信号很好");
            } else if (level < -50 && level >= -70) {
                signlView.setText("信号较好");
            } else if (level < -70 && level >= -80) {
                signlView.setText("信号一般");
            } else if (level < -80 && level >= -100) {
                signlView.setText("信号较差");
            } else {
                signlView.setText("信号很差");
            }

            return view;
        }
    }


    private int WIFICIPHER_WPA = 1;
    private int WIFICIPHER_WEP = 2;
    private int WIFICIPHER_NOPASS = 3;

    /*
    *
    * 连接到Wi-Fi网络
    * */
    private void connect(final ScanResult scanResult) {
        String capabilities = scanResult.capabilities;
        int type = WIFICIPHER_WPA;
        if (capabilities != null && "".equals(capabilities)) {
            if (capabilities.contains("WPA") || capabilities.contains("wpa")) {
                type = WIFICIPHER_WPA;
            } else if (capabilities.contains("WEP") || capabilities.contains("wep")) {
                type = WIFICIPHER_WEP;
            } else {
                type = WIFICIPHER_NOPASS;
            }
        }
        WifiConfiguration config = isExsits(scanResult.SSID);

        if (config == null) {
            // 未连接过的Wi-Fi
            if (type != WIFICIPHER_NOPASS) {
                // 需要密码
                final EditText editText = new EditText(WifiActivity.this);
                final int finalType = type;

                AlertDialog.Builder builder = new AlertDialog.Builder(WifiActivity.this);
                builder.setTitle("请输入Wi-Fi密码");
                builder.setIcon(android.R.drawable.ic_dialog_info);
                builder.setView(editText);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LCAT, "=========输入的Wi-Fi密码========" + editText.getText());

                        WifiConfiguration config = createWifiInfo(scanResult.SSID, editText.getText().toString(), finalType);
                        int wcgID = mWifiManager.addNetwork(config);
                        mWifiManager.enableNetwork(wcgID, true);
                    }
                }).setNegativeButton("取消", null).show();

                return;
            } else {
                // 不需要密码
                config = createWifiInfo(scanResult.SSID, "", type);

                int wcgID = mWifiManager.addNetwork(config);
                mWifiManager.enableNetwork(wcgID, true);
            }
        } else {
            // 连接过的WWi-Fi
            mWifiManager.enableNetwork(config.networkId, true);
        }
    }

    /*
    *
    * 创建Wi-Fi网络配置
    * */
    public WifiConfiguration createWifiInfo(String SSID, String password, int type) {
        Log.v(LCAT, "=============SSID:" + SSID + ", Password:" + password + ", Type:" + type + "================");
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = this.isExsits(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        // 分为三种情况：1、没有密码 2、用wep加密 3、用wpa加密
        if (type == WIFICIPHER_NOPASS) {// WIFICIPHER_NOPASS
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;

        } else if (type == WIFICIPHER_WEP) {  //  WIFICIPHER_WEP
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == WIFICIPHER_WPA) {   // WIFICIPHER_WPA
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }

        return config;
    }

    /*
    * 查找是否连接过Wi-Fi
    * */
    private WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                Log.v(LCAT, "=============isExsits SSID:" + SSID + "================");
                return existingConfig;
            }
        }
        return null;
    }

    /**
     * 通过socket发送数据
     * */
    private void postData() {
        new Thread() {
            @Override
            public void run() {
               final Socket socket;

                try {
                    socket = new Socket("192.168.1.9", 1989);

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                            socket.getOutputStream()));
                    writer.write("how are you?");
                    writer.flush();
                    writer.close();

                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

//        Runnable runnable = new Runnable() {
//
//            @Override
//            public void run() {
//                Socket socket;
//
//                try {
//                    socket = new Socket("192.168.1.9", 1989);
//
//                    OutputStream outputStream = socket.getOutputStream();
//
//                    File tempCropFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),
//                            "temp_crop_photo.jpg");
//                    if (!tempCropFile.exists()) return;
//
//                    InputStream inputStream = new FileInputStream(tempCropFile);
//                    byte buffer[] = new byte[4 * 1024];
//                    int temp = 0;
//                    while ((temp = inputStream.read(buffer)) != -1) {
//                        // 把数据写入到OuputStream对象中
//                        outputStream.write(buffer, 0, temp);
//                    }
//                    inputStream.close();
//                    // 发送数据
//                    outputStream.flush();
//                    outputStream.close();
//
//                    socket.close();
//
//                } catch (UnknownHostException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//        new Thread(runnable).start();
    }


    @Override
    protected void onDestroy() {
        closeWifiHotspot();

        unregisterReceiver(wifiScanReceiver);
        wifiScanReceiver = null;
        unregisterReceiver(wifiStateReceiver);
        wifiStateReceiver = null;

        if (mScanResultAdapter != null) {
            mScanResultAdapter.clear();
            mScanResultAdapter = null;
        }

        super.onDestroy();
    }
}
