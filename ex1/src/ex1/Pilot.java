package ex1;

import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.SensorPortListener;
import lejos.nxt.TouchSensor;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.addon.CompassHTSensor;
import lejos.util.Delay;

public class Pilot implements SensorPortListener {
	class Wheels {
		public NXTRegulatedMotor left;
		public NXTRegulatedMotor right;
		float speed = 300;
		boolean isForward;
		
		public Wheels() {
			this.left = Motor.A;
			this.right = Motor.B;
			this.isForward = true;
		}
		
		public void setSpeed() {
			this.left.setSpeed(speed);
			this.right.setSpeed(speed);
		}
		
		public void backward() {
			this.setAcceleration(1000);
			this.isForward = false;
			this.left.forward();
			this.right.forward();
		}
		
		public void forward() {
			this.setAcceleration(1000);
			this.isForward = true;
			this.left.backward();
			this.right.backward();
		}
		
		public void resume() {
			if (this.isForward) {
				this.forward();
			} else {
				this.backward();
			}
		}
		
		public void setAcceleration(int acc) {
			this.left.setAcceleration(acc);
			this.right.setAcceleration(acc);
		}
		
		public void stop() {
			this.left.stop(true);
			this.right.stop(true);
		}
		
		public long getTachoCount() {
			return Math.abs((this.right.getTachoCount() + this.left.getTachoCount())/2);
		}
		
		public void resetTachoCount() {
			this.left.resetTachoCount();
			this.right.resetTachoCount();
		}
	}
	
	
	TouchSensor touch;
	CompassHTSensor compass;
	UltrasonicSensor ultra;
	Wheels wheels;
	double initialDistance;
	boolean isForward = true;
	boolean done = false;
	long tachoForward = -1;
	int leftAngle = 0;
	
	
	
	public Pilot() {
		this.touch = new TouchSensor(SensorPort.S1);
		this.compass = new CompassHTSensor(SensorPort.S2);
		this.compass.resetCartesianZero();
		this.ultra = new UltrasonicSensor(SensorPort.S3);
		this.wheels = new Wheels();
		this.initialDistance = 0;
	}
	
	public void stateChanged(SensorPort aSource, int aOldValue, int aNewValue) {
		if (aSource.getId() == 0 && this.isForward && this.touch.isPressed()) {
			this.wheels.setAcceleration(10000);
			this.wheels.stop();
			this.tachoForward = this.wheels.getTachoCount();
			this.wheels.resetTachoCount();
			this.wheels.backward();
			this.isForward = false;
		}
	}
	
	double getDistance() {
		double dist[] = new double[10]; 
		for (int i = 0; i < 10; ++i) {
			dist[i] = this.ultra.getDistance();
			Delay.msDelay(100);
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
			System.out.println("bad distance");
		}
		return best;
	}
	
	public void start() {
		SensorPort.S1.addSensorPortListener(this);
		
		this.initialDistance = this.getDistance();
		System.out.println("init=" + this.initialDistance);
		
		
		this.wheels.resetTachoCount();
		this.wheels.setSpeed();
		this.wheels.forward();
		
		boolean corrected = false;
		
		while (!done) {
			Delay.msDelay(100);
			if (!isForward) {
				long distanceLeft = tachoForward - this.wheels.getTachoCount();
				if (distanceLeft < 400 && ! corrected) {
					corrected = true;
					this.wheels.stop();
					double currDist = this.getDistance();
					System.out.println("curr=" + currDist);
					double dist = this.initialDistance - currDist;
					if (Math.abs(dist) > 1) {
						if (dist > 0) {
							this.wheels.right.rotate((int)(11 * Math.abs(dist)));
						} else {
							this.wheels.left.rotate((int)(11 * Math.abs(dist)));
						}
					}
					Delay.msDelay(200);
					this.wheels.resume();
				}
				done = ((tachoForward - this.wheels.getTachoCount()) < 100);
				if (done) {
					System.out.println("done " + (tachoForward - this.wheels.getTachoCount()));
				}
			}
		}
		
		this.wheels.stop();
		while (this.tachoForward - this.wheels.getTachoCount() > 0) {
			this.wheels.left.rotate((int)(tachoForward - this.wheels.left.getTachoCount()), true);
			this.wheels.right.rotate((int)(tachoForward - this.wheels.right.getTachoCount()), true);
		}

		System.out.println("done " + (tachoForward - this.wheels.getTachoCount()));
		System.out.println("start dist=" + this.initialDistance);
		System.out.println("end dist=" + this.ultra.getDistance());
	}	
}
