package ex7;

import javax.microedition.lcdui.Graphics;

import lejos.nxt.Button;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.addon.CompassHTSensor;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.util.Stopwatch;

public class Ex7Controller implements Maze.MazeFinishListener {
	static int NORMAL_ACC = 100;
	static int NORMAL_SPEED = 300;
	static int ARC_SPEED = 20;
	static int ROTATION_SPEED = 50;
	static int WALL_THRESHOLD = 17;
	static int BLACK_THRESHOLD = 40;
	static int BLOCK_SIZE = 30;
	static int RIGHT_TRAGET_DIST = 10;
	static int LEFT_TRAGET_DIST = 8;
	
	
	NXTRegulatedMotor leftWheel;
	NXTRegulatedMotor rightWheel;
	
	DifferentialPilot pilot;
	LightSensor light;
	UltrasonicSensor ultraFront;
	UltrasonicSensor ultraRight;
	CompassHTSensor compass;
	Maze maze;
	Stopwatch timer;
	
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
		pilot.setTravelSpeed(20);
		
		
		ultraFront = new UltrasonicSensor(SensorPort.S1);
		compass = new CompassHTSensor(SensorPort.S2);
		ultraRight = new UltrasonicSensor(SensorPort.S3);
		light = new LightSensor(SensorPort.S4, true);

		maze = new Maze(6, 4, MazeBlock.Direction.SOUTH, this);
		timer = new Stopwatch();
		
		forwardMarked = 0;
		lastForwardMarked = -1;
		movementOffset = 0;
		frontWall = false;
		rightNoWall = false;
		alert = new Object();
		
		rightDistMonitor = new DistanceMonitor(this, this.ultraRight, RIGHT_TRAGET_DIST, Motor.C, Motor.A, NORMAL_SPEED);
		rightDistMonitorThread = new Thread(rightDistMonitor);
		
		frontDistMonitor = new FrontMonitor(this, this.ultraFront, 15);
		frontDistMonitorThread = new Thread(frontDistMonitor);
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
		int blocksDone = forwardDone / (BLOCK_SIZE + 1);
		int offsetInBlock = forwardDone - this.forwardMarked * (BLOCK_SIZE + 1); 
		
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
		this.pilot.arc(12 * ang / Math.abs(ang), ang);
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
					e.printStackTrace();
				}
			}
			this.checkForward(false);
			if (this.rightNoWall) {
				// Turn right
				this.frontDistMonitor.pause();
				this.rightDistMonitor.pause();
				
				this.movementOffset = BLOCK_SIZE / 2;
				this.checkForward(true);
				
				this.doArc(-90 - this.rightDistMonitor.getTrend());
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
				this.checkForward(true);
				
				this.pilot.stop();
				while (!this.rightDistMonitor.isPaused()) {};
				
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
		
		this.pilot.setTravelSpeed(20);
		this.timer.reset();
		this.mapMaze();
		float time = this.timer.elapsed();
		time /= 1000;
		
		this.maze.drawMaze();
		this.frontDistMonitor.pause();
		this.rightDistMonitor.pause();
		
		Button.waitForAnyPress();
		
		new Graphics().clear();
		System.out.println("time to map maze: " + time);
		
		this.timer.reset();
		
		boolean isFirst = true;
		Movement moves[] = this.maze.getPathToBlack();
		
		this.pilot.setTravelSpeed(20);
		
		this.frontDistMonitor.setThreshold(25);
		this.rightDistMonitor.doNotPause();
		
		int travelDist = 0;
		int i = 0;
		for (Movement move : moves) {
			switch (move.getDirection()) {
			case LEFT:
				if (!isFirst) {
					travelDist -= 14;
				}
				break;
			case RIGHT:
				if (!isFirst) {
					travelDist -= 14;
				}
				break;
			default:
				break;
			}
			
			++i;
			if (move.getDirection() != Movement.Direction.FORWARD) {
				boolean isLast = i == moves.length;
				if (move.getDirection() == Movement.Direction.LEFT) {
					if (move.hasWall()) {
						this.frontWall = false;
						this.frontDistMonitor.resume();
						this.rightDistMonitor.resume();
						this.pilot.forward();
						do {
							synchronized (this.alert) {
								try {
									this.alert.wait(100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							if (this.frontWall && this.pilot.getMovementIncrement() < travelDist) {
								this.frontWall = false;
								this.frontDistMonitor.pause();
								this.frontDistMonitor.resume();
							}
						} while (!this.frontWall);
						this.pilot.setAcceleration(400);
						this.pilot.stop();
						this.pilot.setAcceleration(NORMAL_ACC);
						this.frontDistMonitor.pause();
						this.rightDistMonitor.pause();
						
						while (!this.rightDistMonitor.isPaused()) {};
						
						this.doArc(90);
						this.pilot.travel(BLOCK_SIZE * 2 / 3);
						travelDist = 0;
					} else {
						if (travelDist < 0) {
							this.pilot.rotate(90);
							travelDist = BLOCK_SIZE;
						} else {
							this.rightNoWall = false;
							this.rightDistMonitor.setTargetDist(LEFT_TRAGET_DIST);
							this.pilot.rotate(-180);
							this.rightDistMonitor.resume();
							this.pilot.backward();
							do {
								synchronized (this.alert) {
									try {
										this.alert.wait(100);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
								if (this.rightNoWall && Math.abs(this.pilot.getMovementIncrement()) < travelDist - 5) {
									this.rightNoWall = false;
									this.rightDistMonitor.pause();
									this.rightDistMonitor.resume();
								}
								if (! this.rightNoWall && Math.abs(this.pilot.getMovementIncrement()) > travelDist + 5) {
									this.rightNoWall = true;
									this.rightDistMonitor.pause();
								}
							} while (! this.rightNoWall);
							this.rightDistMonitor.pause();
							this.rightDistMonitor.setTargetDist(RIGHT_TRAGET_DIST);
							
							this.pilot.travel(-10);
							
							while (!this.rightDistMonitor.isPaused()) {};
							
							this.pilot.rotate(90);
							this.pilot.travel(-BLOCK_SIZE);
							if (!isLast) {
								this.pilot.rotate(-180);
							}
							travelDist = 0;
						}
					}
					continue;
				} else if (move.getDirection() == Movement.Direction.RIGHT && !isFirst) {
					this.rightNoWall = false;
					this.rightDistMonitor.resume();
					this.pilot.forward();
					do {
						synchronized (this.alert) {
							try {
								this.alert.wait(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						if (this.rightNoWall && Math.abs(this.pilot.getMovementIncrement()) < travelDist - 5) {
							this.rightNoWall = false;
							this.rightDistMonitor.pause();
							this.rightDistMonitor.resume();
						}
						if (! this.rightNoWall && Math.abs(this.pilot.getMovementIncrement()) > travelDist + 5) {
							this.rightNoWall = true;
							this.rightDistMonitor.pause();
						}
					} while (! this.rightNoWall);
					this.rightDistMonitor.pause();
					this.pilot.stop();
					
					while (!this.rightDistMonitor.isPaused()) {};
					
					this.doArc(-90 - this.rightDistMonitor.getTrend());
					this.pilot.travel(BLOCK_SIZE * 2 / 3);
					travelDist = 0;
					continue;
				} else {
					this.rightDistMonitor.resume();
					this.pilot.travel(travelDist);
				}
				this.rightDistMonitor.pause();
				while (!this.rightDistMonitor.isPaused()) {};
				travelDist = 0;
			}

			switch (move.getDirection()) {
			case LEFT:
				if (!isFirst) {
					this.doArc(90);
					this.pilot.travel(BLOCK_SIZE * 2 / 3);
					travelDist = 0;
				} else {
					pilot.rotate(90);
					travelDist = BLOCK_SIZE;
				}
				break;
			case RIGHT:
				if (!isFirst) {
					this.doArc(-90);
					this.pilot.travel(BLOCK_SIZE * 2 / 3);
					travelDist = 0;
				} else {
					pilot.rotate(-90);
					travelDist += BLOCK_SIZE;
				}
				break;
			case BACKWARD:
				this.pilot.rotate(-180);
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
		
		this.rightDistMonitor.resume();
		this.pilot.travel(travelDist);
		
		System.out.println("Time to reach black=" + (float)this.timer.elapsed() / 1000);
		
		this.rightDistMonitor.stop();
		this.frontDistMonitor.stop();
		
		try {
			this.rightDistMonitorThread.join();
			this.frontDistMonitorThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Button.waitForAnyPress();
		
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
