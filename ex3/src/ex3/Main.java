package ex3;

import lejos.nxt.Button;
import ex3.Pilot;

public class Main {

	public static void main(String[] args) {
		Pilot p = new Pilot();
		p.start();
		
		Button.waitForAnyPress();
	}

}
