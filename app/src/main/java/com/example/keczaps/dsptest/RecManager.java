package com.example.keczaps.dsptest;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecManager extends Thread {

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private boolean isRecording = false;
    private WaveFormatManager waveFormatManager = null;
    private String filePath;
    private AudioRecord audioRecord;
    private int frameByteSize = 4096;
    private byte[] buffer;
    //private SoundDetector soundDetector;

    public RecManager(TextView last_detected_t,TextView diff_detected){
        int recBufSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        if(recBufSize > frameByteSize){
            this.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, recBufSize);
        } else {
            this.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING, frameByteSize);
        }
        this.buffer = new byte[frameByteSize];
        this.waveFormatManager = new WaveFormatManager();
        //soundDetector = new SoundDetector("SoundDetector1",this,last_detected_t,diff_detected);
        prepareDirectory();
        filePath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/DSPtest/";
    }

    public boolean isRecording(){
        return this.isAlive() && isRecording;
    }

    public void startRecord(){
        try{
            audioRecord.startRecording();
        } catch (Exception e) {
            e.printStackTrace();
        }
        isRecording = true;
    }

    public void stopRecording(){
        isRecording = false;
        try{
            audioRecord.stop();
            audioRecord.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getFrameBytes(){
        if(isRecording){
        audioRecord.read(buffer, 0, frameByteSize);
        }
        return buffer;
    }

    public void run() {
        Log.e("RecManager ", "STARTED");
        startRecord();
        writeAudioDataToFile();
    }


    void prepareDirectory() {

        try {
            Log.d("Starting", "Checking up directory");
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "DSPtest");

            if (! mediaStorageDir.exists()) {
                if (! mediaStorageDir.mkdir()) {
                    Log.e("Dir Creation Failed",mediaStorageDir.toString());
                }
                else {
                    Log.i("Directory Creation","Success");
                }
            }
        }
        catch(Exception ex) {
            Log.e("Directory Creation",ex.getMessage());
        }
    }

    private void writeAudioDataToFile() {
        String fileName = new SimpleDateFormat("yyyyMMddhhmm'.wav'").format(new Date());
        String fname=filePath+fileName;

        short sData[] = new short[frameByteSize/2];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(fname);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            if(os != null){
            waveFormatManager.writeWavHeader(os, RECORDER_CHANNELS, RECORDER_SAMPLERATE, RECORDER_AUDIO_ENCODING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        while (isRecording) {
            audioRecord.read(sData, 0, frameByteSize/2);

            //Log.i("Short wr -> ",Short.toString(sData[1]));//("Short wirting to file" + sData.toString());
            try {
                byte bData[] = short2byte(sData);
                os.write(bData, 0, frameByteSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if(os != null){
            os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            waveFormatManager.updateWavHeader(new File(fname));
        } catch (IOException ex) {
            ex.getMessage();
        }
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

}
