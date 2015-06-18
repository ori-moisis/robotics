package ex7;

public class Movement {
	public enum Direction {
		FORWARD, LEFT, RIGHT, BACKWARD;
	}
	
	private boolean hasWall;
	private Direction direction;
	
	public Movement(Direction direction) {
		this.direction = direction;
		this.hasWall = false;
	}
	
	public void setWall() {
		this.hasWall = true;
	}
	
	public boolean hasWall() {
		return this.hasWall;
	}
	
	public Direction getDirection() {
		return this.direction;
	}
}
