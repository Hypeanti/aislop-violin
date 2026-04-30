package com.violin;

import javax.sound.midi.*;
import java.util.HashMap;
import java.util.Map;

public class SoundEngine {

    private Synthesizer synthesizer;
    private MidiChannel[] channels;
    private Map<String, Integer> activeNotes;

    // effect parameters (still usable)
    private double volume = 0.6;
    private double sustain = 0.0;

    // velocity tracking for each channel (lines 16-19 are ai)
    private int currentChannel = 0;
    private static final int VIOLIN_PROGRAM = 40; // 40 = violin, 41 = slow violin, 42 = cello

    // tuning offsets in semitones
    private double[] tuningOffsets = { 0, 0, 0, 0 };

    public SoundEngine() {
        activeNotes = new HashMap<>();
        initializeMidi();
        System.out.println("MIDI SoundEngine ready - using built-in violin");
    }

    private void initializeMidi() {
        try {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            channels = synthesizer.getChannels();

            // set all channels to violin (lines 32-37 are ai)
            for (int i = 0; i < channels.length; i++) {
                if (channels[i] != null) {
                    channels[i].programChange(VIOLIN_PROGRAM);
                }
            }

            System.out.println("Synthesizer: " + synthesizer.getDeviceInfo().getName());
            System.out.println("Violin program loaded");

        } catch (MidiUnavailableException e) {
            System.err.println("MIDI not available: " + e.getMessage());
        }
    }

    // plays a note - lines 44-80 are mostly me
    public void playNote(int string, int fret, double velocity) {
        if (synthesizer == null || channels == null)
            return;

        // throttle to prevent spam (lines 47-55 are ai)
        String noteKey = string + "-" + fret;
        if (activeNotes.containsKey(noteKey)) {
            stopNote(string, fret);
        }

        // calculate MIDI note number based on string and fret
        int midiNote = calculateMidiNote(string, fret);
        if (midiNote < 0 || midiNote > 127)
            return;

        // velocity from mouse speed (0-127)
        int midiVelocity = (int) (velocity * 80) + 40; // range 40-120
        midiVelocity = Math.max(40, Math.min(120, midiVelocity));

        // apply volume
        midiVelocity = (int) (midiVelocity * volume);

        // find a free channel or use round-robin (lines 65-72 are ai)
        currentChannel = (currentChannel + 1) % 16;
        while (channels[currentChannel] == null) {
            currentChannel = (currentChannel + 1) % 16;
        }

        // play the note
        channels[currentChannel].noteOn(midiNote, midiVelocity);
        activeNotes.put(noteKey, currentChannel);

        System.out.println("Playing: string=" + string + " fret=" + fret +
                " note=" + midiNote + " vel=" + midiVelocity);

        // sustain handling
        if (sustain > 0.5) {
            // leave it ringing
        } else {
            // schedule note-off after short duration
            new Thread(() -> {
                try {
                    Thread.sleep((long) (150 - sustain * 100));
                    stopNote(string, fret);
                } catch (InterruptedException e) {
                }
            }).start();
        }
    }

    // calculate MIDI note number based on violin tuning (lines 84-106 are ai
    // because math)
    private int calculateMidiNote(int string, int fret) {
        // MIDI note numbers for open strings (standard violin tuning)
        // G3 = 55, D4 = 62, A4 = 69, E5 = 76
        int[] openNotes = { 55, 62, 69, 76 }; // G3, D4, A4, E5

        if (string < 0 || string >= openNotes.length)
            return -1;

        // apply tuning offset (in semitones)
        int tunedNote = openNotes[string] + (int) tuningOffsets[string];

        // add fret (each fret = 1 semitone)
        int finalNote = tunedNote + fret;

        return Math.max(0, Math.min(127, finalNote));
    }

    public void stopNote(int string, int fret) {
        String noteKey = string + "-" + fret;
        if (activeNotes.containsKey(noteKey)) {
            int channel = activeNotes.get(noteKey);
            if (channels != null && channels[channel] != null) {
                // calculate note again to send note-off (lines 114-118 are ai)
                int midiNote = calculateMidiNote(string, fret);
                if (midiNote >= 0 && midiNote <= 127) {
                    channels[channel].noteOff(midiNote);
                }
            }
            activeNotes.remove(noteKey);
        }
    }

    public void stopAllNotes() {
        if (channels != null) {
            for (MidiChannel channel : channels) {
                if (channel != null) {
                    channel.allNotesOff();
                    channel.allSoundOff();
                }
            }
        }
        activeNotes.clear();
        System.out.println("PANIC - all notes stopped");
    }

    // setters
    public void setVolume(double vol) {
        this.volume = Math.max(0, Math.min(1, vol));
        System.out.println("Volume: " + (int) (volume * 100) + "%");
    }

    public void setSustain(double sus) {
        this.sustain = Math.max(0, Math.min(1, sus));
        System.out.println("Sustain: " + (int) (sustain * 100) + "% (MIDI sustain not fully implemented)");
    }

    public void setReverb(double rev) {
        // MIDI reverb via controller 91 (lines 137-144 are ai)
        if (channels != null && channels[0] != null) {
            int reverbVal = (int) (rev * 127);
            for (MidiChannel ch : channels) {
                if (ch != null)
                    ch.controlChange(91, reverbVal);
            }
        }
        System.out.println("Reverb: " + (int) (rev * 100) + "%");
    }

    public void setBitcrush(double crush) {
        // MIDI doesn't have bitcrush, but we can do something silly
        // send random pitch bend for "broken" effect
        if (channels != null && channels[0] != null && crush > 0) {
            int bendValue = (int) (8192 + (Math.random() - 0.5) * crush * 4000);
            for (MidiChannel ch : channels) {
                if (ch != null)
                    ch.setPitchBend(bendValue);
            }
        }
        System.out.println("Mystery knob: " + (int) (crush * 100) + "% (pitch wobble)");
    }

    public void setStringTuning(int string, double offset) {
        if (string >= 0 && string < 4) {
            tuningOffsets[string] = offset;
            String sign = offset > 0 ? "+" : "";
            System.out.println("String " + (string + 1) + " tuned: " + sign + offset + " semitones");
        }
    }

    public void close() {
        stopAllNotes();
        if (synthesizer != null) {
            synthesizer.close();
        }
    }
}