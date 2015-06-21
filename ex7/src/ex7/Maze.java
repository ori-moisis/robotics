package ex7;


import javax.microedition.lcdui.Graphics;

public class Maze {
	public interface MazeFinishListener {
		void handleMazeFinish();
	}
	
	MazeBlock maze[][];
	int x,y;
	int xOffset, yOffset;
	int maxX, maxY;
    int rows, cols;
	boolean needTranspose;
	MazeBlock.Direction currentDirection;
	MazeBlock.Direction initialDirection;
	MazeBlock blackBlock;
	boolean isStarted;
	boolean isMoved;
	MazeFinishListener listener;
	
	public Maze(int rows, int cols, MazeBlock.Direction initialDirection, MazeFinishListener listener) {
        this.rows = rows;
        this.cols = cols;
        int blocks = Math.max(rows, cols);
		this.maze = new MazeBlock[blocks][blocks];
		for (int i = 0; i < blocks; ++i) {
			for (int j = 0; j < blocks; ++j) {
				this.maze[i][j] = new MazeBlock();
			}
		}
		this.x = 0;
		this.y = 0;
		this.xOffset = 0;
		this.yOffset = 0;
		this.maxX = 0;
		this.maxY = 0;
        this.needTranspose = false;
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
		
		Movement[] moves = new Movement[(int)Math.ceil(currBlock.getDistanceToBlack())];
		for (int i = 0; i < moves.length; ++i) {
			MazeBlock.Direction travelDirection = currBlock.getDirectionToBlack();
			MazeBlock nextBlock = currBlock.getNieghbor(travelDirection);
			
			if (travelDirection == currDirection) {
				moves[i] = new Movement(Movement.Direction.FORWARD);
				if (currBlock.hasWall(currDirection.right()) && nextBlock.hasWall(currDirection.right())) {
					moves[i].setWall();
				}
			} else if (travelDirection == currDirection.right()) {
				moves[i] = new Movement(Movement.Direction.RIGHT);
				if (currBlock.hasWall(currDirection.right())) {
					moves[i].setWall();
				}
			} else if (travelDirection == currDirection.opposite()) {
				moves[i] = new Movement(Movement.Direction.BACKWARD);
			} else {
				moves[i] = new Movement(Movement.Direction.LEFT);
				if (currBlock.hasWall(currDirection)) {
					moves[i].setWall();
				}
			}
			
			currBlock = nextBlock;
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
		this.maxX = Math.max(this.maxX, x);
		this.maxY = Math.max(this.maxY, y);
		
		MazeBlock currBlock = this.getBlock(x, y, false);
		
		prevBlock.addNieghbor(this.currentDirection, currBlock);
		currBlock.addNieghbor(this.currentDirection.opposite(), prevBlock);
		
		if (this.isFinished() && this.listener != null) {
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
            if (this.needTranspose) {
                realX = this.cols - 1 - y;
                realY = x;
            }
			realX += this.xOffset;
			realY += this.yOffset;
		} else if (this.maxX - this.xOffset > this.rows ||
				this.maxY - this.yOffset > this.cols) {
            this.needTranspose = true;
        }
        realX = ((realX % this.maze.length) + this.maze.length) % this.maze.length;
        realY = ((realY % this.maze[0].length) + this.maze[0].length) % this.maze[0].length;
		return this.maze[realX][realY];
	}
	
	public void drawMazeNoNXT() {
        System.out.println("");
        System.out.println("");

        MazeBlock.Direction possibles[][][] = new MazeBlock.Direction[][][] {
                {{MazeBlock.Direction.NORTH, MazeBlock.Direction.WEST}, {MazeBlock.Direction.NORTH}, {MazeBlock.Direction.NORTH, MazeBlock.Direction.EAST}},
                {{MazeBlock.Direction.WEST}, {}, {MazeBlock.Direction.EAST}},
                {{MazeBlock.Direction.SOUTH, MazeBlock.Direction.WEST}, {MazeBlock.Direction.SOUTH}, {MazeBlock.Direction.SOUTH, MazeBlock.Direction.EAST}}
        };
		
		for (int i = 0; i < this.rows * 3; ++i) {
			for (int j = 0; j < this.cols * 3; ++j) {
				MazeBlock block = this.getBlock(i / 3, j / 3);
                String c = "   ";

                for (MazeBlock.Direction dir : possibles[i%3][j%3]) {
                    MazeBlock.Direction actDir = this.needTranspose ? dir.right().right().right() : dir;
                    if (block.hasWall(actDir)) {
                        if (i%3 != 1) {
                            if (j%3 == 1) {
                                c = "---";
                            } else {
                                c = "+++";
                            }
                        } else {
                            c = "|||";
                        }
                    }
                }
                if (i%3 == 1 && j%3 == 1) {
                    if (block.isBlack()) {
                        c = " x ";
                    } else if (block == this.getBlock(x, y, false)) {
                        c = " o ";
                    }
                }
                System.out.print(c);
			}
            System.out.println();
		}
	}

	public void drawMaze() {
		Graphics g = new Graphics();
		g.clear();
		
		double xWidth = (g.getWidth() / this.rows) - 1;
		double yWidth = (g.getHeight() / this.cols) - 1;
	
		
		for (int i = 0; i < this.rows; ++i) {
			for (int j = 0; j < this.cols; ++j) {
				MazeBlock block = this.getBlock(i, j);
				block.x = i;
				block.y = j;
				if (this.needTranspose && block.hasWall(MazeBlock.Direction.WEST) ||
					!this.needTranspose && block.hasWall(MazeBlock.Direction.NORTH)) {
					g.drawLine((int)(i * xWidth),     (int)((cols - j) * yWidth), 
							   (int)(i * xWidth),     (int)((cols - j - 1) * yWidth));
				}
				if (this.needTranspose && block.hasWall(MazeBlock.Direction.EAST) || 
					!this.needTranspose && block.hasWall(MazeBlock.Direction.SOUTH)) {
					g.drawLine((int)((i+1) * xWidth), (int)((cols - j) * yWidth), 
							   (int)((i+1) * xWidth), (int)((cols - j - 1) * yWidth));
				}
				if (this.needTranspose && block.hasWall(MazeBlock.Direction.NORTH) || 
					!this.needTranspose && block.hasWall(MazeBlock.Direction.EAST)) {
					g.drawLine((int)(i * xWidth),     (int)((cols - j - 1) * yWidth), 
							   (int)((i+1) * xWidth), (int)((cols - j - 1) * yWidth));
				}
				if (this.needTranspose && block.hasWall(MazeBlock.Direction.SOUTH) || 
					!this.needTranspose && block.hasWall(MazeBlock.Direction.WEST)) {
					g.drawLine((int)(i * xWidth),     (int)((cols - j) * yWidth), 
							   (int)((i+1) * xWidth), (int)((cols - j) * yWidth));
				}
				if (block.isBlack()) {
					g.drawLine((int)(i * xWidth),     (int)((cols - j) * yWidth), 
							   (int)((i+1) * xWidth), (int)((cols - j - 1) * yWidth));
					
					g.drawLine((int)((i+1) * xWidth), (int)((cols - j) * yWidth), 
							   (int)(i * xWidth),     (int)((cols - j - 1) * yWidth));
				}
				if (block.isStart()) {
					g.drawRoundRect((int)((i)*xWidth) + 1, (int)((cols-j-1)*xWidth) + 1, (int)xWidth - 2, (int)yWidth - 2, (int)xWidth - 2, (int)yWidth - 2);
				}
				if (block == this.getBlock(x,y,false)) {
					g.drawRect((int)((i)*xWidth) + 4, (int)((cols-j-1)*xWidth) + 4, (int)xWidth - 8, (int)yWidth - 8);
				}
			}
		}
	}
}
