package ex6;

import lejos.nxt.Button;

public class Main {
	
	public static void main(String[] args) {
		System.out.println("Press any button to start");
		Button.waitForAnyPress();
		Ex6Controller controller = new Ex6Controller();
		controller.start();
		
		Button.waitForAnyPress();
	}

}
