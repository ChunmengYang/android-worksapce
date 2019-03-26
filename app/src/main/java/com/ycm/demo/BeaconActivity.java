package com.ycm.demo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
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

import com.ycm.demo.beacon.iBeacon;
import com.ycm.demo.beacon.iBeaconUtils;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class BeaconActivity extends AppCompatActivity {
    private static final String LCAT = "BeaconActivity";
    private static final int REQUEST_CODE_BLUETOOTH_ON = 1;

    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BleScanCallback mBleScanCallback;

    // 解析线程，解析Beacon数据
    private FromScanDataThread mFromScanDataThread;
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

    // Beacon的View，Key为Beacon的address
    private Map<String, BeaconView> beaconViews = new HashMap<String, BeaconView>();

    private TextView msgView;
    private LinearLayout containerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        msgView = findViewById(R.id.beacon_msg);
        containerView = findViewById(R.id.beacon_devices);
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

//                boolean enable =  mBluetoothAdapter.enable();
//                if (!enable) {
//                    Toast.makeText(this, "打开蓝牙功能失败，请到'系统设置'中手动开启蓝牙功能！", Toast.LENGTH_LONG).show();
//                    return;
//                }
            }
            mBleScanCallback = new BleScanCallback();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

            mFromScanDataThread = new FromScanDataThread(BeaconActivity.this);
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
        mBluetoothAdapter = null;
        mBluetoothLeScanner = null;
        mBleScanCallback = null;
        mFromScanDataThread = null;

        msgView = null;
        containerView.removeAllViews();
        containerView = null;

        queue.clear();

        beaconViews.clear();
        beaconViews = null;

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
        WeakReference<BeaconActivity> weakReference;

        FromScanDataThread(BeaconActivity activity) {
            weakReference = new WeakReference<BeaconActivity>(activity);
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

                        iBeacon ibeacon = iBeaconUtils.fromScanData(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                        if(ibeacon != null && ibeacon.bluetoothAddress != null) {

                            final String address = ibeacon.bluetoothAddress;
                            final String uuid = ibeacon.proximityUuid;
                            final String majorAndMinor = "major:" + ibeacon.major + ", minor:" + ibeacon.minor + ", rssi:" + ibeacon.rssi;

                            if (weakReference.get() != null) {
                                weakReference.get().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(LCAT, Thread.currentThread().getId() + "======FromScanDataThread CallBack======:" + uuid +"\n"+ majorAndMinor);

                                        weakReference.get().renderItem(address, uuid, majorAndMinor);
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
