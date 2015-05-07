package ex6;

import lejos.geom.Point;
import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.addon.CompassHTSensor;
import lejos.nxt.addon.IRSeekerV2;
import lejos.robotics.localization.OdometryPoseProvider;
import lejos.robotics.localization.PoseProvider;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.robotics.navigation.Pose;
import lejos.robotics.navigation.Navigator;
import lejos.util.Delay;

public class Ex6Controller {
	
	static Point GOAL_POSITION = new Point(155, 3);
	static int NORMAL_ACC = 30;
	static int NORMAL_SPEED = 30;
	static int ROTATION_SPEED = 50;
	
	DifferentialPilot pilot;
	PoseProvider pose;
	Navigator nav;
	IRSeekerV2 seeker;
	UltrasonicSensor ultraFront;
	UltrasonicSensor ultraRight;
	NXTRegulatedMotor holderMotor;
	CompassHTSensor compass;
	
	DistanceMonitor distMonitor;
	Thread distMonitorThread;
	boolean holdingBall;
	
	public Ex6Controller() {
		pilot = new DifferentialPilot(DifferentialPilot.WHEEL_SIZE_RCX, 12.8, Motor.C, Motor.A);
		pilot.setTravelSpeed(NORMAL_SPEED);
		pilot.setRotateSpeed(ROTATION_SPEED);
		pilot.setAcceleration(NORMAL_ACC);
		pose = new OdometryPoseProvider(pilot);
		pose.setPose(new Pose());
		
		nav = new Navigator(pilot, pose);
		
		seeker = new IRSeekerV2(SensorPort.S1, IRSeekerV2.Mode.AC);
		ultraFront = new UltrasonicSensor(SensorPort.S4);
		ultraRight = new UltrasonicSensor(SensorPort.S3);
		holderMotor = Motor.B;
		compass = new CompassHTSensor(SensorPort.S2);
		
		distMonitor = new DistanceMonitor(this, this.ultraFront, 7);
		distMonitorThread = new Thread(this.distMonitor);
		holdingBall = false;
	}
	
	public void start() {
		compass.resetCartesianZero();
		distMonitorThread.start();
		
		for (int i = 0; i < 4; ++i) {
			this.goToBall();
			this.kick();
			this.resetPosition();
		}
		
		this.distMonitor.stop();
		this.resetPosition();
		
		try {
			distMonitorThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("done");
	}
	
	public void handleDistance(int dist) {
		// Slow down when approaching the ball
		this.pilot.setTravelSpeed(Math.min(NORMAL_SPEED, dist));
	}
	
	public void goToBall() {
		boolean haveBall = false;
		while (! haveBall) {
			this.raiseHolder();
			this.distMonitor.resume();
			System.out.println("moving forward");
			this.pilot.forward();
			while (! this.holdingBall) {
				float angle = this.seeker.getAngle(false);
				if (angle != 0) {
					this.pilot.stop();
					this.pilot.rotate(-angle);
					this.pilot.forward();
				}
			}
			this.pilot.stop();
			
			int dist = 255;
			while (dist == 255) {
				dist = this.ultraFront.getDistance();
			}
			haveBall = dist < 10;
		}
	}
	
	public void kick() {
		// Rotate towards the goal
		Point myLoc = this.pose.getPose().getLocation();
		Point toGoal = GOAL_POSITION.subtract(myLoc);
		double headingToGoal = 180 * Math.atan2(toGoal.getY(), toGoal.getX()) / Math.PI;
		
		System.out.println("p=" + (int)myLoc.getX() + "," + (int)myLoc.getY());
		System.out.println("tg=" + (int)toGoal.getX() + "," + (int)toGoal.getY());
		System.out.println("h=" + this.pose.getPose().getHeading());
		System.out.println("htg=" + headingToGoal);
		
		this.pilot.rotate(headingToGoal - this.pose.getPose().getHeading());
		
		// Kick
		this.pilot.setAcceleration(1000);
		this.pilot.setTravelSpeed(1000);
		this.pilot.travel(12, true);
		Delay.msDelay(50);
		this.raiseHolder();
		
		this.pilot.setAcceleration(NORMAL_ACC);
		this.pilot.setTravelSpeed(NORMAL_SPEED);
	}
	
	public void resetPosition() {
		this.nav.goTo(0, 0, 0);
		this.nav.waitForStop();
		float deg = 3;
		while (Math.abs(deg) > 2) {
			do {
				deg = compass.getDegreesCartesian();
			} while (deg == -1);
			if (deg > 180) {
				deg = deg - 360;
			}
			System.out.println("fix heading " + deg);
			this.pilot.rotate(-deg);
		}
		
		int dist = 255;
		while (dist == 255) {
			dist = this.ultraRight.getDistance();
		}
		int toTravel = 53 - dist; 
		if (Math.abs(toTravel) > 2) {
			this.pilot.rotate(90);
			this.pilot.travel(toTravel);
			this.pilot.rotate(-90);
		}
		
		deg = 3;
		while (Math.abs(deg) > 2) {
			do {
				deg = compass.getDegreesCartesian();
			} while (deg == -1);
			if (deg > 180) {
				deg = deg - 360;
			}
			System.out.println("fix heading " + deg);
			this.pilot.rotate(-deg);
		}
		
		this.pose.setPose(new Pose());
	}
	
	
	public void handleDistanceThreshold() {
		this.lowerHolder();
	}
	
	public synchronized void lowerHolder() {
		if (! this.holdingBall) {
			this.holdingBall = true;
			holderMotor.rotate(-70);
		}
	}
	
	public synchronized void raiseHolder() {
		if (this.holdingBall) {
			this.holdingBall = false;
			holderMotor.rotate(70);
		}
	}
	
}
