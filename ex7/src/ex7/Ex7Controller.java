package ex7;

import lejos.nxt.Button;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.addon.CompassHTSensor;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.util.Delay;
import lejos.util.Stopwatch;

public class Ex7Controller {
	static int NORMAL_ACC = 30;
	static int NORMAL_SPEED = 30;
	static int ROTATION_SPEED = 50;
	static float DEG_OFFSET = 162;
	static int WALL_THRESHOLD = 20;
	static int BLACK_THRESHOLD = 40;
	static int BLOCK_SIZE = 31;
	
	DifferentialPilot pilot;
	LightSensor light;
	UltrasonicSensor ultraFront;
	UltrasonicSensor ultraRight;
	CompassHTSensor compass;
	Stopwatch stopwatch;
	Maze maze;
	
	DistanceMonitor distMonitor;
	Thread distMonitorThread;
		
	public Ex7Controller() {
		pilot = new DifferentialPilot(DifferentialPilot.WHEEL_SIZE_RCX, 13.9, Motor.C, Motor.A);
		pilot.setTravelSpeed(NORMAL_SPEED);
		pilot.setRotateSpeed(ROTATION_SPEED);
		pilot.setAcceleration(NORMAL_ACC);
		
		
		ultraFront = new UltrasonicSensor(SensorPort.S1);
		compass = new CompassHTSensor(SensorPort.S2);
		ultraRight = new UltrasonicSensor(SensorPort.S3);
		light = new LightSensor(SensorPort.S4, true);
		
		stopwatch = new Stopwatch();
	}
	
	public void start() {
		
		Delay.msDelay(500);
				
		float deg = -1;
		while (deg == -1) {
			deg = compass.getDegrees();
		}
		int dir = ((int)((deg + 45) / 90)) % 4;
		System.out.println("dir=" + dir);
		maze = new Maze(6, 4, MazeBlock.Direction.values()[dir]);
		
		do {
			if (isBlack()) {
				maze.setBlack();
			}
			
			if (hasWall(ultraRight)) {
				maze.setWall();
				if (hasWall(ultraFront)) {
					this.pilot.rotate(90);
					maze.turn();
					maze.turn();
					maze.turn();
				} else {
					this.pilot.travel(BLOCK_SIZE);
					maze.forward();
				}
			} else {
				this.pilot.rotate(-90);
				this.pilot.travel(BLOCK_SIZE);
				maze.turn();
				maze.forward();
			}
			maze.drawMaze();
		} while (! maze.isFinished());
		maze.drawMaze();
	}
	
	public boolean isBlack() {
		int lval = 255;
		while (lval == 255) {
			lval = this.light.getLightValue();
		}
		System.out.println("l=" + lval);
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
