package com.example.keczaps.dsptest;
import android.Manifest;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener, DeviceListFragment.DeviceActionListener, View.OnClickListener {
    // CONSTANTS
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 432;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXT_STR = 433;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXT_STR = 434;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_WIFI_STATE = 435;
    private static final int MY_PERMISSIONS_REQUEST_CHANGE_WIFI_STATE = 436;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 437;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_NET_STATE = 438;
    private static final int MY_PERMISSIONS_REQUEST_CHANGE_NET_STATE = 439;
    private static final int MY_PERMISSIONS_REQUEST_CHANGE_MULT_STATE = 440;
    public static final String EXTRA_MESSAGE_DEV = "com.example.keczaps.dsptest.EXTRA_MESSAGE_DEV";
    public static final String EXTRA_MESSAGE_SMPL_RATE = "com.example.keczaps.dsptest.EXTRA_MESSAGE_SMPL_RATE";
    public static final String EXTRA_MESSAGE_SIGNAL_TIME = "com.example.keczaps.dsptest.EXTRA_MESSAGE_SIGNAL_TIME";
    public static final String EXTRA_MESSAGE_SIGNAL_SEL = "com.example.keczaps.dsptest.EXTRA_MESSAGE_SIGNAL_SEL";
    public static final String EXTRA_MESSAGE_X = "com.example.keczaps.dsptest.EXTRA_MESSAGE_X";
    public static final String EXTRA_MESSAGE_Y = "com.example.keczaps.dsptest.EXTRA_MESSAGE_Y";
    public static final String EXTRA_MESSAGE_Z = "com.example.keczaps.dsptest.EXTRA_MESSAGE_Z";
    public static final String EXTRA_MESSAGE_TIME_BETWEEN = "com.example.keczaps.dsptest.EXTRA_MESSAGE_TIME_BETWEEN";

    private Button rec_btn, play_btn,p2p_enable,p2p_discover;
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver = null;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private TextView last_detected_t,detected_t_difference;
    private RecManager recManager;
    private PlayManager playManager;
    private List peers = new ArrayList();
    private SoundDetector soundDetector;
    private String play_file_name,rec_file_name;
    private int s_rate;


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
        rec_btn.setTextColor(getResources().getColor(R.color.color_rec_off));
        play_btn = (Button) findViewById(R.id.play_button);
        play_btn.setOnClickListener(this);
        play_btn.setTextColor(getResources().getColor(R.color.color_rec_off));
        p2p_enable = (Button) findViewById(R.id.atn_direct_enable);
        p2p_enable.setOnClickListener(this);
        p2p_enable.setTextColor(getResources().getColor(R.color.color_rec_off));
        p2p_discover = (Button) findViewById(R.id.atn_direct_discover);
        p2p_discover.setOnClickListener(this);
        p2p_discover.setTextColor(getResources().getColor(R.color.color_rec_off));
        last_detected_t = (TextView) findViewById(R.id.last_detected_time);
        detected_t_difference = (TextView) findViewById(R.id.detected_time_difference);
        checkAppPerm(Manifest.permission.RECORD_AUDIO, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        checkAppPerm(Manifest.permission.WRITE_EXTERNAL_STORAGE, MY_PERMISSIONS_REQUEST_WRITE_EXT_STR);
        checkAppPerm(Manifest.permission.READ_EXTERNAL_STORAGE, MY_PERMISSIONS_REQUEST_READ_EXT_STR);
        checkAppPerm(Manifest.permission.ACCESS_WIFI_STATE, MY_PERMISSIONS_REQUEST_ACCESS_WIFI_STATE);
        checkAppPerm(Manifest.permission.CHANGE_WIFI_STATE, MY_PERMISSIONS_REQUEST_CHANGE_WIFI_STATE);
        checkAppPerm(Manifest.permission.INTERNET, MY_PERMISSIONS_REQUEST_INTERNET);
        checkAppPerm(Manifest.permission.ACCESS_NETWORK_STATE, MY_PERMISSIONS_REQUEST_ACCESS_NET_STATE);
        checkAppPerm(Manifest.permission.CHANGE_NETWORK_STATE, MY_PERMISSIONS_REQUEST_CHANGE_NET_STATE);
        checkAppPerm(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE, MY_PERMISSIONS_REQUEST_CHANGE_MULT_STATE);

        s_rate = Integer.parseInt(getIntent().getExtras().get(EXTRA_MESSAGE_SMPL_RATE).toString());


        if(getIntent().getExtras().get(EXTRA_MESSAGE_DEV).toString().equals("T")){
            play_file_name = "A" +
                getIntent().getExtras().get(EXTRA_MESSAGE_SMPL_RATE).toString() + "25ms" +
                getIntent().getExtras().get(EXTRA_MESSAGE_SIGNAL_SEL).toString() +".wav";
            rec_file_name = "rec"+getIntent().getExtras().get(EXTRA_MESSAGE_TIME_BETWEEN).toString()+play_file_name;
        } else {
            play_file_name = getIntent().getExtras().get(EXTRA_MESSAGE_DEV).toString() +
                    getIntent().getExtras().get(EXTRA_MESSAGE_SMPL_RATE).toString() + "25ms" +
                    getIntent().getExtras().get(EXTRA_MESSAGE_SIGNAL_SEL).toString() +".wav";
            rec_file_name = "rec"+getIntent().getExtras().get(EXTRA_MESSAGE_TIME_BETWEEN).toString()+play_file_name;
        }

        Log.e("File Name ", play_file_name);

        playManager = new PlayManager(5, play_file_name,Integer.parseInt(getIntent().getExtras().get(EXTRA_MESSAGE_SMPL_RATE).toString()),Integer.parseInt(getIntent().getExtras().get(EXTRA_MESSAGE_TIME_BETWEEN).toString()));

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
    }
    @Override
    protected void onResume() {
        super.onResume();
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(mReceiver, intentFilter);
    }

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

                if (rec_btn.getText().equals(getResources().getString(R.string.recording_off_text))) {
                    rec_btn.setText(R.string.recording_on_text);
                    rec_btn.setTextColor(getResources().getColor(R.color.color_rec_on));
                    if(soundDetector == null){
                        recManager = new RecManager(s_rate,rec_file_name);
                        recManager.start();
                        soundDetector = new SoundDetector("SoundDetector",last_detected_t,detected_t_difference,s_rate, play_file_name);
                        soundDetector.start();
                    } else {
                        recManager.start();
                        soundDetector.start();
                    }
                } else {
                    rec_btn.setText(getResources().getString(R.string.recording_off_text));
                    rec_btn.setTextColor(getResources().getColor(R.color.color_rec_off));
                    recManager.stopRecording();
                    soundDetector.stopRecording();
                    soundDetector = null;
                    recManager = null;
                }

                break;
            case R.id.play_button:
                if (play_btn.getText().equals(getResources().getString(R.string.playing_off_text))) {
                    play_btn.setText(R.string.playing_on_text);
                    play_btn.setTextColor(getResources().getColor(R.color.color_rec_on));
                    playManager.startPlaying();
                } else {
                    play_btn.setText(getResources().getString(R.string.playing_off_text));
                    play_btn.setTextColor(getResources().getColor(R.color.color_rec_off));
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

    //###################################################### CONNECTION STUFF

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
