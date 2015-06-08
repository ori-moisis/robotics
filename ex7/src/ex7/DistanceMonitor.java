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

	@Override
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
					this.paused = true;
				}
			}
			
			System.out.println("d=" + dist);
			int diff = Math.abs(dist - this.targetDistance) * 4;
			if (dist > this.targetDistance) {
				this.left.setSpeed(speed + diff);
				this.right.setSpeed(speed);
			} else if (dist < this.targetDistance) {
				this.left.setSpeed(speed);
				this.right.setSpeed(speed + diff);
			} else {
				this.left.setSpeed(speed);
				this.right.setSpeed(speed);
			}
			
			
			this.pastDistance[this.pastDistanceOffset] = dist;
			this.pastDistanceOffset = (this.pastDistanceOffset + 1) % this.pastDistance.length;
			this.numDists += 1;
			
			if (this.numDists > this.pastDistance.length) {
				int currTrend = (dist - this.pastDistance[this.pastDistanceOffset]);
				if (Math.abs(currTrend) > Math.abs(this.trend)) {
					this.trend = currTrend;
				}
			}
		}
	}
	
	int getLastDist() {
		int offset = this.pastDistanceOffset - 1;
		offset = offset < 0 ? this.pastDistance.length - 1 : offset;
		return this.pastDistance[offset];
	}
	
	public int getTrend() {
		this.numDists = 0;
		int res = this.trend;
		this.trend = 0;
		int dist = this.getLastDist();
		if ((dist < this.targetDistance && res < 0) ||
			(dist > this.targetDistance && res > 0) ||
			(dist == this.targetDistance && res != 0)) {
			return res;
		} else if (dist != this.targetDistance && res == 0) {
			return dist - this.targetDistance;
		} else {
			return 0;
		}
	}
	
	public void pause() {
		this.numDists = 0;
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
