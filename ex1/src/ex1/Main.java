package ex1;

import javax.microedition.sensor.SensorInfo;
import javax.microedition.sensor.SensorListener;

import lejos.nxt.Button;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.SensorPortListener;
import lejos.nxt.TouchSensor;
import lejos.util.Delay;

public class Main {
	
	static class MyListen implements SensorPortListener {
		boolean isForward = true;
		TouchSensor bump;
		
		public MyListen(TouchSensor bump) {
			this.bump = bump;
		}
		
		@Override
		public void stateChanged(SensorPort aSource, int aOldValue, int aNewValue) {
			if (bump.isPressed() && this.isForward) {
				this.isForward = false;
				Motor.A.forward();
				Motor.B.forward();
			}
		}
	}
	
	public static void main(String[] args) {
		System.out.println("hello world");
		
		Motor.A.backward();
		Motor.B.backward();
		SensorPort.S1.addSensorPortListener(new MyListen(new TouchSensor(SensorPort.S1)));
		Button.waitForAnyPress();
	}

}
