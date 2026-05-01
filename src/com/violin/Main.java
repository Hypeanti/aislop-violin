package com.violin;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Robot;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;


class MouseCalc {
    public static int[] getPosition(PointerInfo mouse) {
        int[] pos = new int[2];
        pos[0] = mouse.getLocation().x;
        pos[1] = mouse.getLocation().y;

        return pos;
    }

    public static double getSpeed(PointerInfo mouse) {
        double speed;
        double temp1 = 0;
        double temp2 = 0;
        int[] pos1 = new int[2];
        int[] pos2 = new int[2];
        double time1 = System.nanoTime();

        try {
            pos1 = MouseCalc.getPosition(mouse);
            Thread.sleep(1);
            if (MouseCalc.getPosition(mouse) != pos1) {
                pos2 = MouseCalc.getPosition(mouse);
            }
        }
        catch(Exception e) {
            System.out.println(e);
        }

        temp1 = pos1[1] - pos1[0];
        temp2 = pos2[1] - pos2[0];

        double time2 = System.nanoTime() - time1;

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

    public static void play(double velocity) {
        try {
            Synthesizer synth = MidiSystem.getSynthesizer();
            synth.open();
            Instrument[] instruments = synth.getAvailableInstruments();
            MidiChannel[] channels = synth.getChannels();
            MidiChannel channel = channels[0];
            int index = 40;
            channel.programChange(index);

            
            synth.loadInstrument(instruments[index]);
            channel.noteOn(60, 100);
            Thread.sleep(500);
            channel.noteOn(55, 100);
            Thread.sleep(1000);
            channel.noteOff(60);
            Thread.sleep(1000);
            synth.close();
        }
        catch (Exception e) {

        }
    }

}

class Main {
    public static void main(String[] args) {
        try {
            while (true) {
                PointerInfo mouse = MouseInfo.getPointerInfo();

                try {
                    Thread.sleep(100);
                    Music.play(MouseCalc.getSpeed(mouse));
                    System.out.println(MouseCalc.getSpeed(mouse));
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