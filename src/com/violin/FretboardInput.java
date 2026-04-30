package com.violin;

import java.awt.event.*;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class FretboardInput {

    private SoundEngine soundEngine;
    private BowController bowController;

    // keyboard mapping (lines 12-20 are ai)
    private static final char[][] FRET_MAPPINGS = {
            { '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=' }, // string 1
            { 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']' }, // string 2
            { 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\'', '#' }, // string 3
            { 'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/' } // string 4
    };

    // track which keys are currently pressed
    private Map<Character, int[]> activeKeys;

    public FretboardInput(SoundEngine engine, BowController controller) {
        this.soundEngine = engine;
        this.bowController = controller;
        this.activeKeys = new HashMap<>();
        System.out.println("Fretboard ready - keyboard mapping loaded");
    }

    public void attachToComponent(JComponent component) {
        component.setFocusable(true);
        component.requestFocusInWindow();

        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                char keyChar = Character.toLowerCase(e.getKeyChar());

                // find which string and fret this key belongs to
                for (int stringIndex = 0; stringIndex < FRET_MAPPINGS.length; stringIndex++) {
                    for (int fretIndex = 0; fretIndex < FRET_MAPPINGS[stringIndex].length; fretIndex++) {
                        if (FRET_MAPPINGS[stringIndex][fretIndex] == keyChar) {
                            // store that this key is active
                            int[] noteInfo = { stringIndex, fretIndex + 1 };
                            activeKeys.put(keyChar, noteInfo);

                            // get bow speed for velocity
                            double velocity = bowController.getCurrentVelocity();
                            if (velocity < 10)
                                velocity = 100; // default if mouse not moving
                            velocity = Math.min(1.0, velocity / 800.0);

                            System.out.println("Key pressed: " + keyChar + " | String: " + stringIndex + " | Fret: "
                                    + (fretIndex + 1));

                            soundEngine.playNote(stringIndex, fretIndex + 1, velocity);
                            break;
                        }
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                char keyChar = Character.toLowerCase(e.getKeyChar());

                if (activeKeys.containsKey(keyChar)) {
                    int[] noteInfo = activeKeys.get(keyChar);
                    soundEngine.stopNote(noteInfo[0], noteInfo[1]);
                    activeKeys.remove(keyChar);
                    System.out.println("Key released: " + keyChar);
                }
            }
        });

        System.out.println("Keyboard controls active - rows map to strings 1-4");
        System.out.println("String 1 (top row): 1 2 3 4 5 6 7 8 9 0 - =");
        System.out.println("String 2 (qwerty): Q W E R T Y U I O P [ ]");
        System.out.println("String 3 (home): A S D F G H J K L ; ' #");
        System.out.println("String 4 (bottom): Z X C V B N M , . /");
    }
}