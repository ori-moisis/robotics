package ex6;

import java.io.DataOutputStream;
import java.io.IOException;

import javax.bluetooth.RemoteDevice;

import lejos.nxt.Button;
import lejos.nxt.SensorPort;
import lejos.nxt.addon.IRSeekerV2;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;
import lejos.util.Delay;

public class GoalValidator {

	int SENSOR_THRESH = 100;
	int VALIDATION_TIMES = 5;
	int DELAY = 100;
	
	BTConnection connection;
	IRSeekerV2 ir;
	
	public GoalValidator()	{
		ir = new IRSeekerV2(SensorPort.S1, IRSeekerV2.Mode.AC);
	}
	
	public void start() {
		RemoteDevice receiver = Bluetooth.getKnownDevice("RED");
		do {
			this.connection = Bluetooth.connect(receiver);
			if (this.connection == null) {
				System.out.println("Failed, try again");
			}
		} while (this.connection == null);
		
		System.out.println("connected to RED");
		DataOutputStream output = connection.openDataOutputStream();
		
		int num_validated = 0;
		while (true) {
			int maxVal = 0;
			int[] vals = ir.getSensorValues();
			for (int v : vals) {
				if (v != 255) {
					maxVal = Math.max(v, maxVal);
				}
			}
			//System.out.println("max=" + maxVal + " v=" + num_validated);
			if (maxVal > SENSOR_THRESH) {
				if (++num_validated == VALIDATION_TIMES) {
					try {
						output.writeBoolean(true);
						output.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				num_validated = 0;
			}
			Delay.msDelay(DELAY);
		}
	}
	
	
	public static void main(String[] args) {
		System.out.println("Press any key to start");
		Button.waitForAnyPress();
		GoalValidator validator = new GoalValidator();
		validator.start();
		
		System.out.println("Done");
		Button.waitForAnyPress();
		
	}

}
