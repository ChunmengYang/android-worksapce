package com.ycm.demo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothAdmin {
    public static final int CONNECT_FAILED = 0;
    public static final int CONNECT_SUCCESS = 1;
    public static final int WRITE_FAILED = 2;
    public static final int READ_FAILED = 3;

    private static final String LCAT = "BluetoothAdmin";

    public static interface ActionListener {

        // 发现蓝牙设备
        void onDiscovery(BluetoothDevice device);

        // 开始发现蓝牙设备
        void onDiscoveryStarted();

        // 完成发现蓝牙设备
        void onDiscoveryFinished();

        // 蓝牙已连接成功
        void onConnectSuccess();

        // 蓝牙连接失败
        void onConnectFailed();

        // 蓝牙连接丢失
        void onDisconnection();

        // 当读取到数据
        void onRead(byte[] data, int length);

        void onReadFailed();

        void onWriteFailed();
    }

    private static UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Context context;
    private BluetoothAdmin.ActionListener actionListener;

    private IntentFilter bluetoothFilter;
    private BluetoothReceiver bluetoothReceiver;

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothServerSocket serverSocket;
    private BluetoothSocket socket;

    private Handler handler;

    public BluetoothAdmin(Context context, BluetoothAdmin.ActionListener actionListener) {
        this.context = context;
        this.actionListener = actionListener;

        // 注册蓝牙搜索接收器
        bluetoothFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        bluetoothReceiver = new BluetoothReceiver();
        context.registerReceiver(bluetoothReceiver, bluetoothFilter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        handler = new Handler();
    }

    // 它会对其他蓝牙设备进行搜索，持续时间为12秒
    public void startDiscovery() {
        if (!bluetoothAdapter.isDiscovering()) {
            Set<BluetoothDevice>  devices = bluetoothAdapter.getBondedDevices();
            if (devices.size() > 0) {
                for (BluetoothDevice device : devices) {
                    if (actionListener != null) {
                        actionListener.onDiscovery(device);
                    }
                }
            }

            bluetoothAdapter.startDiscovery();
        }
    }

    // 停止搜索
    public void cancelDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    // 作为服务端
    public void createServer() {
        close();

        new Thread(new Runnable() {
            public void run() {
                try {
                    serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(context.getPackageName(), uuid);

                    socket = serverSocket.accept();
                } catch (IOException e) {
                    setState(CONNECT_FAILED);
                    Log.e(LCAT, "======createServer======" + e.toString());
                    return;
                }
                setState(CONNECT_SUCCESS);

                try {
                    read(socket.getInputStream());
                } catch (IOException e) {
                    Log.e(LCAT, e.toString());
                }

                Log.d(LCAT, "=========Server End==========");
                close();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (actionListener != null) {
                            actionListener.onDisconnection();
                        }
                    }
                });
            }
        }).start();
    }

    // 作为客户端搜索到服务端设备后发起连接
    public void connect(final BluetoothDevice device) {
        close();

        new Thread(new Runnable() {
            public void run() {
                BluetoothSocket tmp = null;
                try {
                    tmp = device.createRfcommSocketToServiceRecord(uuid);
                } catch (Exception e) {
                    setState(CONNECT_FAILED);
                    Log.e(LCAT, e.toString());
                    return;
                }
                socket = tmp;

                try {
                    socket.connect();
                } catch (Exception e) {
                    setState(CONNECT_FAILED);
                    Log.e(LCAT, e.toString());
                    return;
                }
                setState(CONNECT_SUCCESS);

                try {
                    read(socket.getInputStream());
                } catch (IOException e) {
                    Log.e(LCAT, e.toString());
                }

                Log.d(LCAT, "=========connect End==========");
                close();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (actionListener != null) {
                            actionListener.onDisconnection();
                        }
                    }
                });
            }
        }).start();
    }

    public void write(final byte[] data) {
        if (socket != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (socket.isConnected()) {
                            OutputStream out = socket.getOutputStream();
                            out.write(data);
                            out.flush();
                        }
                    } catch (Exception e) {
                        setState(WRITE_FAILED);
                        Log.e(LCAT, "======write======" + e.toString());
                    }
                }
            }).start();
        }
    }

    private void read(InputStream inputStream) {
        try {
            byte buffer[] = new byte[1024];
            int temp = 0;
            // 从InputStream当中读取客户端所发送的数据
            while ((temp = inputStream.read(buffer)) != -1) {
                final byte[] data = buffer;
                final int length = temp;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (actionListener != null) {
                            actionListener.onRead(data, length);
                        }
                    }
                });
                buffer = new byte[1024];
            }
            inputStream.close();
        } catch (IOException e) {
            setState(READ_FAILED);
            Log.e(LCAT, "======read======" + e.toString());
        }
    }

    public void close() {
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }

            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (Exception e) {
            Log.e(LCAT, "======close======" + e.toString());
        }
    }

    private void setState(int state) {
        if (actionListener == null) return;

        switch (state) {
            case CONNECT_FAILED:
                actionListener.onConnectFailed();
                break;
            case CONNECT_SUCCESS:
                actionListener.onConnectSuccess();
                break;
            case READ_FAILED:
                actionListener.onReadFailed();
                break;
            case WRITE_FAILED:
                actionListener.onWriteFailed();
                break;
        }
    }

    public void destroy() {
        // 注销蓝牙搜索接收器
        context.unregisterReceiver(bluetoothReceiver);
        bluetoothReceiver = null;

        cancelDiscovery();
        bluetoothAdapter = null;

        close();

        context = null;
        actionListener = null;
    }

    /*
     * Bluetooth Device搜索接收器
     */
    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null && !"".equals(device.getName())) {
                    if (actionListener != null) {
                        actionListener.onDiscovery(device);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                if (actionListener != null) {
                    actionListener.onDiscoveryStarted();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (actionListener != null) {
                    actionListener.onDiscoveryFinished();
                }
            }

        }
    }
}
