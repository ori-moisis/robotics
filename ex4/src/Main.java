

import lejos.nxt.Button;

public class Main {

	public static void main(String[] args) {
		Pilot p = new Pilot();
		p.start();
		
		Button.waitForAnyPress();
	}

}
