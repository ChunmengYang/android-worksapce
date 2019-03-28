package com.ycm.demo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

public class BLEActivity extends AppCompatActivity {

    private static final String LCAT = "BLEActivity";
    private static final int REQUEST_CODE_BLUETOOTH_ON = 1;
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BLEActivity.BleScanCallback mBleScanCallback;

    // 解析线程，解析Beacon数据
    private BLEActivity.FromScanDataThread mFromScanDataThread;
    // 扫描结果队列，解析线程从中拿数据
    private static java.util.PriorityQueue<ScanResult> queue = new PriorityQueue<ScanResult>(new Comparator<ScanResult>() {
        @Override
        public int compare(ScanResult o1, ScanResult o2) {
            if(o1.getRssi() <= o2.getRssi()){
                return 1;
            }
            else
                return -1;
        }
    });
    // 定时器，10秒后关闭扫描
    private CountDownTimer mTimer;

    private static String BLE_NAME = "Blank";
    private static String BLE_SERVICE_UUID = "B77A9A19-C4C2-4D36-84F4-F2584C97DE1F";
    private static String BLE_READ_CHARACTERISTIC_UUID = "C80804CC-3996-44A1-BE2B-51DFBA3634AC";
    private static String BLE_WIRTE_NO_RESPONSE_CHARACTERISTIC_UUID = "C80804CC-3996-44A1-BE2B-51DFBA3634AC";
    private static String BLE_WIRTE_CHARACTERISTIC_UUID = "DFDAD554-19DD-4C24-B27E-8DC59B53939E";
    private static String BLE_NOTIF_CHARACTERISTIC_UUID = "4C2345A7-6628-4A9A-AFCA-3E9478E8D94A";
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private Boolean isConnected = false;

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
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothGatt == null || !isConnected) {
                    startScan();
                }
            }
        });

        Button readBtn = findViewById(R.id.ble_read_button);
        readBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LCAT, "======Read Button Click======");
                if (mBluetoothGatt != null && isConnected) {
                    BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(BLE_SERVICE_UUID));
                    if (service != null) {
                        Log.d(LCAT, "======BLE Service Discovered======" + service.getUuid());
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BLE_READ_CHARACTERISTIC_UUID));

                        if (characteristic == null) return;

                        if ((characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            Log.d(LCAT, "======BLE Read Characteristic Discovered======" + characteristic.getUuid());
                            mBluetoothGatt.readCharacteristic(characteristic);
                        }


//                        Log.d(LCAT, "======BLE Service Discovered======" + service.getUuid());
//                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BLE_NOTIF_CHARACTERISTIC_UUID));
//
//                        if (characteristic == null) return;
//
//                        if ((characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                            Log.d(LCAT, "======BLE Notif Characteristic Discovered======" + characteristic.getUuid());
//
//                            boolean isEnableNotification = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
//                            if(isEnableNotification) {
//                                List<BluetoothGattDescriptor> descriptorList = characteristic.getDescriptors();
//                                if(descriptorList != null && descriptorList.size() > 0) {
//                                    for(BluetoothGattDescriptor descriptor : descriptorList) {
//                                        Log.d(LCAT, "======BluetoothGattDescriptor Discovered======" + descriptor.getUuid());
//                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                                        mBluetoothGatt.writeDescriptor(descriptor);
//                                    }
//                                }
//                            }
//                        }
                    }
                }
            }
        });

        Button writeBtn = findViewById(R.id.ble_write_button);
        final EditText writeText = findViewById(R.id.ble_write_msg);
        writeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LCAT, "======Write Button Click======");
                if (mBluetoothGatt != null && isConnected) {
                    BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(BLE_SERVICE_UUID));
                    if (service != null) {
                        Log.d(LCAT, "======BLE Service Discovered======" + service.getUuid());
//                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BLE_WIRTE_NO_RESPONSE_CHARACTERISTIC_UUID));
//
//                        if (characteristic == null) return;
//
//                        if ((characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
//                            Log.d(LCAT, "======BLE Write No Response Characteristic Discovered======" + characteristic.getUuid());
//                            byte[] value = stringToHexByte(writeText.getText().toString());
//                            Log.d(LCAT, "======BLE Write No Response Characteristic Value======" + Arrays.toString(value));
//                            characteristic.setValue(value);
//                            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
//                            mBluetoothGatt.writeCharacteristic(characteristic);
//                        }

                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BLE_WIRTE_CHARACTERISTIC_UUID));

                        if (characteristic == null) return;

                        if ((characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                            Log.d(LCAT, "======BLE Write Characteristic Discovered======" + characteristic.getUuid());
                            byte[] value = stringToHexByte(writeText.getText().toString());
                            Log.d(LCAT, "======BLE Write Characteristic Value======" + Arrays.toString(value));
                            characteristic.setValue(value);
                            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            mBluetoothGatt.writeCharacteristic(characteristic);
                        }
                    }
                }
            }
        });
    }

    private void startScan() {
        Log.d(LCAT, "Initializing Scanner");
        msgView.setTextColor(Color.YELLOW);
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

        if (mBluetoothLeScanner == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (!mBluetoothAdapter.isEnabled()) {

                Intent requestBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.startActivityForResult(requestBluetoothOn, REQUEST_CODE_BLUETOOTH_ON);
                return;
            }
            mBleScanCallback = new BLEActivity.BleScanCallback();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }

        // 清空扫描结果队列
        queue.clear();

        // 启动数据解析线程
        mFromScanDataThread = new BLEActivity.FromScanDataThread(BLEActivity.this);
        mFromScanDataThread.start();
        Log.d(LCAT, "======Start Scanning======");

        // 启动扫描
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.startScan(mBleScanCallback);
            Log.d(LCAT, "======Start FromScanDataThread======");
        }


        msgView.setText("Scanning");

        // 定时器，10秒后结束扫描
        if (mTimer == null) {
            mTimer = new CountDownTimer((long)15 * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    //每秒TODO
                }

                @Override
                public void onFinish() {
                    stopScan();
                }
            };
        }
        mTimer.start();
    }

    private void stopScan() {
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mBleScanCallback);
            Log.d(LCAT, "======Stop Scanning======");
        }

        if (mFromScanDataThread != null) {
            mFromScanDataThread.setRunStatus(false);
            Log.d(LCAT, "======Stop FromScanDataThread======");
        }
        if (mTimer != null) {
            mTimer.cancel();
        }

        msgView.setText("Scan completed");
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
                    Toast.makeText(this, "需要打开蓝牙才可以搜索到信标！", Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, "需要打开位置权限才可以搜索到信标", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * api21+低功耗蓝牙接口回调，以下回调的方法可以根据需求去做相应的操作
     */
    private static class BleScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result == null) return;

            Log.d(LCAT, Thread.currentThread().getId() + "======BleScanCallback======:" + result.getDevice().getName());
            synchronized (queue) {
                queue.offer(result);
                queue.notify();
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

    private static class FromScanDataThread extends Thread {
        WeakReference<BLEActivity> weakReference;

        FromScanDataThread(BLEActivity activity) {
            weakReference = new WeakReference<BLEActivity>(activity);
        }
        private boolean runStatus = true;

        public void  setRunStatus(boolean status) {
            this.runStatus = status;
            synchronized (queue) {
                queue.clear();
                queue.notify();
            }
        }

        @Override
        public void run() {
            while (true) {
                synchronized (queue) {
                    while(queue.size() == 0 && this.runStatus) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            queue.notify();
                        }
                    }

                    if(queue.size() != 0) {
                        ScanResult result = queue.poll();

                        Log.d(LCAT, Thread.currentThread().getId() + "======FromScanDataThread======:" + result.getDevice().getName());

                        if(result != null && BLE_NAME.equals(result.getDevice().getName())) {
                            final BluetoothDevice device = result.getDevice();
                            final int rssi = result.getRssi();

                            if (weakReference.get() != null) {
                                weakReference.get().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        weakReference.get().setupBluetoothDevice(device, rssi);
                                    }
                                });
                            }

                        }
                    }
                }

                if(!this.runStatus){
                    Log.d(LCAT, Thread.currentThread().getId() + "======FromScanDataThread End======");
                    return;
                }
            }
        }
    }

    private void setupBluetoothDevice(final BluetoothDevice device, int rssi) {
        stopScan();

        deviceView.removeAllViews();
        mBluetoothDevice = null;
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt = null;
        }

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

        mBluetoothDevice = device;
        mBluetoothGatt = mBluetoothDevice.connectGatt(BLEActivity.this, false, new MyBluetoothGattCallback(BLEActivity.this));
        mBluetoothGatt.connect();
    }

    private static class MyBluetoothGattCallback extends BluetoothGattCallback {
        WeakReference<BLEActivity> weakReference;

        public MyBluetoothGattCallback(BLEActivity activity) {
            weakReference = new WeakReference<BLEActivity>(activity);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(LCAT, "======BLE Connected======" + gatt.getDevice().getName());

                if (weakReference.get() != null) {
                    weakReference.get().isConnected = true;
                    weakReference.get().msgView.setTextColor(Color.GREEN);
                    weakReference.get().msgView.setText("Connected");
                }
                gatt.discoverServices();
                return;
            }

            Log.d(LCAT, "======BLE Disconnected======" + gatt.getDevice().getName());
            if (weakReference.get() != null) {
                weakReference.get().isConnected = false;
                weakReference.get().msgView.setTextColor(Color.RED);
                weakReference.get().msgView.setText("Disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                String value = hexBytesToString(characteristic.getValue());

                Log.d(LCAT, "======On Characteristic Read======UUID: " + characteristic.getUuid() + ", Value: " + Arrays.toString(characteristic.getValue()));

                if (weakReference.get() != null) {
                    TextView readText = weakReference.get().findViewById(R.id.ble_read_msg);
                    readText.setText(value);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            //只有setType是writeWithResponse的时候，才会触发onCharacteristicWrite回调
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String value = hexBytesToString(characteristic.getValue());

                Log.d(LCAT, "======On Characteristic Write======UUID: " + characteristic.getUuid() + ", Value: " + Arrays.toString(characteristic.getValue()));

                if (weakReference.get() != null) {
                    TextView readText = weakReference.get().findViewById(R.id.ble_read_msg);
                    readText.setText(value);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String value = hexBytesToString(characteristic.getValue());

            Log.d(LCAT, "======On Characteristic Changed======UUID: " + characteristic.getUuid() + ", Value: " + value);
        }
    }


    /**
     * 将utf-8字符串转换为16进制字节(写入数据)
     *
     * @param str 需要转换的字符串
     * @return 返回转换完之后的数据
     */
    private static byte[] stringToHexByte(String str) {
        if (str == null || str.equals("")) {
            return null;
        }

        byte[] src = str.getBytes();

        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString().getBytes();
    }
    /**
     * 将16进制的字节转化为UTF-8字符串(解析读出数据)
     *
     * @param src 需要被转换的字节
     * @return 返回转换完之后的数据
     */
    private static String hexBytesToString(byte[] src) {
        String hexString = new String(src);
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }

        String str = null;
        try {
            str = new String (d,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return str;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
    }

    @Override
    protected void onDestroy() {
        mBluetoothDevice = null;
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
        mBluetoothGatt = null;


        mBluetoothManager = null;
        if (mBluetoothAdapter!= null && mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }
        mBluetoothAdapter = null;
        mBluetoothLeScanner = null;
        mBleScanCallback = null;
        mFromScanDataThread = null;
        mTimer = null;
        queue.clear();

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
