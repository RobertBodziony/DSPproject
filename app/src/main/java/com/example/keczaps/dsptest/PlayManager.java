package com.example.keczaps.dsptest;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.musicg.wave.Wave;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayManager{

    private ScheduledExecutorService scheduleTaskExecutor;
    private int sampleRate = 44100;
    private int threadPoolNumberForTask;
    private double duration = 0.1;
    private double numSample = duration*sampleRate;
    private double numSample2 = 2*numSample;
    private double sample[] = new double[(int) numSample/2];
    private byte[] generatedSnd = new byte[(int) numSample];
    private byte[] finalGeneratedSignal;
    private Handler playHandler = new Handler();
    private AudioTrack audioTrack;
    private boolean isPlaying = false;

    public PlayManager(int threadPoolNumberForTask) {
        this.threadPoolNumberForTask = threadPoolNumberForTask;
        finalGeneratedSignal = getWaveFileBytes();
        this.genTone(2000,4000,"signal25ms441k2to3k.wav");
        this.genTone(3000,5000,"signal25ms441k3to4k.wav");
        this.genTone(4000,6000,"signal25ms441k4to5k.wav");
        this.genTone(5000,7000,"signal25ms441k5to6k.wav");
        this.genTone(2500,4500,"signal25ms441k25to35k.wav");
        this.genTone(3500,5500,"signal25ms441k35to45k.wav");
        this.genTone(4500,6500,"signal25ms441k45to55k.wav");
        this.genTone(5500,7500,"signal25ms441k55to65k.wav");
        this.sampleRate = 22050;
        this.numSample = duration*sampleRate;
        this.sample = new double[(int) (numSample/2)];
        this.generatedSnd = new byte[(int) (numSample*2)];
        this.genTone(2000,4000,"signal25ms221k2to3k.wav");
        this.genTone(3000,5000,"signal25ms221k3to4k.wav");
        this.genTone(4000,6000,"signal25ms221k4to5k.wav");
        this.genTone(5000,7000,"signal25ms221k5to6k.wav");
        this.genTone(2500,4500,"signal25ms221k25to35k.wav");
        this.genTone(3500,5500,"signal25ms221k35to45k.wav");
        this.genTone(4500,6500,"signal25ms221k45to55k.wav");
        this.genTone(5500,7500,"signal25ms221k55to65k.wav");
    }

    public void startPlaying() {

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length, AudioTrack.MODE_STATIC);

        scheduleTaskExecutor = Executors.newScheduledThreadPool(threadPoolNumberForTask);
        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                playHandler.post(new Runnable(){
                    public void run(){
                        playSound();
                    }
                });
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stopPlaying() {
        isPlaying = false;
        audioTrack.stop();
        audioTrack.release();
        scheduleTaskExecutor.shutdownNow();
    }

    private void playSound(){
        if(isPlaying){
            audioTrack.stop();
            audioTrack.release();
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length, AudioTrack.MODE_STATIC);
        }
        audioTrack.write(finalGeneratedSignal, 0, finalGeneratedSignal.length);
        audioTrack.play();
        isPlaying = true;
    }

    public boolean isPlaying() {
        return isPlaying;
    }


    public void genTone(double freqDOWN,double freqUP,String fnm){

        double instfreq=0,numerator;

        for (int i=0;i<(numSample-1)/2; i++ ) {
            numerator = (double)(i)/numSample;
            instfreq = freqDOWN+(numerator*(freqUP-freqDOWN));
            if ((i % 1000) == 0) {
                Log.e("Current Freq:", String.format("Freq is:  %f at loop %d of %d", instfreq, i, (int)numSample));
            }
            sample[i]=Math.sin(2*Math.PI*i/(sampleRate/instfreq));
        }

        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        writeAudioDataToFile(fnm,sampleRate);
    }

    private void writeAudioDataToFile(String fnm,int smplRate) {
        WaveFormatManager waveFormatManager = new WaveFormatManager();
        String fileName = fnm;//new SimpleDateFormat("yyyyMMddhhmm'.wav'").format(new Date());
        String filePath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/DSPtestFiles/";
        String fname=filePath+fileName;

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(fname);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e("FileNotFoundException", e.toString());
        }

        try {
            if(os != null){
                waveFormatManager.writeWavHeader(os, AudioFormat.CHANNEL_IN_MONO, smplRate, AudioFormat.ENCODING_PCM_16BIT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

            try {
                os.write(generatedSnd, 0, generatedSnd.length/2);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("os = not write to file", e.toString());
            }

        try {
            if(os != null){
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("os = null close", e.toString());
        }

        try {
            waveFormatManager.updateWavHeader(new File(fname));
        } catch (IOException ex) {
            ex.getMessage();
        }
    }


    public byte[] getWaveFileBytes() {
        Wave wave = new Wave(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/DSPtestFiles/testGenerated5ms.wav");
        return wave.getBytes();
    }



}
