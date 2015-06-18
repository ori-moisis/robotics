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
	static int NORMAL_ACC = 100;
	static int NORMAL_SPEED = 300;
	static int ARC_SPEED = 20;
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
		pilot.setRotateSpeed(ROTATION_SPEED);
		pilot.setAcceleration(NORMAL_ACC);
		
		
		ultraFront = new UltrasonicSensor(SensorPort.S1);
		compass = new CompassHTSensor(SensorPort.S2);
		ultraRight = new UltrasonicSensor(SensorPort.S3);
		light = new LightSensor(SensorPort.S4, true);
		
		//maze = new Maze(6, 4, MazeBlock.Direction.SOUTH, this);
		forwardMarked = 0;
		lastForwardMarked = -1;
		movementOffset = 0;
		frontWall = false;
		rightNoWall = false;
		alert = new Object();
		
		rightDistMonitor = new DistanceMonitor(this, this.ultraRight, 10, Motor.C, Motor.A, NORMAL_SPEED);
		rightDistMonitorThread = new Thread(rightDistMonitor);
		
		frontDistMonitor = new FrontMonitor(this, this.ultraFront, 15);
		frontDistMonitorThread = new Thread(frontDistMonitor);
		
		
		
		
		Maze m = new Maze(6, 4, MazeBlock.Direction.SOUTH, null);
		m.setWall();
		m.forward(); m.setWall();
		m.forward(); m.setWall();
		m.forward(); m.setWall();
		m.forward(); m.setWall();
		m.forward(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.forward(); m.setWall();
		m.forward(); m.setWall();
		m.forward(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.forward(); m.setWall();
		m.forward(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.forward(); m.setWall();
		m.forward(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.forward(); m.turn();
		m.forward(); m.setWall();
		m.setBlack();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.forward(); m.turn();
		m.forward(); m.turn();
		m.forward(); m.setWall();
		m.forward(); m.turn();
		m.forward(); m.setWall();
		m.forward(); m.turn();
		m.forward(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.forward(); m.turn();
		m.forward(); m.setWall();
		m.forward(); m.turn();
		m.forward(); m.setWall();
		m.forward(); m.setWall();
		m.forward(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.forward(); m.setWall();
		m.forward(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.forward(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.forward(); m.turn();
		m.forward(); m.turn();
		m.forward(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.forward(); m.turn();
		m.forward(); m.turn();
		m.forward(); m.setWall();
		m.forward(); m.setWall();
		m.forward(); m.turn();
		m.forward(); m.setWall();
		m.forward(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		m.turn(); m.turn(); m.turn(); m.setWall();
		this.maze = m;
		
		
		
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
		int forwardDone = (int)this.pilot.getMovementIncrement() + this.movementOffset;
		int blocksDone = forwardDone / BLOCK_SIZE;
		int offsetInBlock = forwardDone - this.forwardMarked * BLOCK_SIZE; 
		
		if (blocksDone > this.forwardMarked ||
			(isTurn && offsetInBlock > 10)) {
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
	
	void doArc(double ang) {
		this.pilot.setTravelSpeed(ARC_SPEED);
		this.pilot.arc(13 * ang / Math.abs(ang), ang);
		this.leftWheel.setSpeed(NORMAL_SPEED);
		this.rightWheel.setSpeed(NORMAL_SPEED);
	}
	
	void mapMaze() {
		this.frontDistMonitor.resume();
		this.rightDistMonitor.resume();
		
		do {
			synchronized (this.alert) {
				try {
					if (!this.rightNoWall && !this.frontWall) {
						this.alert.wait(200);
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
				
				this.movementOffset = BLOCK_SIZE / 2;
				// The case in which we have one more forward to perform
				this.checkForward(true);
				
				this.doArc(-90 - this.rightDistMonitor.getTrend());
				//this.pilot.arc(-15, -90 - this.rightDistMonitor.getTrend());
				this.pilot.forward();
				
				this.maze.turn();
				this.forwardMarked = 0;
				this.lastForwardMarked = 0;
				
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
	}
	
	public void start() {

		this.rightDistMonitor.pause();
		this.frontDistMonitor.pause();
		
		this.rightDistMonitorThread.start();
		this.frontDistMonitorThread.start();
		
		//this.mapMaze();
		
		this.maze.drawMaze();
		this.frontDistMonitor.pause();
		this.rightDistMonitor.pause();
		
		
		Button.waitForAnyPress();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		
		
		
		boolean isFirst = true;
		Movement moves[] = this.maze.getPathToBlack();
		
		int travelDist = 0;
		boolean hasWall = false;
		for (Movement move : moves) {
			switch (move.getDirection()) {
			case LEFT:
				if (!isFirst) {
					travelDist -= BLOCK_SIZE / 2;
				}
				break;
			case RIGHT:
				if (!isFirst) {
					travelDist -= BLOCK_SIZE / 2;
				}
				break;
			default:
				break;
			}
			
			if (move.getDirection() != Movement.Direction.FORWARD || hasWall != move.hasWall()) {
				if (hasWall) {
					this.rightDistMonitor.resume();
				}
				this.pilot.travel(travelDist);
				travelDist = 0;
				if (hasWall) {
					this.rightDistMonitor.pause();
				}
			}
			
			hasWall = move.hasWall();
			
			switch (move.getDirection()) {
			case LEFT:
				if (!isFirst) {
					this.doArc(90);
					travelDist += BLOCK_SIZE*2 / 3;
				} else {
					pilot.rotate(90);
					travelDist += BLOCK_SIZE;
				}
				break;
			case RIGHT:
				if (!isFirst) {
					this.doArc(-90);
					travelDist += BLOCK_SIZE*2 / 3;
				} else {
					pilot.rotate(-90);
					travelDist += BLOCK_SIZE;
				}
				break;
			case BACKWARD:
				this.pilot.rotate(180);
				travelDist += BLOCK_SIZE;
				break;
			case FORWARD:
				travelDist += BLOCK_SIZE;
				break;
			default:
				break;
			}
			isFirst = false;
		}
		
		this.pilot.travel(travelDist);
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
}
