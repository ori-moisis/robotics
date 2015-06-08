package ex7;

import java.util.ArrayList;
import java.util.List;

public class MazeBlock {
	public enum Direction {
		NORTH (0, 0), 
		EAST (1, 85), 
		SOUTH (2, 167), 
		WEST (3, 300);
		
		private int offset;
		private int heading;
		
		Direction(int offset, int heading) {
			this.offset = offset;
			this.heading = heading;
		}
		
		public int getOffset() {
			return this.offset;
		}
		
		public int getHeading() {
			return this.heading;
		}
		
		public Direction right() {
			switch (this) {
			case NORTH:
				return EAST;
			case EAST:
				return SOUTH;
			case SOUTH:
				return WEST;
			default:
				return NORTH;
			}
		}
	}
	
	private boolean isStart;
	private boolean isBlack;
	private boolean walls[];
	private List<MazeBlock> nieghbors;
	
	public MazeBlock() {
		this.walls = new boolean[] {false, false, false, false};
		this.nieghbors = new ArrayList<MazeBlock>();
	}

	public boolean isStart() {
		return isStart;
	}

	public void setStart(boolean isStart) {
		this.isStart = isStart;
	}

	public boolean isBlack() {
		return isBlack;
	}

	public void setBlack(boolean isBlack) {
		this.isBlack = isBlack;
	}

	public boolean hasWall(Direction direction) {
		return this.walls[direction.getOffset()];
	}
	
	public void addWall(Direction direction) {
		this.walls[direction.getOffset()] = true;
	}
	
	public List<MazeBlock> getNieghbors() {
		return this.nieghbors;
	}
	
	public void addNieghbor(MazeBlock neighbor) {
		this.nieghbors.add(neighbor);
	}
}
