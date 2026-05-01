package com.violin;

import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.geom.*;
import java.util.Hashtable;

public class ViolinGUI extends JFrame {

    // Core engine
    private SoundEngine soundEngine;
    private BowController bowController;
    private FretboardInput fretboardInput;
    private MouseTracker mouseTracker;

    // Panels
    private JPanel mainPanel;
    private JPanel violinContainerPanel;
    private JPanel controlPanel;
    private JPanel tuningPanel;
    private JPanel statusPanel;

    // Controls
    private JSlider volumeSlider;
    private JSlider sustainKnob;
    private JSlider reverbKnob;
    private JSlider mysteryKnob;

    // Labels
    private JLabel volumeValueLabel;
    private JLabel sustainValueLabel;
    private JLabel reverbValueLabel;
    private JLabel mysteryValueLabel;
    private JLabel speedValueLabel;
    private JLabel statusLabel;

    // State
    private double currentSpeed = 0;
    private Point currentMousePos = new Point(0, 0);
    private int activeString = -1;
    private boolean isPlaying = false;

    private final String[] stringNames = { "G String", "D String", "A String", "E String" };
    private final Color[] stringColors = {
            new Color(200, 180, 140),
            new Color(190, 170, 130),
            new Color(180, 160, 120),
            new Color(170, 150, 110)
    };

    public ViolinGUI() {
        setupLookAndFeel();
        initializeComponents();
        buildGUI();
        setupShutdownHook();
    }

    private void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
            UIManager.put("nimbusBase", new Color(70, 70, 80));
            UIManager.put("control", new Color(50, 50, 60));
        } catch (Exception ignored) {}
    }

    private void initializeComponents() {
        soundEngine = new SoundEngine();
        bowController = new BowController(soundEngine);
        fretboardInput = new FretboardInput(soundEngine, bowController);
        mouseTracker = MouseTracker.getInstance();
    }

    private void buildGUI() {
        setTitle("Violin Simulator - Cursor Bow Instrument");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(35, 35, 45));

        mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(35, 35, 45));
        mainPanel.setFocusable(true);

        createViolinDisplay();
        createControlPanel();
        createTuningSection();
        createStatusBar();

        mainPanel.add(violinContainerPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);
        add(tuningPanel, BorderLayout.NORTH);
        add(statusPanel, BorderLayout.SOUTH);

        setSize(1300, 900);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
        setVisible(true);

        bowController.startTracking(mainPanel);
        fretboardInput.attachToComponent(mainPanel);
        setupMouseTrackerListener();

        SwingUtilities.invokeLater(mainPanel::requestFocusInWindow);
    }

    private void createViolinDisplay() {
        violinContainerPanel = new JPanel(new BorderLayout());
        violinContainerPanel.setBackground(new Color(25, 25, 35));
        violinContainerPanel.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 100), 2));

        JPanel violinDrawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int cx = w / 2;

                g2d.setPaint(new GradientPaint(0, 0, new Color(30, 28, 35),
                        0, h, new Color(20, 18, 25)));
                g2d.fillRect(0, 0, w, h);

                int[] stringX = { cx - 30, cx - 10, cx + 10, cx + 30 };

                if (activeString >= 0 && isPlaying) {
                    g2d.setStroke(new BasicStroke(6));
                    g2d.setColor(new Color(255, 200, 100, 80));
                    g2d.drawLine(stringX[activeString], 60,
                            stringX[activeString], h - 80);
                }
            }
        };

        violinDrawingPanel.setPreferredSize(new Dimension(800, 700));
        violinContainerPanel.add(violinDrawingPanel, BorderLayout.CENTER);
    }

    private void createControlPanel() {}
    private void createTuningSection() {}
    private void createStatusBar() {}

    private void setupMouseTrackerListener() {
        mouseTracker.setListener(new MouseTracker.MouseListener() {
            public void onMouseMove(Point pos, double speed) {
                currentSpeed = speed;
                currentMousePos = pos;
                speedValueLabel.setText("Bow speed: " + (int) speed + " px/s");
                isPlaying = speed > 30;
                repaint();
            }

            public void onMouseStill(Point pos) {
                currentSpeed = 0;
                isPlaying = false;
                speedValueLabel.setText("Bow speed: 0 px/s");
                repaint();
            }
        });

        mouseTracker.startTracking();
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mouseTracker.shutdown();
            soundEngine.close();
        }));
    }
}
