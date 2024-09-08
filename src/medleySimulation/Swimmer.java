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

    private GridBlock currentBlock;
    private Random rand;
    private int movingSpeed;
    private CountDownLatch[] strokeLatches;
    private CountDownLatch[] swimLatches;
    private CountDownLatch swimLatch;

    private PeopleLocation myLocation;
    private int ID; // thread ID
    private int team; // team ID
    private GridBlock start;
    private final SwimStroke swimStroke;

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

    // Constructor
    public Swimmer(int ID, int t, PeopleLocation loc, FinishCounter f, int speed, SwimStroke s,
                   CountDownLatch[] strokeLatches, CountDownLatch[] swimLatches, CountDownLatch swimLatch) {
        this.swimStroke = s;
        this.ID = ID;
        this.movingSpeed = speed;
        this.myLocation = loc;
        this.team = t;
        this.start = stadium.returnStartingBlock(team);
        this.finish = f;
        this.rand = new Random();
        this.strokeLatches = strokeLatches;
        this.swimLatches = swimLatches;
        this.swimLatch = swimLatch;
    }

    // Getter methods
    public int getX() { return currentBlock.getX(); }
    public int getY() { return currentBlock.getY(); }
    public int getSpeed() { return movingSpeed; }
    public SwimStroke getSwimStroke() { return swimStroke; }

    // Swimmer enters stadium
    public void enterStadium() throws InterruptedException {
        for (SwimStroke stroke : SwimStroke.values()) {
            if (swimStroke.ordinal() > stroke.ordinal()) {
                System.out.println("Swimmer " + this.ID + " from team " + team + " with stroke " + swimStroke + " waiting for stroke " + stroke + " to enter.");
                strokeLatches[stroke.ordinal()].await();
            }
        }
        currentBlock = stadium.enterStadium(myLocation);
        sleep(200); // wait a bit at the door
        strokeLatches[swimStroke.ordinal()].countDown();
        System.out.println("Swimmer " + this.ID + " from team " + team + " with stroke " + swimStroke + " has entered the stadium.");
    }

    // Go to the starting blocks
    public void goToStartingBlocks() throws InterruptedException {
        int x_st = start.getX();
        int y_st = start.getY();
        while (currentBlock != start) {
            sleep(movingSpeed * 3);
            currentBlock = stadium.moveTowards(currentBlock, x_st, y_st, myLocation);
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
            sleep((int) (movingSpeed * swimStroke.strokeTime));
        }
        while (currentBlock.getY() != (StadiumGrid.start_y - 1)) {
            currentBlock = stadium.moveTowards(currentBlock, x, StadiumGrid.start_y, myLocation);
            sleep((int) (movingSpeed * swimStroke.strokeTime));
        }
        swimLatches[swimStroke.ordinal()].countDown();
    }

    // Exit the pool
    public void exitPool() throws InterruptedException {
        int bench = stadium.getMaxY() - swimStroke.getOrder();
        int lane = currentBlock.getX() + 1;
        currentBlock = stadium.moveTowards(currentBlock, lane, currentBlock.getY(), myLocation);
        while (currentBlock.getY() != bench) {
            currentBlock = stadium.moveTowards(currentBlock, lane, bench, myLocation);
            sleep(movingSpeed * 3);
        }
    }

    @Override
    public String toString() {
        return "Swimmer " + this.ID + " with stroke " + swimStroke;
    }

    private void waitAccordingToOrder() throws InterruptedException {
        if (swimLatch != null) {
            System.out.println("Swimmer " + ID + " is waiting for the previous swimmer to finish.");
            swimLatch.await();
        }
    }

    private void signalFinish() {
        System.out.println("Swimmer " + ID + " has finished the race.");
        swimLatches[swimStroke.ordinal()].countDown();
    }

    @Override
    public void run() {
        try {
            sleep(movingSpeed + (rand.nextInt(10)));
            /*if (swimStroke.getOrder() != 1) {
                waitAccordingToOrder();
            }*/
            myLocation.setArrived();
            enterStadium();
            goToStartingBlocks();
            dive();
            swimRace();
            if (swimStroke.getOrder() == 4) {
                finish.finishRace(ID, team);
            } else {
                exitPool();
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }
}
