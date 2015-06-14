package ex7;

import lejos.nxt.Button;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.addon.CompassHTSensor;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.util.Delay;

public class Ex7Controller implements Maze.MazeFinishListener {
	static int NORMAL_ACC = 30;
	static int NORMAL_SPEED = 7;
	static int ROTATION_SPEED = 50;
	static float DEG_OFFSET = 162;
	static int WALL_THRESHOLD = 17;
	static int BLACK_THRESHOLD = 40;
	static int BLOCK_SIZE = 31;
	static int BLOCK_SIZE_IN_TACHO = 320;
	
	
	NXTRegulatedMotor leftWheel;
	NXTRegulatedMotor rightWheel;
	
	DifferentialPilot pilot;
	LightSensor light;
	UltrasonicSensor ultraFront;
	UltrasonicSensor ultraRight;
	CompassHTSensor compass;
	Maze maze;
	
	int forwardMarked;
	int lastForwardMarked;
	int movementOffset;
	boolean frontWall;
	boolean rightNoWall;
	Object alert;
	
	DistanceMonitor rightDistMonitor;
	Thread rightDistMonitorThread;
	
	FrontMonitor frontDistMonitor;
	Thread frontDistMonitorThread;
	
		
	public Ex7Controller() {
		leftWheel = Motor.C;
		rightWheel = Motor.A;
		
		pilot = new DifferentialPilot(DifferentialPilot.WHEEL_SIZE_RCX, 13.9, leftWheel, rightWheel);
		pilot.setTravelSpeed(NORMAL_SPEED);
		pilot.setRotateSpeed(ROTATION_SPEED);
		pilot.setAcceleration(NORMAL_ACC);
		
		
		ultraFront = new UltrasonicSensor(SensorPort.S1);
		compass = new CompassHTSensor(SensorPort.S2);
		ultraRight = new UltrasonicSensor(SensorPort.S3);
		light = new LightSensor(SensorPort.S4, true);
		
		forwardMarked = 0;
		lastForwardMarked = -1;
		movementOffset = 0;
		frontWall = false;
		rightNoWall = false;
		alert = new Object();
		
		rightDistMonitor = new DistanceMonitor(this, this.ultraRight, 9, Motor.C, Motor.A);
		rightDistMonitorThread = new Thread(rightDistMonitor);
		
		frontDistMonitor = new FrontMonitor(this, this.ultraFront, 12);
		frontDistMonitorThread = new Thread(frontDistMonitor);
	}
	
	public void bla() {
		while (true) {
			//System.out.println("deg=" + this.compass.getDegrees());
			Delay.msDelay(500);
		}
	}
	
	public void handleFrontWall() {
		synchronized (this.alert) {
			this.frontWall = true;
			this.alert.notifyAll();
		}
	}
	
	public void handleNoRightWall() {
		synchronized (this.alert) {
			this.rightNoWall = true;
			this.alert.notifyAll();
		}
	}
	
	public void handleMazeFinish() {
		synchronized (this.alert) {
			this.alert.notifyAll();
		}
	}

	public void checkForward(boolean isTurn) {
		int forwardDone = (int)this.pilot.getMovementIncrement() - this.movementOffset;
		int blocksDone = forwardDone / BLOCK_SIZE;
		int offsetInBlock = forwardDone - this.forwardMarked * BLOCK_SIZE; 
		
		if ((blocksDone > this.forwardMarked) ||
			(isTurn && offsetInBlock >= (BLOCK_SIZE / 3))) {
			this.forwardMarked += 1;
			this.maze.forward();
		}
		
		if (this.forwardMarked > this.lastForwardMarked) {
			if (this.isBlack()) {
				this.maze.setBlack();
			}
			if (this.hasWall(ultraRight)) {
				this.maze.setWall();
			}
			this.lastForwardMarked += 1;
		}
	}
	
	public void start() {

		this.rightDistMonitor.pause();
		this.frontDistMonitor.pause();
		
		this.rightDistMonitorThread.start();
		this.frontDistMonitorThread.start();
		
		float deg = -1;
		while (deg == -1) {
			deg = compass.getDegrees();
		}
		int dir = ((int)((deg + 45) / 90)) % 4;
		maze = new Maze(6, 4, MazeBlock.Direction.values()[dir], this);
		
		this.frontDistMonitor.resume();
		this.rightDistMonitor.resume();
		
		do {
			synchronized (this.alert) {
				try {
					if (!this.rightNoWall && !this.frontWall) {
						this.alert.wait(500);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.checkForward(false);
			if (this.rightNoWall) {
				// Turn right
				this.frontDistMonitor.pause();
				this.rightDistMonitor.pause();
				// The case in which we have one more forward to perform
				this.checkForward(true);
				
				this.pilot.arc(-15, -90 - this.rightDistMonitor.getTrend());
//				this.pilot.travel(16);
//				int rotate = -90 - this.rightDistMonitor.getTrend();
//				this.pilot.rotate(rotate);
				this.pilot.forward();
				
				this.maze.turn();
				this.maze.forward();
				this.forwardMarked = 1;
				this.lastForwardMarked = 0;
				this.movementOffset = BLOCK_SIZE / 2;
				
				this.frontWall = false;
				this.rightNoWall = false;
				
				this.rightDistMonitor.resume();
				this.frontDistMonitor.resume();
			}
			else if (this.frontWall) {
				// Turn left
				this.rightDistMonitor.pause();
				this.frontDistMonitor.pause();				
				// The case in which we have one more forward to perform
				this.checkForward(true);
				
				this.pilot.stop();
				int rotate = 90 - this.rightDistMonitor.getTrend();
				this.pilot.rotate(rotate);
				this.pilot.forward();
				
				this.maze.turn();
				this.maze.turn();
				this.maze.turn();
				this.forwardMarked = 0;
				this.lastForwardMarked = -1;
				this.movementOffset = 0;
				
				this.frontWall = false;
				this.rightNoWall = false;
				
				this.rightDistMonitor.resume();
				this.frontDistMonitor.resume();
			} else {
				// Go straight
				if (! this.pilot.isMoving()) {
					this.pilot.forward();
				}
				this.rightDistMonitor.resume();
				this.frontDistMonitor.resume();
			}
			this.maze.drawMaze();
		} while (! this.maze.isFinished());
		
		this.pilot.stop();
		this.light.setFloodlight(false);
		this.maze.drawMaze();
		
//		boolean lastDidForward = false;
//		do {
//			if (isBlack()) {
//				maze.setBlack();
//			}
//
//			int trend = this.rightDistMonitor.getTrend();
//			
//			if (hasWall(ultraRight)) {
//				if (lastDidForward && Math.abs(trend) < 6) {
//					System.out.println("trend=" + trend);
//					Delay.msDelay(1000);
//					this.pilot.rotate(trend * -3);
//				}
//				maze.setWall();
//				if (hasWall(ultraFront)) {
//					if (lastDidForward) {
//						int dist = 255;
//						while (dist == 255) {
//							dist = this.ultraFront.getDistance();
//						}
//						this.pilot.travel(dist - 12);
//					}
//					
//					//this.turnWithCompass(105);
//					this.pilot.rotate(90);
//					maze.turn();
//					maze.turn();
//					maze.turn();
//					lastDidForward = false;
//				} else {
//					this.pilot.travel(BLOCK_SIZE);
//					maze.forward();
//					lastDidForward = true;
//				}
//			} else {
//				//this.turnWithCompass(-105);
//				this.pilot.rotate(-90);
//				this.pilot.travel(BLOCK_SIZE);
//				maze.turn();
//				maze.forward();
//				lastDidForward = true;
//			}
//			//maze.drawMaze();
//		} while (! maze.isFinished());
		//maze.drawMaze();
		
//		this.rightDistMonitor.stop();
//		this.frontDistMonitor.stop();
//		try {
//			this.rightDistMonitorThread.join();
//			this.frontDistMonitorThread.join();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	double getHeading() {
		Delay.msDelay(1000);
		double dist[] = new double[10]; 
		for (int i = 0; i < 10; ++i) {
			dist[i] = this.compass.getDegreesCartesian();
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
			System.out.println("bad heading");
		}
		return best;
	}
	
	public void turnWithCompass(int angle) {
		this.compass.resetCartesianZero();
		//this.pilot.rotate(angle);
		angle = angle < 0 ? angle + 360 : angle;
		double deg = this.getHeading();
		while ((int)deg != angle) {
			System.out.println("d=" + deg + " t=" + angle);
			double degMod = (((deg - angle + 180) % 360) + 360) % 360;
			this.pilot.rotate((int)(180 - degMod));
			deg = this.getHeading();
		}
	}
	
	public void turnTo(MazeBlock.Direction direction) {
		Delay.msDelay(500);
		float deg = this.compass.getDegrees();
		while (deg != direction.getHeading()) {
			System.out.println("d=" + deg + " t=" + direction.getHeading());
			float degMod = (((direction.getHeading() - deg + 180) % 360) + 360) % 360;
			this.pilot.rotate(180 - degMod);
			Delay.msDelay(500);
			deg = this.compass.getDegrees();
		}
	}
	
	public boolean isBlack() {
		int lval = 255;
		while (lval == 255) {
			lval = this.light.getLightValue();
		}
		return lval < BLACK_THRESHOLD;
	}
	
	public boolean hasWall(UltrasonicSensor sensor) {
		int dist = 255;
		while (dist == 255) {
			dist = sensor.getDistance();
		}
		return dist < WALL_THRESHOLD;
	}
	
	public void handleDistance(int dist) {
		// Slow down when approaching the ball
		this.pilot.setTravelSpeed(Math.min(NORMAL_SPEED, dist));
	}
	
	public int getDistance(UltrasonicSensor sensor) {
		int dist = 255;
		while (dist == 255) {
			dist = sensor.getDistance();
		}
		return dist;
	}
	
	public void resetHeading() {
		float deg = 3;
		while (Math.abs(deg) > 2) {
			do {
				deg = compass.getDegreesCartesian();
			} while (deg == -1);
			if (deg > 180) {
				deg = deg - 360;
			}
			this.pilot.rotate(-deg);
		}
	}
}
