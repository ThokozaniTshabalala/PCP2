//M. M. Kuttel 2024 mkuttel@gmail.com
//Class to represent a swimmer swimming a race
//Swimmers have one of four possible swim strokes: backstroke, breaststroke, butterfly and freestyle
package medleySimulation;

import java.awt.Color;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class Swimmer extends Thread {

    public static StadiumGrid stadium; // shared
    private FinishCounter finish; // shared

    GridBlock currentBlock;
    private Random rand;
    private int movingSpeed;
    private CountDownLatch[] strokeLatches; 
    private CountDownLatch[] swimLatches; // Added for swim race synchronization

    private PeopleLocation myLocation;
    private int ID; // thread ID
    private int team; // team ID
    private GridBlock start;

    public enum SwimStroke {
        Backstroke(1, 2.5, Color.black),
        Breaststroke(2, 2.1, new Color(255, 102, 0)),
        Butterfly(3, 2.55, Color.magenta),
        Freestyle(4, 2.8, Color.red);

        private final double strokeTime;
        private final int order; // in minutes
        private final Color colour;

        SwimStroke(int order, double sT, Color c) {
            this.strokeTime = sT;
            this.order = order;
            this.colour = c;
        }

        public int getOrder() { return order; }
        public Color getColour() { return colour; }
    }

    private final SwimStroke swimStroke;

    // Constructor
    Swimmer(int ID, int t, PeopleLocation loc, FinishCounter f, int speed, SwimStroke s, CountDownLatch[] strokeLatches, CountDownLatch[] swimLatches) {
        this.swimStroke = s;
        this.ID = ID;
        this.movingSpeed = speed; // range of speeds for swimmers
        this.myLocation = loc;
        this.team = t;
        start = stadium.returnStartingBlock(team);
        finish = f;
        rand = new Random();
        this.strokeLatches = strokeLatches;
        this.swimLatches = swimLatches;
    }

    // getter
    public int getX() { return currentBlock.getX(); }

    // getter
    public int getY() { return currentBlock.getY(); }

    // getter
    public int getSpeed() { return movingSpeed; }

    public SwimStroke getSwimStroke() {
        return swimStroke;
    }

    // Swimmer enters stadium
    public void enterStadium() throws InterruptedException {
        // Wait for previous strokes to enter
        for (SwimStroke stroke : SwimStroke.values()) {
            if (swimStroke.ordinal() > stroke.ordinal()) {
                System.out.println("Swimmer " + this.ID + " from team " + team + " with stroke " + swimStroke + " waiting for stroke " + stroke + " to enter.");
                strokeLatches[stroke.ordinal()].await();
            }
        }
        // Enter the stadium
        currentBlock = stadium.enterStadium(myLocation);
        sleep(200); // wait a bit at the door
        // Signal that this swimmer has entered
        strokeLatches[swimStroke.ordinal()].countDown();
        // Print out information about the swimmer entering the stadium
        System.out.println("Swimmer " + this.ID + " from team " + team + " with stroke " + swimStroke + " has entered the stadium.");
    }

    // Go to the starting blocks
    public void goToStartingBlocks() throws InterruptedException {
        int x_st = start.getX();
        int y_st = start.getY();
        System.out.println("Thread " + this.ID + " target start position: (" + x_st + ", " + y_st + ")");
        System.out.println("Thread " + this.ID + " current position: (" + currentBlock.getX() + ", " + currentBlock.getY() + ")");

        while (currentBlock != start) {
            sleep(movingSpeed * 3); // not rushing 
            currentBlock = stadium.moveTowards(currentBlock, x_st, y_st, myLocation); // head toward starting block
        }
        System.out.println("-----------Thread " + this.ID + " at start " + currentBlock.getX() + " " + currentBlock.getY());
    }

    // Dive into the pool
    private void dive() throws InterruptedException {
        int x = currentBlock.getX();
        int y = currentBlock.getY();
        currentBlock = stadium.jumpTo(currentBlock, x, y - 2, myLocation);
    }

    // Swim the race
    private void swimRace() throws InterruptedException {
        int x = currentBlock.getX();
        while (currentBlock.getY() != 0) {
            currentBlock = stadium.moveTowards(currentBlock, x, 0, myLocation);
            sleep((int) (movingSpeed * swimStroke.strokeTime)); // swim
            System.out.println("Thread " + this.ID + " swimming towards end at position: (" + currentBlock.getX() + ", " + currentBlock.getY() + ") with speed " + movingSpeed);
        }

        while (currentBlock.getY() != (StadiumGrid.start_y - 1)) {
            currentBlock = stadium.moveTowards(currentBlock, x, StadiumGrid.start_y, myLocation);
            sleep((int) (movingSpeed * swimStroke.strokeTime)); // swim
            System.out.println("Thread " + this.ID + " swimming back at position: (" + currentBlock.getX() + ", " + currentBlock.getY() + ") with speed " + movingSpeed);
        }

        // Signal that this swimmer has finished their lap
        swimLatches[swimStroke.ordinal()].countDown();
    }

    // Exit the pool
    public void exitPool() throws InterruptedException {
        int bench = stadium.getMaxY() - swimStroke.getOrder(); // they line up
        int lane = currentBlock.getX() + 1; // slightly offset
        currentBlock = stadium.moveTowards(currentBlock, lane, currentBlock.getY(), myLocation);
        while (currentBlock.getY() != bench) {
            currentBlock = stadium.moveTowards(currentBlock, lane, bench, myLocation);
            sleep(movingSpeed * 3); // not rushing 
        }
    }

    @Override
    public String toString() {
        return "Swimmer " + this.ID + " with stroke " + swimStroke;
    }

    public void run() {
        try {
            // Swimmer arrives
            sleep(movingSpeed + (rand.nextInt(10))); // arriving takes a while
            myLocation.setArrived();
            enterStadium();

            goToStartingBlocks();

            dive();

            // Wait for all previous strokes to finish their race
            for (SwimStroke stroke : SwimStroke.values()) {
                if (swimStroke.ordinal() > stroke.ordinal()) {
                    System.out.println("Swimmer " + this.ID + " from team " + team + " with stroke " + swimStroke + " waiting for stroke " + stroke + " to finish their race.");
                    swimLatches[stroke.ordinal()].await();
                }
            }

            swimRace();
            if (swimStroke.order == 4) {
                finish.finishRace(ID, team); // finish line
            } else {
                exitPool(); // if not last swimmer leave pool
            }

        } catch (InterruptedException e1) { // do nothing
        }
    }
}

