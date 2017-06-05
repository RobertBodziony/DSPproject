package com.example.keczaps.dsptest;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

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
    private int sample_rate;
    private boolean isRecording = false;
    private int frameByteSize = 4096;
    private byte[] buffer;
    private String filePath;
    private String fileName;
    private WaveFormatManager waveFormatManager = null;
    private AudioRecord audioRecord;

    public RecManager(int sample_rate,String rec_f_name){
        this.sample_rate = sample_rate;
        this.fileName = rec_f_name;
        int recBufSize = AudioRecord.getMinBufferSize(sample_rate, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        if(recBufSize > frameByteSize){
            this.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, recBufSize);
        } else {
            this.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, frameByteSize);
        }
        this.buffer = new byte[frameByteSize];
        this.waveFormatManager = new WaveFormatManager();
        prepareDirectory();
        filePath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/DSPtestFiles/recorded/";
    }


    public void run() {
        Log.e("RecManager ", "STARTED");
        startRecord();
        writeAudioDataToFile();
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

    public boolean isRecording(){
        return this.isAlive() && isRecording;
    }

    private void prepareDirectory() {

        try {
            Log.d("Starting", "Checking up directory");
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "DSPtestFiles/recorded");

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
        String fileNameCorrect = new SimpleDateFormat("yyyyMMddhhmm'"+fileName+"'").format(new Date());
        String fname=filePath+fileNameCorrect;

        short sData[] = new short[frameByteSize/2];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(fname);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            if(os != null){
            waveFormatManager.writeWavHeader(os, RECORDER_CHANNELS, sample_rate, RECORDER_AUDIO_ENCODING);
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
