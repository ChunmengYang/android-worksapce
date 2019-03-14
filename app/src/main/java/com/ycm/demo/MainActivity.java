package com.ycm.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.ycm.zxinglibrary.android.Intents;
import com.ycm.zxinglibrary.common.Constant;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    Intent qrIntent = new Intent(Intents.Scan.ACTION);
                    qrIntent.setPackage("com.ycm.demo");
                    startActivityForResult(qrIntent, 1);

                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    Intent bleIntent = new Intent(MainActivity.this, BeaconActivity.class);
                    startActivity(bleIntent);

                    mTextMessage.setText(R.string.title_notifications);
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

}
