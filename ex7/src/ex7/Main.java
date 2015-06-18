package ex7;

import lejos.nxt.Button;
import lejos.util.Delay;

public class Main {

	public static void main(String[] args) {
		System.out.println("Press any button to start");
		Button.waitForAnyPress();
		
//		Maze m = new Maze(6, 4, MazeBlock.Direction.SOUTH, null);
//		m.setWall();
//		m.forward(); m.setWall();
//		m.forward(); m.setWall();
//		m.forward(); m.setWall();
//		m.forward(); m.setWall();
//		m.forward(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.forward(); m.setWall();
//		m.forward(); m.setWall();
//		m.forward(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.forward(); m.setWall();
//		m.forward(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.forward(); m.setWall();
//		m.forward(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.forward(); m.turn();
//		m.forward(); m.setWall();
//		m.setBlack();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.forward(); m.turn();
//		m.forward(); m.turn();
//		m.forward(); m.setWall();
//		m.forward(); m.turn();
//		m.forward(); m.setWall();
//		m.forward(); m.turn();
//		m.forward(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.forward(); m.turn();
//		m.forward(); m.setWall();
//		m.forward(); m.turn();
//		m.forward(); m.setWall();
//		m.forward(); m.setWall();
//		m.forward(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.forward(); m.setWall();
//		m.forward(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.forward(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.forward(); m.turn();
//		m.forward(); m.turn();
//		m.forward(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.forward(); m.turn();
//		m.forward(); m.turn();
//		m.forward(); m.setWall();
//		m.forward(); m.setWall();
//		m.forward(); m.turn();
//		m.forward(); m.setWall();
//		m.forward(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
//		m.turn(); m.turn(); m.turn(); m.setWall();
		
//		m.drawMaze();
//		
//		Button.waitForAnyPress();
//		
//		Movement[] path = m.getPathToBlack();
//		for (Movement move : path) {
//			if (move != null) {
//				Delay.msDelay(500);
//				System.out.println(move.getDirection().name() + " w=" + move.hasWall());
//			}
//		}
		
		Ex7Controller controller = new Ex7Controller();
		controller.start();
		
		Button.waitForAnyPress();

	}

}
