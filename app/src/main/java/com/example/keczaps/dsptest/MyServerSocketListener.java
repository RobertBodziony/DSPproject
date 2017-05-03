package com.example.keczaps.dsptest;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyServerSocketListener {
    private ScheduledExecutorService serverScheduleTaskExecutor = null;
    private Context context;
    private TextView statusText;
    private String inMsg = null,host;
    private int serverPort;
    private ServerSocket serverSocket = null;
    private Handler handler = null;
    private Socket client;
    private PlayManager playManager;


    MyServerSocketListener(Context context, View statusText, Handler handler, PlayManager playManager, int serverPort) {
        this.context = context;
        this.statusText = (TextView) statusText;
        this.handler = handler;
        this.playManager = playManager;
        this.serverPort = serverPort;
        Log.i("Server socket listener ", " Server socket listener - CREATED.");
    }

    void startServerSocketListener() {

        serverScheduleTaskExecutor = Executors.newScheduledThreadPool(6);
        serverScheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    if(serverSocket == null){
                        serverSocket = new ServerSocket(serverPort);
                        Log.i(getClass().getSimpleName(), "Running on port: " + serverSocket.getLocalPort());
                    }
                    client = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    inMsg = in.readLine() + System.getProperty("line.separator");

                } catch (IOException e) {
                    Log.e("Server Socket error ", e.getMessage());
                }
                handler.post(new Runnable(){
                    public void run(){
                        if(!statusText.getText().equals(inMsg) && inMsg != null) {
                            statusText.setText(inMsg);
                            if(!playManager.isPlaying()) {
                                if(Integer.parseInt(inMsg.substring(2,3)) == 0){
                                    playManager.startPlaying();
                                }
                            } else {
                                if(Integer.parseInt(inMsg.substring(2,3)) == 1){
                                    playManager.stopPlaying();
                                }
                            }
                        }
                    }
                });
            }
        }, 0, 5, TimeUnit.MILLISECONDS);
    }


    void sendServerSocketMessage() {
        if(serverSocket != null) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        DataOutputStream mDataOutputStream = new DataOutputStream(client.getOutputStream());
                        mDataOutputStream.writeUTF("MESSAGE");
                        mDataOutputStream.flush();
                        Log.e("Server Socket msg ", "Message sent?");

                    } catch (IOException e) {
                        Log.e("Server Socket error ", e.getMessage());
                    }
                }
            });
            thread.start();
        } else {
            Log.e("Server Socket closed ", " closed.");
        }

    }

    void stopServerSocketListener() {
        if(serverScheduleTaskExecutor != null){
            serverScheduleTaskExecutor.shutdownNow();
        }
        if(serverSocket != null){
            try {
                serverSocket.close();
                serverSocket = null;
                Log.i("Socket close ", " Success.");
            } catch (IOException e) {
                Log.e("Socket closeing error ", e.getMessage());
            }
        }
        if(playManager.isPlaying()) {
            playManager.stopPlaying();
        }
    }
}