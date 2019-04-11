package com.ycm.webserver;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;


public class WebServerService extends Service {
    private Server mServer;

    @Override
    public void onCreate() {
        mServer = AndServer.serverBuilder()
                .inetAddress(NetUtils.getLocalIPAddress())
                .port(8080)
                .timeout(30, TimeUnit.SECONDS)
                .listener(new Server.ServerListener() {
                    @Override
                    public void onStarted() {
                        String hostAddress = mServer.getInetAddress().getHostAddress();
                        WebServerManagerReceiver.onServerStart(WebServerService.this, hostAddress);
                    }

                    @Override
                    public void onStopped() {
                        WebServerManagerReceiver.onServerStop(WebServerService.this);
                    }

                    @Override
                    public void onException(Exception e) {
                        WebServerManagerReceiver.onServerError(WebServerService.this, e.getMessage());
                    }
                }).build();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startServer();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopServer();
        super.onDestroy();
    }

    /**
     * Start server.
     */
    private void startServer() {
        if (mServer.isRunning()) {
            String hostAddress = mServer.getInetAddress().getHostAddress();
            WebServerManagerReceiver.onServerStart(WebServerService.this, hostAddress);
        } else {
            mServer.startup();
        }
    }

    /**
     * Stop server.
     */
    private void stopServer() {
        mServer.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
