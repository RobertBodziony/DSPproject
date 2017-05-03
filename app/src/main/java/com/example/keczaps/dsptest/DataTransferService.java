package com.example.keczaps.dsptest;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataTransferService extends IntentService {
    private static final int SOCKET_TIMEOUT = 500;
    public static final String ACTION_SEND_DATA = "com.example.keczaps.dsptest.SEND_DATA";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
    public static final String EXTRAS_GROUP_MSG_COUNT = "msg_count";

    public DataTransferService(String name) {
        super(name);
    }

    public DataTransferService() {
        super("DataTransferService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_DATA)) {
            Log.e("Service ", "I am in the service - ACTION_SEND_DATA.");
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            final Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            int count = intent.getExtras().getInt(EXTRAS_GROUP_MSG_COUNT);
            try {
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Log.e("Client socket ", " in the service - ACTION_SEND_DATA - " + socket.isConnected());

                DataOutputStream mDataOutputStream = new DataOutputStream(socket.getOutputStream());
                mDataOutputStream.writeUTF(Integer.toString(count));
                mDataOutputStream.flush();
                Log.e("ClientSoc - MSG SENT! "," in the service - ACTION_SEND_DATA - " + Integer.toString(count));

            } catch (IOException e) {
                Log.e("Client socket ", e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}