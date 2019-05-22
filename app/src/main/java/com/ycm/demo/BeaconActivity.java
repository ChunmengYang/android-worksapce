package com.ycm.demo;

import android.Manifest;
import android.app.Activity;
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ycm.demo.beacon.iBeacon;
import com.ycm.demo.beacon.iBeaconUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BeaconActivity extends AppCompatActivity {
    private static final String LCAT = "BeaconActivity";
    private static final int REQUEST_CODE_BLUETOOTH_ON_SCANNER = 1;
    private static final int REQUEST_CODE_BLUETOOTH_ON_ADVERTISER = 2;

    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private MyAdvertiseCallback mMyAdvertiseCallback;

    private BluetoothLeScanner mBluetoothLeScanner;
    private MyScanCallback mMyScanCallback;

    private Map<String, BeaconView> beaconViews = new HashMap<String, BeaconView>();

    private TextView msgView;
    private Button  advertiseBtn;
    private LinearLayout containerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        msgView = findViewById(R.id.beacon_msg);
        advertiseBtn = findViewById(R.id.beacon_advertise);
        containerView = findViewById(R.id.beacon_devices);

        advertiseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAdvertising("FDA50693-A4E2-4FB1-AFCF-C6EB07647825", 5,1505,0);
            }
        });

        startScan();
    }

    private void startScan() {
        Log.d(LCAT, "=========Initializing Scanner=========");

        msgView.setText("Initializing Scanner");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //判断是否具有权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                //判断是否需要向用户解释为什么需要申请该权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(this, "需要打开位置权限才可以搜索到信标", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ACCESS_COARSE_LOCATION);
                return;
            }
        }

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent requestBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(requestBluetoothOn, REQUEST_CODE_BLUETOOTH_ON_SCANNER);
            return;
        }

        mMyScanCallback = new MyScanCallback(BeaconActivity.this);
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.startScan(mMyScanCallback);
        }
        msgView.setText("Scanning");

        Log.d(LCAT, "=========Scanning=========");
    }

    public void stopScan() {
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mMyScanCallback);
            Log.d(LCAT, "======Stop Scanning======");
        }

        mBluetoothLeScanner = null;
        mMyScanCallback = null;
    }


    public void startAdvertising(final String uuid, final int major, final int minor, int timeoutMillis) {
        Log.d(LCAT, "=========Start Advertising=========");

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent requestBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(requestBluetoothOn, REQUEST_CODE_BLUETOOTH_ON_ADVERTISER);
            return;
        }

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mMyAdvertiseCallback = new MyAdvertiseCallback(BeaconActivity.this, uuid, major, minor);

        mBluetoothLeAdvertiser.startAdvertising(
                createAdvSettings(false, timeoutMillis),
                createIBeaconAdvertiseData(UUID.fromString(uuid), (short) major, (short) minor, (byte) 0xc8),
                mMyAdvertiseCallback);
    }

    public void stopAdvertising() {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mMyAdvertiseCallback);
            Log.d(LCAT, "=========Stop Advertising=========");
        }

        mBluetoothLeAdvertiser = null;
        mMyAdvertiseCallback = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_BLUETOOTH_ON_SCANNER) {
            switch (resultCode) {
                // 点击确认按钮
                case Activity.RESULT_OK:
                    startScan();
                    break;
                // 点击取消按钮或点击返回键
                case Activity.RESULT_CANCELED:
                    Toast.makeText(this, "需要打开蓝牙才可以搜索到信标！", Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        } else if (requestCode == REQUEST_CODE_BLUETOOTH_ON_ADVERTISER) {
            switch (resultCode) {
                // 点击确认按钮
                case Activity.RESULT_OK:
                    startAdvertising("FDA50693-A4E2-4FB1-AFCF-C6EB07647825", 5,1505,0);
                    break;
                // 点击取消按钮或点击返回键
                case Activity.RESULT_CANCELED:
                    Toast.makeText(this, "需要打开蓝牙才可以设置为信标！", Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
                // 这里进行授权被允许的处理
                startScan();
            } else {
                // 这里进行权限被拒绝的处理
                Toast.makeText(this, "需要打开位置权限才可以搜索到信标", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void renderItem(String address, String uuid, String majorAndMinor) {
        if (beaconViews.containsKey(address)) {
            BeaconView beaconView = beaconViews.get(address);
            beaconView.setUUID(uuid);
            beaconView.setMajorAndMinor(majorAndMinor);
        } else  {
            BeaconView beaconView = new BeaconView(BeaconActivity.this, address, uuid, majorAndMinor);
            beaconViews.put(address, beaconView);
            containerView.addView(beaconView.getView());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        mBluetoothManager = null;
        mBluetoothAdapter = null;

        stopAdvertising();
        stopScan();

        beaconViews.clear();
        beaconViews = null;

        msgView = null;
        containerView.removeAllViews();
        containerView = null;

        super.onDestroy();
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

    /**
     * 广播回调类
     */
    private static class MyAdvertiseCallback extends AdvertiseCallback {
        WeakReference<BeaconActivity> weakReference;

        private String uuid;
        private int major;
        private int minor;

        public MyAdvertiseCallback(BeaconActivity activity, String uuid, int major, int minor) {
            weakReference = new WeakReference<BeaconActivity>(activity);

            this.uuid = uuid;
            this.major = major;
            this.minor = minor;
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(LCAT, String.format("========UUID: %s, major: %d, minor: %d StartSuccess========", uuid, major, minor));

            if (weakReference.get() != null) {
                weakReference.get().msgView.setText(String.format("UUID: %s, \nmajor: %d, minor: %d \nStart Success", uuid, major, minor));
            }
        }

        @Override
        public void onStartFailure(int errorCode) {
            // TODO Auto-generated method stub
            super.onStartFailure(errorCode);
            Log.d(LCAT, String.format("========UUID: %s, major: %d, minor: %d StartFailure, errorCode: %d========", uuid, major, minor, errorCode));
            if (weakReference.get() != null) {
                weakReference.get().msgView.setText(String.format("UUID: %s,\n major: %d, minor: %d \nStart Failure, errorCode: %d", uuid, major, minor, errorCode));
            }
        }
    }

    /**
     * 扫描回调类
     */
    private static class MyScanCallback extends ScanCallback {
        WeakReference<BeaconActivity> weakReference;

        public MyScanCallback(BeaconActivity activity) {
            weakReference = new WeakReference<BeaconActivity>(activity);
        }
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result == null) return;
            Log.d(LCAT, "======ScanCallback======:" + result.getDevice().getAddress());

            iBeacon ibeacon = iBeaconUtils.fromScanData(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());

            if(ibeacon != null && ibeacon.bluetoothAddress != null) {
                final String address = ibeacon.bluetoothAddress;
                final String uuid = ibeacon.proximityUuid;
                final String majorAndMinor = "major:" + ibeacon.major + ", minor:" + ibeacon.minor + ", rssi:" + ibeacon.rssi;

                Log.d(LCAT, "======ScanCallback======:" + uuid +"\n"+ majorAndMinor);
                if (weakReference.get() != null) {
                    weakReference.get().renderItem(address, uuid, majorAndMinor);
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
        }
    }

    /**
     *  显示Beacon的类
     */
    private static class BeaconView {
        ConstraintLayout itemLayout;
        TextView addressView;
        TextView uuidView;
        TextView majAndMinView;

        public BeaconView(Context context, String address, String uuid, String majorAndMinor) {
            LayoutInflater inflater = LayoutInflater.from(context);
            itemLayout = (ConstraintLayout)inflater.inflate(R.layout.beacon_item, null);

            addressView = itemLayout.findViewById(R.id.beacon_item_address);
            addressView.setText(address);


            uuidView = itemLayout.findViewById(R.id.beacon_item_UUID);
            uuidView.setText(uuid);

            majAndMinView = itemLayout.findViewById(R.id.beacon_item_major_and_minor);
            majAndMinView.setText(majorAndMinor);
        }

        public void setUUID(String uuid) {
            if (uuidView != null) {
                uuidView.setText(uuid);
            }
        }

        public void setMajorAndMinor(String majorAndMinor) {
            if (majAndMinView != null) {
                majAndMinView.setText(majorAndMinor);
            }
        }

        public View getView() {
            return itemLayout;
        }
    }
}
