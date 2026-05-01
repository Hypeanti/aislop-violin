package com.violin;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Robot;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;


class MouseCalc {
    public static int[] getPosition(PointerInfo mouse) {
        int[] pos = new int[2];
        pos[0] = mouse.getLocation().x;
        pos[1] = mouse.getLocation().y;

        return pos;
    }

    public static double getSpeed(PointerInfo mouse) {
        PointerInfo mouse2 = MouseInfo.getPointerInfo();
        double speed;
        double temp1 = 0;
        double temp2 = 0;
        int[] pos1 = new int[2];
        int[] pos2 = new int[2];
        double time1 = System.nanoTime();

        try {
            pos1 = MouseCalc.getPosition(mouse);
            Thread.sleep(20);
            //if (MouseCalc.getPosition(mouse) != pos1) {
                pos2 = MouseCalc.getPosition(mouse2);
            //}
        }
        catch(Exception e) {
            System.out.println(e);
        }

        temp1 = pos2[0] - pos1[0];
        temp2 = pos2[1] - pos1[1];

        double time2 = (System.nanoTime() - time1)/1000000000.0;

        speed = (Math.sqrt((temp1 * temp1) + (temp2 * temp2)))/time2;
        return speed;
    }

    public static int getColor(PointerInfo mouse) {
        int[] pos = MouseCalc.getPosition(mouse);
        int rgba = 0;

        try {
            Robot newRobot = new Robot();
            rgba = newRobot.getPixelColor(pos[0], pos[1]).getRGB();
        }
        catch (Exception e) {
            System.out.println(e);
        }

        return rgba;
    }

}

class Music {

    public static void play(double velocity, int note) {
        int rand1 = (int)(Math.random() * 100);
        int rand2 = (int)(Math.random() * 1000) + 100;
        int rand3 = (int)(Math.random() * 10) - 1;
        int rand4 = (int)(Math.random() * 500) + 250;

        try {
            Synthesizer synth = MidiSystem.getSynthesizer();
            synth.open();
            Instrument[] instruments = synth.getAvailableInstruments();
            MidiChannel[] channels = synth.getChannels();
            MidiChannel channel = channels[0];
            int index = 40;
            channel.programChange(index);

            
            synth.loadInstrument(instruments[index]);
            channel.setPitchBend(8192 + (int)(velocity * 50));
            channel.noteOn(note, (int) velocity);
            Thread.sleep(rand4);
            channel.noteOn(note-5, (int) velocity);
            Thread.sleep(rand2);
            channel.noteOff(60);
            Thread.sleep(rand2);
            synth.close();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

}

class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ViolinFrame());
        
        try {
            while (true) {
                PointerInfo mouse = MouseInfo.getPointerInfo();

                try {
                    Thread.sleep(1);
                    Music.play(MouseCalc.getSpeed(mouse),60);
                }
                catch(Exception e) {
                    System.out.print(e);
                }
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}

class ViolinFrame extends JFrame {
    private ViolinPanel violinPanel;
    private ControlPanel controlPanel;
    private StatsPanel statsPanel;
    
    public ViolinFrame() {
        setTitle("🎻 HORRIBLE VIOLIN 🎻");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        violinPanel = new ViolinPanel();
        controlPanel = new ControlPanel(violinPanel);
        statsPanel = new StatsPanel(violinPanel);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(violinPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.EAST);
        mainPanel.add(statsPanel, BorderLayout.WEST);
        
        add(mainPanel);
        setSize(1400, 750);
        setLocationRelativeTo(null);
        setVisible(true);
        
        violinPanel.startAnimation();
    }
}

class ViolinPanel extends JPanel {
    private Point bowPos = new Point(400, 400);
    private Point[] bowTrail = new Point[30];
    private int trailIndex = 0;
    private double bowVelocity = 0;
    private int[] stringActivity = new int[4];
    
    private Timer animationTimer;
    
    private int[] stringX = {350, 400, 500, 550};
    private int stringStartY = 120;
    private int stringEndY = 650;
    
    public ViolinPanel() {
        setBackground(new Color(240, 220, 200));
        setPreferredSize(new Dimension(1000, 750));
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateBow(e.getPoint());
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                updateBow(e.getPoint());
            }
        });
    }
    
    private void updateBow(Point newPos) {
        double distance = bowPos.distance(newPos);
        bowVelocity = Math.min(distance, 50);
        
        bowPos = newPos;
        bowTrail[trailIndex] = new Point(newPos);
        trailIndex = (trailIndex + 1) % bowTrail.length;
        
        int stringIndex = getStringIndex(newPos.x);
        if (stringIndex >= 0 && bowVelocity > 1) {
            stringActivity[stringIndex] = 10;
        }
    }
    
    private int getStringIndex(int x) {
        if (x >= 330 && x < 380) return 0;
        if (x >= 380 && x < 430) return 1;
        if (x >= 470 && x < 520) return 2;
        if (x >= 520 && x < 570) return 3;
        return -1;
    }
    
    public void startAnimation() {
        animationTimer = new Timer(30, e -> {
            for (int i = 0; i < 4; i++) {
                stringActivity[i] = Math.max(0, stringActivity[i] - 1);
            }
            repaint();
        });
        animationTimer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        drawViolinBody(g2d);
        drawStrings(g2d);
        drawBow(g2d);
        drawDecorations(g2d);
    }
    
    private void drawViolinBody(Graphics2D g2d) {
        g2d.setColor(new Color(160, 82, 45));
        g2d.fillRoundRect(250, 200, 500, 400, 80, 100);
        
        g2d.fillRect(420, 80, 60, 130);
        g2d.fillRoundRect(410, 30, 80, 50, 20, 20);
        
        g2d.setColor(new Color(80, 40, 20));
        g2d.fillRoundRect(320, 320, 40, 120, 15, 20);
        g2d.fillRoundRect(640, 320, 40, 120, 15, 20);
        
        g2d.setColor(new Color(120, 60, 30));
        g2d.fillRect(430, 420, 40, 80);
        
        g2d.setColor(new Color(100, 50, 20));
        g2d.fillRoundRect(350, 600, 300, 30, 10, 10);
    }
    
    private void drawStrings(Graphics2D g2d) {
        for (int i = 0; i < 4; i++) {
            if (stringActivity[i] > 0) {
                g2d.setColor(new Color(255, 200 - stringActivity[i] * 15, 0, 150));
                g2d.setStroke(new BasicStroke(8));
            } else {
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(2));
            }
            g2d.drawLine(stringX[i], stringStartY, stringX[i], stringEndY);
        }
    }
    
    private void drawBow(Graphics2D g2d) {
        for (int i = 0; i < bowTrail.length; i++) {
            if (bowTrail[i] != null) {
                int alpha = (int)(200 * (1.0 - (float)i / bowTrail.length));
                g2d.setColor(new Color(255, 150, 50, alpha));
                g2d.fillOval(bowTrail[i].x - 8, bowTrail[i].y - 8, 16, 16);
            }
        }
        
        g2d.setColor(new Color(255, 0, 0, 200));
        g2d.setStroke(new BasicStroke(6));
        int waveAmount = (int)(bowVelocity * 2);
        g2d.drawLine(bowPos.x - 40 - waveAmount, bowPos.y, bowPos.x + 40 + waveAmount, bowPos.y);
        g2d.drawLine(bowPos.x, bowPos.y - 25, bowPos.x, bowPos.y + 25);
        
        int auraSize = (int)(bowVelocity * 3) + 15;
        g2d.setColor(new Color(255, 100, 0, (int)(100 * (bowVelocity / 50.0))));
        g2d.fillOval(bowPos.x - auraSize/2, bowPos.y - auraSize/2, auraSize, auraSize);
        
        g2d.setColor(new Color(255, 200, 0, 100));
        g2d.setStroke(new BasicStroke(2));
        int circleSize = (int)(bowVelocity * 1.5) + 10;
        g2d.drawOval(bowPos.x - circleSize, bowPos.y - circleSize, circleSize * 2, circleSize * 2);
    }
    
    private void drawDecorations(Graphics2D g2d) {
        g2d.setColor(new Color(200, 150, 100));
        g2d.setFont(new Font("Serif", Font.ITALIC, 16));
        g2d.drawString("TERRIBLE", 380, 450);
        g2d.drawString("VIOLINS CO.", 360, 475);
        
        g2d.setColor(new Color(200, 180, 160));
        g2d.setFont(new Font("Arial", Font.BOLD, 9));
        for (int i = 1; i <= 5; i++) {
            int y = stringStartY + (i * 80);
            g2d.fillOval(465, y - 3, 6, 6);
        }
    }
    
    public double getBowVelocity() {
        return bowVelocity;
    }
}

class ControlPanel extends JPanel {
    private JLabel speedLabel;
    private Timer speedTimer;
    
    public ControlPanel(ViolinPanel violinPanel) {
        setBackground(new Color(50, 50, 50));
        setPreferredSize(new Dimension(250, 750));
        setBorder(BorderFactory.createTitledBorder("🎛️ CONTROLS"));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        add(Box.createVerticalStrut(15));
        
        JLabel volumeLabel = createLabel("🔊 VOLUME: 50%");
        add(volumeLabel);
        JSlider volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.addChangeListener(e -> 
            volumeLabel.setText("🔊 VOLUME: " + volumeSlider.getValue() + "%")
        );
        add(volumeSlider);
        
        add(Box.createVerticalStrut(20));
        
        JLabel sustainLabel = createLabel("🎵 SUSTAIN: 0%");
        add(sustainLabel);
        JSlider sustainSlider = new JSlider(0, 100, 0);
        sustainSlider.addChangeListener(e -> 
            sustainLabel.setText("🎵 SUSTAIN: " + sustainSlider.getValue() + "%")
        );
        add(sustainSlider);
        
        add(Box.createVerticalStrut(20));
        
        JLabel reverbLabel = createLabel("🌀 REVERB: 0%");
        add(reverbLabel);
        JSlider reverbSlider = new JSlider(0, 100, 0);
        reverbSlider.addChangeListener(e -> 
            reverbLabel.setText("🌀 REVERB: " + reverbSlider.getValue() + "%")
        );
        add(reverbSlider);
        
        add(Box.createVerticalStrut(30));
        
        speedLabel = createLabel("🏹 BOW SPEED: 0");
        add(speedLabel);
        
        add(Box.createVerticalGlue());
        
        speedTimer = new Timer(50, e -> 
            speedLabel.setText("🏹 BOW SPEED: " + String.format("%.1f", violinPanel.getBowVelocity()))
        );
        speedTimer.start();
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}

class StatsPanel extends JPanel {
    private int rotationAngle = 0;
    private int badNotesCount = 0;
    
    public StatsPanel(ViolinPanel violinPanel) {
        setBackground(new Color(30, 30, 50));
        setPreferredSize(new Dimension(200, 750));
        setBorder(BorderFactory.createTitledBorder("📊 USELESS STATS"));
        
        Timer animationTimer = new Timer(30, e -> {
            rotationAngle = (rotationAngle + 2) % 360;
            badNotesCount++;
            repaint();
        });
        animationTimer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        
        int centerX = width / 2;
        int centerY = 80;
        g2d.setColor(new Color(100, 150, 200));
        g2d.fillOval(centerX - 30, centerY - 30, 60, 60);
        
        g2d.setColor(new Color(255, 100, 100));
        g2d.setStroke(new BasicStroke(3));
        int x2 = (int)(centerX + 30 * Math.cos(Math.toRadians(rotationAngle)));
        int y2 = (int)(centerY + 30 * Math.sin(Math.toRadians(rotationAngle)));
        g2d.drawLine(centerX, centerY, x2, y2);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.drawString("TONE QUALITY", 15, 160);
        
        int qualityValue = (badNotesCount / 10) % 100;
        g2d.setColor(new Color(50, 200, 50));
        g2d.fillRect(15, 170, qualityValue * 1, 15);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(15, 170, 100, 15);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2d.drawString("BAD NOTES:", 15, 220);
        g2d.setColor(new Color(255, 100, 100));
        g2d.drawString(String.valueOf(badNotesCount), 20, 245);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.drawString("💧 HUMIDITY: " + (50 + (badNotesCount % 40)) + "%", 15, 290);
        
        g2d.setColor(new Color(255, 100, 100));
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("⭐ TERRIBLE", 15, 330);
    }
}