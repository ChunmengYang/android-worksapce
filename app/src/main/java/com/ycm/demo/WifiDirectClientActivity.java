package com.ycm.demo;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WifiDirectClientActivity extends AppCompatActivity implements WifiDirectClientManager.ActionListener{
    private static final String LCAT = "WifiDirectClientActivity";

    private TextView msgView;
    private Button startSearchBtn;
    private Button stopSearchBtn;
    private Button postDataBtn;
    private ListView wifiListView;
    private ScanResultAdapter mScanResultAdapter;

    private WifiDirectClientManager clientManager;

    private WifiP2pInfo currentWifiP2pinfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_direct_client);

        msgView = findViewById(R.id.wifi_p2p_msg);
        startSearchBtn = findViewById(R.id.wifi_p2p_search_start);
        stopSearchBtn = findViewById(R.id.wifi_p2p_search_stop);
        postDataBtn = findViewById(R.id.wifi_p2p_post_data);

        wifiListView = findViewById(R.id.wifi_p2p_items);
        mScanResultAdapter = new ScanResultAdapter(WifiDirectClientActivity.this, R.layout.wifi_item, new ArrayList<WifiP2pDevice>());
        wifiListView.setAdapter(mScanResultAdapter);
        wifiListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                WifiP2pDevice result = mScanResultAdapter.getItem(position);
                clientManager.connect(result);
            }
        });

        clientManager = new WifiDirectClientManager(WifiDirectClientActivity.this, this);

        postDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentWifiP2pinfo != null) {
                    clientManager.postData(currentWifiP2pinfo, 8988, "name=HUAWEI-1505-Plus;password=mash51505");
                }
            }
        });
    }

    @Override
    public void onWifiP2pEnabled(Boolean enabled) {
        if (enabled) {
            msgView.setText("点击开始搜索，发现P2P设备！");

            startSearchBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 开始搜索P2P设备
                    mScanResultAdapter.clear();
                    clientManager.startDiscover();
                }
            });
            stopSearchBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 停止搜索P2P设
                    clientManager.stopDiscover();
                }
            });
        } else {
            msgView.setText("该设备不支持Wi-Fi Direct模式");
        }
    }

    @Override
    public void onPeers(Collection<WifiP2pDevice> peerList) {
        mScanResultAdapter.clear();
        if (peerList.size() == 0) {
            msgView.setText("没有搜索到P2P设备");
            return;
        }

        mScanResultAdapter.addAll(peerList);
    }

    @Override
    public void onConnection(WifiP2pInfo wifiP2pInfo) {
        currentWifiP2pinfo = wifiP2pInfo;
        msgView.setText("已连接到P2P设备");

        clientManager.stopDiscover();
    }

    @Override
    public void onDisconnection() {
        currentWifiP2pinfo = null;
        msgView.setText("已失去连接");
    }

    /*
     * Wi-FiP2P设备列表Adapter
     */
    private class ScanResultAdapter extends ArrayAdapter<WifiP2pDevice> {
        private int resource;
        private List<WifiP2pDevice> objects;
        public ScanResultAdapter(Context context, int resource, List<WifiP2pDevice> objects) {
            super(context, resource, objects);
            this.resource = resource;
            this.objects = objects;
        }

        @Override
        public View getView(int position, View convertView,ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(this.resource, null);

            TextView nameView = (TextView) view.findViewById(R.id.wifi_item_name);
            TextView signlView = (TextView) view.findViewById(R.id.wifi_item_signl);

            WifiP2pDevice scanResult = getItem(position);
            nameView.setText(scanResult.deviceName);
            signlView.setText(scanResult.deviceAddress);

            return view;
        }
    }

    @Override
    protected void onDestroy() {
        clientManager.destroy();
        clientManager = null;

        super.onDestroy();
    }
}
