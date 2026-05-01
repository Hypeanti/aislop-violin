package com.violin;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class BowController {

    private Point lastMousePos;
    private long lastMoveTime;
    private double currentVelocity;
    private SoundEngine soundEngine;
    private boolean[] mouseButtonsPressed;

    // throttle control (lines 15-24 are ai)
    private long lastPlayTime = 0;
    private static final int PLAY_COOLDOWN_MS = 30;
    private int lastPlayedString = -1;

    public BowController(SoundEngine engine) {
        this.soundEngine = engine;
        this.lastMousePos = new Point(0, 0);
        this.currentVelocity = 0;
        this.mouseButtonsPressed = new boolean[4];
    }

    public void startTracking(Component component) {

        component.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateVelocity(e.getPoint());

                // lower threshold and shorter cooldown (lines 5-12 are ai)
                long now = System.currentTimeMillis();
                if (currentVelocity > 15 && (now - lastPlayTime) > 15) {
                    double velocity = Math.min(1.0, currentVelocity / 800.0);
                    soundEngine.playNote(0, 0, velocity);
                    lastPlayTime = now;
                    lastPlayedString = 0;
                    System.out.println("Mouse speed: " + String.format("%.0f", currentVelocity) + " px/s");
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updateVelocity(e.getPoint());

                int stringIndex = getStringFromMouseButton(e.getButton());
                if (stringIndex >= 0 && currentVelocity > 30) {
                    long now = System.currentTimeMillis();
                    if ((now - lastPlayTime) > PLAY_COOLDOWN_MS || lastPlayedString != stringIndex) {
                        double velocity = Math.min(1.0, currentVelocity / 800.0);
                        soundEngine.playNote(stringIndex, 0, velocity);
                        lastPlayTime = now;
                        lastPlayedString = stringIndex;
                    }
                }
            }
        });

        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int buttonIndex = getButtonIndex(e.getButton());
                if (buttonIndex >= 0) {
                    mouseButtonsPressed[buttonIndex] = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int buttonIndex = getButtonIndex(e.getButton());
                if (buttonIndex >= 0) {
                    mouseButtonsPressed[buttonIndex] = false;
                    int stringIndex = buttonIndex + 1;
                    soundEngine.stopNote(stringIndex, 0);
                }
            }
        });
    }

    private void updateVelocity(Point newPos) {
        long now = System.currentTimeMillis();

        if (lastMousePos.x != 0 && lastMousePos.y != 0) {
            double distance = lastMousePos.distance(newPos);
            long timeDelta = Math.max(1, now - lastMoveTime);
            currentVelocity = distance / timeDelta * 1000;

            if (currentVelocity > 2000)
                currentVelocity = 2000;
        }

        lastMousePos = newPos;
        lastMoveTime = now;
    }

    private int getStringFromMouseButton(int button) {
        switch (button) {
            case MouseEvent.BUTTON1:
                return 1;
            case MouseEvent.BUTTON2:
                return 2;
            case MouseEvent.BUTTON3:
                return 3;
            default:
                return -1;
        }
    }

    private int getButtonIndex(int button) {
        switch (button) {
            case MouseEvent.BUTTON1:
                return 1;
            case MouseEvent.BUTTON2:
                return 2;
            case MouseEvent.BUTTON3:
                return 3;
            default:
                return -1;
        }
    }

    public double getCurrentVelocity() {
        return currentVelocity;
    }
}