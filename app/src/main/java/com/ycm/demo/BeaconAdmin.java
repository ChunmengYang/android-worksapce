package com.ycm.demo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.ycm.demo.beacon.iBeacon;
import com.ycm.demo.beacon.iBeaconUtils;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

public class BeaconAdmin {
    private static final String LCAT = "BeaconAdmin";

    public enum StartScanResult {
        RESULT_NO_LOCATION_PERMISSION,
        RESULT_BLUETOOTH_DISABLED,
        RESULT_SCANNING,
        RESULT_SUCCESS
    }

    public enum StartAdvertisingResult {
        RESULT_BLUETOOTH_DISABLED,
        RESULT_ADVERTISING,
        RESULT_SUCCESS
    }

    public static interface ActionListener {
        // 开启广播成功
        void onAdvertiseStartSuccess(String uuid, int major, int minor);

        // 开启广播失败
        void onAdvertiseStartFailure(int errorCode);

        // 扫描成功
        void onScanResult(iBeacon beacon);

        // 扫描失败
        void onScanFailed(int errorCode);
    }

    private Context context;
    private BeaconAdmin.ActionListener actionListener;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private MyAdvertiseCallback mMyAdvertiseCallback;
    private Boolean advertising = false;

    private BluetoothLeScanner mBluetoothLeScanner;
    private MyScanCallback mMyScanCallback;
    private Boolean scanning = false;

    public BeaconAdmin(Context context, BeaconAdmin.ActionListener actionListener) {
        this.context = context;
        this.actionListener = actionListener;

    }

    public StartAdvertisingResult startAdvertising(String uuid, int major, int minor, int timeoutMillis) {
        Log.d(LCAT, "=========Start Advertising=========");
        if (advertising) {
            return StartAdvertisingResult.RESULT_ADVERTISING;
        }

        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        // 判断蓝牙是否开启
        if (!mBluetoothAdapter.isEnabled()) {
            return StartAdvertisingResult.RESULT_BLUETOOTH_DISABLED;
        }

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mMyAdvertiseCallback = new MyAdvertiseCallback(BeaconAdmin.this, uuid, major, minor);

        mBluetoothLeAdvertiser.startAdvertising(
                createAdvSettings(false, timeoutMillis),
                createIBeaconAdvertiseData(UUID.fromString(uuid), (short) major, (short) minor, (byte) 0xc8),
                mMyAdvertiseCallback);

        return StartAdvertisingResult.RESULT_SUCCESS;
    }

    public void stopAdvertising() {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mMyAdvertiseCallback);
            Log.d(LCAT, "=========Stop Advertising=========");
        }

        mBluetoothLeAdvertiser = null;
        mMyAdvertiseCallback = null;
        advertising = false;
    }

    public static AdvertiseSettings createAdvSettings(boolean connectable, int timeoutMillis) {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        //设置广播的模式, 功耗相关
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setConnectable(connectable);
        builder.setTimeout(timeoutMillis);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        AdvertiseSettings mAdvertiseSettings = builder.build();
        if (mAdvertiseSettings == null) {
            Log.e(LCAT, "==============mAdvertiseSettings is null==============");
        }
        return mAdvertiseSettings;
    }

    public static AdvertiseData createIBeaconAdvertiseData(UUID proximityUuid, short major, short minor, byte txPower) {
        String[] uuidstr = proximityUuid.toString().replaceAll("-", "").toLowerCase().split("");
        byte[] uuidBytes = new byte[16];
        for (int i = 1, x = 0; i < uuidstr.length; x++) {
            uuidBytes[x] = (byte) ((Integer.parseInt(uuidstr[i++], 16) << 4) | Integer.parseInt(uuidstr[i++], 16));
        }
        byte[] majorBytes = {(byte) (major >> 8), (byte) (major & 0xff)};
        byte[] minorBytes = {(byte) (minor >> 8), (byte) (minor & 0xff)};
        byte[] mPowerBytes = {txPower};
        byte[] manufacturerData = new byte[0x17];
        byte[] flagibeacon = {0x02, 0x15};

        System.arraycopy(flagibeacon, 0x0, manufacturerData, 0x0, 0x2);
        System.arraycopy(uuidBytes, 0x0, manufacturerData, 0x2, 0x10);
        System.arraycopy(majorBytes, 0x0, manufacturerData, 0x12, 0x2);
        System.arraycopy(minorBytes, 0x0, manufacturerData, 0x14, 0x2);
        System.arraycopy(mPowerBytes, 0x0, manufacturerData, 0x16, 0x1);

        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.addManufacturerData(0x004c, manufacturerData);

        AdvertiseData adv = builder.build();
        return adv;
    }

    public StartScanResult startScan() {
        if (scanning) {
            return StartScanResult.RESULT_SCANNING;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 判断是否具有位置权限
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return StartScanResult.RESULT_NO_LOCATION_PERMISSION;
            }
        }

        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        // 判断蓝牙是否开启
        if (!mBluetoothAdapter.isEnabled()) {
            return StartScanResult.RESULT_BLUETOOTH_DISABLED;
        }

        mMyScanCallback = new MyScanCallback(BeaconAdmin.this);
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.startScan(mMyScanCallback);
            scanning = true;
        }

        Log.d(LCAT, "=========Scanning=========");
        return StartScanResult.RESULT_SUCCESS;
    }

    public void stopScan() {
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mMyScanCallback);
            Log.d(LCAT, "======Stop Scanning======");
        }

        mBluetoothLeScanner = null;
        mMyScanCallback = null;
        scanning = false;
    }

    public boolean isScanning() {
        return scanning;
    }

    /**
     * 广播回调类
     */
    private static class MyAdvertiseCallback extends AdvertiseCallback {
        WeakReference<BeaconAdmin> weakReference;

        private String uuid;
        private int major;
        private int minor;

        public MyAdvertiseCallback(BeaconAdmin beaconAdmin, String uuid, int major, int minor) {
            weakReference = new WeakReference<BeaconAdmin>(beaconAdmin);

            this.uuid = uuid;
            this.major = major;
            this.minor = minor;
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(LCAT, String.format("========UUID: %s, major: %d, minor: %d StartSuccess========", uuid, major, minor));

            if (weakReference.get() != null) {
                weakReference.get().advertising = true;
                weakReference.get().actionListener.onAdvertiseStartSuccess(uuid, major, minor);
            }
        }

        @Override
        public void onStartFailure(int errorCode) {
            // TODO Auto-generated method stub
            super.onStartFailure(errorCode);
            Log.d(LCAT, String.format("========UUID: %s, major: %d, minor: %d StartFailure, errorCode: %d========", uuid, major, minor, errorCode));

            if (weakReference.get() != null) {
                weakReference.get().advertising = false;
                weakReference.get().actionListener.onAdvertiseStartFailure(errorCode);
            }
        }
    }

    /**
     * 扫描回调类
     */
    private static class MyScanCallback extends ScanCallback {
        WeakReference<BeaconAdmin> weakReference;

        public MyScanCallback(BeaconAdmin beaconAdmin) {
            weakReference = new WeakReference<BeaconAdmin>(beaconAdmin);
        }
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result == null) return;
            Log.d(LCAT, "======ScanCallback======:" + result.getDevice().getAddress());

            iBeacon ibeacon = iBeaconUtils.fromScanData(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());

            if(ibeacon != null && ibeacon.bluetoothAddress != null) {
                Log.d(LCAT, String.format("========address: %s, uuid: %s, major: %d, minor: %d, rssi: %d========", ibeacon.bluetoothAddress, ibeacon.proximityUuid, ibeacon.major, ibeacon.minor, ibeacon.rssi));

                if (weakReference.get() != null) {
                    weakReference.get().actionListener.onScanResult(ibeacon);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            if (weakReference.get() != null) {
                weakReference.get().actionListener.onScanFailed(errorCode);
            }
        }
    }
}
