package com.ycm.webserver;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ycm.demo.R;


public class WebServerActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView msgView;
    private Button startBtn;
    private Button stopBtn;

    private WebServerManagerReceiver mServerManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_server);

        msgView = findViewById(R.id.webserver_msg);
        startBtn = findViewById(R.id.webserver_start);
        startBtn.setOnClickListener(this);
        stopBtn = findViewById(R.id.webserver_stop);
        stopBtn.setVisibility(View.GONE);
        stopBtn.setOnClickListener(this);

        mServerManager = new WebServerManagerReceiver(this);
        mServerManager.register();

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.webserver_start) {
            msgView.setText("正在开启服务...");
            mServerManager.startServer();
            return;
        }

        if (v.getId() == R.id.webserver_stop) {
            msgView.setText("正在关闭服务...");
            mServerManager.stopServer();
            return;
        }
    }

    /**
     * Start notify.
     */
    public void onServerStart(String ip) {
        startBtn.setVisibility(View.GONE);
        stopBtn.setVisibility(View.VISIBLE);

        msgView.setText("服务已经开启，URL：http://" + ip + ":8080");
    }

    /**
     * Error notify.
     */
    public void onServerError(String message) {
        startBtn.setVisibility(View.VISIBLE);
        stopBtn.setVisibility(View.GONE);

        msgView.setText("服务开始出错! \n" + message);
    }

    /**
     * Stop notify.
     */
    public void onServerStop() {
        startBtn.setVisibility(View.VISIBLE);
        stopBtn.setVisibility(View.GONE);

        msgView.setText("服务已经停止");
    }

    @Override
    protected void onDestroy() {
        mServerManager.unRegister();

        super.onDestroy();
    }
}
