package com.ycm.demo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.ycm.demo.beacon.iBeacon;
import com.ycm.demo.beacon.iBeaconUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BeaconActivity extends AppCompatActivity {
    private static final String LCAT = "BeaconActivity";

    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BleScanCallback mBleScanCallback;

    private ArrayList<iBeacon> mLeDevices = new ArrayList<iBeacon>();
    private TextView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);

        view = findViewById(R.id.beacons);
        view.setText("Beacon Scanner");
    }

    private void initScanner() {
        Log.d(LCAT, "Initializing Scanner");
        view.setText("Initializing Scanner");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //判断是否具有权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                //判断是否需要向用户解释为什么需要申请该权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(this, "需要打开位置权限才可以搜索到信标", Toast.LENGTH_LONG).show();
                    return;
                }
                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ACCESS_COARSE_LOCATION);
                return;
            }
        }

        if (mBluetoothLeScanner == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (!mBluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "需要打开蓝牙才可以搜索到信标", Toast.LENGTH_LONG).show();
                return;
            }
            mBleScanCallback = new BleScanCallback();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.startScan(mBleScanCallback);
        }

        Log.d(LCAT, "Scanning");
        view.setText("Scanning");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
                //这里进行授权被允许的处理
                Toast.makeText(this, "需要打开位置权限才可以搜索到信标", Toast.LENGTH_LONG).show();
            } else {
                //permission denied, boo! Disable the functionality that depends on this permission.
                //这里进行权限被拒绝的处理
                initScanner();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        initScanner();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mBleScanCallback);
            Log.d(LCAT, "======Stop Scanning======");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBluetoothManager = null;
        mBluetoothAdapter = null;
        mBluetoothLeScanner = null;
        mBleScanCallback = null;
    }

    /**
     * api21+低功耗蓝牙接口回调，以下回调的方法可以根据需求去做相应的操作
     */
    private class BleScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result == null) return;
            iBeacon ibeacon = iBeaconUtils.fromScanData(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
            if(ibeacon == null) return;

            addDevice(ibeacon);
            Collections.sort(mLeDevices, new Comparator<iBeacon>() {
                @Override
                public int compare(iBeacon h1, iBeacon h2) {
                    return h2.rssi - h1.rssi;
                }
            });

            String msg = "";
            for (iBeacon item : mLeDevices) {
                msg += "UUID:" + item.proximityUuid + ",\nmajor:" + item.major + ",\nminor:" + item.minor + "\naddress:" + item.bluetoothAddress + "\n";
            }

            final String text = msg;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.setText(text);
                }
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    }

    private void addDevice(iBeacon device) {
        if (device == null) {
            Log.d(LCAT, "device==null ");
            return;
        }
        if (device.bluetoothAddress == null) {
            Log.d(LCAT, "device.bluetoothAddress==null ");
            return;
        }

        Log.d(LCAT, "UUID:" + device.proximityUuid + ",major:" + device.major + ",minor:" + device.minor + "address:" + device.bluetoothAddress + "\n");

        for (int i = 0; i < mLeDevices.size(); i++) {
            String btAddress = mLeDevices.get(i).bluetoothAddress;
            if (btAddress.equals(device.bluetoothAddress)) {
                mLeDevices.add(i + 1, device);
                mLeDevices.remove(i);
                return;
            }
        }
        mLeDevices.add(device);
    }
}
