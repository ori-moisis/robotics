package ex7;

import lejos.nxt.UltrasonicSensor;
import lejos.robotics.RegulatedMotor;
import lejos.util.Delay;

public class DistanceMonitor implements Runnable {

	static int CORRECTION_RANGE = 4;
	static int TURN_RATE = 8;
	
	Ex7Controller controller;
	UltrasonicSensor sesnsor;
	RegulatedMotor left;
	RegulatedMotor right;
	int targetDistance;
	int speed;
	
	int pastDistance[];
	int pastDistanceOffset;
	int numDists;
	int trend;
	boolean shouldPause;
	
	
	boolean paused;
	boolean reallyPaused;
	Object pausedEvent;
	boolean stopped;
	
	public DistanceMonitor(Ex7Controller controller, UltrasonicSensor sesnsor, int targetDistance, RegulatedMotor left, RegulatedMotor right, int speed) {
		this.controller = controller;
		this.sesnsor = sesnsor;
		this.targetDistance = targetDistance;
		this.left = left;
		this.right = right;
		this.speed = speed;
		
		this.pastDistance = new int[10];
		this.pastDistanceOffset = 0;
		this.numDists = 0;
		this.trend = 0;
		this.shouldPause = true;
		
		this.paused = false;
		this.reallyPaused = false;
		this.pausedEvent = new Object();
		this.stopped = false;
	}

	public void run() {
		while (! this.stopped) {
			while (this.paused) {
				this.reallyPaused = true;
				try {
					synchronized (pausedEvent) {
						this.pausedEvent.wait(1000);
					}
				} catch (InterruptedException e) {
				}
			}
			this.reallyPaused = false;
			
			int dist = this.sesnsor.getDistance();
			if (Math.abs(dist - this.targetDistance) > CORRECTION_RANGE) {
				if (this.numDists > 1) {
					this.controller.handleNoRightWall();
					if (this.shouldPause) {
						this.pause();
					}
				} else {
					this.numDists = 0;
					this.trend = 0;
				}
				continue;
			}
			
			int diff = Math.abs(dist - this.targetDistance) * TURN_RATE;
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
	
	public void doNotPause() {
		this.shouldPause = false;
	}
	
	public void setTargetDist(int target) {
		this.targetDistance = target;
	}
	
	public void pause() {
		this.left.setSpeed(speed);
		this.right.setSpeed(speed);
		this.numDists = 0;
		this.trend = 0;
		this.paused = true;
	}
	
	public boolean isPaused() {
		return this.reallyPaused;
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
