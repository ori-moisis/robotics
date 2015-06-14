package ex7;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.lcdui.Graphics;

import lejos.nxt.Button;
import ex7.MazeBlock.Direction;

public class Maze {
	public interface MazeFinishListener {
		void handleMazeFinish();
	}
	
	public enum Movement {
		FORWARD, LEFT, RIGHT, BACKWARD;
	}
	
	
	MazeBlock maze[][];
	int x,y;
	int xOffset, yOffset;
	MazeBlock.Direction currentDirection;
	MazeBlock.Direction initialDirection;
	MazeBlock blackBlock;
	boolean isStarted;
	boolean isMoved;
	MazeFinishListener listener;
	
	public Maze(int rows, int cols, Direction initialDirection, MazeFinishListener listener) {
		this.maze = new MazeBlock[rows][cols];
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				this.maze[i][j] = new MazeBlock();
			}
		}
		this.x = 0;
		this.y = 0;
		this.xOffset = 0;
		this.yOffset = 0;
		this.isStarted = false;
		this.isMoved = false;
		this.currentDirection = initialDirection;
		this.initialDirection = initialDirection;
		this.listener = listener;
	}
	
	public MazeBlock.Direction getDirection() {
		return this.currentDirection;
	}
	
	public void setWall() {
		if (!this.isStarted) {
			this.getBlock(x, y, false).setStart(true);
			this.initialDirection = this.currentDirection;
			this.isStarted = true;
		}
		this.getBlock(x, y, false).addWall(this.currentDirection.right());
	}
	
	public void setBlack() {
		this.blackBlock = this.getBlock(x, y, false); 
		this.blackBlock.setBlack(true);
	}
	
	public void turn() {
		this.currentDirection = this.currentDirection.right();
		if (this.isFinished() && this.listener != null) {
			this.listener.handleMazeFinish();
		}
	}
	
	public boolean isFinished() {
		return this.isMoved && this.getBlock(x, y, false).isStart() && currentDirection == initialDirection;
	}
	
	
	public Movement[] getPathToBlack() {
		// First, calculate all distances
		this.blackBlock.updateNeighbors();
		
		MazeBlock currBlock = this.getBlock(x, y, false);
		MazeBlock.Direction currDirection = this.currentDirection;
		
		Movement[] moves = new Movement[currBlock.getDistanceToBlack()];
		for (int i = 0; i < moves.length; ++i) {
			MazeBlock.Direction travelDirection = currBlock.getDirectionToBlack();
			System.out.println("td=" + travelDirection.name());
			if (travelDirection == currDirection) {
				moves[i] = Movement.FORWARD;
			} else if (travelDirection == currDirection.right()) {
				moves[i] = Movement.RIGHT;
			} else if (travelDirection == currDirection.opposite()) {
				moves[i] = Movement.BACKWARD;
			} else {
				moves[i] = Movement.LEFT;
			}
			
			currBlock = currBlock.getNieghbor(travelDirection);
			currDirection = travelDirection;
		}
		
		return moves;
	}
	
	public void forward() {
		int xDir = 0,yDir = 0;
		this.isMoved = true;
		switch (this.currentDirection) {
		case NORTH:
			xDir = -1;
			yDir = 0;
			break;
		case EAST:
			xDir = 0;
			yDir = 1;
			break;
		case SOUTH:
			xDir = 1;
			yDir = 0;
			break;
		case WEST:
			xDir = 0;
			yDir = -1;
			break;
		default:
			break;
		}
		
		MazeBlock prevBlock = this.getBlock(x, y, false);
		
		this.x += xDir;
		this.y += yDir;
		this.xOffset = Math.min(this.x, this.xOffset);
		this.yOffset = Math.min(this.y, this.yOffset);
		
		MazeBlock currBlock = this.getBlock(x, y, false);
		
		prevBlock.addNieghbor(this.currentDirection, currBlock);
		currBlock.addNieghbor(this.currentDirection.opposite(), prevBlock);
		
		if (this.isFinished()) {
			this.listener.handleMazeFinish();
		}
	}
	
	MazeBlock getBlock(int x, int y) {
		return this.getBlock(x, y, true);
	}
	
	MazeBlock getBlock(int x, int y, boolean applyOffset) {
		int realX = x;
		int realY = y;
		if (applyOffset) {
			realX += this.xOffset;
			realY += this.yOffset;
		}
		return this.maze[realX < 0 ? realX + this.maze.length : realX][realY < 0 ? realY + this.maze[0].length : realY]; 
	}
	
	public void drawMaze() {
		Graphics g = new Graphics();
		g.clear();
		
		int rows = this.maze.length;
		int cols = this.maze[0].length;
		double xWidth = (g.getWidth() / rows) - 1;
		double yWidth = (g.getHeight() / cols) - 1;
	
		
		for (int i = 0; i < this.maze.length; ++i) {
			for (int j = 0; j < this.maze[i].length; ++j) {
				MazeBlock block = this.getBlock(i, j);
				if (block.hasWall(MazeBlock.Direction.NORTH)) {
					g.drawLine((int)(i * xWidth),     (int)((cols - j) * yWidth), 
							   (int)(i * xWidth),     (int)((cols - j - 1) * yWidth));
				}
				if (block.hasWall(MazeBlock.Direction.SOUTH)) {
					g.drawLine((int)((i+1) * xWidth), (int)((cols - j) * yWidth), 
							   (int)((i+1) * xWidth), (int)((cols - j - 1) * yWidth));
				}
				if (block.hasWall(MazeBlock.Direction.EAST)) {
					g.drawLine((int)(i * xWidth),     (int)((cols - j - 1) * yWidth), 
							   (int)((i+1) * xWidth), (int)((cols - j - 1) * yWidth));
				}
				if (block.hasWall(MazeBlock.Direction.WEST)) {
					g.drawLine((int)(i * xWidth),     (int)((cols - j) * yWidth), 
							   (int)((i+1) * xWidth), (int)((cols - j) * yWidth));
				}
				if (block.isBlack()) {
					g.drawLine((int)(i * xWidth),     (int)((cols - j) * yWidth), 
							   (int)((i+1) * xWidth), (int)((cols - j - 1) * yWidth));
					
					g.drawLine((int)((i+1) * xWidth), (int)((cols - j) * yWidth), 
							   (int)(i * xWidth),     (int)((cols - j - 1) * yWidth));
				}
			}
		}
	}
}
