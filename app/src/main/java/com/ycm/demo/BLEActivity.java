package com.ycm.demo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.UUID;

public class BLEActivity extends AppCompatActivity implements BLEAdmin.ActionListener {
    private static final String LCAT = "BLEActivity";

    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    private static final int REQUEST_CODE_BLUETOOTH_ON = 2;

    private static String BLE_NAME = "Blank";
    private static String BLE_SERVICE_UUID = "0E61690A-B38D-43A0-9394-1FA76DD65E80";
    private static String BLE_READ_CHARACTERISTIC_UUID = "10E662A7-C116-41D5-9C25-6C6996FFB06A";
    private static String BLE_WIRTE_NO_RESPONSE_CHARACTERISTIC_UUID = "B309B160-234B-4015-900A-5C08E07770BC";
    private static String BLE_WIRTE_CHARACTERISTIC_UUID = "8AD25B3F-82EF-47C9-82AA-6910C7D29BAD";


    private BLEAdmin bleAdmin;
    private LinearLayout deviceView;
    private TextView msgView;
    private Button scanBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "该设备不支持BLE", Toast.LENGTH_SHORT).show();
            finish();
        }
        setContentView(R.layout.activity_ble);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        deviceView = findViewById(R.id.ble_devices);
        msgView = findViewById(R.id.ble_msg);
        scanBtn = findViewById(R.id.ble_connect_button);

        bleAdmin = new BLEAdmin(BLEActivity.this, this, BLEAdmin.ENCODE_UTF8);

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });

        Button readBtn = findViewById(R.id.ble_read_button);
        readBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LCAT, "======Read Button Click======");

                bleAdmin.read(UUID.fromString(BLE_SERVICE_UUID), UUID.fromString(BLE_WIRTE_CHARACTERISTIC_UUID));
            }
        });

        Button writeBtn = findViewById(R.id.ble_write_button);
        final EditText writeText = findViewById(R.id.ble_write_msg);
        writeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LCAT, "======Write Button Click======");

                bleAdmin.write(UUID.fromString(BLE_SERVICE_UUID), UUID.fromString(BLE_WIRTE_CHARACTERISTIC_UUID), writeText.getText().toString());
            }
        });
    }

    private void startScan() {
        if (bleAdmin.isConnected()) {
            return;
        }

        BLEAdmin.StartScanResult status = bleAdmin.startScan(30);
        if (status == BLEAdmin.StartScanResult.RESULT_NO_LOCATION_PERMISSION) {
            // 判断是否需要向用户解释为什么需要申请该权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(this, "需要打开位置权限才可以搜索到BLE设备", Toast.LENGTH_LONG).show();
                return;
            }
            // 请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_ACCESS_COARSE_LOCATION);

        } else if (status == BLEAdmin.StartScanResult.RESULT_BLUETOOTH_DISABLED) {
            Intent requestBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(requestBluetoothOn, REQUEST_CODE_BLUETOOTH_ON);

        } else if (status == BLEAdmin.StartScanResult.RESULT_SCANNING) {
            msgView.setTextColor(Color.YELLOW);
            msgView.setText("Scanning");

        } else {
            msgView.setTextColor(Color.YELLOW);
            msgView.setText("Start Scan");

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // requestCode 与请求开启 Bluetooth 传入的 requestCode 相对应
        if (requestCode == REQUEST_CODE_BLUETOOTH_ON) {
            switch (resultCode) {
                // 点击确认按钮
                case Activity.RESULT_OK:
                    Log.d(LCAT, "用户选择开启 Bluetooth，Bluetooth 会被开启");
                    startScan();
                    break;
                // 点击取消按钮或点击返回键
                case Activity.RESULT_CANCELED:
                    Log.d(LCAT, "用户拒绝打开 Bluetooth, Bluetooth 不会被开启");
                    Toast.makeText(this, "需要打开蓝牙才可以搜索到BLE设备！", Toast.LENGTH_LONG).show();
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
                //用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
                //这里进行授权被允许的处理
                startScan();
            } else {
                //permission denied, boo! Disable the functionality that depends on this permission.
                //这里进行权限被拒绝的处理
                Toast.makeText(this, "需要打开位置权限才可以搜索到BLE设备", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onScanResult(ScanResult result) {
         BluetoothDevice device = result.getDevice();
        if (device.getName() != null && device.getName().equals(BLE_NAME)) {
            setupBluetoothDevice(device, result.getRssi());

            bleAdmin.connect(result);
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        msgView.setTextColor(Color.RED);
        msgView.setText("Scan Failed");
    }

    @Override
    public void onConnection() {
        msgView.setTextColor(Color.GREEN);
        msgView.setText("Connected");
    }

    @Override
    public void onDisconnection() {
        if (msgView != null) {
            msgView.setTextColor(Color.RED);
            msgView.setText("Disconnected");
        }
    }

    @Override
    public void onCharacteristicRead(UUID uuid, String value) {
        TextView readText = findViewById(R.id.ble_read_msg);
        readText.setText("Read Success, UUID: " + uuid + ", Value: " + value);
    }

    @Override
    public void onCharacteristicWrite(UUID uuid, String value) {
        TextView readText = findViewById(R.id.ble_read_msg);
        readText.setText("Write Success, UUID: " + uuid + ", Value: " + value);
    }

    @Override
    public void onCharacteristicChanged(UUID uuid, String value) {
        TextView readText = findViewById(R.id.ble_read_msg);
        readText.setText("Value Changed, UUID: " + uuid + ", Value: " + value);
    }

    private void setupBluetoothDevice(BluetoothDevice device, int rssi) {
        bleAdmin.stopScan();

        deviceView.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(BLEActivity.this);
        ConstraintLayout layout = (ConstraintLayout)inflater.inflate(R.layout.ble_item, null);

        TextView nameView = layout.findViewById(R.id.ble_item_name);
        String name = device.getName();
        if (name == null || "".equals(name)) {
            name = "Unnamed";
        }
        nameView.setText(name);

        TextView addressView = layout.findViewById(R.id.ble_item_address);
        addressView.setText(device.getAddress());

        TextView rssiView = layout.findViewById(R.id.ble_item_rssi);
        rssiView.setText(String.valueOf(rssi));

        deviceView.addView(layout);
    }

    @Override
    protected void onStop() {
        bleAdmin.stopScan();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        bleAdmin.stopScan();
        bleAdmin.disconnect();
        bleAdmin = null;

        deviceView.removeAllViews();
        deviceView = null;
        msgView = null;
        scanBtn = null;

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
