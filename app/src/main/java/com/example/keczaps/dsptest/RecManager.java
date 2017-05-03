package com.example.keczaps.dsptest;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import com.musicg.wave.Wave;
import com.musicg.wave.WaveFileManager;
import com.musicg.wave.WaveHeader;
import com.musicg.wave.WaveTypeDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import be.tarsos.dsp.AudioEvent;


/*public class RecManager {

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord audioRecorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private String filePath;
    private int bufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private int bytesPerElement = 2; // 2 bytes in 16bit format

    RecManager() {
        audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferElements2Rec * bytesPerElement);
    }

    void startRecording() {

        audioRecorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        String fileName = new SimpleDateFormat("yyyyMMddhhmm'.pcm'").format(new Date());
        String fname=filePath+fileName;

        short sData[] = new short[bufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(fname);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            audioRecorder.read(sData, 0, bufferElements2Rec);

            Log.i("Short wr -> ",Short.toString(sData[1]));//("Short wirting to file" + sData.toString());
            try {
                byte bData[] = short2byte(sData);
                os.write(bData, 0, bufferElements2Rec * bytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void stopRecording() {
        // stops the recording activity
        if (null != audioRecorder) {
            isRecording = false;
            audioRecorder.stop();
            audioRecorder.release();

            audioRecorder = null;
            recordingThread = null;
        }
    }


    void prepareDirectory(String dirName) {

        try {
            Log.d("Starting", "Checking up directory");
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), dirName);

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
        filePath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/"+dirName+"/";
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
}*/

public class RecManager extends Thread {

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private boolean isRecording = false;
    private WaveFormatManager waveFormatManager = null;
    private String filePath;
    private AudioRecord audioRecord;
    private int frameByteSize = 2048; // for 1024 fft size (16bit sample size)
    private byte[] buffer;

    public RecManager(){
        int recBufSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING); // need to be larger than size of a frame
        this.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, recBufSize);
        this.buffer = new byte[frameByteSize];
        this.waveFormatManager = new WaveFormatManager();
    }

    public AudioRecord getAudioRecord(){
        return audioRecord;
    }

    public boolean isRecording(){
        return this.isAlive() && isRecording;
    }

    public void startRecording(){
        try{
            audioRecord.startRecording();
            isRecording = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording(){
        try{
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getFrameBytes(){
        audioRecord.read(buffer, 0, frameByteSize);

        // analyze sound
        int totalAbsValue = 0;
        short sample = 0;
        float averageAbsValue = 0.0f;

        for (int i = 0; i < frameByteSize; i += 2) {
            sample = (short)((buffer[i]) | buffer[i + 1] << 8);
            totalAbsValue += Math.abs(sample);
        }
        averageAbsValue = totalAbsValue / frameByteSize / 2;

        // no input
        if (averageAbsValue < 30){
            return null;
        }

        return buffer;
    }

    public void run() {
        startRecording();
        writeAudioDataToFile();
    }


    void prepareDirectory(String dirName) {

        try {
            Log.d("Starting", "Checking up directory");
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), dirName);

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
        filePath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/"+dirName+"/";
    }

    private void writeAudioDataToFile() {
        String fileName = new SimpleDateFormat("yyyyMMddhhmm'.pcm'").format(new Date());
        String fname=filePath+"file1.wav";

        short sData[] = new short[frameByteSize/2];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(fname);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            waveFormatManager.writeWavHeader(os, RECORDER_CHANNELS, RECORDER_SAMPLERATE, RECORDER_AUDIO_ENCODING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            audioRecord.read(sData, 0, frameByteSize/2);

            Log.i("Short wr -> ",Short.toString(sData[1]));//("Short wirting to file" + sData.toString());
            try {
                byte bData[] = short2byte(sData);
                os.write(bData, 0, frameByteSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
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
