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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mhandler = new Handler();
        mPlayerManager = new PlayManager(7);
        mMyServerSocketListener = new MyServerSocketListener(getActivity(),mContentView.findViewById(R.id.status_text),mhandler,mPlayerManager,SERVER_PORT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
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
                            //mMyServerSocketListener.sendServerSocketMessage();
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
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            mMyServerSocketListener.startServerSocketListener();
            Log.i("Server socket listener "," Starterd. ");
            amIaGroupOwner = true;
        } else if (info.groupFormed) {
            messageCount = 0;
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
            //ClientMulticastAsyncTask mClientMulticastAsyncTask = new ClientMulticastAsyncTask(statusV);
            //mClientMulticastAsyncTask.execute();
            //Log.i("Client multicast "," ClientMulticastAsyncTask sent to queue.");
            amIaGroupOwner = false;
        }
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    public void showDetails(WifiP2pDevice device) {
        this.mWifiP2pDevice = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.deviceName);
        Log.i("Showing details ", " Device name : "+device.deviceName+" | Device address : " + device.deviceAddress);
    }


    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
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


    public class ClientMulticastAsyncTask extends AsyncTask<Void, TextView, String> {

        private TextView textView1;
        private byte[] data;

        public ClientMulticastAsyncTask(TextView textView1) {
            this.textView1 = textView1;
            Log.i("Client multicast "," ClientMulticastAsyncTask created.");

        }

        @Override
        protected String doInBackground(Void... params) {
            Log.i("Client multicast "," Executing task.");
            //Acquire the MulticastLock
            WifiManager wifi = (WifiManager)  getActivity().getSystemService(Context.WIFI_SERVICE);
            WifiManager.MulticastLock multicastLock = wifi.createMulticastLock("multicastLock");
            multicastLock.setReferenceCounted(true);
            multicastLock.acquire();

            //Join a Multicast Group
            InetAddress address=null;
            MulticastSocket clientSocket=null;
            int multicastServerPort = 1212;
            try {
                //address = InetAddress.getByName("224.0.0.1");
                clientSocket = new MulticastSocket(multicastServerPort);
                //clientSocket.joinGroup(address);
                //clientSocket.setSoTimeout(10000);
                clientSocket.setBroadcast(true);
                InetSocketAddress socketAddress = new InetSocketAddress("224.0.0.1",1212);
                NetworkInterface wifiDirectNetworkInterface = getWifiDirectNetworkInterface();
                clientSocket.joinGroup(socketAddress, wifiDirectNetworkInterface);
                //clientSocket.setNetworkInterface(wifiDirectNetworkInterface);

                //clientSocket.joinGroup(new InetSocketAddress(address, multicastServerPort));
                Log.i("Client multicast ", " Socket Group joined on address : " +address+" | Multicast socket created on port : " + multicastServerPort);
            } catch (UnknownHostException e) {
                Log.e("Client multicast ", " Socket error UnknownHostException : "+e.getMessage());
            } catch (IOException e) {
                Log.e("Client multicast ", " Socket error IOException : "+e.getMessage());
            }

            DatagramPacket packet=null;
            byte[] buf = new byte[1];
            packet = new DatagramPacket(buf, buf.length);
            //Receive packet and get the Data
            try {
                clientSocket.receive(packet);
                data = packet.getData();
                Log.i("Server broadcast : ", data[0] + "");
            } catch (Exception e) {
                Log.e("Client multicast ", "Socket receive error : "+e.getMessage());
            }
            multicastLock.release();

            try {
                clientSocket.leaveGroup(address);
            } catch (IOException e) {
                Log.e("Client multicast ", "Socket leave group error : "+e.getMessage());
            }
            clientSocket.close();
            return data[0]+"";
        }

        @Override
        protected void onPostExecute(String result) {
            if(textView1 != null){
                textView1.setText(result);
            }
        }

    }

    public class ServerMulticastAsyncTask extends AsyncTask<Void, View, String> {

        public ServerMulticastAsyncTask() {
            Log.i("Server multicast "," MulticastAsyncTask created.");
        }

        @Override
        protected String doInBackground(Void... params) {

            int multicastServerPort =1212;
            DatagramSocket socket=null;
            try {
                socket = new DatagramSocket(multicastServerPort);
                Log.i("Server multicast ", " Socket Created.");
            } catch (SocketException e) {
                Log.e("Server multicast ", " Socket error : "+e.getMessage());
            }
            InetAddress group = null;
            try {
                group = InetAddress.getByName("224.0.0.1");
            } catch (UnknownHostException e) {
                socket.close();
                Log.e("Server multicast ", " Socket CLOSED | " + e.getMessage());
            }

            byte[] buf = new byte[1];
            buf[0] = 0;
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, multicastServerPort);
            try {
                socket.send(packet);
                Log.i("Server multicast ", " Sending Packet. Data : " + buf[0]);
            } catch (IOException e) {
                socket.close();
                Log.e("Server multicast ", " Socket CLOSED | Error : " + e.getMessage());
            }

            socket.close();
            Log.i("Server multicast ", " Socket CLOSED | Success?");
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            //do whatever ...
        }
    }

    public static NetworkInterface getWifiDirectNetworkInterface() {

        List<NetworkInterface> foundInterfaces = new ArrayList<>();

        Enumeration<NetworkInterface> interfaceEnumeration = null;
        try {
            interfaceEnumeration = NetworkInterface
                    .getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while (interfaceEnumeration != null && interfaceEnumeration.hasMoreElements
                ()) {

            NetworkInterface anInterface = interfaceEnumeration.nextElement();

            if (anInterface.getName().contains("p2p")) {

                foundInterfaces.add(anInterface);
            }

        }

        for (int i = 0; i < foundInterfaces.size(); i++) {

            NetworkInterface networkInterface = foundInterfaces.get(i);

            Enumeration<InetAddress> inetAdresses = networkInterface.getInetAddresses();

            for (InetAddress inetAddress : Collections.list(inetAdresses)) {

                if (inetAddress instanceof Inet4Address) {

                    return networkInterface;
                }

            }

        }

        return null;

    }
}
