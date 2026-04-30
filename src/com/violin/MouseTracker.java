package com.violin;

import java.awt.Point;
import java.awt.MouseInfo;
import java.awt.PointerInfo;

public class MouseTracker {

    private static MouseTracker instance;
    private Thread trackingThread;
    private volatile boolean isTracking;
    private volatile boolean running;

    private Point lastPosition;
    private long lastTime;
    private double currentSpeed;

    // listener interface for other classes to get mouse data
    private MouseListener listener;

    public interface MouseListener {
        void onMouseMove(Point position, double speed);

        void onMouseStill(Point position);
    }

    // singleton pattern so only one tracker runs (lines 24-32 are ai)
    public static MouseTracker getInstance() {
        if (instance == null) {
            instance = new MouseTracker();
        }
        return instance;
    }

    private MouseTracker() {
        this.isTracking = false;
        this.running = true;
        this.currentSpeed = 0;
        this.lastPosition = new Point(0, 0);
    }

    public void setListener(MouseListener listener) {
        this.listener = listener;
    }

    public void startTracking() {
        if (trackingThread != null && trackingThread.isAlive()) {
            System.out.println("MouseTracker: already running");
            return;
        }

        isTracking = true;
        trackingThread = new Thread(() -> {
            // lines 45-50 are ai (initialization)
            lastTime = System.nanoTime();
            lastPosition = getMousePosition();

            System.out.println("MouseTracker: started tracking");

            while (running) {
                if (isTracking) {
                    Point current = getMousePosition();

                    if (current != null && lastPosition != null) {
                        // calculate speed if mouse moved (lines 57-68 are ai)
                        if (!current.equals(lastPosition)) {
                            long now = System.nanoTime();
                            double timeDelta = (now - lastTime) / 1_000_000_000.0;

                            if (timeDelta > 0) {
                                double distance = lastPosition.distance(current);
                                currentSpeed = distance / timeDelta;

                                // output to terminal
                                System.out.printf("[Mouse] Pos: (%d, %d) | Speed: %.2f px/s%n",
                                        current.x, current.y, currentSpeed);

                                // notify listener if one exists
                                if (listener != null) {
                                    listener.onMouseMove(current, currentSpeed);
                                }
                            }

                            lastTime = now;
                            lastPosition = current;
                        } else {
                            // mouse is still
                            System.out.printf("[Mouse] Pos: (%d, %d) | Speed: 0.00 px/s (still)%n",
                                    current.x, current.y);

                            if (listener != null) {
                                listener.onMouseStill(current);
                            }
                        }
                    }
                }

                // delay to prevent CPU spam (line 85 is ai)
                try {
                    Thread.sleep(16); // ~60fps tracking
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        trackingThread.setDaemon(true);
        trackingThread.start();
    }

    public void stopTracking() {
        isTracking = false;
        System.out.println("MouseTracker: stopped tracking");
    }

    public void shutdown() {
        running = false;
        if (trackingThread != null) {
            trackingThread.interrupt();
        }
        System.out.println("MouseTracker: shut down");
    }

    private Point getMousePosition() {
        try {
            PointerInfo info = MouseInfo.getPointerInfo();
            if (info != null) {
                return info.getLocation();
            }
        } catch (Exception e) {
            // mouse info failed, just return null
        }
        return null;
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public Point getCurrentPosition() {
        return getMousePosition();
    }

    public boolean isTracking() {
        return isTracking;
    }
}