package com.example.keczaps.dsptest;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.keczaps.dsptest.DeviceListFragment.DeviceActionListener;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {
    
    private View mContentView = null;
    private WifiP2pDevice mWifiP2pDevice;
    private WifiP2pInfo mWifiP2pInfo;
    private ProgressDialog mProgressDialog = null;
    private MyServerSocketListener mMyServerSocketListener;
    private Handler mhandler;
    private PlayManager mPlayerManager = null;
    private boolean amIaGroupOwner = false;
    private int messageCount = 0;
    private int SERVER_PORT = 9111;
    public static final String EXTRA_MESSAGE_DEV = "com.example.keczaps.dsptest.EXTRA_MESSAGE_DEV";
    public static final String EXTRA_MESSAGE_SMPL_RATE = "com.example.keczaps.dsptest.EXTRA_MESSAGE_SMPL_RATE";
    public static final String EXTRA_MESSAGE_SIGNAL_TIME = "com.example.keczaps.dsptest.EXTRA_MESSAGE_SIGNAL_TIME";
    public static final String EXTRA_MESSAGE_SIGNAL_SEL = "com.example.keczaps.dsptest.EXTRA_MESSAGE_SIGNAL_SEL";
    public static final String EXTRA_MESSAGE_X = "com.example.keczaps.dsptest.EXTRA_MESSAGE_X";
    public static final String EXTRA_MESSAGE_Y = "com.example.keczaps.dsptest.EXTRA_MESSAGE_Y";
    public static final String EXTRA_MESSAGE_Z = "com.example.keczaps.dsptest.EXTRA_MESSAGE_Z";
    public static final String EXTRA_MESSAGE_TIME_BETWEEN = "com.example.keczaps.dsptest.EXTRA_MESSAGE_TIME_BETWEEN";


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mhandler = new Handler();
        int time_between_ms = (int) getActivity().getIntent().getExtras().get(EXTRA_MESSAGE_TIME_BETWEEN);
        int sample_r = (int) getActivity().getIntent().getExtras().get(EXTRA_MESSAGE_SMPL_RATE);
        String f_name = "";

        if(getActivity().getIntent().getExtras().get(EXTRA_MESSAGE_DEV).toString().equals("T")){
            f_name = "A" +
                    getActivity().getIntent().getExtras().get(EXTRA_MESSAGE_SMPL_RATE).toString() + "25ms" +
                    getActivity().getIntent().getExtras().get(EXTRA_MESSAGE_SIGNAL_SEL).toString() +".wav";
        } else {
            f_name = getActivity().getIntent().getExtras().get(EXTRA_MESSAGE_DEV).toString() +
                    getActivity().getIntent().getExtras().get(EXTRA_MESSAGE_SMPL_RATE).toString() + "25ms" +
                    getActivity().getIntent().getExtras().get(EXTRA_MESSAGE_SIGNAL_SEL).toString() +".wav";
        }

        Log.e("DevDetFr","Fname = "+f_name);
        Log.e("DevDetFr","S_rate = "+sample_r);
        Log.e("DevDetFr","time_between = "+time_between_ms);

        mPlayerManager = new PlayManager(7,f_name,sample_r,time_between_ms);
        mMyServerSocketListener = new MyServerSocketListener(mContentView.findViewById(R.id.status_text),mhandler,mPlayerManager,SERVER_PORT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                if(mWifiP2pDevice.deviceName.equals("AndroidASUS")){
                    config.groupOwnerIntent = 1;
                    Log.e("GROUP OWNER "," AndroidAsus GOI = 1");
                } else {
                    config.groupOwnerIntent = 15;
                    Log.e("GROUP OWNER ","NO AndroidAsus GOI = 15");
                }
                config.deviceAddress = mWifiP2pDevice.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                mProgressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + mWifiP2pDevice.deviceAddress, true, true
                );
                ((DeviceActionListener) getActivity()).connect(config);
            }
        });
        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mMyServerSocketListener != null){
                            mMyServerSocketListener.stopServerSocketListener();
                        }
                        if(mhandler != null){
                            mhandler = null;
                        }
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });
        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!amIaGroupOwner) {
                            if(messageCount > 1) messageCount = 0;
                            TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
                            statusText.setText("Sending message : " + messageCount);
                            Intent serviceIntent = new Intent(getActivity(), DataTransferService.class);
                            serviceIntent.setAction(DataTransferService.ACTION_SEND_DATA);
                            serviceIntent.putExtra(DataTransferService.EXTRAS_GROUP_OWNER_ADDRESS, mWifiP2pInfo.groupOwnerAddress.getHostAddress());
                            serviceIntent.putExtra(DataTransferService.EXTRAS_GROUP_OWNER_PORT, SERVER_PORT);
                            serviceIntent.putExtra(DataTransferService.EXTRAS_GROUP_MSG_COUNT, messageCount);
                            getActivity().startService(serviceIntent);
                            Log.i("Service started ", "ACTION_SEND_DATA | GROUP_OWNER_PORT : " + SERVER_PORT);
                            messageCount++;
                        } else {
                            TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
                            Log.i("Server Send ", " send button pressed.");
                            statusText.setText("");
                        }
                    }
                });
        return mContentView;
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        this.mWifiP2pInfo = info;
        this.getView().setVisibility(View.VISIBLE);

        String groupOwnerText = getResources().getString(R.string.group_owner_text) + ((info.isGroupOwner) ? getResources().getString(R.string.yes) : getResources().getString(R.string.no));

        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(groupOwnerText);

        if (info.groupFormed && info.isGroupOwner) {
            Log.i("Server group formed "," I am a group owner - my address : " + info.groupOwnerAddress);
            mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.VISIBLE);
            mMyServerSocketListener.startServerSocketListener();
            Log.i("Server socket listener "," Starterd. ");
            amIaGroupOwner = true;
        } else if (info.groupFormed) {
            messageCount = 0;
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
            amIaGroupOwner = false;
        }
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    public void showDetails(WifiP2pDevice device) {
        this.mWifiP2pDevice = device;
        this.getView().setVisibility(View.VISIBLE);
        //TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        //view.setText(device.deviceAddress);
        //view = (TextView) mContentView.findViewById(R.id.device_info);
        //view.setText(device.deviceName);
        Log.i("Showing details ", " Device name : "+device.deviceName+" | Device address : " + device.deviceAddress);
    }


    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
        if(mMyServerSocketListener != null && mhandler != null){
            mMyServerSocketListener.stopServerSocketListener();
            mhandler = null;
            Log.i("Reseting views ", " Server socket listener - CLOSED | Handler - NULL");
        }
        Log.i("Reseting views ", " Success.");
    }
}
