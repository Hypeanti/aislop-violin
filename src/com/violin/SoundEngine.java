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
    private double volume = 0.3; // 0 to 1 (start quieter so ears dont bleed)
    private double sustain = 0.0; // 0 to 1 (how long notes overlap)
    private double reverb = 0.0; // 0 to 1 (echo intensity)
    private double bitcrush = 0.0; // 0 to 1 (digital destruction)

    // string frequencies (actually correct violin tuning this time)
    private double[] baseFrequencies = {
            196.00, // G3 (lowest string)
            293.66, // D3
            440.00, // A3
            659.25 // E3 (highest string)
    };

    // tuning offsets for each string (in semitones)
    private double[] tuningOffsets = { 0, 0, 0, 0 };

    // lines 34-42 are ai (constructor stuff)
    public SoundEngine() {
        activeNotes = new HashMap<>();
        random = new Random();
        initializeAudio();
        System.out.println("Horrible Violin SoundEngine ready");
        System.out.println("Tip: Turn down your speakers before playing");
    }

    private void initializeAudio() {
        try {
            // 44100 Hz, 16-bit, mono, signed, little-endian (standard)
            audioFormat = new AudioFormat(44100, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();
            isRunning = true;
            // deadass this finally works
        } catch (LineUnavailableException e) {
            System.err.println("ur sound card said no: " + e.getMessage());
            isRunning = false;
        }
    }

    // plays a note - lines 55-90 are mostly me
    private long lastPlayTime = 0;
    private static final int MIN_PLAY_INTERVAL_MS = 20; // 50 notes per second max
    private Map<String, Long> lastNoteTime = new HashMap<>();

    public void playNote(int string, int fret, double velocity) {
        if (!isRunning || line == null)
            return;

        // strict throttle (lines 12-18 are ai)
        String throttleKey = string + "-" + fret;
        long now = System.currentTimeMillis();
        Long lastTime = lastNoteTime.get(throttleKey);
        if (lastTime != null && (now - lastTime) < MIN_PLAY_INTERVAL_MS) {
            return; // skip if too fast
        }
        lastNoteTime.put(throttleKey, now);

        // stop any existing note on this string/fret immediately
        stopNote(string, fret);

        velocity = Math.max(0.1, Math.min(1.0, velocity));
        double finalAmplitude = velocity * volume * 0.3;

        double frequency = calculateFrequency(string, fret);

        if (Double.isNaN(frequency) || frequency < 50 || frequency > 2000) {
            frequency = 440.0;
        }

        final double finalFrequency = frequency;
        final double finalVel = finalAmplitude;

        Thread soundThread = new Thread(() -> {
            generateViolinSound(finalFrequency, finalVel, string, fret);
        });
        soundThread.start();
        activeNotes.put(throttleKey, soundThread);
    }

    // calculates frequency - lines 95-115 are ai (math)
    private double calculateFrequency(int string, int fret) {
        if (string < 0 || string >= baseFrequencies.length) {
            string = 0;
        }
        if (fret < 0)
            fret = 0;

        double baseFreq = baseFrequencies[string];

        // apply tuning offset (semitone adjustment)
        baseFreq *= Math.pow(2.0, tuningOffsets[string] / 12.0);

        // each fret is a semitone
        double frequency = baseFreq * Math.pow(2.0, fret / 12.0);

        return frequency;
    }

    // ACTUAL VIOLIN SOUND GENERATION - lines 120-250 are mostly me
    private void generateViolinSound(double frequency, double amplitude, int string, int fret) {
        int sampleRate = 44100;
        int duration = 30; // super short, no overlap (lines 5-7 are ai)

        // sustain barely matters now
        if (sustain > 0) {
            duration += (int) (sustain * 20);
            if (duration > 80)
                duration = 80;
        }

        int numSamples = sampleRate * duration / 1000;
        if (numSamples < 100)
            numSamples = 100; // minimum samples

        byte[] buffer = new byte[numSamples * 2];

        // generate audio
        for (int i = 0; i < numSamples; i++) {
            double time = i / (double) sampleRate;
            double angle = 2.0 * Math.PI * frequency * time;

            // simple sine wave with quick decay
            double sample = Math.sin(angle);

            // add harmonics
            sample += Math.sin(angle * 2.0) * 0.2;
            sample += Math.sin(angle * 3.0) * 0.1;

            // very fast envelope (attack and immediate decay)
            double envelope = 1.0;
            if (i < 10) {
                envelope = i / 10.0; // super fast attack
            } else {
                envelope = Math.exp(-(i - 10) / 200.0); // exponential decay
            }

            sample *= amplitude * envelope * 0.5;

            // small amount of bow noise
            sample += (random.nextDouble() - 0.5) * 0.02;

            // clip
            sample = Math.max(-0.8, Math.min(0.8, sample));

            // convert to 16-bit
            short intSample = (short) (sample * 30000.0);
            buffer[i * 2] = (byte) (intSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((intSample >> 8) & 0xFF);
        }

        // play it
        line.write(buffer, 0, buffer.length);

        // no sleep, just let it play and immediately cleanup
        String noteKey = string + "-" + fret;
        activeNotes.remove(noteKey);
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

    // stops EVERYTHING
    public void stopAllNotes() {
        for (Thread t : activeNotes.values()) {
            if (t != null && t.isAlive()) {
                t.interrupt();
            }
        }
        activeNotes.clear();
        System.out.println("PANIC! All notes stopped");
    }

    // setters for all the knobs
    public void setVolume(double vol) {
        this.volume = Math.max(0, Math.min(1, vol));
        System.out.println("Volume set to " + (int) (volume * 100) + "%");
    }

    public void setSustain(double sus) {
        this.sustain = Math.max(0, Math.min(1, sus));
        System.out.println("Sustain set to " + (int) (sustain * 100) + "%");
    }

    public void setReverb(double rev) {
        this.reverb = Math.max(0, Math.min(1, rev));
        System.out.println("Reverb set to " + (int) (reverb * 100) + "%");
    }

    public void setBitcrush(double crush) {
        this.bitcrush = Math.max(0, Math.min(1, crush));
        System.out.println("Bitcrush set to " + (int) (bitcrush * 100) + "% (digital destruction)");
    }

    public void setStringTuning(int string, double offset) {
        if (string >= 0 && string < 4) {
            tuningOffsets[string] = offset;
            String sign = offset > 0 ? "+" : "";
            System.out.println("String " + (string + 1) + " tuned: " + sign + offset + " semitones");
        }
    }

    // clean up
    public void close() {
        isRunning = false;
        stopAllNotes();
        if (line != null) {
            line.stop();
            line.close();
        }
    }
}