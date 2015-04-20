import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.SensorPortListener;
import lejos.nxt.Sound;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.addon.CompassHTSensor;
import lejos.util.Delay;

public class Pilot implements SensorPortListener {
	static int TURN_ANGLE = 50;
	static int CORNER_TURN_ANGLE = 206;
	static double LONG_AXIS_HEADING = 180;
	static double LONG_AXIS_BIAS = 11;
	static double SHORT_AXIS_HEADING = 66;
	static double SHORT_AXIS_BIAS = 30;
	static double OTHER_SIDE_BIAS = 2;
	static double LIGHT_THRESHOLD = 42;
	
	class Wheels {
		public NXTRegulatedMotor left;
		public NXTRegulatedMotor right;
		public float speed = 500;
		public int acc = 500;
		
		public Wheels() {
			this.right = Motor.A;
			this.left = Motor.C;
			this.setSpeed(this.speed);
			this.setAcceleration(this.acc);
		}
		
		public void turnCorner() {
			this.left.rotate(CORNER_TURN_ANGLE, true);
			this.right.rotate(-CORNER_TURN_ANGLE);
		}
		
		public void move(double distance) {
			if (Math.abs(distance) > 15) {
				distance *= 6.5;
			} else if (Math.abs(distance) > 5) {
				distance *= 3;
			} else {
				distance *= 1.5;
			}
			
			if (distance > 600) {
				distance = 600;
			} else if (distance < -600) {
				distance = -600;
			}
			
			this.left.rotate((int)distance, true);
			this.right.rotate((int)distance);
		}
		
		public void turn(double degrees) {
			if (Math.abs(degrees) < 5) {
				degrees *= 0.6;
			} else {
				degrees *= 1.2;
			}
			
			// Make sure the turn is at least 1 so we don't get stuck
			if (Math.abs(degrees) < 1) {
				degrees = degrees / Math.abs(degrees);
			}
			
			this.left.rotate((int)-degrees, true);
			this.right.rotate((int)degrees);
		}
		
		public void turnRight() {
			this.left.rotate(TURN_ANGLE, true);
			this.right.rotate(-TURN_ANGLE);
		}
		
		public void turnLeft() {
			this.left.rotate(-TURN_ANGLE, true);
			this.right.rotate(TURN_ANGLE);
		}
		
		public void setAcceleration(int acc) {
			this.left.setAcceleration(acc);
			this.right.setAcceleration(acc);
		}
		
		public void setSpeed(float speed) {
			this.left.setSpeed(speed);
			this.right.setSpeed(speed);
		}
		
		public void stop() {
			this.left.stop(true);
			this.right.stop();
		}
	}
	
	UltrasonicSensor ultraFront;
	UltrasonicSensor ultraBack;
	LightSensor light;
	CompassHTSensor compass;
	Wheels wheels;
	boolean stopped;
	boolean skipLight;
	int lastDirection;
	long startTime;
	
	
	public Pilot() {
		this.wheels = new Wheels();
		this.ultraFront = new UltrasonicSensor(SensorPort.S1);
		this.ultraBack = new UltrasonicSensor(SensorPort.S3);
		this.light = new LightSensor(SensorPort.S2, true);
		this.compass = new CompassHTSensor(SensorPort.S4);
		this.stopped = false;
		this.skipLight = true;
		this.lastDirection = 1;
	}
	
	double[] getDistances() {
		double dist[][] = new double[2][10]; 
		for (int i = 0; i < 10; ++i) {
			dist[0][i] = this.ultraFront.getDistance();
			dist[1][i] = this.ultraBack.getDistance();
		}
		
		double[] best = {0,0};
		double[] bestAgree = {-1, -1};
		for (int j = 0; j < 2; ++j) {
			for (int i = 0; i < 10; ++i) {
				double numAgree = 0;
				for (int k = 0; k < 10; ++k) {
					if (Math.abs(dist[j][i] - dist[j][k]) < 0.1) {
						if (255 - dist[j][i] < 1) {
							numAgree += 0.1;
						} else {
							numAgree += 1;
						}
					}
				}
				if (numAgree > bestAgree[j]) {
					bestAgree[j] = numAgree;
					best[j] = dist[j][i];
				}
			}
			if (bestAgree[j] < 5) {
				System.out.println("bad distance " + j);
			}
		}
		return best;
	}
	
	double getHeading() {
		double dist[] = new double[10]; 
		for (int i = 0; i < 10; ++i) {
			dist[i] = this.compass.getDegrees();
		}
		double best = 0;
		int bestAgree = -1;
		for (int i = 0; i < 10; ++i) {
			int numAgree = 0;
			for (int j = 0; j < 10; ++j) {
				if (Math.abs(dist[i] - dist[j]) < 0.1) {
					++numAgree;
				}
			}
			if (numAgree > bestAgree) {
				bestAgree = numAgree;
				best = dist[i];
			}
		}
		if (bestAgree < 5) {
			System.out.println("bad heading");
		}
		return best;
	}
	
	void alignToHeading(double targetHeading) {
		if (this.stopped) {
			return;
		}
		
		double heading = this.getHeading();
		boolean didTrick = false;
		
		while (Math.abs(heading - targetHeading) > 1) {
			if (this.stopped) {
				return;
			}
			this.wheels.turn((heading - targetHeading) % 360);
			heading = this.getHeading();
			if (targetHeading == SHORT_AXIS_HEADING && ! didTrick) {
				double[] dists = this.getDistances();
				didTrick = true;
				if (dists[0] - dists[1] > 0) {
					targetHeading += OTHER_SIDE_BIAS;
				}
			}
		}
	}
	
	void moveToMiddle(double bias, double prec) {
		if (this.stopped) {
			return;
		}
		double[] dists = this.getDistances();
		dists[0] -= bias;
		while (Math.abs(dists[0] - dists[1]) > prec) {
			if (this.stopped) {
				return;
			}
			if (dists[0] > dists[1]) {
				this.lastDirection = 1;
				this.wheels.move(Math.min(dists[0] - dists[1], 100));
			} else {
				this.lastDirection = -1;
				this.wheels.move(Math.max(dists[0] - dists[1], -100));
			}
			dists = this.getDistances();
			dists[0] -= bias;
		}
	}

	public void start() {		
		// Listen for changes in light
		SensorPort.S2.addSensorPortListener(this);
		
		Delay.msDelay(100);
		
		startTime = System.currentTimeMillis();
		
		this.skipLight = false;
		
		// Align to long axis
		this.alignToHeading(LONG_AXIS_HEADING);
		
		// Go to middle
		this.moveToMiddle(LONG_AXIS_BIAS, 2);
		
		// Align to short axis
		this.alignToHeading(SHORT_AXIS_HEADING);
		
		// Go to middle
		double[] dists = this.getDistances();
		this.lastDirection = (dists[0] > dists[1]) ? 1 : -1;
		if (! this.stopped) {
			// Try moving a bit too much in the hope we are perfectly aligned
			this.wheels.move(dists[0] - SHORT_AXIS_BIAS - dists[1] + (20 * this.lastDirection));
		}
		this.moveToMiddle(SHORT_AXIS_BIAS, 5);
		
		// Search mode
		this.wheels.setSpeed(300);
		
		while (! this.stopped) {
			if (! this.stopped) {
				this.wheels.left.forward();
				this.wheels.right.backward();
			}
			int i = 0;
			while (! this.stopped && i <= 400) {
				Delay.msDelay(100);
				this.wheels.right.setSpeed(300 - i);
				i += 5;
			}
			
			
			this.wheels.setSpeed(300);
			
			i = 0;
			while (! this.stopped && i < 12) {
				this.wheels.setAcceleration(this.wheels.acc);
				this.wheels.turnLeft();
				this.wheels.setAcceleration(10000);
				if (! this.stopped) {
					this.wheels.move(20);
				}
				if (! this.stopped) {
					this.wheels.move(-40);
				}
				if (! this.stopped) {
					this.wheels.move(20);
				}
				++i;
			}
		}
	}

	@Override
	public void stateChanged(SensorPort aSource, int aOldValue, int aNewValue) {
		// TODO Auto-generated method stub
		if (! this.stopped && !this.skipLight) {
			int lightVal = this.light.readValue();
			if (this.light.readValue() <= LIGHT_THRESHOLD) {
				long totalTime = System.currentTimeMillis() - this.startTime;
				double timeInSec = totalTime / 1000.0;
				this.stopped = true;
				this.wheels.setAcceleration(10000);
				this.wheels.stop();
				this.wheels.setAcceleration(this.wheels.acc);
				System.out.println("time=" + timeInSec);
				Sound.systemSound(true, 1);
			}
		}
	}
	
}
