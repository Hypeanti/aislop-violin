package com.violin;

import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SoundEngine {
    
    // audio stuff - lines 8-12 are ai (i hate audio programming ngl)
    private AudioFormat audioFormat;
    private SourceDataLine line;
    private boolean isRunning;
    
    // keeps track of active notes so we can stop them later
    private Map<String, Thread> activeNotes;
    
    // random generator for that chaotic energy
    private Random random;
    
    // effect parameters (knobs control these)
    private double volume = 0.5;      // 0 to 1
    private double sustain = 0.0;     // 0 to 1 (how long notes overlap)
    private double reverb = 0.0;       // 0 to 1 (echo intensity)
    private double bitcrush = 0.0;     // 0 to 1 (digital destruction)
    
    // string frequencies (horribly tuned by default)
    private double[] baseFrequencies = {
        196.00,  // G3 - should be standard but we'll detune anyway
        293.66,  // D3
        440.00,  // A3
        329.63   // E3
    };
    
    // tuning offsets for each string (in semitones, cause why not)
    private double[] tuningOffsets = {0, 0, 0, 0};
    
    // reverb buffer for delay effect
    private double[] reverbBuffer;
    private int reverbWritePos;
    
    // lines 34-45 are ai (constructor stuff is always copy-pasted anyway)
    public SoundEngine() {
        activeNotes = new HashMap<>();
        random = new Random();
        reverbBuffer = new double[8820]; // ~200ms at 44100Hz
        reverbWritePos = 0;
        initializeAudio();
        System.out.println("🔥 SoundEngine initialized - prepare for auditory suffering 🔥");
    }
    
    private void initializeAudio() {
        try {
            // 44100 Hz, 16-bit, mono, signed, big-endian (standard java audio)
            audioFormat = new AudioFormat(44100, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();
            isRunning = true;
            // deadass this took me 3 hours to get working
        } catch (LineUnavailableException e) {
            System.err.println("bro ur sound card said no: " + e.getMessage());
            isRunning = false;
        }
    }
    
    // plays a note on a specific string at a specific fret with a given velocity (bow speed)
    // lines 60-90 are mostly me, but the frequency calculation is ai ngl
    public void playNote(int string, int fret, double velocity) {
        if (!isRunning || line == null) return;
        
        // clamp velocity so it doesnt break everything
        velocity = Math.max(0.1, Math.min(1.0, velocity));
        
        // apply volume from knob
        velocity *= volume;
        
        // generate unique key for this note (string + fret)
        String noteKey = string + "-" + fret;
        
        // stop existing note on same note if sustain is low
        if (sustain < 0.3 && activeNotes.containsKey(noteKey)) {
            stopNote(string, fret);
        }
        
        // calculate frequency with tuning offsets
        double frequency = calculateFrequency(string, fret);
        
        // add random detuning for that "organic" horrible sound (0.5% random wobble)
        frequency += frequency * (random.nextDouble() - 0.5) * 0.01;
        
        // start a new thread for this note so multiple notes can play at once
        Thread soundThread = new Thread(() -> {
            generateNote(frequency, velocity, string, fret);
        });
        soundThread.start();
        activeNotes.put(noteKey, soundThread);
        
        // if sustain is high, keep the note around longer
        if (sustain > 0.5) {
            try {
                Thread.sleep((long)(sustain * 500));
                // don't stop immediately, let it ring
            } catch (InterruptedException e) {
                // who cares
            }
        }
    }
    
    // calculates frequency based on string, fret, and tuning offsets
    // lines 94-110 are ai because math is hard
    private double calculateFrequency(int string, int fret) {
        // base frequency of the open string
        double baseFreq = baseFrequencies[string];
        
        // apply tuning offset (in semitones, 1 semitone = 2^(1/12))
        baseFreq *= Math.pow(2, tuningOffsets[string] / 12.0);
        
        // calculate fret frequency (each fret = 1 semitone)
        double frequency = baseFreq * Math.pow(2, fret / 12.0);
        
        // add a little chaos based on mystery knob
        if (bitcrush > 0) {
            frequency += frequency * (random.nextDouble() - 0.5) * bitcrush * 0.1;
        }
        
        return frequency;
    }
    
    // generates the actual audio samples for a note
    // lines 114-180 are half ai, half me (mostly me struggling with byte buffers)
    private void generateNote(double frequency, double amplitude, int string, int fret) {
        int sampleRate = 44100;
        int duration = 300; // milliseconds
        
        // apply sustain (longer duration = more sustain)
        if (sustain > 0) {
            duration += (int)(sustain * 700);
        }
        
        int numSamples = sampleRate * duration / 1000;
        byte[] buffer = new byte[numSamples * 2]; // 2 bytes per sample (16-bit)
        
        // generate the audio
        for (int i = 0; i < numSamples; i++) {
            double time = i / (double)sampleRate;
            double angle = 2 * Math.PI * frequency * time;
            
            // start with basic sine wave
            double sample = Math.sin(angle);
            
            // add harmonics for that "violin" sound (even to make it sound jank)
            sample += Math.sin(angle * 2) * 0.3;
            sample += Math.sin(angle * 3) * 0.15;
            sample += Math.sin(angle * 4) * 0.07;
            
            // apply amplitude envelope (attack and decay)
            double envelope = 1.0;
            if (i < 50) {
                envelope = i / 50.0; // quick attack
            } else if (i > numSamples - 200) {
                envelope = (numSamples - i) / 200.0; // release
            }
            
            sample *= amplitude * envelope;
            
            // apply reverb (add delayed and attenuated version of itself)
            // lines 145-155 are ai (reverb is complicated)
            if (reverb > 0) {
                int delaySamples = (int)(sampleRate * 0.05); // 50ms delay
                if (i > delaySamples) {
                    // read from reverb buffer with delay
                    int readPos = (reverbWritePos - delaySamples + reverbBuffer.length) % reverbBuffer.length;
                    double delayedSample = reverbBuffer[readPos];
                    sample += delayedSample * reverb * 0.5;
                }
                // write current sample to reverb buffer
                reverbBuffer[reverbWritePos] = sample;
                reverbWritePos = (reverbWritePos + 1) % reverbBuffer.length;
            }
            
            // apply bitcrush (reduce sample resolution)
            if (bitcrush > 0) {
                int bits = (int)(16 - (bitcrush * 14)); // 2 to 16 bits
                int maxValue = (int)Math.pow(2, bits) - 1;
                sample = Math.round(sample * maxValue) / (double)maxValue;
            }
            
            // add random noise for that authentic horrible sound
            sample += (random.nextDouble() - 0.5) * 0.05;
            
            // clip to prevent ear explosion
            sample = Math.max(-0.9, Math.min(0.9, sample));
            
            // convert to 16-bit little-endian
            short intSample = (short) (sample * 32767);
            buffer[i * 2] = (byte) (intSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((intSample >> 8) & 0xFF);
        }
        
        // write to audio line
        line.write(buffer, 0, buffer.length);
        
        // sleep for a bit to let it play
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            // note was stopped early
        }
        
        // if sustain is low, remove from active notes
        if (sustain < 0.5) {
            stopNote(string, fret);
        }
    }
    
    // stops a specific note
    public void stopNote(int string, int fret) {
        String noteKey = string + "-" + fret;
        Thread noteThread = activeNotes.get(noteKey);
        if (noteThread != null && noteThread.isAlive()) {
            noteThread.interrupt();
            activeNotes.remove(noteKey);
        }
    }
    
    // stops EVERYTHING (panic button behavior)
    public void stopAllNotes() {
        for (Thread t : activeNotes.values()) {
            if (t != null && t.isAlive()) {
                t.interrupt();
            }
        }
        activeNotes.clear();
        System.out.println("[!] PANIC! All notes stopped");
    }
    
    // setters for all the knobs (called by GUI)
    public void setVolume(double vol) {
        this.volume = Math.max(0, Math.min(1, vol));
        System.out.println("[*] Volume set to " + (int)(volume * 100) + "%");
    }
    
    public void setSustain(double sus) {
        this.sustain = Math.max(0, Math.min(1, sus));
        System.out.println("[*] Sustain set to " + (int)(sustain * 100) + "%");
    }
    
    public void setReverb(double rev) {
        this.reverb = Math.max(0, Math.min(1, rev));
        System.out.println("[*] Reverb set to " + (int)(reverb * 100) + "%");
    }
    
    public void setBitcrush(double crush) {
        this.bitcrush = Math.max(0, Math.min(1, crush));
        System.out.println("[*] Bitcrush set to " + (int)(bitcrush * 100) + "% (your ears are bleeding now)");
    }
    
    public void setStringTuning(int string, double offset) {
        if (string >= 0 && string < 4) {
            tuningOffsets[string] = offset;
            System.out.println("[*] String " + (string+1) + " tuned to " + (offset > 0 ? "+" : "") + offset + " semitones of horror");
        }
    }
    
    // clean up when closing
    public void close() {
        isRunning = false;
        stopAllNotes();
        if (line != null) {
            line.stop();
            line.close();
        }
    }
}
