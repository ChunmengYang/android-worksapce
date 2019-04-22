package com.ycm.demo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;


public class WifiAdmin {
    private static final String LCAT = "WifiAdmin";

    public static interface ActionListener {

        // 发现Wi-Fi热点
        void onScanResults(Collection<ScanResult> results);

        // Wi-Fi已连接
        void onConnection(WifiInfo wifiInfo);

        // Wi-Fi连接丢失
        void onDisconnection();

        // Wi-Fi正在连接中
        void onConnecting(int state);
    }

    private Context context;
    private WifiAdmin.ActionListener actionListener;

    private IntentFilter wifiScanFilter;
    private IntentFilter wifiStateFilter;

    private WifiManager mWifiManager;

    public WifiAdmin(Context context, WifiAdmin.ActionListener actionListener) {
        this.context = context;
        this.actionListener = actionListener;

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        // 注册Wi-Fi扫描结果接收器
        wifiScanFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(wifiScanReceiver, wifiScanFilter);

        // 注册Wi-Fi状态改变接收器
        wifiStateFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiStateReceiver, wifiStateFilter);
    }

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

    // Wi-Fi连接状态改变接收器
    private BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    // 丢失连接
                    actionListener.onDisconnection();

                } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    // 连接成功
                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                    actionListener.onConnection(wifiInfo);

                } else {
                    NetworkInfo.DetailedState state = info.getDetailedState();
                    if (state == state.CONNECTING) {
                        // 正在连接中
                        actionListener.onConnecting(1);

                    } else if (state == state.AUTHENTICATING) {
                        // 正在验证身份信息
                        actionListener.onConnecting(2);

                    } else if (state == state.OBTAINING_IPADDR) {
                        // 正在获取IP地址
                        actionListener.onConnecting(3);

                    } else if (state == state.FAILED) {
                        // 连接失败
                        actionListener.onConnecting(4);

                    }
                }
            }
        }
    };

    /*
     * 搜索Wi-Fi热点
     */
    public void search() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //判断是否具有位置权限
            if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "需要打开位置权限才可以搜索到Wi-Fi热点", Toast.LENGTH_LONG).show();
                return;
            }

            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!gps && !network) {
                Toast.makeText(context, "需要打开位置服务才可以搜索到Wi-Fi热点", Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
        boolean success = mWifiManager.startScan();
        if (!success) {
            scanFailure();
        }
    }

    /*
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

        actionListener.onScanResults(effectiveResults);
    }

    /*
     * 扫描失败回调，使用上次扫描的结果
     */
    private void scanFailure() {
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
        Log.d(LCAT, "===========ScanFailure===========" + effectiveResults.size());

        actionListener.onScanResults(effectiveResults);
    }

    // 没有密码
    private int WIFICIPHER_WPA = 1;
    // 用WEP加密
    private int WIFICIPHER_WEP = 2;
    // 用WPA加密
    private int WIFICIPHER_NOPASS = 3;

    /*
     * 连接到Wi-Fi网络
     */
    public void connect(final ScanResult scanResult, String password) {
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
                config = createWifiInfo(scanResult.SSID, password, type);

                int wcgID = mWifiManager.addNetwork(config);
                mWifiManager.enableNetwork(wcgID, true);
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

        if (type == WIFICIPHER_NOPASS) {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;

        } else if (type == WIFICIPHER_WEP) {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }

        Log.d(LCAT, "=============Create WifiInfo，SSID:" + SSID + ", Password:" + password + ", Type:" + type + "================");
        return config;
    }

    /*
     * 查找是否连接过该Wi-Fi
     */
    private WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                Log.d(LCAT, "=============WifiConfiguration isExsits SSID:" + SSID + "================");
                return existingConfig;
            }
        }
        return null;
    }

    public void destroy() {
        // 注销Wi-Fi扫描结果接收器
        context.unregisterReceiver(wifiScanReceiver);
        wifiScanReceiver = null;

        // 注销Wi-Fi状态改变接收器
        context.unregisterReceiver(wifiStateReceiver);
        wifiStateReceiver = null;

        mWifiManager = null;

        context = null;
        actionListener = null;
    }
}
