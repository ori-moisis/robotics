package ex7;

import lejos.nxt.UltrasonicSensor;
import lejos.robotics.RegulatedMotor;
import lejos.util.Delay;

public class DistanceMonitor implements Runnable {

	static int CORRECTION_RANGE = 5;
	static int TURN_RATE = 5;
	
	Ex7Controller controller;
	UltrasonicSensor sesnsor;
	RegulatedMotor left;
	RegulatedMotor right;
	
	int targetDistance;
	int pastDistance[];
	int pastDistanceOffset;
	int numDists;
	int trend;
	
	boolean paused;
	Object pausedEvent;
	boolean stopped;
	
	public DistanceMonitor(Ex7Controller controller, UltrasonicSensor sesnsor, int targetDistance, RegulatedMotor left, RegulatedMotor right) {
		this.controller = controller;
		this.sesnsor = sesnsor;
		this.targetDistance = targetDistance;
		this.left = left;
		this.right = right;
		
		this.pastDistance = new int[10];
		this.pastDistanceOffset = 0;
		this.numDists = 0;
		this.trend = 0;
		
		this.paused = false;
		this.pausedEvent = new Object();
		this.stopped = false;
	}

	public void run() {
		while (! this.stopped) {
			while (this.paused) {
				try {
					synchronized (pausedEvent) {
						this.pausedEvent.wait(1000);
					}
				} catch (InterruptedException e) {
				}
			}
			
			int speed = 100;
			int dist = this.sesnsor.getDistance();
			if (Math.abs(dist - this.targetDistance) > CORRECTION_RANGE) {
				if (this.numDists > 3) {
					this.controller.handleNoRightWall();
					this.pause();
				}
				continue;
			}
			
			//System.out.println("d=" + dist + " t=" + this.trend);
			int diff = Math.abs(dist - this.targetDistance) * 7;
			if (dist > this.targetDistance && this.trend >= 0) {
				this.left.setSpeed(speed + diff);
				this.right.setSpeed(speed);
			} else if (dist < this.targetDistance && this.trend <= 0) {
				this.left.setSpeed(speed);
				this.right.setSpeed(speed + diff);
			} else if (dist == this.targetDistance && this.trend != 0) {
				if (this.trend > 0) {
					this.left.setSpeed(speed + 10);
					this.right.setSpeed(speed);
				} else {
					this.left.setSpeed(speed);
					this.right.setSpeed(speed + 10);
				}
			} else {
				this.left.setSpeed(speed);
				this.right.setSpeed(speed);
			}
			
			
			this.pastDistance[this.pastDistanceOffset] = dist;
			this.pastDistanceOffset = (this.pastDistanceOffset + 1) % this.pastDistance.length;
			this.numDists += 1;
			
			if (this.numDists > this.pastDistance.length) {
				this.trend = dist - this.pastDistance[this.pastDistanceOffset];
			} else {
				this.trend = dist - this.getDist(this.numDists);
			}
		}
	}
	
	int getDist(int before) {
		int offset = (this.pastDistanceOffset - before) % this.pastDistance.length;
		offset = offset < 0 ? this.pastDistance.length - 1 : offset;
		return this.pastDistance[offset];
	}
	
	int getLastDist() {
		int offset = this.pastDistanceOffset - 1;
		offset = offset < 0 ? this.pastDistance.length - 1 : offset;
		return this.pastDistance[offset];
	}
	
	public int getTrend() {
		return this.trend;
	}
	
	public void pause() {
		this.numDists = 0;
		this.trend = 0;
		this.paused = true;
	}
	
	public void stop() {
		this.stopped = true;
		this.resume();
	}

	public void resume() {
		this.paused = false;
		synchronized (pausedEvent) {
			this.pausedEvent.notifyAll();
		}
	}
}
