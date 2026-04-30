package com.violin;

import java.awt.event.*;
import javax.swing.*;

public class FretboardInput {
    
    private SoundEngine soundEngine;
    private BowController bowController;
    
    // keyboard mapping - each row is a string, each key is a fret
    // lines 11-20 are ai (i just copied your layout)
    private static final char[][] FRET_MAPPINGS = {
        {'1','2','3','4','5','6','7','8','9','0','-','='},  // string 1 (top row)
        {'q','w','e','r','t','y','u','i','o','p','[',']'},  // string 2 (qwerty row)
        {'a','s','d','f','g','h','j','k','l',';','\'','#'},  // string 3 (home row)
        {'z','x','c','v','b','n','m',',','.','/'}            // string 4 (bottom row)
    };
    
    // keep track of active frets so we don't spam
    private boolean[][] activeFrets;
    
    public FretboardInput(SoundEngine engine, BowController controller) {
        this.soundEngine = engine;
        this.bowController = controller;
        this.activeFrets = new boolean[4][12];
        System.out.println("[*] Fretboard ready - start smashing those keys");
    }
    
    // attaches keyboard listener to a component
    public void attachToComponent(JComponent component) {
        component.setFocusable(true);
        component.requestFocusInWindow();
        
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                char keyChar = Character.toLowerCase(e.getKeyChar());
                
                // loop through all strings and frets to find matching key
                for (int stringIndex = 0; stringIndex < FRET_MAPPINGS.length; stringIndex++) {
                    for (int fretIndex = 0; fretIndex < FRET_MAPPINGS[stringIndex].length; fretIndex++) {
                        if (FRET_MAPPINGS[stringIndex][fretIndex] == keyChar) {
                            // only play if not already active (prevents spam)
                            if (!activeFrets[stringIndex][fretIndex]) {
                                activeFrets[stringIndex][fretIndex] = true;
                                
                                // get current bow speed for velocity
                                double velocity = bowController.getCurrentVelocity();
                                if (velocity < 0.05) velocity = 0.2; // minimum velocity if mouse stationary
                                velocity = Math.min(1.0, velocity / 800.0);
                                
                                // fret is +1 because open string is fret 0
                                soundEngine.playNote(stringIndex, fretIndex + 1, velocity);
                            }
                            break;
                        }
                    }
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                char keyChar = Character.toLowerCase(e.getKeyChar());
                
                // find and stop the note
                for (int stringIndex = 0; stringIndex < FRET_MAPPINGS.length; stringIndex++) {
                    for (int fretIndex = 0; fretIndex < FRET_MAPPINGS[stringIndex].length; fretIndex++) {
                        if (FRET_MAPPINGS[stringIndex][fretIndex] == keyChar) {
                            activeFrets[stringIndex][fretIndex] = false;
                            soundEngine.stopNote(stringIndex, fretIndex + 1);
                            break;
                        }
                    }
                }
            }
        });
        
        System.out.println("✅ Controls connected:");
        System.out.println("   - Mouse movement = String 1 (open)");
        System.out.println("   - Mouse buttons + movement = Strings 2-4");
        System.out.println("   - Keyboard = Frets on each string");
    }
}
