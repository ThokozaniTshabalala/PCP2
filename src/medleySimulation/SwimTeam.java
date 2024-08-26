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
    private CountDownLatch[] swimLatches; // Added for swim race synchronization

    SwimTeam(int ID, FinishCounter finish, PeopleLocation[] locArr) {
        this.teamNo = ID;

        swimmers = new Swimmer[sizeOfTeam];
        SwimStroke[] strokes = SwimStroke.values(); // Get all enum constants
        stadium.returnStartingBlock(ID);

        // Initialize CountDownLatch arrays
        strokeLatches = new CountDownLatch[SwimStroke.values().length];
        swimLatches = new CountDownLatch[SwimStroke.values().length];
        for (SwimStroke stroke : SwimStroke.values()) {
            strokeLatches[stroke.ordinal()] = new CountDownLatch(sizeOfTeam / SwimStroke.values().length);
            swimLatches[stroke.ordinal()] = new CountDownLatch(sizeOfTeam / SwimStroke.values().length);
        }
		 

        for (int i = teamNo * sizeOfTeam, s = 0; i < ((teamNo + 1) * sizeOfTeam); i++, s++) { // initialize swimmers in team
            locArr[i] = new PeopleLocation(i, strokes[s].getColour());
            int speed = (int) (Math.random() * (3) + 30); // range of speeds
            swimmers[s] = new Swimmer(i, teamNo, locArr[i], finish, speed, strokes[s], strokeLatches, swimLatches);
        }
    }

    public void run() {
        try {
            // Sort the array from backstroke to freestyle
            Arrays.sort(swimmers, (a, b) -> a.getSwimStroke().compareTo(b.getSwimStroke()));

            // Print sorted swimmers
            /* 
            System.out.println("Sorted Swimmers in team " + this.teamNo + " are: ");
            for (Swimmer swimmer : swimmers) {
                System.out.println("In team " + this.teamNo + " " + swimmer);
            }*/

            for (Swimmer swimmer : swimmers) { // start swimmer threads
                swimmer.start();
            }

            for (Swimmer swimmer : swimmers) {
                swimmer.join(); // wait for all swimmers to finish
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

	

