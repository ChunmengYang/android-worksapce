package com.ycm.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 *
 * 局域网内才能发送UDP广播
 *
 * */
public class UdpBroadCastActivity extends AppCompatActivity {
    private static final String LCAT = "UdpBroadCastActivity";
    private TextView msgView;
    private Button postBtn;
    private Button receiveBtn;

    private UdpBroadCastAdmin mUdpBroadCastAdmin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp_broad_cast);

        mUdpBroadCastAdmin = new UdpBroadCastAdmin();

        msgView = findViewById(R.id.udp_broad_cast_msg);
        postBtn = findViewById(R.id.udp_broad_cast_post);
        postBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUdpBroadCastAdmin.send("224.0.0.1", 1989, "我是小米智能音箱!");
            }
        });

        receiveBtn = findViewById(R.id.udp_broad_cast_receive);
        receiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUdpBroadCastAdmin.startReceive("224.0.0.1", 1989, new UdpBroadCastAdmin.ActionListener() {
                    @Override
                    public void onReceive(String ipAddress, String result) {
                        msgView.setText("收到来自："+ ipAddress + "的广播 \n" + "广播内容："+ result);
                    }
                });
            }
        });


    }

    @Override
    protected void onDestroy() {
        mUdpBroadCastAdmin.stopReceive();
        mUdpBroadCastAdmin = null;

        super.onDestroy();
    }
}
