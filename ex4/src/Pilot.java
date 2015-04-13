

import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.UltrasonicSensor;
import lejos.util.Delay;

public class Pilot {

	static int TURN_ANGLE = 668;
	static double FIRST_DIST_THRESH = 50;
	
	class Wheels {
		public NXTRegulatedMotor left;
		public NXTRegulatedMotor right;
		float speed = 100;
		
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
			this.left.forward();
			this.right.forward();
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
	
	UltrasonicSensor ultra;
	LightSensor light;
	Wheels wheels;
	double edges[];
	
	public Pilot() {
		this.wheels = new Wheels();
		this.ultra = new UltrasonicSensor(SensorPort.S1);
		this.light = new LightSensor(SensorPort.S2, false);
		this.edges = new double[2];
		this.edges[0] = 2000;
		this.edges[1] = 800;
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
		//Delay.msDelay(1000);
		//this.wheels.resetTachoCount();
		//this.wheels.forward();
		
		while (true) {
			Delay.msDelay(1000);
			double dis = this.getDistance();
			System.out.println("dist=" + dis);
		}
	}
	
}
