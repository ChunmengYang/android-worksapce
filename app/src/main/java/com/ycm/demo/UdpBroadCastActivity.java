package com.ycm.demo;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

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

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp_broad_cast);

        msgView = findViewById(R.id.udp_broad_cast_msg);
        postBtn = findViewById(R.id.udp_broad_cast_post);
        postBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            MulticastSocket sender = new MulticastSocket();
                            byte[] data = new byte[1024];
                            data = "我是小米智能音箱，请求管理！".getBytes();

                            InetAddress groupAddress = InetAddress.getByName("224.0.0.1");
                            System.out.println(groupAddress);
                            DatagramPacket dp = new DatagramPacket(data,data.length, groupAddress, 1989);
                            sender.send(dp);

                            sender.close();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });

        receiveBtn = findViewById(R.id.udp_broad_cast_receive);
        receiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isReceiving) {
                    receiveThread.start();
                    msgView.setText("正在接收UDP广播");
                }
            }
        });


    }

    private Boolean isReceiving = false;
    private Thread receiveThread = new Thread() {
        public MulticastSocket ms;

        @Override
        public void run() {
            isReceiving = true;

            Log.d(LCAT, "==========UDP广播接收开启==========");
            try {
                if (ms == null) {
                    ms = new MulticastSocket(1989);
                    InetAddress groupAddress = InetAddress.getByName("224.0.0.1");
                    ms.joinGroup(groupAddress);
                }

                byte[] data = new byte[1024];
                while (!ms.isClosed()) {

                    DatagramPacket dp = new DatagramPacket(data, data.length);
                    ms.receive(dp);

                    if (dp.getAddress() != null) {
                        final String quest_ip = dp.getAddress().toString();
                        final String codeString = new String(data, 0, dp.getLength());

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                msgView.setText("收到来自："+ quest_ip + "的广播 \n" + "广播内容："+ codeString);
                            }
                        });

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d(LCAT, "==========UDP广播接收关闭==========");

            isReceiving = false;
        }
    };
}
