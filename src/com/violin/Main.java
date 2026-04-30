package com.violin;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Set look and feel to system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Launch the horrible violin
        SwingUtilities.invokeLater(() -> {
            ViolinGUI violin = new ViolinGUI();
            violin.setVisible(true);
            
            System.out.println("🎻 HORRIBLE VIOLIN ACTIVATED! 🎻");
            System.out.println("Your ears will never recover!");
            System.out.println("Move your mouse, press keys, twist knobs!");
            System.out.println("The more horrible it sounds, the better!");
        });
    }
}
