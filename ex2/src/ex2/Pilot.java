package ex2;

import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.UltrasonicSensor;
import lejos.util.Delay;

public class Pilot {

	class Wheels {
		public NXTRegulatedMotor left;
		public NXTRegulatedMotor right;
		float speed = 1000;
		
		public Wheels() {
			this.right = Motor.A;
			this.left = Motor.B;
			this.left.setSpeed(this.speed);
			this.right.setSpeed(this.speed);
		}
		
		public void turnLeft() {
			this.left.rotate(450, true);
			this.right.rotate(-450);
		}
		
		public void forward() {
			this.setAcceleration(1000);
			this.left.backward();
			this.right.backward();
		}
		
		public void correctForward() {
			if (this.left.getTachoCount() > this.right.getTachoCount()) {
				this.left.setSpeed(this.speed + 0.5f);
				this.right.setSpeed(this.speed - 0.5f);
			}
			if (this.left.getTachoCount() < this.right.getTachoCount()) {
				this.left.setSpeed(this.speed - 0.5f);
				this.right.setSpeed(this.speed + 0.5f);
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
	Wheels wheels;
	double edges[];
	int currEdge;
	
	public Pilot() {
		this.wheels = new Wheels();
		this.ultra = new UltrasonicSensor(SensorPort.S1);
		this.edges = new double[2];
		this.currEdge = 0;
	}
	
	double getDistance() {
		return this.ultra.getDistance();
//		double dist[] = new double[10]; 
//		for (int i = 0; i < 10; ++i) {
//			dist[i] = this.ultra.getDistance();
//			Delay.msDelay(100);
//		}
//		double best = 0;
//		int bestAgree = -1;
//		for (int i = 0; i < 10; ++i) {
//			int numAgree = 0;
//			for (int j = 0; j < 10; ++j) {
//				if (Math.abs(dist[i] - dist[j]) < 0.1) {
//					++numAgree;
//				}
//			}
//			if (numAgree > bestAgree) {
//				bestAgree = numAgree;
//				best = dist[i];
//			}
//		}
//		if (bestAgree < 5) {
//			System.out.println("bad distance");
//		}
//		return best;
	}
	
	public void start() {		
		Delay.msDelay(1000);
		this.wheels.resetTachoCount();
		this.wheels.forward();
		
		while (this.currEdge < 4) {
			Delay.msDelay(10);
			this.wheels.correctForward();
			double dist = this.getDistance();
			if ((this.currEdge < 2 && dist < 30) ||
				(this.currEdge >= 2 && this.edges[this.currEdge-2] - this.wheels.getTachoCount() < 1000)) {
				this.wheels.stop();
				System.out.println("e=" + this.currEdge + " t=" + this.wheels.getTachoCount());
				if (this.currEdge < 2) {
					this.edges[this.currEdge] = this.wheels.getTachoCount();
				} else {
					this.wheels.left.rotate((int)(this.wheels.getTachoCount() - this.edges[this.currEdge-2]), true);
					this.wheels.right.rotate((int)(this.wheels.getTachoCount() - this.edges[this.currEdge-2]));
				}
				++this.currEdge;
				this.wheels.turnLeft();
				this.wheels.resetTachoCount();
				this.wheels.forward();
			}
		}
		
		this.wheels.stop();
	}
	
}
