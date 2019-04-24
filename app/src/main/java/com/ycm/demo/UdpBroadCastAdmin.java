package com.ycm.demo;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class UdpBroadCastAdmin {

    private static final String LCAT = "UdpBroadCastAdmin";

    public static interface ActionListener {

        //接收到广播数据
        void onReceive(String ipAddress, String result);
    }

    public UdpBroadCastAdmin() {

    }

    public void send(final String host, final int port, final String dataStr) {
        if (host == null || "".equals(host) || port <= 0 || dataStr == null || "".equals(dataStr)) return;

        new Thread() {
            @Override
            public void run() {
                try {
                    MulticastSocket sender = new MulticastSocket();
                    byte[] data = new byte[1024];
                    data = dataStr.getBytes();

                    InetAddress groupAddress = InetAddress.getByName(host);
                    System.out.println(groupAddress);
                    DatagramPacket dp = new DatagramPacket(data,data.length, groupAddress, port);
                    sender.send(dp);

                    sender.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private ReceiveAsyncTask mReceiveAsyncTask;
    public void startReceive(final String host, final int port, UdpBroadCastAdmin.ActionListener actionListener) {
        if (mReceiveAsyncTask == null) {
            mReceiveAsyncTask = new ReceiveAsyncTask(host, port, actionListener);
            mReceiveAsyncTask.execute();
        }
    }
    public void stopReceive() {
        if (mReceiveAsyncTask != null && mReceiveAsyncTask.isReceiving()) {
            mReceiveAsyncTask.close();
        }
        mReceiveAsyncTask = null;
    }

    private static class ReceiveAsyncTask extends AsyncTask<Void, Void, Void> {
        private String host;
        private int port;
        private UdpBroadCastAdmin.ActionListener actionListener;

        private MulticastSocket ms;
        private boolean receiving = false;

        private Handler mHandler = new Handler();

        public ReceiveAsyncTask(String host, int port, UdpBroadCastAdmin.ActionListener actionListener) {
            this.host = host;
            this.port = port;
            this.actionListener = actionListener;
        }

        @Override
        protected Void doInBackground(Void...voids) {
            receiving = true;

            Log.d(LCAT, "==========UDP广播接收开启==========");
            try {
                if (ms == null) {
                    ms = new MulticastSocket(port);
                    InetAddress groupAddress = InetAddress.getByName(host);
                    ms.joinGroup(groupAddress);
                }

                byte[] data = new byte[1024];
                while (!ms.isClosed()) {

                    DatagramPacket dp = new DatagramPacket(data, data.length);
                    ms.receive(dp);

                    if (dp.getAddress() != null) {
                        final String quest_ip = dp.getAddress().getHostAddress();
                        final String codeString = new String(data, 0, dp.getLength());

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                actionListener.onReceive(quest_ip, codeString);
                            }
                        });

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d(LCAT, "==========UDP广播接收关闭==========");

            receiving = false;
            return null;
        }

        public boolean isReceiving() {
            return this.receiving;
        }

        public void close() {
            if (ms != null) {
                try {
                    ms.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.d(LCAT, "=========onPostExecute=========");
        }

        @Override
        protected void onCancelled(Void aVoid) {
            Log.d(LCAT, "=========onCancelled=========");
        }
    }
}
