package ex1;

import javax.microedition.sensor.SensorInfo;
import javax.microedition.sensor.SensorListener;

import lejos.nxt.Button;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.SensorPortListener;
import lejos.nxt.TouchSensor;
import lejos.nxt.addon.CompassHTSensor;
import lejos.nxt.addon.CompassMindSensor;
import lejos.util.Delay;

public class Main {
	public static void main(String[] args) {
		System.out.println("hello world");
		
		Pilot p = new Pilot();
		p.start();
				
		Button.waitForAnyPress();
	}

}
