package ex6;

import java.io.DataInputStream;
import java.io.IOException;

import lejos.nxt.comm.BTConnection;

public class GoalMonitor implements Runnable {

	Ex6Controller controller;
	BTConnection connection;
	boolean stopped;
	
	public GoalMonitor(Ex6Controller controller, BTConnection connection) {
		this.controller = controller;
		this.connection = connection;
		stopped = false;
	}
	
	@Override
	public void run() {
		DataInputStream input = this.connection.openDataInputStream();
		while (! this.stopped) {
			try {
				input.readBoolean();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.controller.goalScored();
		}
	}
	
	public void stop() {
		this.stopped = true;
	}

}
