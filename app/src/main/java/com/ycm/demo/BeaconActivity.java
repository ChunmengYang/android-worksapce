package com.ycm.demo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ycm.demo.beacon.iBeacon;

import java.util.HashMap;
import java.util.Map;

public class BeaconActivity extends AppCompatActivity implements BeaconAdmin.ActionListener {
    private static final String LCAT = "BeaconActivity";
    private static final int REQUEST_CODE_BLUETOOTH_ON_SCANNER = 1;
    private static final int REQUEST_CODE_BLUETOOTH_ON_ADVERTISER = 2;
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 3;

    private BeaconAdmin beaconAdmin;

    private Map<String, BeaconView> beaconViews = new HashMap<String, BeaconView>();

    private TextView msgView;
    private Button  advertiseBtn;
    private LinearLayout containerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        beaconAdmin = new BeaconAdmin(BeaconActivity.this, this);

        msgView = findViewById(R.id.beacon_msg);
        advertiseBtn = findViewById(R.id.beacon_advertise);
        containerView = findViewById(R.id.beacon_devices);

        advertiseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startAdvertising();
            }
        });

        startScan();
    }

    public void startScan() {
        BeaconAdmin.StartScanResult status = beaconAdmin.startScan();
        if (status ==  BeaconAdmin.StartScanResult.RESULT_NO_LOCATION_PERMISSION) {
            // 判断是否需要向用户解释为什么需要申请该权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(this, "需要打开位置权限才可以搜索到信标", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            // 请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_ACCESS_COARSE_LOCATION);

        } else if (status ==  BeaconAdmin.StartScanResult.RESULT_BLUETOOTH_DISABLED) {

            Intent requestBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(requestBluetoothOn, REQUEST_CODE_BLUETOOTH_ON_SCANNER);

        } else if (status ==  BeaconAdmin.StartScanResult.RESULT_SCANNING)  {
            msgView.setText("Scanning");
        } else {
            msgView.setText("Start Scann Success");
        }
    }

    public void startAdvertising() {
        BeaconAdmin.StartAdvertisingResult status = beaconAdmin.startAdvertising("FDA50693-A4E2-4FB1-AFCF-C6EB07647825", 5,1505,0);

        if (status == BeaconAdmin.StartAdvertisingResult.RESULT_BLUETOOTH_DISABLED) {
            Intent requestBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            BeaconActivity.this.startActivityForResult(requestBluetoothOn, REQUEST_CODE_BLUETOOTH_ON_ADVERTISER);

        } else if (status == BeaconAdmin.StartAdvertisingResult.RESULT_ADVERTISING) {
            msgView.setText("Advertising");
        } else {
            msgView.setText("Start Advertising Success");
        }
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
                    startAdvertising();
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
    @Override
    public void onAdvertiseStartSuccess(String uuid, int major, int minor) {
        msgView.setText(String.format("UUID: %s, \nmajor: %d, minor: %d \nStart Success", uuid, major, minor));
    }

    @Override
    public void onAdvertiseStartFailure(int errorCode) {
        msgView.setText(String.format("Start Failure, errorCode: %d", errorCode));
    }

    @Override
    public void onScanResult(iBeacon beacon) {
        final String address = beacon.bluetoothAddress;
        final String uuid = beacon.proximityUuid;
        final String majorAndMinor = "major:" + beacon.major + ", minor:" + beacon.minor + ", rssi:" + beacon.rssi;

        renderItem(address, uuid, majorAndMinor);
    }

    @Override
    public void onScanFailed(int errorCode) {

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
        beaconAdmin.startScan();
        beaconAdmin.stopAdvertising();

        beaconViews.clear();
        beaconViews = null;

        msgView = null;
        containerView.removeAllViews();
        containerView = null;

        super.onDestroy();
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
