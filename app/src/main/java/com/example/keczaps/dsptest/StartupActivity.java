package com.example.keczaps.dsptest;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Keczaps on 2017-05-25.
 */

public class StartupActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    public static final String EXTRA_MESSAGE_DEV = "com.example.keczaps.dsptest.EXTRA_MESSAGE_DEV";
    public static final String EXTRA_MESSAGE_SMPL_RATE = "com.example.keczaps.dsptest.EXTRA_MESSAGE_SMPL_RATE";
    public static final String EXTRA_MESSAGE_SIGNAL_TIME = "com.example.keczaps.dsptest.EXTRA_MESSAGE_SIGNAL_TIME";
    public static final String EXTRA_MESSAGE_SIGNAL_SEL = "com.example.keczaps.dsptest.EXTRA_MESSAGE_SIGNAL_SEL";
    public static final String EXTRA_MESSAGE_X = "com.example.keczaps.dsptest.EXTRA_MESSAGE_X";
    public static final String EXTRA_MESSAGE_Y = "com.example.keczaps.dsptest.EXTRA_MESSAGE_Y";
    public static final String EXTRA_MESSAGE_Z = "com.example.keczaps.dsptest.EXTRA_MESSAGE_Z";
    public static final String EXTRA_MESSAGE_TIME_BETWEEN = "com.example.keczaps.dsptest.EXTRA_MESSAGE_TIME_BETWEEN";


    private Spinner device_spinner, smpl_rate_spinner, signal_time_spinner, signal_select_spinner, time_between_spinner;
    private Button start_btn;
    private EditText x_editText,y_editText,z_editText;
    private TextView label_text_v,sig_time_text_v,sig_sel_text_v,time_between_text_v;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        device_spinner = (Spinner) findViewById(R.id.device_selection_spinner);
        smpl_rate_spinner = (Spinner) findViewById(R.id.sampling_rate_selection_spinner);
        signal_select_spinner = (Spinner) findViewById(R.id.signal_select_spinner);
        signal_time_spinner = (Spinner) findViewById(R.id.signal_time_spinner);
        time_between_spinner = (Spinner) findViewById(R.id.time_between_spinner);
        x_editText = (EditText) findViewById(R.id.x_EditText);
        y_editText = (EditText) findViewById(R.id.y_EditText);
        z_editText = (EditText) findViewById(R.id.z_EditText);
        start_btn = (Button) findViewById(R.id.start_btn);
        label_text_v = (TextView) findViewById(R.id.label_txt_v);
        sig_sel_text_v = (TextView) findViewById(R.id.signal_select_txt_v);
        sig_time_text_v = (TextView) findViewById(R.id.signal_time_txt_v);
        time_between_text_v = (TextView) findViewById(R.id.time_between_txt_v);


        device_spinner.setOnItemSelectedListener(this);

        Log.e("Spinner Dev ",device_spinner.getSelectedItem().toString());
        Log.e("Spinner Smpl ",smpl_rate_spinner.getSelectedItem().toString());
        Log.e("Spinner Sig time ",signal_time_spinner.getSelectedItem().toString());
        Log.e("Spinner time between",time_between_spinner.getSelectedItem().toString());
        Log.e("EditText x ",x_editText.getText().toString());
        Log.e("EditText y",y_editText.getText().toString());
        Log.e("EditText z",z_editText.getText().toString());

        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("Spinner Dev ",device_spinner.getSelectedItem().toString());
                Log.e("Spinner Smpl ",smpl_rate_spinner.getSelectedItem().toString());
                Log.e("Spinner Sig time ",signal_time_spinner.getSelectedItem().toString());
                Log.e("Spinner Sig sel ",signal_select_spinner.getSelectedItem().toString());
                Log.e("Spinner time between",time_between_spinner.getSelectedItem().toString());
                Log.e("EditText x ",x_editText.getText().toString());
                Log.e("EditText y",y_editText.getText().toString());
                Log.e("EditText z",z_editText.getText().toString());

                startNewAct(device_spinner.getSelectedItem().toString(),Integer.parseInt(smpl_rate_spinner.getSelectedItem().toString()),
                        signal_time_spinner.getSelectedItem().toString(),Integer.parseInt(signal_select_spinner.getSelectedItem().toString()),
                        Integer.parseInt(time_between_spinner.getSelectedItem().toString()), Double.parseDouble(x_editText.getText().toString()),
                        Double.parseDouble(y_editText.getText().toString()), Double.parseDouble(z_editText.getText().toString()));
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void startNewAct(String target,int smplRate, String signalTime,int signalSelection, int timeBetween, double x, double y, double z) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_MESSAGE_DEV, target);
        intent.putExtra(EXTRA_MESSAGE_SIGNAL_SEL, signalSelection);
        intent.putExtra(EXTRA_MESSAGE_SIGNAL_TIME, signalTime);
        intent.putExtra(EXTRA_MESSAGE_SMPL_RATE, smplRate);
        intent.putExtra(EXTRA_MESSAGE_TIME_BETWEEN, timeBetween);
        intent.putExtra(EXTRA_MESSAGE_X, x);
        intent.putExtra(EXTRA_MESSAGE_Y, z);
        intent.putExtra(EXTRA_MESSAGE_Z, y);

        startActivity(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String chosenDev = device_spinner.getItemAtPosition(position).toString();
        label_text_v.setText(chosenDev);
        if(chosenDev.equals("T")){
            sig_time_text_v.setVisibility(View.GONE);
            sig_sel_text_v.setVisibility(View.GONE);
            signal_select_spinner.setVisibility(View.GONE);
            signal_time_spinner.setVisibility(View.GONE);
            time_between_text_v.setVisibility(View.GONE);
            time_between_spinner.setVisibility(View.GONE);
        } else {
            sig_time_text_v.setVisibility(View.VISIBLE);
            sig_sel_text_v.setVisibility(View.VISIBLE);
            signal_select_spinner.setVisibility(View.VISIBLE);
            signal_time_spinner.setVisibility(View.VISIBLE);
            time_between_text_v.setVisibility(View.VISIBLE);
            time_between_spinner.setVisibility(View.VISIBLE);
        }


    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
