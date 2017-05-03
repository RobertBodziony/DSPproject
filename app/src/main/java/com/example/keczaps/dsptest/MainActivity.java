package com.example.keczaps.dsptest;
import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener, DeviceListFragment.DeviceActionListener, View.OnClickListener {
    private Button rec_btn, play_btn,p2p_enable,p2p_discover;
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver = null;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private TextView rec_textView, pitch_textView;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 432;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXT_STR = 433;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXT_STR = 434;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_WIFI_STATE = 435;
    private static final int MY_PERMISSIONS_REQUEST_CHANGE_WIFI_STATE = 436;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 437;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_NET_STATE = 438;
    private static final int MY_PERMISSIONS_REQUEST_CHANGE_NET_STATE = 439;
    private static final int MY_PERMISSIONS_REQUEST_CHANGE_MULT_STATE = 440;
    private RecManager recManager;
    private PlayManager playManager;
    private List peers = new ArrayList();

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  Indicates a change in the Wi-Fi Peer-to-Peer status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        rec_btn = (Button) findViewById(R.id.rec_button);
        rec_btn.setOnClickListener(this);
        play_btn = (Button) findViewById(R.id.play_button);
        play_btn.setOnClickListener(this);
        p2p_enable = (Button) findViewById(R.id.atn_direct_enable);
        p2p_enable.setOnClickListener(this);
        p2p_discover = (Button) findViewById(R.id.atn_direct_discover);
        p2p_discover.setOnClickListener(this);
        rec_textView = (TextView) findViewById(R.id.rec_textView);
        pitch_textView = (TextView) findViewById(R.id.pitch_textView);
        pitch_textView.setText(getResources().getString(R.string.playing_off_text));
        checkAppPerm(Manifest.permission.RECORD_AUDIO, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        checkAppPerm(Manifest.permission.WRITE_EXTERNAL_STORAGE, MY_PERMISSIONS_REQUEST_WRITE_EXT_STR);
        checkAppPerm(Manifest.permission.READ_EXTERNAL_STORAGE, MY_PERMISSIONS_REQUEST_READ_EXT_STR);
        checkAppPerm(Manifest.permission.ACCESS_WIFI_STATE, MY_PERMISSIONS_REQUEST_ACCESS_WIFI_STATE);
        checkAppPerm(Manifest.permission.CHANGE_WIFI_STATE, MY_PERMISSIONS_REQUEST_CHANGE_WIFI_STATE);
        checkAppPerm(Manifest.permission.INTERNET, MY_PERMISSIONS_REQUEST_INTERNET);
        checkAppPerm(Manifest.permission.ACCESS_NETWORK_STATE, MY_PERMISSIONS_REQUEST_ACCESS_NET_STATE);
        checkAppPerm(Manifest.permission.CHANGE_NETWORK_STATE, MY_PERMISSIONS_REQUEST_CHANGE_NET_STATE);
        checkAppPerm(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE, MY_PERMISSIONS_REQUEST_CHANGE_MULT_STATE);
        recManager = new RecManager();
        playManager = new PlayManager(5);
        recManager.prepareDirectory("DSPtest");

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        /*mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e("Wi-Fi P2P","discoverPeers() #SUCCESS");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.e("Wi-Fi P2P : ","discoverPeers() #FAIL");
            }
        });*/
    }

    /*private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            // Out with the old, in with the new.
            peers.clear();
            peers.addAll(peerList.getDeviceList());

            Log.e("Wi-Fi P2P : ", "devices found");

            // If an AdapterView is backed by this data, notify it
            // of the change.  For instance, if you have a ListView of available
            // peers, trigger an update.
            //((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
            if (peers.size() == 0) {
                Log.e("Wi-Fi P2P : ", "No devices found");
                return;
            }
        }
    };

     register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(mReceiver, intentFilter);
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.rec_button:

                if (rec_textView.getText().equals(getResources().getString(R.string.recording_off_text))) {
                    rec_textView.setText(R.string.recording_on_text);
                    rec_textView.setTextColor(getResources().getColor(R.color.color_rec_on));
                    if(recManager == null){
                        recManager = new RecManager();
                        recManager.start();
                    } else {
                        recManager.start();
                    }
                    //recManager.startRecording();
                } else {
                    rec_textView.setText(getResources().getString(R.string.recording_off_text));
                    rec_textView.setTextColor(getResources().getColor(R.color.color_rec_off));
                    recManager.stopRecording();
                    recManager = null;
                }

                break;
            case R.id.play_button:
                if (pitch_textView.getText().equals(getResources().getString(R.string.playing_off_text))) {
                    pitch_textView.setText(R.string.playing_on_text);
                    pitch_textView.setTextColor(getResources().getColor(R.color.color_rec_on));
                    //SoundDetector soundDetector = new SoundDetector("SoundDetector1");
                    //soundDetector.run();
                    //showToast("soundDetector executed.");
                    playManager.startPlaying();
                } else {
                    pitch_textView.setText(getResources().getString(R.string.playing_off_text));
                    pitch_textView.setTextColor(getResources().getColor(R.color.color_rec_off));
                    playManager.stopPlaying();
                }
                break;
            case R.id.atn_direct_enable:
                if (mManager != null && mChannel != null) {
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e("WIFI p2p ", "channel or manager is null");
                }
                break;
            case R.id.atn_direct_discover:
                if (!isWifiP2pEnabled) {
                    showToast(getResources().getString(R.string.p2p_off_warning));
                    break;
                }
                final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                        .findFragmentById(R.id.frag_list);
                fragment.onInitiateDiscovery();
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        showToast("Discovery Initiated");
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        showToast("Discovery Failed : " + reasonCode);
                    }
                });
                break;
        }
    }

    //###################################################### PERM/CONVERT STUFF

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("RECORD_AUDIO PERMISSION GRANTED YAY!");
                } else {
                    showToast("RECORD_AUDIO PERMISSION DENIED UPS!");
                }
                break;
            case MY_PERMISSIONS_REQUEST_WRITE_EXT_STR:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("WRITE_EXT_STR PERMISSION GRANTED YAY!");
                } else {
                    showToast("WRITE_EXT_STR PERMISSION DENIED UPS!");
                }
                break;
            case MY_PERMISSIONS_REQUEST_READ_EXT_STR:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("READ_EXT_STR PERMISSION GRANTED YAY!");
                } else {
                    showToast("READ_EXT_STR PERMISSION DENIED UPS!");
                }
                break;
            case MY_PERMISSIONS_REQUEST_ACCESS_WIFI_STATE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("ACCESS_WIFI_STATE PERMISSION GRANTED YAY!");
                } else {
                    showToast("ACCESS_WIFI_STATE PERMISSION DENIED UPS!");
                }
                break;
            case MY_PERMISSIONS_REQUEST_CHANGE_WIFI_STATE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("CHANGE_WIFI_STATE PERMISSION GRANTED YAY!");
                } else {
                    showToast("CHANGE_WIFI_STATE PERMISSION DENIED UPS!");
                }
                break;
            case MY_PERMISSIONS_REQUEST_INTERNET:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("INTERNET PERMISSION GRANTED YAY!");
                } else {
                    showToast("INTERNET PERMISSION DENIED UPS!");
                }
                break;
            case MY_PERMISSIONS_REQUEST_ACCESS_NET_STATE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("ACCESS_NET_STATE PERMISSION GRANTED YAY!");
                } else {
                    showToast("ACCESS_NET_STATE PERMISSION DENIED UPS!");
                }
                break;
            case MY_PERMISSIONS_REQUEST_CHANGE_NET_STATE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("CHANGE_NET_STATE PERMISSION GRANTED YAY!");
                } else {
                    showToast("CHANGE_NET_STATE PERMISSION DENIED UPS!");
                }
                break;
            case MY_PERMISSIONS_REQUEST_CHANGE_MULT_STATE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("CHANGE_NET_STATE PERMISSION GRANTED YAY!");
                } else {
                    showToast("CHANGE_NET_STATE PERMISSION DENIED UPS!");
                }
                break;


        }
    }

    public void checkAppPerm(String perm, int permID) {
        if (ContextCompat.checkSelfPermission(this,
                perm)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    perm)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{perm},
                        permID);
            }
        }
    }

    public void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (mManager != null && !retryChannel) {
            showToast("Channel lost. Trying again");
            resetData();
            retryChannel = true;
            mManager.initialize(this, getMainLooper(), this);
        } else {
            showToast("Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.");
        }
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);
    }

    @Override
    public void cancelDisconnect() {

        if (mManager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {
                mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        showToast("Aborting connection");
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        showToast("Connect abort request failed. Reason Code: ");
                    }
                });
            }
        }
    }

    @Override
    public void connect(WifiP2pConfig config) {
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }
            @Override
            public void onFailure(int reason) {
                showToast("Connect failed. Retry.");
            }
        });
    }

    @Override
    public void disconnect() {

        final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.e("WIFI p2p ", "Disconnect failed. Reason :" + reasonCode);
            }

            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
            }
        });

    }


}