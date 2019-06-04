package com.ycm.demo;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BLEAdmin {
    private static final String LCAT = "BLEAdmin";

    public enum StartScanResult {
        RESULT_NO_LOCATION_PERMISSION,
        RESULT_BLUETOOTH_DISABLED,
        RESULT_SCANNING,
        RESULT_SUCCESS
    }

    public static interface ActionListener {
        // 扫描成功
        void onScanResult(ScanResult result);

        // 扫描失败
        void onScanFailed(int errorCode);

        // 连接成功
        void onConnection();

        // 连接丢失
        void onDisconnection();

        // 读取数据
        void onCharacteristicRead(UUID uuid, String value);

        // 写入数据
        void onCharacteristicWrite(UUID uuid, String value);

        // 数据改变
        void onCharacteristicChanged(UUID uuid, String value);
    }

    public static final String ENCODE_HEX = "Hex";
    public static final String ENCODE_UTF8 = "UTF-8";

    private Context context;
    private BLEAdmin.ActionListener actionListener;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BLEAdmin.BleScanCallback mBleScanCallback;

    private CountDownTimer mTimer;

    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private String encodeType;
    private Boolean scanning = false;
    private Boolean connected = false;

    public BLEAdmin(@NonNull Context context, @NonNull BLEAdmin.ActionListener actionListener, @NonNull String encodeType) {
        this.context = context;
        this.actionListener = actionListener;
        this.encodeType = encodeType;

    }

    public StartScanResult startScan(int stopSecondsInFuture) {
        if (scanning) {
            return StartScanResult.RESULT_SCANNING;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //判断是否具有权限
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return StartScanResult.RESULT_NO_LOCATION_PERMISSION;
            }
        }

        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            return StartScanResult.RESULT_BLUETOOTH_DISABLED;
        }

        mBleScanCallback = new BLEAdmin.BleScanCallback(BLEAdmin.this);
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // 启动扫描
        mBluetoothLeScanner.startScan(mBleScanCallback);

        if (stopSecondsInFuture <= 0) {
            // 默认15秒后关闭扫描
            stopSecondsInFuture = 15;
        }
        mTimer = new CountDownTimer((long)stopSecondsInFuture * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                //每秒TODO
            }

            @Override
            public void onFinish() {
                stopScan();
            }
        };
        mTimer.start();

        Log.d(LCAT, "=========Start Scanning==========");
        scanning = true;

        return StartScanResult.RESULT_SUCCESS;
    }

    public void stopScan() {
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mBleScanCallback);
            Log.d(LCAT, "======Stop Scanning======");
        }

        if (mTimer != null) {
            mTimer.cancel();
        }

        scanning = false;
    }

    public void connect(ScanResult result) {
        if (connected || result == null) {
            return;
        }

        mBluetoothDevice = result.getDevice();
        mBluetoothGatt = mBluetoothDevice.connectGatt(context, false, new MyBluetoothGattCallback(BLEAdmin.this));
        mBluetoothGatt.connect();
    }

    public void disconnect() {
        mBluetoothDevice = null;
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt = null;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void read(@NonNull UUID serviceUUID, @NonNull UUID characteristicUUID) {
        if (!connected || mBluetoothGatt == null) {
            return;
        }

        BluetoothGattService service = mBluetoothGatt.getService(serviceUUID);
        if (service != null) {
            Log.d(LCAT, "======BLE Service Discovered======" + service.getUuid());
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);

            if (characteristic == null) return;

            if ((characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                Log.d(LCAT, "======BLE Read Characteristic======UUID: " + characteristic.getUuid());

                mBluetoothGatt.readCharacteristic(characteristic);
            }
        }
    }

    public void writeNoResponse(@NonNull UUID serviceUUID, @NonNull UUID characteristicUUID, @NonNull String value) {
        if (!connected || mBluetoothGatt == null) {
            return;
        }

        BluetoothGattService service = mBluetoothGatt.getService(serviceUUID);
        if (service != null) {
            Log.d(LCAT, "======BLE Service Discovered======" + service.getUuid());
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);

            if (characteristic == null) return;

            if ((characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                Log.d(LCAT, "======BLE Write No Response Characteristic======UUID: " + characteristic.getUuid() + "=======Value: " + value);

                byte[] data = toWriteByte(value, encodeType);
                characteristic.setValue(value);
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                mBluetoothGatt.writeCharacteristic(characteristic);
            }
        }
    }

    public void write(@NonNull UUID serviceUUID, @NonNull UUID characteristicUUID, @NonNull String value) {
        if (!connected || mBluetoothGatt == null) {
            return;
        }

        BluetoothGattService service = mBluetoothGatt.getService(serviceUUID);
        if (service != null) {
            Log.d(LCAT, "======BLE Service Discovered======" + service.getUuid());
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);

            if (characteristic == null) return;

            if ((characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                Log.d(LCAT, "======BLE Write Characteristic======UUID: " + characteristic.getUuid() + "=======Value: " + value);

                byte[] data = toWriteByte(value, encodeType);
                characteristic.setValue(data);
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                mBluetoothGatt.writeCharacteristic(characteristic);
            }
        }
    }

    /*
     * 扫描蓝牙回调
     */
    private static class BleScanCallback extends ScanCallback {
        WeakReference<BLEAdmin> weakReference;

        public BleScanCallback(BLEAdmin bleAdmin) {
            weakReference = new WeakReference<BLEAdmin>(bleAdmin);

        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result == null) return;

            Log.d(LCAT,"======Scan Callback======:" + result.getDevice().getName());

            if (weakReference.get() != null) {
                weakReference.get().actionListener.onScanResult(result);
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


    /*
     * 连接蓝牙回调
     */
    private static class MyBluetoothGattCallback extends BluetoothGattCallback {
        WeakReference<BLEAdmin> weakReference;

        public MyBluetoothGattCallback(BLEAdmin bleAdmin) {
            weakReference = new WeakReference<BLEAdmin>(bleAdmin);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(LCAT, "======BLE Connected======" + gatt.getDevice().getName());

                if (weakReference.get() != null) {
                    weakReference.get().connected = true;
                    weakReference.get().actionListener.onConnection();
                }
                gatt.discoverServices();
                return;
            }

            Log.d(LCAT, "======BLE Disconnected======" + gatt.getDevice().getName());
            if (weakReference.get() != null) {
                weakReference.get().connected = false;
                weakReference.get().actionListener.onDisconnection();
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

                if (weakReference.get() != null) {
                    String value = toReadString(characteristic.getValue(), weakReference.get().encodeType);

                    Log.d(LCAT, "======On Characteristic Read======UUID: " + characteristic.getUuid() + ", Value: " + value);

                    weakReference.get().actionListener.onCharacteristicRead(characteristic.getUuid(), value);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            //只有setType是writeWithResponse的时候，才会触发onCharacteristicWrite回调
            if (status == BluetoothGatt.GATT_SUCCESS) {

                if (weakReference.get() != null) {
                    String value = toReadString(characteristic.getValue(), weakReference.get().encodeType);

                    Log.d(LCAT, "======On Characteristic Write======UUID: " + characteristic.getUuid() + ", Value: " + Arrays.toString(characteristic.getValue()));

                    weakReference.get().actionListener.onCharacteristicWrite(characteristic.getUuid(), value);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            if (weakReference.get() != null) {
                String value = toReadString(characteristic.getValue(), weakReference.get().encodeType);

                Log.d(LCAT, "======On Characteristic Changed======UUID: " + characteristic.getUuid() + ", Value: " + value);

                weakReference.get().actionListener.onCharacteristicChanged(characteristic.getUuid(), value);
            }
        }
    }

    /*
     * 将UTF-8字符串转成byte[]
     *
     * @param str 需要转换的字符串
     * @param encode 返回数据编码格式，UTF-8或Hex
     * @return 返回转换完之后的数据
     * */
    private static byte[] toWriteByte(String str, String encode) {
        if (ENCODE_HEX.equals(encode)) {
            return stringToHexByte(str);
        } else {
            return str.getBytes();
        }
    }
    /*
     * 将byte[]转成UTF-8字符串
     *
     * @param str 需要转换的byte[]
     * @param encode 参数的编码格式，UTF-8或Hex
     * @return 返回转换完之后的数据
     * */
    private static String toReadString(byte[] str, String encode) {
        if (ENCODE_HEX.equals(encode)) {
            return hexBytesToString(str);
        } else {
            try {
                return new String(str, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return  "";
            }
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

}
