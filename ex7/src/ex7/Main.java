package ex7;

import lejos.nxt.Button;

public class Main {

	public static void main(String[] args) {
		System.out.println("Press any button to start");
		Button.waitForAnyPress();
		
//		Maze m = new Maze(6, 4, MazeBlock.Direction.SOUTH);
//		m.setWall();
//		m.forward();
//		
//		m.forward();
//		m.setWall();
//		m.forward();
//		m.setWall();
//		m.forward();
//		m.setWall();
//		m.forward();
//		m.setWall();
//		m.turn();
//		m.turn();
//		m.turn();
//		m.setWall();
//		m.forward();
//		m.setWall();
//		m.setBlack();
//		
//		
//		m.turn();
//		m.turn();
//		m.turn();
//		m.setWall();
//		
//		
//		m.turn();
//		m.turn();
//		m.turn();
//		m.setWall();
//		
//		
//		m.forward();
//		m.turn();
//		m.forward();
//		m.setWall();
//		m.forward();
//		
//		m.forward();
//		m.setWall();
//		m.forward();
//		m.forward();
//		m.setWall();
//		m.drawMaze();
		
		Ex7Controller controller = new Ex7Controller();
		controller.start();
		
		Button.waitForAnyPress();

	}

}
