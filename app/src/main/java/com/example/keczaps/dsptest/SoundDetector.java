package com.example.keczaps.dsptest;


import android.os.Environment;
import android.util.Log;

import com.musicg.wave.Wave;

import java.util.Arrays;

public class SoundDetector extends Thread {

    public SoundDetector(String name) {
        super(name);
    }

    public void run() {
        String test1Path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/DSPtest/test1.wav";
        String test4Path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath()+"/DSPtest/testGenerated5ms.wav";
        Wave wave = new Wave(test1Path);
        Wave wave1 = new Wave(test4Path);
        byte[] waveb = wave.getBytes();
        byte[] wave1b = wave1.getBytes();
        Log.i("WaveAndWave1 "," length wave = "+waveb.length+" | length waveGen5ms = "+wave1b.length);
    }

    public static double getFloat64(byte[] bytes)
    {
        return Double.longBitsToDouble(((bytes[0] & 0xFFL) << 56)
                | ((bytes[1] & 0xFFL) << 48)
                | ((bytes[2] & 0xFFL) << 40)
                | ((bytes[3] & 0xFFL) << 32)
                | ((bytes[4] & 0xFFL) << 24)
                | ((bytes[5] & 0xFFL) << 16)
                | ((bytes[6] & 0xFFL) << 8)
                | ((bytes[7] & 0xFFL) << 0));
    }

    /**
     * Convolves sequences a and b.  The resulting convolution has
     * length a.length+b.length-1.
     */
    public static double[] conv(float[] a, float[] b)
    {
        double[] y = new double[a.length+b.length-1];

        // make sure that a is the shorter sequence
        if(a.length > b.length)
        {
            float[] tmp = a;
            a = b;
            b = tmp;
        }

        for(int lag = 0; lag < y.length; lag++)
        {
            y[lag] = 0;

            // where do the two signals overlap?
            int start = 0;
            // we can't go past the left end of (time reversed) a
            if(lag > a.length-1)
                start = lag-a.length+1;

            int end = lag;
            // we can't go past the right end of b
            if(end > b.length-1)
                end = b.length-1;

            //System.out.println("lag = " + lag +": "+ start+" to " + end);
            for(int n = start; n <= end; n++)
            {
                //System.out.println("  ai = " + (lag-n) + ", bi = " + n);
                y[lag] += b[n]*a[lag-n];
            }
        }

        return(y);
    }

    /**
     * Computes the cross correlation between sequences a and b.
     */
    public static double[] xcorr(float[] a, float[] b)
    {
        int len = a.length;
        if(b.length > a.length)
            len = b.length;

        return xcorr(a, b, len-1);

        // // reverse b in time
        // double[] brev = new double[b.length];
        // for(int x = 0; x < b.length; x++)
        //     brev[x] = b[b.length-x-1];
        //
        // return conv(a, brev);
    }

    /**
     * Computes the auto correlation of a.
     */
    public static double[] xcorr(float[] a)
    {
        return xcorr(a, a);
    }

    /**
     * Computes the cross correlation between sequences a and b.
     * maxlag is the maximum lag to
     */
    public static double[] xcorr(float[] a, float[] b, int maxlag)
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

            // where do the two signals overlap?
            int start = 0;
            // we can't start past the left end of b
            if(lag < 0)
            {
                //System.out.println("b");
                start = -lag;
            }

            int end = a.length-1;
            // we can't go past the right end of b
            if(end > b.length-lag-1)
            {
                end = b.length-lag-1;
                //System.out.println("a "+end);
            }

            //System.out.println("lag = " + lag +": "+ start+" to " + end+"   idx = "+idx);
            for(int n = start; n <= end; n++)
            {
                //System.out.println("  bi = " + (lag+n) + ", ai = " + n);
                y[idx] += a[n]*b[lag+n];
            }
            //System.out.println(y[idx]);
        }

        return(y);
    }


}