//M. M. Kuttel 2024 mkuttel@gmail.com
//Class to represent a swim team - which has four swimmers
package medleySimulation;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import medleySimulation.Swimmer.SwimStroke;

public class SwimTeam extends Thread {

    public static StadiumGrid stadium; // shared
    private Swimmer[] swimmers;
    private int teamNo; // team number

    public static final int sizeOfTeam = 4;
    private CountDownLatch[] strokeLatches;
    private CountDownLatch[] swimLatches;
    private CountDownLatch swimLatch;

    public SwimTeam(int ID, FinishCounter finish, PeopleLocation[] locArr) {
        this.teamNo = ID;

        swimmers = new Swimmer[sizeOfTeam];
        SwimStroke[] strokes = SwimStroke.values(); // Get all enum constants
        stadium.returnStartingBlock(ID);

        // Initialize CountDownLatch arrays
        strokeLatches = new CountDownLatch[SwimStroke.values().length];
        swimLatches = new CountDownLatch[SwimStroke.values().length];
        swimLatch = new CountDownLatch(1); // Initial latch for the final swimmer

        for (SwimStroke stroke : SwimStroke.values()) {
            int swimmersForStroke = sizeOfTeam / SwimStroke.values().length;
            strokeLatches[stroke.ordinal()] = new CountDownLatch(swimmersForStroke);
            swimLatches[stroke.ordinal()] = new CountDownLatch(swimmersForStroke);
        }

        for (int i = teamNo * sizeOfTeam, s = 0; i < ((teamNo + 1) * sizeOfTeam); i++, s++) {
            locArr[i] = new PeopleLocation(i, strokes[s].getColour());
            int speed = (int) (Math.random() * (3) + 30); // range of speeds
            swimmers[s] = new Swimmer(i, teamNo, locArr[i], finish, speed, strokes[s], strokeLatches, swimLatches, swimLatch);
        }
    }

    @Override
    public void run() {
        try {
            // Sort the array from backstroke to freestyle
            Arrays.sort(swimmers, (a, b) -> a.getSwimStroke().compareTo(b.getSwimStroke()));

            // Start swimmer threads
            for (Swimmer swimmer : swimmers) {
                swimmer.start();
            }

            // Wait for all swimmers to finish
            for (Swimmer swimmer : swimmers) {
                swimmer.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
