package com.violin;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.sound.sampled.*;

public class ViolinGUI extends JFrame {
    
    // Sound engine components
    private SoundEngine soundEngine;
    private BowController bowController;
    private FretboardInput fretboardInput;
    private MouseTracker mouseTracker;
    
    // GUI Components
    private JPanel mainPanel;
    private JPanel violinBodyPanel;
    private JPanel controlPanel;
    private JPanel tuningPanel;
    
    // Knobs and sliders
    private JSlider volumeSlider;
    private JSlider sustainKnob;
    private JSlider reverbKnob;
    private JSlider mysteryKnob;
    
    // Labels for displaying values
    private JLabel volumeLabel;
    private JLabel sustainLabel;
    private JLabel reverbLabel;
    private JLabel mysteryLabel;
    private JLabel speedLabel;
    
    // Tuning controls
    private JSlider[] tuningSliders = new JSlider[4];
    private String[] stringNames = {"String 1 (G)", "String 2 (D)", "String 3 (A)", "String 4 (E)"};
    private double[] tuningOffsets = new double[4];
    
    // Mouse speed tracking
    private Timer speedTimer;
    private double currentSpeed = 0;
    
    public ViolinGUI() {
        initializeSoundEngine();
        setupGUI();
        startMouseTracking();
    }
    
    private void initializeSoundEngine() {
        soundEngine = new SoundEngine();
        bowController = new BowController(soundEngine);
        fretboardInput = new FretboardInput(soundEngine, bowController);
        mouseTracker = new MouseTracker();
        
        // Initialize tuning offsets (horribly out of tune by default)
        for (int i = 0; i < 4; i++) {
            tuningOffsets[i] = 0;
        }
    }
    
    private void setupGUI() {
        setTitle("🎻 HORRIBLE VIOLIN - Your Ears Will Hate You 🎻");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Create main panel with horrible lime green background
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(50, 100, 50)); // Puke green
        
        // Create the three main sections
        createViolinBody();
        createControlPanel();
        createTuningPanel();
        
        // Add everything to the main panel
        mainPanel.add(violinBodyPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.EAST);
        mainPanel.add(tuningPanel, BorderLayout.NORTH);
        
        add(mainPanel);
        
        // Set window size
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        // Make focusable for keyboard input
        mainPanel.setFocusable(true);
        mainPanel.requestFocusInWindow();
        fretboardInput.attachToComponent(mainPanel);
        bowController.startTracking(mainPanel);
        
        // Add instructions
        createInstructionsDialog();
    }
    
    private void createViolinBody() {
        violinBodyPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                
                // Draw horrible violin body
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Violin body (brown blob)
                g2d.setColor(new Color(139, 69, 19));
                g2d.fillRoundRect(200, 150, 400, 600, 100, 100);
                
                // Inner circle (f-holes approximation)
                g2d.setColor(new Color(80, 40, 10));
                g2d.fillRoundRect(250, 250, 80, 200, 40, 40);
                g2d.fillRoundRect(470, 250, 80, 200, 40, 40);
                
                // Draw strings
                g2d.setColor(Color.WHITE);
                int[] stringX = {300, 350, 450, 500};
                for (int x : stringX) {
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawLine(x, 100, x, 650);
                }
                
                // Draw bridge
                g2d.setColor(new Color(101, 67, 33));
                g2d.fillRect(350, 500, 100, 20);
                
                // Draw bow indicator (mouse position)
                Point mousePos = getMousePosition();
                if (mousePos != null && mousePos.x > 0 && mousePos.y > 0) {
                    g2d.setColor(new Color(255, 0, 0, 100));
                    g2d.setStroke(new BasicStroke(5));
                    g2d.drawLine(mousePos.x - 20, mousePos.y, mousePos.x + 20, mousePos.y);
                    g2d.drawLine(mousePos.x, mousePos.y - 20, mousePos.x, mousePos.y + 20);
                    
                    // Draw speed effect
                    int speedSize = (int)(currentSpeed * 50) + 10;
                    g2d.setColor(new Color(255, 100, 0, 50));
                    g2d.fillOval(mousePos.x - speedSize/2, mousePos.y - speedSize/2, speedSize, speedSize);
                }
                
                // Draw fret markers
                g2d.setColor(Color.WHITE);
                for (int i = 1; i <= 12; i++) {
                    int y = 120 + (i * 40);
                    if (i == 3 || i == 5 || i == 7 || i == 9 || i == 12) {
                        g2d.fillOval(395, y - 5, 10, 10);
                    }
                }
            }
        };
        
        violinBodyPanel.setPreferredSize(new Dimension(800, 700));
        violinBodyPanel.setBackground(new Color(255, 228, 196));
        
        // Add mouse motion for repaint and speed tracking
        violinBodyPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateBowPosition(e.getX(), e.getY());
                violinBodyPanel.repaint();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                updateBowPosition(e.getX(), e.getY());
                violinBodyPanel.repaint();
            }
        });
    }
    
    private void updateBowPosition(int x, int y) {
        // Speed is tracked in the timer
        // This just triggers sounds based on string proximity
        int stringIndex = getStringFromX(x);
        if (stringIndex >= 0 && currentSpeed > 0.05) {
            double velocity = Math.min(1.0, currentSpeed / 1000.0);
            soundEngine.playNote(stringIndex, 0, velocity);
        }
    }
    
    private int getStringFromX(int x) {
        if (x >= 280 && x < 330) return 0;
        if (x >= 330 && x < 380) return 1;
        if (x >= 420 && x < 470) return 2;
        if (x >= 470 && x < 520) return 3;
        return -1;
    }
    
    private void createControlPanel() {
        controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(new Color(50, 50, 50));
        controlPanel.setPreferredSize(new Dimension(300, 700));
        controlPanel.setBorder(BorderFactory.createTitledBorder("🎛️ HORRIBLE CONTROLS"));
        
        // Volume Fader
        volumeLabel = new JLabel("🔊 VOLUME: 50%");
        volumeLabel.setForeground(Color.WHITE);
        volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.addChangeListener(e -> {
            int vol = volumeSlider.getValue();
            volumeLabel.setText("🔊 VOLUME: " + vol + "%");
            soundEngine.setVolume(vol / 100.0);
        });
        
        // Sustain Knob
        sustainLabel = new JLabel("🎵 SUSTAIN: 0%");
        sustainLabel.setForeground(Color.WHITE);
        sustainKnob = new JSlider(0, 100, 0);
        sustainKnob.addChangeListener(e -> {
            int sus = sustainKnob.getValue();
            sustainLabel.setText("🎵 SUSTAIN: " + sus + "%");
            soundEngine.setSustain(sus / 100.0);
        });
        
        // Reverb Knob
        reverbLabel = new JLabel("🌀 REVERB: 0%");
        reverbLabel.setForeground(Color.WHITE);
        reverbKnob = new JSlider(0, 100, 0);
        reverbKnob.addChangeListener(e -> {
            int rev = reverbKnob.getValue();
            reverbLabel.setText("🌀 REVERB: " + rev + "%");
            soundEngine.setReverb(rev / 100.0);
        });
        
        // Mystery Knob (Bitcrush)
        mysteryLabel = new JLabel("❓ MYSTERY: 0% (Bitcrush)");
        mysteryLabel.setForeground(Color.WHITE);
        mysteryKnob = new JSlider(0, 100, 0);
        mysteryKnob.addChangeListener(e -> {
            int mys = mysteryKnob.getValue();
            mysteryLabel.setText("❓ MYSTERY: " + mys + "% (Bitcrush)");
            soundEngine.setBitcrush(mys / 100.0);
        });
        
        // Speed display
        speedLabel = new JLabel("🏹 BOW SPEED: 0 px/s");
        speedLabel.setForeground(Color.WHITE);
        speedLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        
        // Add everything to panel
        controlPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        controlPanel.add(volumeLabel);
        controlPanel.add(volumeSlider);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        controlPanel.add(sustainLabel);
        controlPanel.add(sustainKnob);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        controlPanel.add(reverbLabel);
        controlPanel.add(reverbKnob);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        controlPanel.add(mysteryLabel);
        controlPanel.add(mysteryKnob);
        controlPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        controlPanel.add(speedLabel);
        
        // Add horrible button that does nothing useful
        JButton panicButton = new JButton("🔥 PANIC BUTTON 🔥");
        panicButton.setBackground(Color.RED);
        panicButton.addActionListener(e -> {
            soundEngine.stopAllNotes();
            JOptionPane.showMessageDialog(this, "You panicked! All sounds stopped.\n(For now...)");
        });
        controlPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        controlPanel.add(panicButton);
    }
    
    private void createTuningPanel() {
        tuningPanel = new JPanel(new GridLayout(1, 4));
        tuningPanel.setBackground(new Color(80, 60, 40));
        tuningPanel.setBorder(BorderFactory.createTitledBorder("🎵 TUNE YOUR STRINGS (Out of tune encouraged)"));
        
        for (int i = 0; i < 4; i++) {
            JPanel stringPanel = new JPanel(new BorderLayout());
            stringPanel.setBackground(new Color(100, 70, 50));
            
            JLabel stringLabel = new JLabel(stringNames[i]);
            stringLabel.setForeground(Color.WHITE);
            stringLabel.setHorizontalAlignment(SwingConstants.CENTER);
            
            JSlider tuneSlider = new JSlider(-50, 50, 0);
            tuneSlider.setMajorTickSpacing(25);
            tuneSlider.setPaintTicks(true);
            tuneSlider.setPaintLabels(true);
            
            final int stringIndex = i;
            tuneSlider.addChangeListener(e -> {
                tuningOffsets[stringIndex] = tuneSlider.getValue() / 100.0;
                soundEngine.setStringTuning(stringIndex, tuningOffsets[stringIndex]);
                stringLabel.setText(stringNames[stringIndex] + " " + (tuneSlider.getValue() > 0 ? "+" : "") + tuneSlider.getValue() + "¢");
            });
            
            tuningSliders[i] = tuneSlider;
            stringPanel.add(stringLabel, BorderLayout.NORTH);
            stringPanel.add(tuneSlider, BorderLayout.CENTER);
            tuningPanel.add(stringPanel);
        }
    }
    
    private void startMouseTracking() {
        speedTimer = new Timer(50, e -> {
            // Get current speed from mouse movement
            Point current = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(current, violinBodyPanel);
            if (current.x > 0 && current.x < violinBodyPanel.getWidth() && 
                current.y > 0 && current.y < violinBodyPanel.getHeight()) {
                // Speed would be calculated by a separate thread
                // For now, use a simulated value
                currentSpeed = Math.random() * 1000; // Temporary
                speedLabel.setText("🏹 BOW SPEED: " + String.format("%.0f", currentSpeed) + " px/s");
                violinBodyPanel.repaint();
            }
        });
        speedTimer.start();
    }
    
    private void createInstructionsDialog() {
        String instructions = 
            "🎻 HORRIBLE VIOLIN INSTRUCTIONS 🎻\n\n" +
            "BOW CONTROL:\n" +
            "- Move mouse over the violin body to play!\n" +
            "- Faster mouse = LOUDER sound\n" +
            "- Mouse X position determines which string\n\n" +
            "FRETBOARD (Keyboard):\n" +
            "Row 1 (String 1): 1 2 3 4 5 6 7 8 9 0 - =\n" +
            "Row 2 (String 2): Q W E R T Y U I O P [ ]\n" +
            "Row 3 (String 3): A S D F G H J K L ; ' #\n" +
            "Row 4 (String 4): Z X C V B N M , . /\n\n" +
            "MOUSE BUTTONS:\n" +
            "- LMB + movement = String 2\n" +
            "- MWB + movement = String 3\n" +
            "- RMB + movement = String 4\n\n" +
            "KNOBS DO HORRIBLE THINGS:\n" +
            "- Volume: Makes it louder or quieter\n" +
            "- Sustain: Notes overlap and become muddy\n" +
            "- Reverb: ECHO ECHO ECHO\n" +
            "- Mystery: Bitcrush for digital destruction!\n\n" +
            "Tuning: Intentionally detune strings for maximum horror!\n\n" +
            "🔥 PANIC BUTTON: Stops all sounds temporarily 🔥\n\n" +
            "Remember: If it sounds good, you're doing it wrong!";
        
        JOptionPane.showMessageDialog(this, instructions, "How to Destroy Your Ears", 
                                      JOptionPane.INFORMATION_MESSAGE);
    }
}
