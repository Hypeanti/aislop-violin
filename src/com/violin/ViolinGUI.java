package com.violin;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Hashtable;

public class ViolinGUI extends JFrame {

    // Sound engine components
    private SoundEngine soundEngine;
    private BowController bowController;
    private FretboardInput fretboardInput;
    private MouseTracker mouseTracker;

    // GUI Components
    private JPanel mainPanel;
    private JPanel violinContainerPanel;
    private JPanel controlPanel;
    private JPanel tuningPanel;
    private JPanel statusPanel;

    // Knobs and sliders
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

    // Tuning controls
    private JSlider[] tuningSliders = new JSlider[4];
    private String[] stringNames = { "G String", "D String", "A String", "E String" };
    private Color[] stringColors = { new Color(200, 180, 140), new Color(190, 170, 130),
            new Color(180, 160, 120), new Color(170, 150, 110) };
    private double[] tuningOffsets = new double[4];

    // Mouse tracking
    private double currentSpeed = 0;
    private Point currentMousePos = new Point(0, 0);
    private int activeString = -1;
    private boolean isPlaying = false;

    public ViolinGUI() {
        initializeComponents();
        setupLookAndFeel();
        setupGUI();
        startMouseTracking();
        setupShutdownHook();
    }

    private void initializeComponents() {
        soundEngine = new SoundEngine();
        bowController = new BowController(soundEngine);
        fretboardInput = new FretboardInput(soundEngine, bowController);
        mouseTracker = MouseTracker.getInstance();

        for (int i = 0; i < 4; i++) {
            tuningOffsets[i] = 0;
        }
    }

    private void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
            UIManager.put("nimbusBase", new Color(70, 70, 80));
            UIManager.put("nimbusBlueGrey", new Color(100, 100, 110));
            UIManager.put("control", new Color(50, 50, 60));
        } catch (Exception e) {
            // fallback to default
        }
    }

    private void setupGUI() {
        setTitle("Violin Simulator - Cursor Bow Instrument");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(35, 35, 45));

        // Main panel with padding
        mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(35, 35, 45));

        // Create sections
        createViolinDisplay();
        createControlPanel();
        createTuningSection();
        createStatusBar();

        // Assemble
        mainPanel.add(violinContainerPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.EAST);
        add(mainPanel, BorderLayout.CENTER);
        add(tuningPanel, BorderLayout.NORTH);
        add(statusPanel, BorderLayout.SOUTH);

        setSize(1300, 900);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 700));

        // Setup input
        mainPanel.setFocusable(true);
        mainPanel.requestFocusInWindow();
        fretboardInput.attachToComponent(mainPanel);
        bowController.startTracking(mainPanel);
        setupMouseTrackerListener();
    }

    private void createViolinDisplay() {
        violinContainerPanel = new JPanel(new BorderLayout());
        violinContainerPanel.setBackground(new Color(25, 25, 35));
        violinContainerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 100), 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JPanel violinDrawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int width = getWidth();
                int height = getHeight();
                int centerX = width / 2;

                // Draw dark background with subtle gradient
                GradientPaint gradient = new GradientPaint(0, 0, new Color(30, 28, 35),
                        0, height, new Color(20, 18, 25));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, width, height);

                // Draw violin body with realistic shape
                int bodyWidth = 380;
                int bodyHeight = 550;
                int bodyX = centerX - bodyWidth / 2;
                int bodyY = 80;

                // Shadow
                g2d.setColor(new Color(0, 0, 0, 50));
                g2d.fillRoundRect(bodyX + 5, bodyY + 5, bodyWidth, bodyHeight, 80, 80);

                // Main body
                GradientPaint bodyGrad = new GradientPaint(bodyX, bodyY, new Color(160, 100, 50),
                        bodyX + bodyWidth, bodyY + bodyHeight, new Color(100, 60, 30));
                g2d.setPaint(bodyGrad);
                g2d.fillRoundRect(bodyX, bodyY, bodyWidth, bodyHeight, 80, 80);

                // Inner highlight
                g2d.setColor(new Color(180, 120, 70, 80));
                g2d.fillRoundRect(bodyX + 10, bodyY + 10, bodyWidth - 20, bodyHeight - 20, 70, 70);

                // F-holes
                drawFHole(g2d, bodyX + 70, bodyY + 180, 30, 140);
                drawFHole(g2d, bodyX + bodyWidth - 100, bodyY + 180, 30, 140);

                // Fingerboard
                int fingerboardX = centerX - 20;
                int fingerboardY = 20;
                int fingerboardWidth = 40;
                int fingerboardHeight = 300;
                GradientPaint fbGrad = new GradientPaint(fingerboardX, fingerboardY, new Color(40, 35, 30),
                        fingerboardX + fingerboardWidth, fingerboardY, new Color(60, 55, 50));
                g2d.setPaint(fbGrad);
                g2d.fillRect(fingerboardX, fingerboardY, fingerboardWidth, fingerboardHeight);

                // Fret lines
                g2d.setColor(new Color(200, 190, 170));
                g2d.setStroke(new BasicStroke(1.5f));
                for (int i = 1; i <= 12; i++) {
                    int fretY = fingerboardY + (i * 23);
                    g2d.drawLine(fingerboardX, fretY, fingerboardX + fingerboardWidth, fretY);

                    // Fret markers (dots)
                    if (i == 3 || i == 5 || i == 7 || i == 9 || i == 12) {
                        g2d.setColor(new Color(220, 200, 170));
                        g2d.fillOval(centerX - 5, fretY - 15, 10, 10);
                        g2d.setColor(new Color(180, 160, 130));
                        g2d.fillOval(centerX - 4, fretY - 14, 8, 8);
                    }
                }

                // Strings
                int[] stringX = { centerX - 30, centerX - 10, centerX + 10, centerX + 30 };
                float[] stringThickness = { 2.5f, 2.0f, 1.5f, 1.2f };
                Color[] stringColor = { new Color(220, 200, 140), new Color(210, 190, 135),
                        new Color(200, 180, 130), new Color(190, 170, 125) };

                for (int i = 0; i < 4; i++) {
                    g2d.setStroke(new BasicStroke(stringThickness[i]));
                    g2d.setColor(stringColor[i]);
                    g2d.drawLine(stringX[i], fingerboardY + 20, stringX[i], bodyY + bodyHeight - 40);

                    // Add string glow when active
                    if (activeString == i && isPlaying) {
                        g2d.setColor(new Color(255, 200, 100, 80));
                        g2d.setStroke(new BasicStroke(stringThickness[i] + 4));
                        g2d.drawLine(stringX[i], fingerboardY + 20, stringX[i], bodyY + bodyHeight - 40);
                    }
                }

                // Bridge
                int bridgeY = bodyY + bodyHeight - 120;
                g2d.setColor(new Color(120, 75, 45));
                g2d.fillRoundRect(centerX - 45, bridgeY, 90, 15, 10, 10);
                g2d.setColor(new Color(100, 60, 35));
                g2d.fillRoundRect(centerX - 35, bridgeY - 5, 70, 8, 8, 8);

                // Tailpiece
                g2d.setColor(new Color(50, 45, 40));
                g2d.fillRoundRect(centerX - 25, bridgeY + 15, 50, 80, 15, 15);

                // Draw bow when mouse is over
                if (currentMousePos.x > 0 && currentMousePos.y > 0 &&
                        currentMousePos.x < width && currentMousePos.y < height) {
                    drawBow(g2d, currentMousePos.x, currentMousePos.y, currentSpeed);
                }

                // Draw string highlight based on mouse X position
                int highlightedString = getStringFromX(currentMousePos.x, stringX);
                if (highlightedString >= 0 && highlightedString < 4) {
                    g2d.setColor(new Color(255, 200, 100, 60));
                    g2d.setStroke(new BasicStroke(stringThickness[highlightedString] + 8));
                    g2d.drawLine(stringX[highlightedString], fingerboardY + 20,
                            stringX[highlightedString], bodyY + bodyHeight - 40);
                }
            }

            private void drawFHole(Graphics2D g2d, int x, int y, int width, int height) {
                Shape fHole = new Ellipse2D.Double(x, y, width, height);
                g2d.setColor(new Color(40, 25, 15));
                g2d.fill(fHole);
                g2d.setColor(new Color(60, 40, 25));
                g2d.setStroke(new BasicStroke(2f));
                g2d.draw(fHole);

                // F-hole notches
                int[] notchX = { x + width / 2 - 5, x + width / 2 + 5 };
                int[] notchY = { y + height / 2, y + height / 2 };
                g2d.fillOval(notchX[0], notchY[0] - 3, 6, 6);
                g2d.fillOval(notchX[1] - 6, notchY[1] - 3, 6, 6);
            }

            private void drawBow(Graphics2D g2d, int x, int y, double speed) {
                // Bow stick
                int bowLength = 120 + (int) (speed / 15);
                int bowHeight = 8;

                g2d.setStroke(new BasicStroke(3));
                g2d.setColor(new Color(101, 67, 33));
                g2d.drawLine(x - bowLength / 2, y, x + bowLength / 2, y);

                // Bow hair
                g2d.setColor(new Color(230, 220, 200, 180));
                g2d.setStroke(new BasicStroke(bowHeight));
                g2d.drawLine(x - bowLength / 2 + 5, y + 2, x + bowLength / 2 - 5, y + 2);

                // Speed particles
                if (speed > 200) {
                    int particleCount = Math.min(15, (int) (speed / 100));
                    g2d.setColor(new Color(255, 150, 50, 100));
                    for (int i = 0; i < particleCount; i++) {
                        int px = x + (int) (Math.random() * 60) - 30;
                        int py = y + (int) (Math.random() * 30) - 15;
                        g2d.fillOval(px, py, 3, 3);
                    }
                }
            }
        };

        violinDrawingPanel.setPreferredSize(new Dimension(800, 700));
        violinDrawingPanel.setBackground(new Color(25, 25, 35));

        violinContainerPanel.add(violinDrawingPanel, BorderLayout.CENTER);
    }

    private void createControlPanel() {
        controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(new Color(45, 45, 55));
        controlPanel.setPreferredSize(new Dimension(280, 0));
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 100), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        // Title
        JLabel titleLabel = new JLabel("CONTROLS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(220, 220, 240));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(titleLabel);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Volume control
        controlPanel.add(createKnobPanel("Volume", volumeSlider = new JSlider(0, 100, 40),
                volumeValueLabel = new JLabel("40%")));
        volumeSlider.addChangeListener(e -> {
            int val = volumeSlider.getValue();
            volumeValueLabel.setText(val + "%");
            soundEngine.setVolume(val / 100.0);
        });

        controlPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Sustain control
        controlPanel.add(createKnobPanel("Sustain", sustainKnob = new JSlider(0, 100, 0),
                sustainValueLabel = new JLabel("0%")));
        sustainKnob.addChangeListener(e -> {
            int val = sustainKnob.getValue();
            sustainValueLabel.setText(val + "%");
            soundEngine.setSustain(val / 100.0);
        });

        controlPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Reverb control
        controlPanel.add(createKnobPanel("Reverb", reverbKnob = new JSlider(0, 100, 0),
                reverbValueLabel = new JLabel("0%")));
        reverbKnob.addChangeListener(e -> {
            int val = reverbKnob.getValue();
            reverbValueLabel.setText(val + "%");
            soundEngine.setReverb(val / 100.0);
        });

        controlPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Mystery (Bitcrush) control
        controlPanel.add(createKnobPanel("Mystery (Bitcrush)", mysteryKnob = new JSlider(0, 100, 0),
                mysteryValueLabel = new JLabel("0%")));
        mysteryKnob.addChangeListener(e -> {
            int val = mysteryKnob.getValue();
            mysteryValueLabel.setText(val + "%");
            soundEngine.setBitcrush(val / 100.0);
        });

        controlPanel.add(Box.createRigidArea(new Dimension(0, 25)));

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(80, 80, 100));
        controlPanel.add(sep);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Panic button
        JButton panicButton = new JButton("PANIC");
        panicButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panicButton.setBackground(new Color(180, 60, 50));
        panicButton.setForeground(Color.WHITE);
        panicButton.setFocusPainted(false);
        panicButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        panicButton.addActionListener(e -> {
            soundEngine.stopAllNotes();
            isPlaying = false;
            activeString = -1;
            statusLabel.setText("Panic button pressed - all sounds stopped");
            statusLabel.setForeground(new Color(255, 150, 150));
            Timer timer = new Timer(2000, ev -> statusLabel.setForeground(new Color(180, 180, 200)));
            timer.setRepeats(false);
            timer.start();
        });
        controlPanel.add(panicButton);
    }

    private JPanel createKnobPanel(String label, JSlider slider, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBackground(new Color(45, 45, 55));

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        nameLabel.setForeground(new Color(200, 200, 220));

        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setPaintTrack(true);
        slider.setBackground(new Color(45, 45, 55));
        slider.setForeground(new Color(150, 150, 180));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        valueLabel.setForeground(new Color(100, 180, 200));
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        panel.add(nameLabel, BorderLayout.WEST);
        panel.add(valueLabel, BorderLayout.EAST);
        panel.add(slider, BorderLayout.SOUTH);

        return panel;
    }

    private void createTuningSection() {
        tuningPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        tuningPanel.setBackground(new Color(40, 40, 50));
        tuningPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(80, 80, 100)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        for (int i = 0; i < 4; i++) {
            JPanel stringPanel = new JPanel(new BorderLayout(5, 5));
            stringPanel.setBackground(new Color(40, 40, 50));
            stringPanel.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 90), 1));

            JLabel nameLabel = new JLabel(stringNames[i]);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            nameLabel.setForeground(stringColors[i]);
            nameLabel.setHorizontalAlignment(SwingConstants.CENTER);

            JSlider tuneSlider = new JSlider(-100, 100, 0);
            tuneSlider.setMajorTickSpacing(50);
            tuneSlider.setMinorTickSpacing(10);
            tuneSlider.setPaintTicks(true);
            tuneSlider.setPaintLabels(true);
            tuneSlider.setBackground(new Color(40, 40, 50));

            Hashtable<Integer, JLabel> labels = new Hashtable<>();
            labels.put(-100, new JLabel("-100¢"));
            labels.put(0, new JLabel("0"));
            labels.put(100, new JLabel("+100¢"));
            tuneSlider.setLabelTable(labels);
            tuneSlider.setPaintLabels(true);

            JLabel valueLabel = new JLabel("0 cents");
            valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            valueLabel.setForeground(new Color(150, 150, 170));
            valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

            final int stringIndex = i;
            tuneSlider.addChangeListener(e -> {
                int val = tuneSlider.getValue();
                tuningOffsets[stringIndex] = val / 100.0;
                soundEngine.setStringTuning(stringIndex, tuningOffsets[stringIndex]);
                String sign = val > 0 ? "+" : "";
                valueLabel.setText(sign + val + "¢");
                nameLabel.setForeground(val == 0 ? stringColors[stringIndex] : new Color(255, 180, 100));
            });

            stringPanel.add(nameLabel, BorderLayout.NORTH);
            stringPanel.add(tuneSlider, BorderLayout.CENTER);
            stringPanel.add(valueLabel, BorderLayout.SOUTH);
            tuningPanel.add(stringPanel);
            tuningSliders[i] = tuneSlider;
        }
    }

    private void createStatusBar() {
        statusPanel = new JPanel(new BorderLayout(10, 0));
        statusPanel.setBackground(new Color(30, 30, 40));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        statusLabel = new JLabel("Ready - Move mouse over strings to play");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(180, 180, 200));

        speedValueLabel = new JLabel("Bow speed: 0 px/s");
        speedValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        speedValueLabel.setForeground(new Color(100, 180, 200));

        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(speedValueLabel, BorderLayout.EAST);
    }

    private int getStringFromX(int x, int[] stringX) {
        if (stringX == null)
            return -1;
        for (int i = 0; i < stringX.length; i++) {
            if (Math.abs(x - stringX[i]) < 25) {
                return i;
            }
        }
        return -1;
    }

    private void setupMouseTrackerListener() {
        mouseTracker.setListener(new MouseTracker.MouseListener() {
            public void onMouseMove(Point position, double speed) {
                currentSpeed = speed;
                currentMousePos = position;
                speedValueLabel.setText(String.format("Bow speed: %.0f px/s", speed));

                // Convert to panel coordinates
                if (violinContainerPanel != null && violinContainerPanel.getComponent(0) != null) {
                    Point panelPos = new Point(position);
                    SwingUtilities.convertPointFromScreen(panelPos, violinContainerPanel.getComponent(0));

                    // Estimate string from X position
                    int centerX = violinContainerPanel.getComponent(0).getWidth() / 2;
                    int[] stringX = { centerX - 30, centerX - 10, centerX + 10, centerX + 30 };
                    activeString = getStringFromX(panelPos.x, stringX);

                    if (activeString >= 0 && speed > 30) {
                        double velocity = Math.min(1.0, speed / 800.0);
                        soundEngine.playNote(activeString, 0, velocity);
                        isPlaying = true;
                        statusLabel.setText("Playing string " + (activeString + 1) + " | Speed: "
                                + String.format("%.0f", speed) + " px/s");
                        statusLabel.setForeground(new Color(150, 220, 150));
                    } else if (speed < 30) {
                        isPlaying = false;
                        statusLabel.setText("Move faster to play - minimum speed 30 px/s");
                        statusLabel.setForeground(new Color(180, 180, 200));
                    }
                }

                violinContainerPanel.getComponent(0).repaint();
            }

            public void onMouseStill(Point position) {
                currentSpeed = 0;
                isPlaying = false;
                activeString = -1;
                speedValueLabel.setText("Bow speed: 0 px/s");
                statusLabel.setText("Mouse still - move mouse across strings");
                statusLabel.setForeground(new Color(180, 180, 200));
                violinContainerPanel.getComponent(0).repaint();
            }
        });
        mouseTracker.startTracking();
    }

    private void startMouseTracking() {
        // Already handled by setupMouseTrackerListener
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (mouseTracker != null)
                mouseTracker.shutdown();
            if (soundEngine != null)
                soundEngine.close();
        }));
    }
}