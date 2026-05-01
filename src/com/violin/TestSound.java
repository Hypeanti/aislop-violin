package com.violin;

import javax.sound.sampled.*;

public class TestSound {
    public static void main(String[] args) throws Exception {
        System.out.println("Testing audio...");
        
        // simple test tone
        AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        line.start();
        
        // play a simple A4 note (440Hz) for 1 second
        int sampleRate = 44100;
        int duration = 1000;
        int numSamples = sampleRate * duration / 1000;
        byte[] buffer = new byte[numSamples * 2];
        
        for (int i = 0; i < numSamples; i++) {
            double time = i / (double)sampleRate;
            double angle = 2.0 * Math.PI * 440.0 * time;
            double sample = Math.sin(angle) * 0.5;
            
            short intSample = (short) (sample * 32767);
            buffer[i * 2] = (byte) (intSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((intSample >> 8) & 0xFF);
        }
        
        line.write(buffer, 0, buffer.length);
        Thread.sleep(1000);
        line.stop();
        line.close();
        
        System.out.println("If you heard a clean tone, audio works!");
        System.out.println("If you heard nothing or white noise, audio is broken");
    }
}