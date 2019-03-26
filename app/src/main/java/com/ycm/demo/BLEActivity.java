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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

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

    // BLE的View，Key为Beacon的address
    private Map<String, BLEView> bleViews = new HashMap<String, BLEView>();

    private TextView msgView;
    private LinearLayout containerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        msgView = findViewById(R.id.ble_msg);
        containerView = findViewById(R.id.ble_devices);
    }

    private void initScanner() {
        Log.d(LCAT, "Initializing Scanner");
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

            mFromScanDataThread = new BLEActivity.FromScanDataThread(BLEActivity.this);
        }

        if (mFromScanDataThread != null) {
            queue.clear();
            mFromScanDataThread.start();
        }
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.startScan(mBleScanCallback);
        }

        Log.d(LCAT, "Scanning");
        msgView.setText("Scanning");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // requestCode 与请求开启 Bluetooth 传入的 requestCode 相对应
        if (requestCode == REQUEST_CODE_BLUETOOTH_ON) {
            switch (resultCode) {
                // 点击确认按钮
                case Activity.RESULT_OK:
                    Log.d(LCAT, "用户选择开启 Bluetooth，Bluetooth 会被开启");
                    initScanner();
                    break;
                // 点击取消按钮或点击返回键
                case Activity.RESULT_CANCELED:
                    Log.d(LCAT, "用户拒绝打开 Bluetooth, Bluetooth 不会被开启");
                    Toast.makeText(this, "需要打开蓝牙才可以搜索到信标！", Toast.LENGTH_LONG).show();
                    finish();
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
                initScanner();
            } else {
                //permission denied, boo! Disable the functionality that depends on this permission.
                //这里进行权限被拒绝的处理
                Toast.makeText(this, "需要打开位置权限才可以搜索到信标", Toast.LENGTH_LONG).show();
                finish();
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

        if (mFromScanDataThread != null) {
            mFromScanDataThread.setRunStatus(false);
            Log.d(LCAT, "======Stop FromScanDataThread======");
        }
    }

    @Override
    protected void onDestroy() {
        mBluetoothManager = null;

        if (mBluetoothAdapter!= null && mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }
        mBluetoothAdapter = null;

        mBluetoothLeScanner = null;
        mBleScanCallback = null;

        mFromScanDataThread = null;

        msgView = null;
        containerView.removeAllViews();
        containerView = null;

        queue.clear();

        bleViews.clear();
        bleViews = null;

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

    private void renderItem(BluetoothDevice device, int rssi) {
        if (!bleViews.containsKey(device.getAddress())) {
            BLEView bleView = new BLEView(BLEActivity.this, device, rssi);
            bleViews.put(device.getAddress(), bleView);
            containerView.addView(bleView.getView());
        }

//        if (bleViews.size() > 10) {
//            if (mBluetoothLeScanner != null) {
//                mBluetoothLeScanner.stopScan(mBleScanCallback);
//                Log.d(LCAT, "======Stop Scanning======");
//            }
//
//            if (mFromScanDataThread != null) {
//                mFromScanDataThread.setRunStatus(false);
//                Log.d(LCAT, "======Stop FromScanDataThread======");
//            }
//        }
    }

    /**
     * api21+低功耗蓝牙接口回调，以下回调的方法可以根据需求去做相应的操作
     */
    private static class BleScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result == null) return;

            Log.d(LCAT, Thread.currentThread().getId() + "======BleScanCallback======:" + result.getDevice().getAddress());
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
                        Log.d(LCAT, Thread.currentThread().getId() + "======FromScanDataThread======:" + result.getDevice().getAddress());

                        if(result != null && result.getDevice().getAddress() != null) {
                            final BluetoothDevice device = result.getDevice();
                            final int rssi = result.getRssi();

                            if (weakReference.get() != null) {
                                weakReference.get().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        weakReference.get().renderItem(device, rssi);
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

    private static class BLEView implements View.OnClickListener {
        ConstraintLayout itemLayout;
        TextView nameView;
        TextView addressView;
        TextView rssiView;

        BluetoothDevice device;
        BluetoothGatt gatt;

        public BLEView(Context context, BluetoothDevice device, int rssi) {
            this.device = device;

            LayoutInflater inflater = LayoutInflater.from(context);
            itemLayout = (ConstraintLayout)inflater.inflate(R.layout.ble_item, null);

            nameView = itemLayout.findViewById(R.id.ble_item_name);
            nameView.setText(device.getName());

            addressView = itemLayout.findViewById(R.id.ble_item_address);
            addressView.setText(device.getAddress());


            rssiView = itemLayout.findViewById(R.id.ble_item_rssi);
            rssiView.setText(String.valueOf(rssi));

            itemLayout.setOnClickListener(this);
        }

        public void setRSSI(int rssi) {
            rssiView.setText(rssi);
        }

        public View getView() {
            return itemLayout;
        }

        @Override
        public void onClick(View v) {
            if (gatt != null) {
                gatt.disconnect();
                gatt = null;
                return;
            }
            gatt = device.connectGatt(v.getContext(), true, new BluetoothGattCallback() {

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(LCAT, "======BLE Connected======" + gatt.getDevice().getAddress());
                        gatt.discoverServices();
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        List<BluetoothGattService> supportedGattServices = gatt.getServices();
                        for(int i = 0; i < supportedGattServices.size(); i++){
                            Log.d(LCAT, "======BLE Service Discovered======" + supportedGattServices.get(i).getUuid());
//                            List<BluetoothGattCharacteristic> listGattCharacteristic = supportedGattServices.get(i).getCharacteristics();
                        }
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                }
            });
        }
    }
}
