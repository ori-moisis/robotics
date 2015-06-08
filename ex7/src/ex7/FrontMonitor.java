package ex7;

import lejos.nxt.UltrasonicSensor;

public class FrontMonitor implements Runnable {

	Ex7Controller controller;
	UltrasonicSensor sesnsor;
	int threshhold;
	
	boolean paused;
	Object pausedEvent;
	boolean stopped;
	
	public FrontMonitor(Ex7Controller controller, UltrasonicSensor sensor, int threshhold) {
		this.controller = controller;
		this.sesnsor = sensor;
		this.threshhold = threshhold;
		
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
			
			int dist = this.sesnsor.getDistance();
			if (dist < this.threshhold) {
				this.controller.handleFrontWall();
				this.paused = true;
			}
		}
	}

	public void pause() {
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
