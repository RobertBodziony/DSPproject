package com.example.keczaps.dsptest;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.musicg.wave.Wave;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayManager{

    private ScheduledExecutorService scheduleTaskExecutor;
    private int sampleRate = 44100;
    private int threadPoolNumberForTask;
    private double duration = 0.1;
    private double numSample = duration*sampleRate;
    private double sample[] = new double[(int) numSample];
    private byte[] generatedSnd = new byte[(int) numSample];
    private byte[] finalGeneratedSignal;
    private Handler playHandler = new Handler();
    private AudioTrack audioTrack;
    private boolean isPlaying = false;

    public PlayManager(int threadPoolNumberForTask) {
        this.threadPoolNumberForTask = threadPoolNumberForTask;
        finalGeneratedSignal = getWaveFileBytes();
        //this.genTone(5000,6000);
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


    public void genTone(double freqDOWN,double freqUP){

        double instfreq=0,numerator;

        for (int i=0;i<numSample; i++ ) {
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
    }


    public byte[] getWaveFileBytes() {
        Wave wave = new Wave(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/DSPtestFiles/testGenerated5ms.wav");
        return wave.getBytes();
    }



}
