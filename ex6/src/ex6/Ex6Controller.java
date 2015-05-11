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
		pilot = new DifferentialPilot(DifferentialPilot.WHEEL_SIZE_RCX, 13.9, Motor.C, Motor.A);
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
			do {
				this.goToBall();
				this.kick();
			} while (! this.isGoal());
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
	
	public int getDistance(UltrasonicSensor sensor) {
		int dist = 255;
		while (dist == 255) {
			dist = sensor.getDistance();
		}
		return dist;
	}
	
	public float getBallAngle() {
		final int ANGLES[] = {-60, -30, 0, 30, 60};
		
		float angle = 0;
		int[] vals = this.seeker.getSensorValues();
		float sumVals = 0;
		for (int i = 0; i < vals.length; ++i) {
			sumVals += vals[i];
		}
		for (int i = 0; i < vals.length; ++i) {
			angle += ANGLES[i] * vals[i] / sumVals;
		}
		System.out.println("angle=" + angle);
		return angle;
	}
	
	public void goToBall() {
		boolean haveBall = false;
		while (! haveBall) {
			this.raiseHolder();
			this.distMonitor.resume();
			this.pilot.forward();
			while (! this.holdingBall) {
				float angle = this.getBallAngle();
				if (angle != 0) {
					this.pilot.setAcceleration(1000);
					this.pilot.stop();
					this.pilot.setAcceleration(NORMAL_ACC);
					do {
						this.pilot.rotate(-angle);
						angle = this.getBallAngle();
					} while (angle != 0);
					this.pilot.forward();
				}
			}
			this.pilot.stop();
			
			Delay.msDelay(500);
			haveBall = this.getDistance(this.ultraFront) < 10;
		}
	}
	
	public boolean isGoal() {
		return true;
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
		Delay.msDelay(100);
		this.raiseHolder();
		
		while (this.pilot.isMoving()) {
			Delay.msDelay(100);
		}
		Delay.msDelay(100);
		
		this.pilot.stop();
		this.pilot.setAcceleration(NORMAL_ACC);
		this.pilot.setTravelSpeed(NORMAL_SPEED);
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
			System.out.println("fix heading " + deg);
			this.pilot.rotate(-deg);
		}
	}
	
	public void resetPosition() {
		// Return to initial position
		this.nav.goTo(0, 0, 0);
		this.nav.waitForStop();
		
		// Fix position again
		this.resetHeading();
		int y = this.getDistance(this.ultraRight) - 53;
		if (true) {
			if (y != 0) {
				this.pilot.rotate(-90);
				this.pilot.travel(-y);
				this.pilot.rotate(90);
				this.pose.setPose(new Pose());
			}
		} else {
			this.pilot.rotate(-90);
			int x = this.getDistance(this.ultraRight) - 30;
			this.resetHeading();
			
			this.pose.setPose(new Pose(x, y, 0));
		}
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
