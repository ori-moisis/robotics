package ex5;

import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.SensorPortListener;
import lejos.nxt.Sound;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.addon.CompassHTSensor;
import lejos.util.Delay;

public class Pilot {
	static int CORNER_TURN_ANGLE = 140;
	static int[] DISTANCE_FROM_MIDDLE = {53, 164};
	static int ALLOWED_MARGIN = 2;
	static String FRIEND_ADDR = "00165314394B";
	
	class Wheels {
		public NXTRegulatedMotor left;
		public NXTRegulatedMotor right;
		public float speed = 300;
		public int acc = 500;
		
		public Wheels() {
			this.right = Motor.A;
			this.left = Motor.C;
			this.setSpeed(this.speed);
			this.setAcceleration(this.acc);
		}
		
		public void turnCorner() {
			this.setSpeed(300);
			this.left.rotate(CORNER_TURN_ANGLE, true);
			this.right.rotate(-CORNER_TURN_ANGLE);
		}
		
		public void forward() {
			this.setSpeed(100);
			this.left.forward();
			this.right.forward();
		}
		
		public void backward() {
			this.setSpeed(100);
			this.left.backward();
			this.right.backward();
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
		
		public void setAcceleration(int acc) {
			this.left.setAcceleration(acc);
			this.right.setAcceleration(acc);
		}
		
		public void setSpeed(float speed) {
			this.left.setSpeed(speed);
			this.right.setSpeed(speed);
		}
		
		public void stop() {
			this.setAcceleration(10000);
			this.left.stop(true);
			this.right.stop();
			this.setAcceleration(this.acc);
		}
	}
	
	UltrasonicSensor ultraFront;
	UltrasonicSensor ultraLeft;
	UltrasonicSensor ultraRight;
	Wheels wheels;
	int[] boxLocation = {0,0};
	int direction;
	
	
	public Pilot() {
		this.wheels = new Wheels();
		this.ultraFront = new UltrasonicSensor(SensorPort.S1);
		this.ultraRight = new UltrasonicSensor(SensorPort.S2);
		this.ultraLeft = new UltrasonicSensor(SensorPort.S3);
		this.direction = 1;
	}
	
	int getDistance(UltrasonicSensor sensor) {
		int best = 255;
		do {
			int dist[] = new int[10]; 
			for (int i = 0; i < 10; ++i) {
				dist[i] = sensor.getDistance();
			}
			
			int bestAgree = -1;
			for (int i = 0; i < 10; ++i) {
				int numAgree = 0;
				for (int k = 0; k < 10; ++k) {
					if (Math.abs(dist[i] - dist[k]) < 0.1) {
						if (255 - dist[i] < 1) {
							numAgree += 0.1;
						} else {
							numAgree += 1;
						}
					}
				}
				if (numAgree > bestAgree) {
					bestAgree = numAgree;
					best = dist[i];
				}
			}
			if (bestAgree < 5) {
				System.out.println("bad distance");
			}
		} while (best == 255);
		return best;
	}
	

	public void start() {
		int dist = this.getDistance(this.ultraFront);
		System.out.println("fd=" + dist);
		
		
		int left = 0;
		int right = 0;
		do {
			left = this.getDistance(this.ultraLeft);
			right = this.getDistance(this.ultraRight);
			System.out.println("l=" + left + " r=" + right);
		} while (left != right);
		
		System.out.println("ready");
		Delay.msDelay(1000);
		
		dist = this.getDistance(this.ultraFront);
		System.out.println("fd=" + dist);
		
		if (dist < DISTANCE_FROM_MIDDLE[1] - ALLOWED_MARGIN) {
			System.out.println("box in the way");
			this.boxLocation[0] = dist;
			this.scanSide();
			return;
		}
		
		this.wheels.forward();
		dist = DISTANCE_FROM_MIDDLE[0];
		do {
			dist = Math.min(this.getDistance(ultraLeft), this.getDistance(ultraRight));
			System.out.println("d=" + dist);
		} while (((dist + ALLOWED_MARGIN) > DISTANCE_FROM_MIDDLE[0]) &&
				 this.ultraFront.getDistance() > ALLOWED_MARGIN);
		
		this.wheels.stop();
		
		if (dist < DISTANCE_FROM_MIDDLE[0] - ALLOWED_MARGIN) {
			int leftDist = this.getDistance(this.ultraLeft);
			int rightDist = this.getDistance(this.ultraRight);
			System.out.println("l=" + leftDist + " r=" + rightDist);
			if (leftDist < rightDist) {
				this.boxLocation[1] = DISTANCE_FROM_MIDDLE[0] + leftDist;
			} else {
				this.boxLocation[1] = DISTANCE_FROM_MIDDLE[0] - rightDist;
			}
			this.boxLocation[0] = DISTANCE_FROM_MIDDLE[1] - this.getDistance(this.ultraFront);
			this.scanComplete();
			return;
		}
		
		System.out.println("No box");
	}
	
	public void scanSide() {
		this.wheels.turnCorner();
		this.wheels.backward();
		
		int dist = 255;
		do {
			do {
				dist = this.ultraLeft.getDistance();
			} while (dist == 255);
			System.out.println("d=" + dist);
		} while (dist < this.boxLocation[0] + ALLOWED_MARGIN);
		this.wheels.stop();
		
		// Read Y coordinate
		do {
			dist = this.getDistance(this.ultraFront);
		} while (dist == 255);
		this.boxLocation[0] = DISTANCE_FROM_MIDDLE[1] - this.boxLocation[0];
		this.boxLocation[1] = dist;
		
		this.scanComplete();
	}
	
	public void scanComplete() {
		System.out.println("Box=" + this.boxLocation[0] + "," + this.boxLocation[1]);
	}
	
}
