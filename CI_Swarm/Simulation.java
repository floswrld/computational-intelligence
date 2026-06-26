
import java.util.ArrayList;

import javax.swing.*;

public class Simulation extends JFrame {
    static int sleep = 8;
    static double pix = 0.2;
    int anzFz = 160;
    public static int[] displaySize = {1600,700};

    public static double[] foodPos = {100.0,100.0};

    public static double[][] obstacles = {
            {400 * pix, 400 * pix, 60 * pix},
            {1000 * pix, 300 * pix, 80 * pix},
            {800 * pix, 700 * pix, 70 * pix}
    };

    ArrayList<Vehicle> allVehicles = new ArrayList<>();

    Simulation() {
        setTitle("Swarm");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        for (int k = 0; k < anzFz; k++) {
            Vehicle car = new Vehicle();
            if (k == 0) car.type = 1;
            allVehicles.add(car);
        }

        add(new Canvas(allVehicles, pix));
        setSize(displaySize[0], displaySize[1]);
        setVisible(true);

        new Timer(sleep, e -> {
            for (Vehicle v : allVehicles) {
                v.move(allVehicles, foodPos);

                double distToFood = Math.sqrt(Math.pow(v.pos[0] - foodPos[0], 2) + Math.pow(v.pos[1] - foodPos[1], 2));

                double eatRadius = 5.0;
                if (distToFood < eatRadius) {
                    boolean validPosition = false;

                    while (!validPosition) {
                        double newX = 10 + Math.random() * ((displaySize[0]-100) * Simulation.pix - 20);
                        double newY = 10 + Math.random() * ((displaySize[1]-100) * Simulation.pix - 20);

                        validPosition = true;

                        for (int i = 0; i < obstacles.length; i++) {
                            double obsX = obstacles[i][0];
                            double obsY = obstacles[i][1];
                            double obsR = obstacles[i][2];

                            double distToObs = Math.sqrt(Math.pow(newX - obsX, 2) + Math.pow(newY - obsY, 2));

                            if (distToObs < obsR + 5.0) {
                                validPosition = false;
                                break;
                            }
                        }

                        if (validPosition) {
                            foodPos[0] = newX;
                            foodPos[1] = newY;
                        }
                    }
                }
            }
            repaint();
        }).start();
    }

    public static void main(String[] args) {
        new Simulation();
    }
}