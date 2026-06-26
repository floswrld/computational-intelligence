import java.util.ArrayList;

public class Vehicle {
	static int allId = 0;
	int id; 
	double rad_sep; 
	double rad_zus; 
	int type; 
	final double FZL; 
	final double FZB;
    double avoidance_margin;

    double[] pos;
	double[] vel; 
	
	final double max_acc; 
	final double max_vel; 


	Vehicle() {
		allId++;
		this.id = allId;
		this.FZL = 1.0;
		this.FZB = 0.5;
		this.rad_sep = 5;
		this.rad_zus = 25;
		this.max_acc = 0.2;
		this.max_vel = 1;
        this.avoidance_margin = 25.0;

		pos = new double[2];
		vel = new double[2];
		
		pos[0] = Simulation.pix * Simulation.displaySize[0] * Math.random();
		pos[1] = Simulation.pix * Simulation.displaySize[1] * Math.random();
		double angle = 2 * Math.PI * Math.random();
		vel[0] = Math.cos(angle) * max_vel;
		vel[1] = Math.sin(angle) * max_vel;
	}

	ArrayList<Vehicle> neighbours(ArrayList<Vehicle> all, double radius1, double radius2) {
		ArrayList<Vehicle> neighbours = new ArrayList<Vehicle>();
		for (int i = 0; i < all.size(); i++) {
			Vehicle v = all.get(i);
			if (v.id != this.id) {
				double dist = Math.sqrt(Math.pow(v.pos[0] - this.pos[0], 2) + Math.pow(v.pos[1] - this.pos[1], 2));
				if (dist >= radius1 && dist < radius2) {
					neighbours.add(v);
				}
			}
		}
		return neighbours;
	}

	double[] calculateAcc(double[] vel_dest) {
		double[] acc_dest = new double[2];

		if (VectorCalculation.length(vel_dest) > 1e-8) {
		    vel_dest = VectorCalculation.normalize(vel_dest);
		}
		
		vel_dest[0] = vel_dest[0] * max_vel;
		vel_dest[1] = vel_dest[1] * max_vel;

		acc_dest[0] = vel_dest[0] - vel[0];
		acc_dest[1] = vel_dest[1] - vel[1];

		return acc_dest;
	}


	double[] cohesion(ArrayList<Vehicle> all) {
		ArrayList<Vehicle> neighbours;
		
		double[] pos_dest = new double[2];
		double[] vel_dest = new double[2];
		double[] acc_dest = new double[2];

		acc_dest[0] = 0;
		acc_dest[1] = 0;
		neighbours = neighbours(all, rad_sep, rad_zus);

		if (neighbours.size() > 0) {
			pos_dest[0] = 0;
			pos_dest[1] = 0;
			for (int i = 0; i < neighbours.size(); i++) {
				Vehicle v = neighbours.get(i);
				pos_dest[0] = pos_dest[0] + v.pos[0];
				pos_dest[1] = pos_dest[1] + v.pos[1];
			}
			pos_dest[0] = pos_dest[0] / neighbours.size();
			pos_dest[1] = pos_dest[1] / neighbours.size();

			vel_dest[0] = pos_dest[0] - pos[0];
			vel_dest[1] = pos_dest[1] - pos[1];

			acc_dest = calculateAcc(vel_dest);
			acc_dest = VectorCalculation.truncate(acc_dest, max_acc);

		}
		return acc_dest;
	}

	double[] separation(ArrayList<Vehicle> all) {
		ArrayList<Vehicle> neighbours;
		double[] vel_dest = new double[2];
		double[] acc_dest = new double[2];

		acc_dest[0] = 0;
		acc_dest[1] = 0;
		neighbours  = neighbours(all, 0, rad_sep);

		if (neighbours.size() > 0) {
			vel_dest[0] = 0;
			vel_dest[1] = 0;
			
			for (int i = 0; i < neighbours.size(); i++) {
				Vehicle v    = neighbours.get(i);
				double[] vel = new double[2];
				double dist;

				vel[0] = v.pos[0] - pos[0];
				vel[1] = v.pos[1] - pos[1];
				
				dist   = rad_sep  - VectorCalculation.length(vel);
				if (dist < 0)System.out.println("mistake in rad");
			
				if (VectorCalculation.length(vel) > 1e-8) {
				    vel = VectorCalculation.normalize(vel);
				}
				
				vel[0] = -vel[0] * dist;
				vel[1] = -vel[1] * dist;
				
				vel_dest[0] = vel_dest[0] + vel[0];
				vel_dest[1] = vel_dest[1] + vel[1];
			}

			acc_dest = calculateAcc(vel_dest);
			acc_dest = VectorCalculation.truncate(acc_dest, max_acc);

		}

		return acc_dest;
	}

	double[] alignment(ArrayList<Vehicle> all) {
	
		
		ArrayList<Vehicle> neighbours = new ArrayList<Vehicle>();
		double[] vel_dest = new double[2];
		double[] acc_dest = new double[2];
		acc_dest[0] = 0;
		acc_dest[1] = 0;

		neighbours = neighbours(all, 0, rad_zus);


		if (neighbours.size() > 0) {
			vel_dest[0] = 0;
			vel_dest[1] = 0;
			
			for (int i = 0; i < neighbours.size(); i++) {
				Vehicle v = neighbours.get(i);
				vel_dest[0] = vel_dest[0] + v.vel[0];
				vel_dest[1] = vel_dest[1] + v.vel[1];
			}
			vel_dest[0] = vel_dest[0] / neighbours.size();
			vel_dest[1] = vel_dest[1] / neighbours.size();

			
			acc_dest = calculateAcc(vel_dest);
			acc_dest = VectorCalculation.truncate(acc_dest, max_acc);

		}

		return acc_dest;
	}

    double[] foraging(double[] targetPos) {
        double[] velDest = new double[2];
        double[] accDest = new double[2];

        velDest[0] = targetPos[0] - pos[0];
        velDest[1] = targetPos[1] - pos[1];

        accDest = calculateAcc(velDest);
        accDest = VectorCalculation.truncate(accDest, max_acc);

        return accDest;
    }

    double[] avoidObstacles(double[][] obstacles) {
        double[] vel_dest = new double[2];
        double[] acc_dest = new double[2];
        boolean avoiding = false;

        for (int i = 0; i < obstacles.length; i++) {
            double obsX = obstacles[i][0];
            double obsY = obstacles[i][1];
            double obsR = obstacles[i][2];

            double dist = Math.sqrt(Math.pow(pos[0] - obsX, 2) + Math.pow(pos[1] - obsY, 2));

            if (dist < obsR + avoidance_margin) {
                double[] diff = new double[2];

                diff[0] = pos[0] - obsX;
                diff[1] = pos[1] - obsY;

                if (VectorCalculation.length(diff) > 1e-8) {
                    diff = VectorCalculation.normalize(diff);
                }

                double strength = (obsR + avoidance_margin - dist);
                vel_dest[0] += diff[0] * strength;
                vel_dest[1] += diff[1] * strength;
                avoiding = true;
            }
        }

        if (avoiding) {
            acc_dest = calculateAcc(vel_dest);

            //acc_dest = VectorCalculation.truncate(acc_dest, max_acc * ); //maximal beschleuigung anpassen um crash zu vermeiden
        }

        return acc_dest;
    }

	public double[] calculateWeightedAcc(ArrayList<Vehicle> allVehicles,double[] targetPos) {
		double[] acc_dest  = new double[2];
		double[] acc_dest1 = new double[2];
		double[] acc_dest2 = new double[2];
		double[] acc_dest3 = new double[2];
        double[] acc_dest4 = new double[2];
        double[] acc_dest5 = new double[2];

		double f_zus = 0.05;
		double f_sep = 0.55;
		double f_aus = 0.4;
        double f_food = 0.3;
        double f_obs = 100;

		acc_dest1 = cohesion(allVehicles);
		acc_dest2 = separation(allVehicles);
		acc_dest3 = alignment(allVehicles);
        acc_dest4 = foraging(targetPos);
        acc_dest5 = avoidObstacles(Simulation.obstacles);

        acc_dest[0] = (f_zus * acc_dest1[0]) + (f_sep * acc_dest2[0]) + (f_aus * acc_dest3[0]) + (f_food * acc_dest4[0])+(f_obs*acc_dest5[0]);
        acc_dest[1] = (f_zus * acc_dest1[1]) + (f_sep * acc_dest2[1]) + (f_aus * acc_dest3[1]) + (f_food * acc_dest4[1])+(f_obs*acc_dest5[1]);


		acc_dest = VectorCalculation.truncate(acc_dest, max_acc);
		return acc_dest;
	}

	void move(ArrayList<Vehicle> allVehicles,double[] targetPos) {
		//STEP 1: Accelaration or Force 
		double[] acc = calculateWeightedAcc(allVehicles,targetPos);
	
		//STEP 2: Speed
		vel[0] = vel[0] + acc[0];
		vel[1] = vel[1] + acc[1];	
		if (VectorCalculation.length(vel) > 1e-8) {
		    vel = VectorCalculation.normalize(vel);
		}
		vel[0] = vel[0] * max_vel;
		vel[1] = vel[1] * max_vel;
		
		//STEP 3: Position
		pos[0] = pos[0] + vel[0];
		pos[1] = pos[1] + vel[1];
		
		
		//STEP 4: Box-Simulation
		position_Box();
	}

	public void position_Box() {
		if (pos[0] < 10) {
			vel[0] = Math.abs(vel[0]);
			pos[0] = pos[0] + vel[0];
		}
		if (pos[0] > (Simulation.displaySize[0]-100) * Simulation.pix) {
			vel[0] = -Math.abs(vel[0]);
			pos[0] = pos[0] + vel[0];
		}
		if (pos[1] < 10) {
			vel[1] = Math.abs(vel[1]);
			pos[1] = pos[1] + vel[1];
		}
		if (pos[1] > (Simulation.displaySize[1]-100) * Simulation.pix) {
			vel[1] = -Math.abs(vel[1]);
			pos[1] = pos[1] + vel[1];
		}
	}
	

	
}
