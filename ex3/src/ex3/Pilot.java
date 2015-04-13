package ex3;

import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.LightSensor;
import lejos.util.Delay;
import ex3.Pilot.Wheels;

public class Pilot {

	static int HIST_LEN = 3;
	static int SENSOR_ROTATION = 10;
	static int NUM_SAMPLES = 3;
	
	class Wheels {
		public NXTRegulatedMotor left;
		public NXTRegulatedMotor right;
		float speed = 50;
		
		public Wheels() {
			this.right = Motor.A;
			this.left = Motor.C;
			this.left.setSpeed(this.speed);
			this.right.setSpeed(this.speed);
		}
		
		public void forward() {
			this.setAcceleration(1000);
			this.left.forward();
			this.right.forward();
		}
		
		public void setSpeedDiff(int diff) {
//			int intDiff = 0;
//			if (diff > 1) {
//				intDiff = (int)(10 * Math.min(diff, 3));
//			} else if (diff < -1) {
//				intDiff = (int)(10 * Math.max(diff, -3));
//			}
			this.left.setSpeed(this.speed - diff);
			this.right.setSpeed(this.speed + diff);
		}
		
		public void setAcceleration(int acc) {
			this.left.setAcceleration(acc);
			this.right.setAcceleration(acc);
		}
		
		public void stop() {
			this.left.stop(true);
			this.right.stop();
		}
		
		public void resetTachoCount() {
			this.left.resetTachoCount();
			this.right.resetTachoCount();
		}
	}
	

	NXTRegulatedMotor sensorEng;
	LightSensor light;
	Wheels wheels;
	int leftHist[];
	int rightHist[];
	int currIndex;
	int lastLightVal;
	int angleCorrection;
	int turn;
	int initialLight;
	int minVal;
	
	public Pilot() {
		this.wheels = new Wheels();
		this.light = new LightSensor(SensorPort.S1, true);
		this.sensorEng = Motor.B;
		this.leftHist = new int[HIST_LEN];
		this.rightHist = new int[HIST_LEN];
		this.currIndex = 0;
		this.angleCorrection = 0;
		this.turn = 0;
		this.lastLightVal = 1000;
		for (int i = 0; i < HIST_LEN; ++i) {
			this.leftHist[i] = 0;
			this.rightHist[i] = 0;
		}
	}
	
	public void start() {
		this.light.setFloodlight(true);
		this.initialLight = this.light.readValue();
		this.minVal = this.initialLight; 
		this.sensorEng.rotate(SENSOR_ROTATION * ((NUM_SAMPLES - 1) / 2));
		
		this.wheels.forward();
		int sweepDirection = 1;
		while (true) {
			int val = this.light.readValue();
			if (val > this.lastLightVal) {
				//int sweepDirection = -this.turn;
				int bestPos = 0;
				int bestVal = 10000;
//				if (this.turn == 0) {
//					this.sensorEng.rotate(SENSOR_ROTATION);
//					sweepDirection = 1;
//				}
				for (int i = 0; i < NUM_SAMPLES; ++i) {
					val = this.light.readValue();
					if (val < bestVal) {
						bestPos = i;
						bestVal = val; 
					}
					if (i < NUM_SAMPLES - 1) {
						this.sensorEng.rotate(-sweepDirection * SENSOR_ROTATION);
					}
				}
				this.turn = sweepDirection * (bestPos - ((NUM_SAMPLES - 1) / 2));
				sweepDirection = -sweepDirection;
				
				this.minVal = Math.min(this.minVal, bestVal);
				
				System.out.println("turn=" + this.turn + " fm=" + (bestVal - this.minVal));
				this.wheels.setSpeedDiff(this.turn * 2 * (bestVal - this.minVal));
				
				// Move back to best pos
//				for (int i = 0; i < (NUM_SAMPLES - bestPos - 1); ++i) {
//					this.sensorEng.rotate(sweepDirection * SENSOR_ROTATION);
//				}
				
				this.lastLightVal = bestVal;
			} else {
				this.lastLightVal = val;
			}
			this.minVal = Math.min(this.minVal, this.lastLightVal);
		}
	}
}
