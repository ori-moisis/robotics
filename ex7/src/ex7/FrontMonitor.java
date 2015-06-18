package ex7;

import lejos.nxt.UltrasonicSensor;

public class FrontMonitor implements Runnable {

	static final int SAFETY = 2;
	
	Ex7Controller controller;
	UltrasonicSensor sesnsor;
	int threshhold;
	int numTresh;
	
	boolean paused;
	Object pausedEvent;
	boolean stopped;
	
	public FrontMonitor(Ex7Controller controller, UltrasonicSensor sensor, int threshhold) {
		this.controller = controller;
		this.sesnsor = sensor;
		this.threshhold = threshhold;
		this.numTresh = 0;
		
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
			
			int dist = this.sesnsor.getDistance();
			if (dist < this.threshhold) {
				if (++this.numTresh >= SAFETY) {
					this.controller.handleFrontWall();
					this.pause();
				}
			} else {
				this.numTresh = 0;
			}
		}
	}

	public void pause() {
		this.paused = true;
		this.numTresh = 0;
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
