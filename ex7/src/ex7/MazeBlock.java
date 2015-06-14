package ex7;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		
		public Direction opposite() {
			return this.right().right();
		}
	}
	
	private boolean isStart;
	private boolean isBlack;
	private boolean walls[];
	private MazeBlock nieghbors[];
	private int distToBlack;
	private Direction directionToBlack;
	
	@SuppressWarnings("deprecation")
	public MazeBlock() {
		this.isStart = false;
		this.isBlack = false;
		this.walls = new boolean[] {false, false, false, false};
		this.nieghbors = new MazeBlock[4];
		this.distToBlack = 100;
		this.directionToBlack = Direction.NORTH;
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
		this.distToBlack = 0;
		this.isBlack = isBlack;
	}

	public boolean hasWall(Direction direction) {
		return this.walls[direction.getOffset()];
	}
	
	public void addWall(Direction direction) {
		this.walls[direction.getOffset()] = true;
	}
	
	public void addNieghbor(Direction direction, MazeBlock neighbor) {
		this.nieghbors[direction.getOffset()] = neighbor;
	}
	
	public void updateNeighbors() {
		List<MazeBlock> blocksToUpdate = new ArrayList<MazeBlock>();
		
		for (Direction direction : Direction.values()) {
			MazeBlock neigh = this.nieghbors[direction.getOffset()];
			if (neigh != null) {
				if (neigh.setDistance(this.distToBlack + 1, direction.opposite())) {
					blocksToUpdate.add(neigh);
				}
			}
		}
		
		for (MazeBlock block : blocksToUpdate) {
			block.updateNeighbors();
		}
	}
	
	public boolean setDistance(int dist, Direction direction) {
		if (dist < this.distToBlack) {
			this.distToBlack = dist;
			this.directionToBlack = direction;
			return true;
		}
		return false;
	}
	
	public int getDistanceToBlack() {
		return this.distToBlack;
	}
	
	public Direction getDirectionToBlack() {
		return this.directionToBlack;
	}
	
	public MazeBlock getNieghbor(Direction direction) {
		return this.nieghbors[direction.getOffset()];
	}
}
