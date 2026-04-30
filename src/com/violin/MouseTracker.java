import java.awt.Point;
import java.awt.MouseInfo;
import java.util.Scanner;

public class MouseTracker {
    
    //shared flag to control if tracking is active, runs over all thread bc of volatile
    static volatile boolean running = false;

    public MouseTracker() {
        // default constructor for ViolinGUI integration
    }
    
    public static void main(String[] args) throws InterruptedException {
        
        //lines 13 and 14 are ai
        long lastTime;
        lastTime = System.nanoTime();
        //handles user input, outside of main loop
        Thread inputThread = new Thread(() -> {
            Scanner sc = new Scanner(System.in);

            while (true) {

                //pauses when you press enter
                String input = sc.nextLine();

                //stops the program when you type exit
                if (input.equalsIgnoreCase("exit")) {
                    System.out.println("EXIT");
                    System.exit(0);
                }

                //ts runs when you press enter, acts like a switch
                running = !running;

                //read it dawg, started or stopped
                System.out.println(running ? "Started" : "Stopped");
            }
        });

        //the "Daemon" thread stops when main thread ends, just stops it from constantly running after shutdown
        inputThread.setDaemon(true);
        inputThread.start();

        //deadass just gets the mouse position at the start
        Point last = MouseInfo.getPointerInfo().getLocation();

        while (true) {

            //only works if running is true, so you can switch on/off
            if (running) {
                Point current = MouseInfo.getPointerInfo().getLocation();
                
                //if you wanna display the mouse coordinates uncomment the line
                //System.out.println(current);
                
                //this compares the current position with the last
                if (!current.equals(last)) {
                    //lines 57, 58, 59, 60 are ai
                    long now = System.nanoTime();
                    double speed = last.distance(current) / ((now - lastTime) / 1_000_000_000.0);
                    System.out.printf("Mouse speed: %.2f px/s%n", speed);
                    lastTime = now;

                } else {
                    System.out.println("Mouse is still");
                }

                //update the position of the mouse 
                last = current;

                //delay to use less CPU
                Thread.sleep(100);
            }
        }
    }
}
