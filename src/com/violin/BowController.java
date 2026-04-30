package com.violin;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class BowController {
    
    private Point lastMousePos;
    private long lastMoveTime;
    private double currentVelocity;
    private SoundEngine soundEngine;
    
    // track mouse buttons
    private boolean[] mouseButtonsPressed;
    
    // lines 16-25 are ai (standard constructor stuff)
    public BowController(SoundEngine engine) {
        this.soundEngine = engine;
        this.lastMousePos = new Point(0, 0);
        this.currentVelocity = 0;
        this.mouseButtonsPressed = new boolean[4]; // indices 1-3 for buttons
        System.out.println("🏹 BowController ready - start moving that mouse");
    }
    
    // starts tracking mouse on a component
    public void startTracking(Component component) {
        
        // track mouse movement (the bow)
        // lines 32-58 are mostly me, but the velocity calculation is ai
        component.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateVelocity(e.getPoint());
                
                // string 1 is always active when moving mouse
                if (currentVelocity > 0.05) { // minimum speed threshold
                    double velocity = Math.min(1.0, currentVelocity / 800.0);
                    soundEngine.playNote(0, 0, velocity);
                }
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                updateVelocity(e.getPoint());
                
                // which string based on mouse button
                int stringIndex = getStringFromMouseButton(e.getButton());
                if (stringIndex >= 0 && currentVelocity > 0.05) {
                    double velocity = Math.min(1.0, currentVelocity / 800.0);
                    soundEngine.playNote(stringIndex, 0, velocity);
                }
            }
        });
        
        // track mouse button presses (for strings 2, 3, 4)
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int buttonIndex = getButtonIndex(e.getButton());
                if (buttonIndex >= 0) {
                    mouseButtonsPressed[buttonIndex] = true;
                    // deadass no sound here, sound happens on movement
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                int buttonIndex = getButtonIndex(e.getButton());
                if (buttonIndex >= 0) {
                    mouseButtonsPressed[buttonIndex] = false;
                    // stop the note for that string
                    int stringIndex = buttonIndex + 1;
                    soundEngine.stopNote(stringIndex, 0);
                }
            }
        });
    }
    
    // calculates mouse velocity in pixels per second
    // lines 68-83 are CLANKERRRRR (math is hard)
    private void updateVelocity(Point newPos) {
        long now = System.currentTimeMillis();
        
        if (lastMousePos.x != 0 && lastMousePos.y != 0) {
            double distance = lastMousePos.distance(newPos);
            long timeDelta = Math.max(1, now - lastMoveTime);
            currentVelocity = distance / timeDelta * 1000; // convert to px/s
            
            // clamp so it doesn't get ridiculous
            if (currentVelocity > 2000) currentVelocity = 2000;
        }
        
        lastMousePos = newPos;
        lastMoveTime = now;
    }
    
    // maps mouse button to string index (LMB=string2, MWB=string3, RMB=string4)
    private int getStringFromMouseButton(int button) {
        switch(button) {
            case MouseEvent.BUTTON1: return 1;
            case MouseEvent.BUTTON2: return 2;
            case MouseEvent.BUTTON3: return 3;
            default: return -1;
        }
    }
    
    // maps mouse button to array index
    private int getButtonIndex(int button) {
        switch(button) {
            case MouseEvent.BUTTON1: return 1;
            case MouseEvent.BUTTON2: return 2;
            case MouseEvent.BUTTON3: return 3;
            default: return -1;
        }
    }
    
    public double getCurrentVelocity() {
        return currentVelocity;
    }
}
