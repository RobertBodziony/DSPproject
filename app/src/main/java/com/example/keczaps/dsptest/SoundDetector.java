package com.example.keczaps.dsptest;

import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.os.Handler;
import android.widget.TextView;

import com.musicg.wave.Wave;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import be.tarsos.dsp.util.fft.FFT;

public class SoundDetector extends Thread {

    private boolean first = false,isRecording=false;
    private int maxIndex = 0,sample_rate;
    private double[] waved,wave1d;
    private double t_last_det=0,t_now_det=0,diff,t_lastTEMP, t_nowTEMP, diffTEMP;
    private byte[] wave1b;
    private String test1Path,test2Path;
    private Wave wave1;
    private AudioRecord mAudioRecord;
    private Handler handler=new Handler();
    private TextView last_detected_TV,diff_detected_TV;
    private FFT fft;

    public SoundDetector(String name,TextView last_detected_t,TextView diff_detected,int sample_rate,String f_name) {
        super(name);

        //test1Path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/DSPtest/test1full2048.wav";
        //wave1d = byte2double(wave1b);
        //Wave wave1 = new Wave(test2Path);
        //wave1b = wave1.getBytes();
        Log.e("File Name ",f_name);
        this.sample_rate = sample_rate;
        test2Path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/DSPtestFiles/proper/"+f_name;
        Log.e("File Path ",test2Path);
        Wave wave = new Wave(test2Path);
        this.mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT, 4096);
        byte[] waveb = wave.getBytes();
        Log.i("WaveAndWave1 "," length waveB = "+waveb.length);
        waved = byte2double(waveb);
        Log.i("WaveAndWave1 "," length waveD = "+waved.length);
        this.last_detected_TV = last_detected_t;
        this.diff_detected_TV = diff_detected;
        if(sample_rate == 44100){
            this.wave1b = new byte[2048];
            Log.i("Wave1b "," length wave1b = "+wave1b.length);
        } else {
            this.wave1b = new byte[1024];
            Log.i("Wave1b "," length wave1b = "+wave1b.length);
        }

    }

    public void run() {
        Log.e("SoundDetector ", "STARTED");
        startRecord();
        while (isRecording) {

            if(sample_rate == 44100){
                mAudioRecord.read(wave1b,0,2048);
            } else {
                mAudioRecord.read(wave1b,0,1024);
            }

            wave1d = byte2double(wave1b);
            long t = SystemClock.currentThreadTimeMillis();
            //Log.e("XCORR TIME START ", ""+t);
            double[] result = xcorr(wave1d, waved);
            long t2 = SystemClock.currentThreadTimeMillis();
            //Log.e("XCORR TIME END   ", "" + t2);
            //Log.e("XCORR TIME DIFF  ", "" + (t2 - t));
            if(result[maxIndex] > 10000) {
                Log.e("SIGNAL DETECTED ", "DETECTED MAX : " + result[maxIndex] + " | Index of Max : " + maxIndex + " | Lag : " + (maxIndex - waved.length) + " | TIME Lag : " + ((((double) (maxIndex - waved.length)) / sample_rate)*1000));
                if(t_now_det != 0){
                    t_lastTEMP = t_now_det;
                    t_nowTEMP = t;//+((((double) (maxIndex - waved.length)) / sample_rate)*1000);
                    diffTEMP = t_nowTEMP - t_lastTEMP;
                    if(diffTEMP < 500){
                        Log.e("SIGNAL DETECTED ", "OUTLIER CATCHED LIKE POKEMON!");
                    } else {
                        t_last_det = t_now_det;
                        t_now_det = t;//+((((double) (maxIndex - waved.length)) / sample_rate)*1000);
                        diff = t_now_det - t_last_det;
                        first = false;
                        Log.e("XCORR DETECTED ", "TIME Difference : "+t_now_det+" - ("+t_last_det+") = " + diff);
                    }
                } else {
                    t_now_det = t;//+((((double) (maxIndex - waved.length)) / sample_rate)*1000);
                    first = true;
                    Log.e("XCORR DETECTED ", "TIME : "+(t));//+((((double) (maxIndex - waved.length)) / sample_rate)*1000)));
                }

                handler.post(new Runnable(){
                    public void run(){
                        if(!first) {
                            last_detected_TV.setText(String.format("%.2f", t_last_det));
                            last_detected_TV.setTextColor(Color.RED);
                            diff_detected_TV.setText(String.format("%.2f", diff));
                            diff_detected_TV.setTextColor(Color.RED);
                        } else {
                            last_detected_TV.setText(String.format("%.2f", t_now_det));
                            last_detected_TV.setTextColor(Color.RED);
                        }
                    }
                });

            } else {
                if(result[maxIndex] > 1000){
                    Log.e("XCORR NOT DETECTED ", " maxNum : " + result[maxIndex]);
                        final double res = result[maxIndex];
                    handler.post(new Runnable(){
                        public void run(){
                                last_detected_TV.setText(String.format("%.2f", res));
                                last_detected_TV.setTextColor(Color.RED);
                        }
                    });
                    }
            }
        }
    }

    public void startRecord(){
        try{
            mAudioRecord.startRecording();
        } catch (Exception e) {
            e.printStackTrace();
        }
        isRecording = true;
    }

    public void stopRecording(){
        isRecording = false;
        try{
            mAudioRecord.stop();
            mAudioRecord.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printResultToTextFile(double[] res){
        try {
            String filename= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/DSPtest/test1TEXT.txt";;
            FileWriter fw = new FileWriter(filename,false);
                fw.write("XCORR ARRAY \n");
            for(int i = 0;i<res.length-1;i++) {
                fw.write(Double.toString(res[i])+";\n");
            }
            fw.close();
        } catch (IOException e) {
            System.out.println("NOPE.");
        }
    }
    private double[] byte2double(byte[] data){
        double d[] = new double[data.length/2];
        ByteBuffer buf = ByteBuffer.wrap(data, 0, data.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int counter = 0;
        while (buf.remaining() >= 2) {
            double s = buf.getShort();
            d[counter] = s/1000;
            counter++;
        }
        return d;
    }

    private   double[] xcorr(double[] a, double[] b)
    {
        int len = a.length;
        if(b.length > a.length)
            len = b.length;

        return xcorr(a, b, len-1);
    }

    private double[] xcorr(double[] a, double[] b, int maxlag)
    {
        double[] y = new double[2*maxlag+1];
        Arrays.fill(y, 0);

        for(int lag = b.length-1, idx = maxlag-b.length+1;
            lag > -a.length; lag--, idx++)
        {
            if(idx < 0)
                continue;

            if(idx >= y.length)
                break;

            int start = 0;
            if(lag < 0)
            {
                start = -lag;
            }

            int end = a.length-1;
            if(end > b.length-lag-1)
            {
                end = b.length-lag-1;
            }

            for(int n = start; n <= end; n++)
            {
                y[idx] += a[n]*b[lag+n];
            }
            double newnumber = y[idx];

            if ((newnumber > y[maxIndex])) {
                maxIndex = idx;
            }
        }

        return (y);
    }


}