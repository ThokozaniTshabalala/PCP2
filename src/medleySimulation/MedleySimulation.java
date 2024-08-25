//M. M. Kuttel 2024 mkuttel@gmail.com
// MedleySimulation main class, starts all threads
package medleySimulation;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MedleySimulation {
	static final int numTeams = 10;

	static int frameX = 300; // frame width
	static int frameY = 600;  // frame height
	static int yLimit = 400;
	static int max = 5;

	static int gridX = 50; // number of x grid points
	static int gridY = 120; // number of y grid points

	static SwimTeam[] teams; // array for team threads
	static PeopleLocation[] peopleLocations;  // array to keep track of where people are
	static StadiumView stadiumView; // threaded panel to display stadium
	static StadiumGrid stadiumGrid; // stadium on a discrete grid

	static FinishCounter finishLine; // records who won
	static CounterDisplay counterDisplay; // threaded display of counter

	// Method to setup all the elements of the GUI
	public static void setupGUI(int frameX, int frameY) {
		// Frame initialize and dimensions
		JFrame frame = new JFrame("Swim medley relay animation");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(frameX, frameY);

		JPanel g = new JPanel();
		g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
		g.setSize(frameX, frameY);

		stadiumView = new StadiumView(peopleLocations, stadiumGrid);
		stadiumView.setSize(frameX, frameY);
		g.add(stadiumView);

		// Add text labels to the panel - this can be extended
		JPanel txt = new JPanel();
		txt.setLayout(new BoxLayout(txt, BoxLayout.LINE_AXIS));
		JLabel winner = new JLabel("");
		txt.add(winner);
		g.add(txt);

		counterDisplay = new CounterDisplay(winner, finishLine); // thread to update score

		// Add start and exit buttons
		JPanel b = new JPanel();
		b.setLayout(new BoxLayout(b, BoxLayout.LINE_AXIS));

		JButton startB = new JButton("Start");
		// Add the listener to the JButton to handle the "pressed" event
		startB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Start the viewer thread
				Thread view = new Thread(stadiumView);
				view.start();

				// Start the counter thread for updating results
				Thread results = new Thread(counterDisplay);
				results.start();

				// Start the teams, which in turn start the swimmers
				for (int i = 0; i < numTeams; i++) {
					teams[i].start();
				}

				// Disable the start button to prevent multiple clicks
				startB.setEnabled(false);
			}
		});

		JButton endB = new JButton("Quit");
		// Add the listener to the JButton to handle the "pressed" event
		endB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		b.add(startB);
		b.add(endB);
		g.add(b);

		frame.setLocationRelativeTo(null); // Center window on screen.
		frame.add(g); // Add contents to window
		frame.setContentPane(g);
		frame.setVisible(true);
	}

	// Main method - starts it all
	public static void main(String[] args) throws InterruptedException {
		finishLine = new FinishCounter(); // Counters for people inside and outside club

		stadiumGrid = new StadiumGrid(gridX, gridY, numTeams, finishLine); // Setup stadium with size
		SwimTeam.stadium = stadiumGrid; // Grid shared with class
		Swimmer.stadium = stadiumGrid; // Grid shared with class
		peopleLocations = new PeopleLocation[numTeams * SwimTeam.sizeOfTeam]; // Four swimmers per team
		teams = new SwimTeam[numTeams];
		for (int i = 0; i < numTeams; i++) {
			teams[i] = new SwimTeam(i, finishLine, peopleLocations);
		}
		setupGUI(frameX, frameY); // Start Panel thread - for drawing animation
	}
}
