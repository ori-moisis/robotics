package ex7;

import lejos.nxt.UltrasonicSensor;

public class DistanceMonitor implements Runnable {

	Ex7Controller controller;
	UltrasonicSensor sesnsor;
	int threshold;
	
	
	boolean paused;
	Object pausedEvent;
	boolean stopped;
	
	public DistanceMonitor(Ex7Controller controller, UltrasonicSensor sesnsor, int threshold) {
		this.controller = controller;
		this.sesnsor = sesnsor;
		this.threshold = threshold;
		
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
			this.controller.handleDistance(dist);
			if (dist < this.threshold) {
				this.paused = true;
			}
		}
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
