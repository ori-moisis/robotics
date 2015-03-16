package ex3;

import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.LightSensor;
import ex3.Pilot.Wheels;

public class Pilot {

	static int TURN_ANGLE = 50;
	
	class Wheels {
		public NXTRegulatedMotor left;
		public NXTRegulatedMotor right;
		float speed = 800;
		
		public Wheels() {
			this.right = Motor.A;
			this.left = Motor.C;
			this.left.setSpeed(this.speed);
			this.right.setSpeed(this.speed);
		}
		
		public void turnLeft() {
			this.left.rotate(TURN_ANGLE, true);
			this.right.rotate(-TURN_ANGLE);
		}
		
		public void forward() {
			this.setAcceleration(1000);
			this.left.backward();
			this.right.backward();
		}
		
		public void correctForward() {
			if (this.left.getTachoCount() > this.right.getTachoCount()) {
				this.left.setSpeed(this.speed + 1f);
				this.right.setSpeed(this.speed - 1f);
			}
			if (this.left.getTachoCount() < this.right.getTachoCount()) {
				this.left.setSpeed(this.speed - 1f);
				this.right.setSpeed(this.speed + 1f);
			}
			if (this.left.getTachoCount() == this.right.getTachoCount()) {
				this.left.setSpeed(this.speed);
				this.right.setSpeed(this.speed);
			}
		}
		
		public void setAcceleration(int acc) {
			this.left.setAcceleration(acc);
			this.right.setAcceleration(acc);
		}
		
		public void stop() {
			this.left.stop(true);
			this.right.stop();
		}
		
		public long getTachoCount() {
			return Math.abs((this.right.getTachoCount() + this.left.getTachoCount())/2);
		}
		
		public void resetTachoCount() {
			this.left.resetTachoCount();
			this.right.resetTachoCount();
		}
	}
	
	NXTRegulatedMotor sensorEng;
	LightSensor light;
	Wheels wheels;
	
	public Pilot() {
		this.wheels = new Wheels();
		this.light = new LightSensor(SensorPort.S2, true);
		this.sensorEng = Motor.B;
	}
	
	public void moveLeft() {
		this.sensorEng.rotate(10);
	}
	
	public void moveRight() {
		this.sensorEng.rotate(-10);
	}
}
