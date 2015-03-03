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
	
	@Override
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
	
	public void start() {
		SensorPort.S1.addSensorPortListener(this);
		
		for (int i = 0; i < 10; ++i) {
			this.initialDistance += (double)this.ultra.getDistance() / 10;
			Delay.msDelay(100);
		}
		System.out.println("init=" + this.initialDistance);
		
		
		this.wheels.resetTachoCount();
		this.wheels.setSpeed();
		this.wheels.forward();
		
		int badDist = 0;
		
		while (!done) {
			Delay.msDelay(500);
			if (!isForward) {
				done = ((tachoForward - this.wheels.getTachoCount()) < 	100);
				if (done) {
					System.out.println("done");
				}
			}
			
			double dist = this.ultra.getDistance() - this.initialDistance;
			if (Math.abs(dist) > 1) {
				if (++badDist > 2) {
					System.out.println("move=" + dist);
					this.wheels.stop();
					if ((dist > 0 && this.isForward) || (dist < 0 && !this.isForward)) {
						if (this.leftAngle > -20) {
							this.wheels.right.rotate(10);
							this.leftAngle -= 10;
						}
					} else {
						if (this.leftAngle < 20) {
							this.wheels.left.rotate(10);
							this.leftAngle += 10;
						}
					}
					this.wheels.resume();
				}
			} else {
				System.out.println("back " + this.leftAngle);
				if (this.leftAngle != 0) {
					if (this.leftAngle > 0) {
						this.wheels.left.rotate(this.leftAngle);
					} else {
						this.wheels.right.rotate(-this.leftAngle);
					}
				}
				this.leftAngle = 0;
				badDist = 0;
			}
		}
		
		this.wheels.stop();
	}
	
}
