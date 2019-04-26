package com.ycm.demo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class BluetoothActivity extends AppCompatActivity implements BluetoothAdmin.ActionListener {
    private static final String LCAT = "BluetoothActivity";

    private TextView msgView;
    private TextView rwMsgView;

    private ListView devicesView;
    private ScanResultAdapter adapter;

    List<String> deviceNames = new ArrayList<String>();


    private BluetoothAdmin mBluetoothAdmin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        mBluetoothAdmin= new BluetoothAdmin(BluetoothActivity.this, this);


        msgView = findViewById(R.id.bluetooth_msg);
        rwMsgView = findViewById(R.id.bluetooth_rw_msg);
        final EditText editText = findViewById(R.id.bluetooth_post_text);
        Button postBtn = findViewById(R.id.bluetooth_post_button);
        postBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String data = editText.getText().toString();
                mBluetoothAdmin.write(data.getBytes());
            }
        });
        // 客户端
//        devicesView = findViewById(R.id.bluetooth_device_list);
//        adapter = new ScanResultAdapter(BluetoothActivity.this, R.layout.bluetooth_device_item, new ArrayList<BluetoothDevice>());
//        devicesView.setAdapter(adapter);
//        devicesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//
//                BluetoothDevice device = adapter.getItem(position);
//                mBluetoothAdmin.connect(device);
//            }
//        });
//        mBluetoothAdmin.startDiscovery();

        // 服务端
        mBluetoothAdmin.createServer();
    }

    @Override
    public void onDiscovery(BluetoothDevice device) {
        String address = device.getAddress();
        if (deviceNames.contains(address)) {
            return;
        }

        adapter.add(device);
        deviceNames.add(address);
    }

    @Override
    public void onConnectSuccess() {
        msgView.setText("Connect Success");
        msgView.setTextColor(Color.BLUE);
    }

    @Override
    public void onConnectFailed() {
        msgView.setText("Connect Failed");
        msgView.setTextColor(Color.RED);
    }

    @Override
    public void onDisconnection() {
        msgView.setText("Disconnection");
        msgView.setTextColor(Color.RED);

        mBluetoothAdmin.createServer();
    }

    @Override
    public void onRead(byte[] data, int length) {
        rwMsgView.setText(new String(data));
    }

    @Override
    public void onReadFailed() {
        rwMsgView.setText("Read Failed");
    }

    @Override
    public void onWriteFailed() {
        rwMsgView.setText("Write Failed");
    }

    /*
     * Bluetooth Device列表适配器
     */
    private class ScanResultAdapter extends ArrayAdapter<BluetoothDevice> {
        private int resource;
        private List<BluetoothDevice> objects;
        public ScanResultAdapter(Context context, int resource, List<BluetoothDevice> objects) {
            super(context, resource, objects);
            this.resource = resource;
            this.objects = objects;
        }

        @Override
        public View getView(int position, View convertView,ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(this.resource, null);

            TextView nameView = (TextView) view.findViewById(R.id.bluetooth_device_item_name);
            TextView addressView = (TextView) view.findViewById(R.id.bluetooth_device_item_address);

            BluetoothDevice device = getItem(position);
            nameView.setText(device.getName());

            String state = "未配对";
            if(device.getBondState() == BluetoothDevice.BOND_BONDED) {
                state = "已配对";
            }
            addressView.setText(state);

            return view;
        }
    }

    @Override
    protected void onDestroy() {
        mBluetoothAdmin.destroy();
        mBluetoothAdmin = null;

        super.onDestroy();
    }
}
