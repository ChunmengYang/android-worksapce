package com.ycm.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ycm.demo.ui.login.LoginActivity;
import com.ycm.webserver.WebServerActivity;
import com.ycm.zxinglibrary.android.Intents;
import com.ycm.zxinglibrary.common.Constant;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String LCAT = "MainActivity";

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_pie_chart:
                    mTextMessage.setText(R.string.main_pie_chart);
                    Intent pieChartIntent = new Intent(MainActivity.this, PieChartActivity.class);
                    startActivity(pieChartIntent);
                    return true;
                case R.id.navigation_qrcode:
                    Intent qrIntent = new Intent(Intents.Scan.ACTION);
                    qrIntent.setPackage("com.ycm.demo");
                    startActivityForResult(qrIntent, 1);

                    mTextMessage.setText(R.string.main_scan_qrcode);
                    return true;
                case R.id.navigation_beacons:
                    Intent bleIntent = new Intent(MainActivity.this, BeaconActivity.class);
                    startActivity(bleIntent);

                    mTextMessage.setText(R.string.main_scan_beacons);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK && resultCode != Activity.RESULT_CANCELED) {
            mTextMessage.setText("Scan QRCode Failed:" + resultCode);
            return;
        } else if (resultCode == Activity.RESULT_CANCELED) {
            mTextMessage.setText("Scan QRCode Canceled:" + resultCode);
            return;
        }

        try {
            mTextMessage.setText("Scan QRCode Successful:" + resultCode + "\n" + data.getStringExtra(Constant.CODED_CONTENT));
        } catch (Exception e) {
            e.printStackTrace();
            mTextMessage.setText("Scan QRCode Failed:" + resultCode);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        getMenuInflater().inflate(R.menu.main_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.main_options_list_popup:
                Intent signInIntent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(signInIntent);

//                final ListPopupWindow mListPop = new ListPopupWindow(this);
//                List<MyMenu> lists = new ArrayList<MyMenu>();
//                lists.add(new MyMenu(R.drawable.ic_scan_qrcode_24dp, getResources().getString(R.string.main_scan_qrcode)));
//                lists.add(new MyMenu(R.drawable.ic_scan_beacons_24dp, getResources().getString(R.string.main_scan_beacons)));
//
//                mListPop.setAdapter(new MyArrayAdapter(this, R.layout.my_menu, lists));
//                mListPop.setWidth(ConstraintLayout.LayoutParams.WRAP_CONTENT);
//                mListPop.setHeight(ConstraintLayout.LayoutParams.WRAP_CONTENT);
//                mListPop.setAnchorView(mTextMessage);
//                mListPop.setModal(true);
//
//                mListPop.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                    @Override
//                    public void onItemClick(AdapterView<?> parent, View view,
//                                            int position, long id) {
//                        mTextMessage.setText(String.valueOf(position));
//                        mListPop.dismiss();
//                    }
//                });
//                mListPop.show();
                break;
            case R.id.main_options_scrolling:
                Intent scrollingIntent = new Intent(MainActivity.this, ScrollingActivity.class);
                startActivity(scrollingIntent);
                break;
            case R.id.main_options_image_upload_and_download:
                Intent imageIntent = new Intent(MainActivity.this, ImageUploadAndDownLoadActivity.class);
                startActivity(imageIntent);
                break;
            case R.id.main_options_bluetooth:
                Intent bluetoothIntent = new Intent(MainActivity.this, BluetoothActivity.class);
                startActivity(bluetoothIntent);
                break;
            case R.id.main_options_ble:
                Intent bleIntent = new Intent(MainActivity.this, BLEActivity.class);
                startActivity(bleIntent);
                break;
            case R.id.main_webserver:
                Intent serverIntent = new Intent(MainActivity.this, WebServerActivity.class);
                startActivity(serverIntent);
                break;
            case R.id.main_wifi:
                Intent wifiIntent = new Intent(MainActivity.this, WifiActivity.class);
                startActivity(wifiIntent);
                break;
            case R.id.main_wifi_direct_server:
                Intent wifiServerIntent = new Intent(MainActivity.this, WifiDirectServerActivity.class);
                startActivity(wifiServerIntent);
                break;
            case R.id.main_wifi_direct_client:
                Intent wifiClientIntent = new Intent(MainActivity.this, WifiDirectClientActivity.class);
                startActivity(wifiClientIntent);
                break;
            case R.id.main_udp_broad_cast:
                Intent udpIntent = new Intent(MainActivity.this, UdpBroadCastActivity.class);
                startActivity(udpIntent);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private class MyMenu {
        public int icon;
        public String title;

        public MyMenu(int icon, String title) {
            this.icon = icon;
            this.title = title;
        }
    }
    private class MyArrayAdapter extends ArrayAdapter<MyMenu> {
        private int resource;
        private List<MyMenu> objects;

        public MyArrayAdapter(Context context, int resource, List<MyMenu> objects) {
            super(context, resource, objects);

            this.resource = resource;
            this.objects = objects;
        }

        @Override
        public View getView(int position, View convertView,ViewGroup parent) {
            MyMenu item = getItem(position);
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(resource, null);
            ImageView iconView = (ImageView) view.findViewById(R.id.my_menu_icon);
            TextView titleView = (TextView) view.findViewById(R.id.my_menu_title);
            iconView.setImageResource(item.icon);
            titleView.setText(item.title);

            return view;
        }
    }
}
