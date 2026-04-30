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
    public void playNote(int string, int fret, double velocity) {
        if (!isRunning || line == null)
            return;

        // velocity is bow speed (0 to 1)
        velocity = Math.max(0.1, Math.min(1.0, velocity));

        // make it quieter overall so it doesnt destroy ears
        double finalAmplitude = velocity * volume * 0.4;

        // generate unique key for this note
        String noteKey = string + "-" + fret;

        // stop existing note if sustain is low
        if (sustain < 0.3 && activeNotes.containsKey(noteKey)) {
            stopNote(string, fret);
        }

        // calculate frequency with tuning
        double frequency = calculateFrequency(string, fret);

        // clamp to valid range
        if (Double.isNaN(frequency) || frequency < 50 || frequency > 2000) {
            frequency = 440.0;
        }

        final double finalFrequency = frequency;
        final double finalVel = finalAmplitude;

        // play in background thread
        Thread soundThread = new Thread(() -> {
            generateViolinSound(finalFrequency, finalVel, string, fret);
        });
        soundThread.start();
        activeNotes.put(noteKey, soundThread);

        // sustain effect
        if (sustain > 0.5) {
            try {
                Thread.sleep((long) (sustain * 400));
            } catch (InterruptedException e) {
                // whatever
            }
        }
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
        int duration = 600; // milliseconds

        // sustain makes it longer
        if (sustain > 0) {
            duration += (int) (sustain * 600);
        }

        int numSamples = sampleRate * duration / 1000;
        byte[] buffer = new byte[numSamples * 2];

        // generate each sample
        for (int i = 0; i < numSamples; i++) {
            double time = i / (double) sampleRate;

            // --- MAIN NOTE (sine wave) ---
            double angle = 2.0 * Math.PI * frequency * time;
            double sample = Math.sin(angle);

            // --- ADD VIOLIN HARMONICS (what makes it sound like a violin) ---
            // odd harmonics give it that warm string sound
            sample += Math.sin(angle * 2.0) * 0.15; // 2nd harmonic
            sample += Math.sin(angle * 3.0) * 0.25; // 3rd harmonic (stronger)
            sample += Math.sin(angle * 4.0) * 0.08; // 4th harmonic
            sample += Math.sin(angle * 5.0) * 0.05; // 5th harmonic

            // --- ADD BOW NOISE (the scratchy sound) ---
            // this is what makes it sound like a bow on strings
            double bowNoise = (random.nextDouble() - 0.5) * 0.08;
            sample += bowNoise;

            // --- ADD VIBRATO (pitch wobble for expression) ---
            double vibratoRate = 6.0; // Hz
            double vibratoDepth = 0.005;
            double vibratoAngle = 2.0 * Math.PI * vibratoRate * time;
            double vibrato = 1.0 + Math.sin(vibratoAngle) * vibratoDepth;

            // --- APPLY VIBRATO TO THE SAMPLE ---
            double vibratoAngle2 = 2.0 * Math.PI * (frequency * vibrato) * time;
            double vibratoSample = Math.sin(vibratoAngle2);
            vibratoSample += Math.sin(vibratoAngle2 * 2.0) * 0.15;
            vibratoSample += Math.sin(vibratoAngle2 * 3.0) * 0.25;

            // mix between regular and vibrato based on bow speed
            sample = sample * 0.6 + vibratoSample * 0.4;

            // --- VOLUME ENVELOPE (attack and decay like a real violin) ---
            double envelope = 1.0;
            if (i < 80) {
                // attack: quick swell when bow hits string
                envelope = i / 80.0;
            } else if (i > numSamples - 400) {
                // release: fade out when bow leaves
                envelope = (numSamples - i) / 400.0;
            }

            sample *= amplitude * envelope;

            // --- REVERB (echo effect) ---
            if (reverb > 0) {
                // simple delay line (i know it's hacky but it works)
                int delaySamples = (int) (sampleRate * 0.08); // 80ms delay
                if (i > delaySamples && i - delaySamples < buffer.length / 2) {
                    short delayedShort = (short) ((buffer[(i - delaySamples) * 2] & 0xFF) |
                            (buffer[(i - delaySamples) * 2 + 1] << 8));
                    double delayedSample = delayedShort / 32768.0;
                    sample += delayedSample * reverb * 0.4;
                }
            }

            // --- BITCRUSH (digital destruction) ---
            if (bitcrush > 0) {
                int bits = (int) (14 - (bitcrush * 10));
                bits = Math.max(3, Math.min(14, bits));
                int maxValue = (1 << bits) - 1;
                sample = Math.round(sample * maxValue) / (double) maxValue;
                // bitcrush also adds a little grit
                sample += (random.nextDouble() - 0.5) * bitcrush * 0.03;
            }

            // final clipping (keep it safe)
            sample = Math.max(-0.8, Math.min(0.8, sample));

            // convert to 16-bit (lines 190-195 are ai)
            short intSample = (short) (sample * 30000.0); // slightly lower volume
            buffer[i * 2] = (byte) (intSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((intSample >> 8) & 0xFF);
        }

        // play it
        line.write(buffer, 0, buffer.length);

        // let it finish
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            // stopped early
        }

        // cleanup
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