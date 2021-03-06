package model;

import controller.Player;
import model.tiles.Door;
import model.tiles.Hall;
import model.tiles.Room;
import model.tiles.Tile;
import model.tiles.Warp;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import view.BoardCanvas;

/**
 * The class that represents and handles the board
 * 
 * @author Carl
 *
 */
public class Board {
	public int tileSize = 20;
	public int wallThickness = 2;
	
	private Map<Character, Room> rooms = new HashMap<Character, Room>();
	private Set<Hall> spawn = new HashSet<Hall>();
	private Tile[][] board;

	// Enum to represent the direction the door is facing
	public enum Direction {
		UP, DOWN, LEFT, RIGHT, WARP
	}

	public Board(int x, int y) {
		board = new Tile[x][y];

		// Initialise the board as tiles
		for (int i = 0; i < x; i++) {
			for (int j = 0; j < y; j++) {
				board[i][j] = new Hall(i, j);
			}
		}
	}

	/**
	 * moves the player in the given direction
	 * 
	 * @param player
	 *            The player to be moved
	 * @param 
TODO:
	 * @return whether the move was successful
	 */
	public boolean move(Player player, Tile tile, boolean finalTile) {
		//TODO: verify that there isn't another player on the tile if not allowed
		
		Tile fromTile = player.getTile();
		//if the player is reaching their destination then make sure they are viewed as right up
		tile.setPlayer(player);
		fromTile.removePlayer(player);
		return true;
	}
	
	/**
	 * Finds a path between two tiles
	 * @param fromTile
	 * @param toTile
	 * @return
	 */
	public List<Tile> findPath(Tile fromTile, Tile toTile){
		return PathFinder.findPath(this, fromTile, toTile);
	}

	/**
	 * Adds a player to a king prawn point
	 * 
	 * @param p
	 *            the player to be added
	 */
	public void addPlayer(Player p) {
		for (Hall h : spawn) {
			if (!h.containsPlayer()) {
				h.setPlayer(p);
				p.setTile(h);
				return;
			}
		}
	}
	
	/**
	 * Draws the board to the specified canvas
	 * @param g
	 */

	public void drawBoard(Graphics g, Tile hoverTile, Tile selectedTile){
		if(hoverTile != null && hoverTile.getKey() == '#'){hoverTile = null;} //Ignore the blank room as the selected tile
		if(selectedTile != null && selectedTile.getKey() == '#'){selectedTile = null;}
		
		//Draw the underlying tiles
		for (int i = 0; i < board.length; i++){
			for (int j = 0; j < board[0].length; j++){
				board[i][j].draw(g, i, j, tileSize);
				if(hoverTile != null && board[i][j] == hoverTile || 
						(board[i][j] instanceof Door && ((Door)board[i][j]).getRoom() == hoverTile)){
					 drawHighlightedTile(g, i, j);
				}
				if(selectedTile != null && board[i][j] == selectedTile ||
						(board[i][j] instanceof Door && ((Door)board[i][j]).getRoom() == selectedTile)){
					 drawSelectedTile(g, i, j);
				}
				//If a player is on the tile, draw that as well
				if (board[i][j].getPlayer() != null){
					Image img = board[i][j].getPlayer().getImage();
					g.drawImage(img, i*tileSize, j*tileSize, tileSize, tileSize, null);
				}
			}
		}
		
		//Iterate through again, and draw walls where appropriate
		g.setColor(Color.BLACK);
		int avg = wallThickness/2;
		for (int i = 0; i < board.length; i++){
			for (int j = 0; j < board[0].length; j++){
				if (board[i][j] instanceof Hall){ continue; }
				Tile r = board[i][j];
				
				//UP
				if (j > 0 && (!r.canMoveTo(board[i][j-1]) || !board[i][j-1].canMoveTo(r))){
					g.fillRect(i*tileSize, j*tileSize-avg, tileSize, wallThickness);
				}
				
				//DOWN
				if (j < board[0].length-1 && (!r.canMoveTo(board[i][j+1]) || !board[i][j+1].canMoveTo(r))){
					g.fillRect(i*tileSize, (j+1)*tileSize-avg, tileSize, wallThickness);
				}
				
				//LEFT
				if (i > 0 && (!r.canMoveTo(board[i-1][j]) || !board[i-1][j].canMoveTo(r))){
					g.fillRect(i*tileSize-avg, j*tileSize, wallThickness, tileSize);
				}
				
				//RIGHT
				if (i < board.length-1 && (!r.canMoveTo(board[i+1][j]) || !board[i+1][j].canMoveTo(r))){
					g.fillRect((i+1)*tileSize-avg, j*tileSize, wallThickness, tileSize);
				}	
			}
		}	
	}
	
	/**
	 * Draws the details surrounding the room, such as names,
	 * and the current players inside it
	 * @param g
	 */
	public void drawRoomDetails(Graphics g){
		FontMetrics fm = g.getFontMetrics();
		
		for (Room r : rooms.values()){
			if (r.getKey() == '#'){continue;}
			Rectangle2D bounds = fm.getStringBounds(r.getName(), g);
			int midX = (int)(r.getX() + r.getWidth()/2)*tileSize;
			int midY = (int)(r.getY() + r.getHeight()/2)*tileSize;

			//Draw the Room name
			g.drawString(r.getName(), (int)(midX-bounds.getWidth()/2), midY);
			
			//Draw the players standing in the room
			int offset = midX-(tileSize*r.getPlayers().size()/2);
			for (Player p : r.getPlayers()){
				Image img = p.getImage();
				g.drawImage(img, offset, midY, tileSize, tileSize, null);
				offset += tileSize;
			}
			
			//Draw the weapons in the room
			offset = midX-(tileSize*r.getWeapons().size()/2);
			for (Weapons w : r.getWeapons()){
				Image img = BoardCanvas.loadImage("weapons/"+w.toString().toLowerCase()+".png");
				g.drawImage(img, offset, midY+tileSize, tileSize, tileSize, null);
				offset += tileSize;
			}
		}
	}

	private void drawHighlightedTile(Graphics g, int i, int j) {	
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(i*tileSize, j*tileSize, tileSize, tileSize);		
	}
	
	private void drawSelectedTile(Graphics g, int i, int j) {
		g.setColor(Color.magenta);
		g.fillRect(i*tileSize, j*tileSize, this.tileSize, this.tileSize);
	}
	
	/**
	 * Gets the tile in the direction of the provided tile, or null if there is
	 * no eligible tile in that direction.
	 * 
	 * @param tile
	 *            the tile to move from
	 * @param dir
	 *            the direction to move
	 * @return the tile in the given direction, or null if there is none
	 */
	public Tile getTile(Tile tile, Direction dir) {
		if (tile instanceof Room) {
			// Check whether any of the doors can move in that direction
			for (Tile t : ((Room) tile).getEntrances()) {
				Tile target = getTile(t, dir);
				if (target != null) {
					return target;
				}
			}

			return null;
		}

		// Check whether we're trying to move the wrong way through a door
		if (tile instanceof Door && ((Door) tile).getDirection() != dir) {
			return null;
		}

		int x = tile.getX();
		int y = tile.getY();

		// Get the tile in the corresponding direction
		switch (dir) {
		case UP:
			return y > 0 ? board[x][y - 1] : null;
		case DOWN:
			return y < board.length - 1 ? board[x][y + 1] : null;
		case LEFT:
			return x > 0 ? board[x - 1][y] : null;
		case RIGHT:
			return x < board.length - 1 ? board[x + 1][y] : null;
		case WARP:
			if (tile instanceof Warp) {
				return ((Warp) tile).getTarget();
			}
		}
		return null;
	}

	/**
	 * Parses the board from a scanner containing a valid board representation
	 * 
	 * @param sc
	 * @return
	 */
	public static Board parseBoard(String filename) {
		Scanner sc;

		InputStream input = Board.class.getClassLoader().getResourceAsStream(filename);
		
		try {
			sc = new Scanner(input);
		} catch (Exception e1) {
			e1.printStackTrace();
			throw new RuntimeException("Could not parse: File not found");
		}

		int width;
		int height;

		sc.useDelimiter("[;\r\n]+");

		// Parse the rooms from the file
		Map<Character, Room> rooms = parseRooms(sc);

		Pattern intRegex = Pattern.compile("[0-9]+");

		// Parse the board
		try {
			width = Integer.parseInt(sc.next(intRegex));
			height = Integer.parseInt(sc.next(intRegex));

		} catch (RuntimeException e) {
			// Catch the exception if something goes wrong, and provide a more
			// useful error
			throw new RuntimeException(
					"Could not parse: Board dimensions not declared");
		}

		//
		Board board = new Board(width, height);
		board.rooms = rooms;

		// Parse the tokens from file
		parseTokens(sc, board);
		linkTokens(board.board);

		sc.close();

		return board;
	}

	/**
	 * Parses each character in the scanner into a 2D tile array
	 * 
	 * @param sc
	 * @param width
	 * @param height
	 */
	private static Tile[][] parseTokens(Scanner sc, Board b) {
		String line;
		int width = b.board.length;
		int height = width > 0 ? b.board[0].length : 0;

		Tile[][] board = b.board;

		// Maps the tiles to the warps
		Map<Character, Warp> warps = new HashMap<Character, Warp>();

		// Parse the board tokens
		for (int i = 0; i < width; i++) {
			if ((line = sc.next()) == null || line.length() < height) {
				throw new RuntimeException(
						"Could not parse: Not enough tokens provided for board size");
			}

			for (int j = 0; j < height; j++) {
				char token = line.charAt(j);
				if (b.rooms.containsKey(token)) {
					board[j][i] = b.rooms.get(token);
					((Room)board[j][i]).addLocation(j, i);
				} else {
					// Parse the default tokens
					switch (token) {
					case '_': // Hallway
						board[j][i] = new Hall(j, i);
						break;
					case '?': // Spawn Token
						Hall h = new Hall(j, i);
						b.spawn.add(h);
						board[j][i] = h;
						break;
					// Door tokens
					case 'u':
						board[j][i] = new Door('^', Direction.UP, j, i);
						break;
					case 'd':
						board[j][i] = new Door('v', Direction.DOWN, j, i);
						break;
					case 'l':
						board[j][i] = new Door('<', Direction.LEFT, j, i);
						break;
					case 'r':
						board[j][i] = new Door('>', Direction.RIGHT, j, i);
						break;
					default:
						// Default is assume we have a warp tile token
						Warp warp = new Warp(token, j, i);
						board[j][i] = warp;

						// Check to see if we've already found the matched warp
						// tile
						if (warps.containsKey(token)) {
							Warp other = warps.get(token);

							if (other == null) {
								throw new RuntimeException(
										"Warp tokens can only match 1 other warp token");
							}

							// Link the tiles
							warp.setTarget(other);
							other.setTarget(warp);

							// set the token to null to indicate it's been
							// matched
							warps.put(token, null);
						} else {
							warps.put(token, warp);
						}
					}
				}
			}
		}

		// Check all warps were paired
		for (Warp w : warps.values()) {
			if (w != null) {
				throw new RuntimeException("Unmatched warp token found");
			}
		}

		return board;
	}

	/**
	 * Link together the relevant tokens
	 * 
	 * @param board
	 */
	private static void linkTokens(Tile[][] board) {
		for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[i].length; j++) {
				Tile tile = board[i][j];

				// Link doors to their rooms
				if (tile instanceof Door) {
					Door door = (Door) tile;
					Tile r = null;

					try {
						switch (door.getDirection()) {
						case UP:
							r = board[i][j + 1];
							break;
						case DOWN:
							r = board[i][j - 1];
							break;
						case LEFT:
							r = board[i + 1][j];
							break;
						case RIGHT:
							r = board[i - 1][j];
							break;
						case WARP:
							// Check the surrounding tiles for a room tile that
							// isn't
							// the blank room
							outerLoop: for (int k = -1; k <= 1; k++) {
								for (int l = -1; l <= 1; l++) {
									r = board[i + k][j + l];

									// check the tile is adjacent, and then
									// check whether it's a room
									if ((l == 0 || k == 0) && r instanceof Room
											&& ((Room) r).getKey() != '#') {
										break outerLoop;
									}
									r = null; // reset r if it's wrong
								}
							}
							if (r == null) { // check we found a link
								throw new RuntimeException(
										"Could not parse: Warp tile was not connected to a room");
							}

						}
					} catch (IndexOutOfBoundsException e) {
						throw new RuntimeException("Could not parse: Door at "
								+ i + " " + j + " points off the board");
					}

					if (!(r instanceof Room)) {
						throw new RuntimeException("Could not parse: Door at "
								+ i + " " + j + " doesn't lead to a room");
					}

					Room room = (Room) r;
					room.addEntrance(door);
				}
			}
		}
	}

	/**
	 * Parse the room declarations from a file, adding them to the map of rooms
	 * 
	 * @param sc
	 */
	private static Map<Character, Room> parseRooms(Scanner sc) {
		Pattern roomReg = Pattern.compile("[A-Z]:.+");
		Map<Character, Room> rooms = new HashMap<Character, Room>();

		// This is a special case room key for squares the player can't go
		rooms.put('#', new Room("Blank", '#'));

		// Parse the room list
		while (sc.hasNext(roomReg)) {
			String[] rs = sc.next(roomReg).split(":");

			char key = rs[0].charAt(0);
			Room room = new Room(rs[1], key);
			if (rooms.containsKey(key)) {
				throw new RuntimeException(
						"Could not parse: Rooms declared with duplicate keys");
			}
			rooms.put(key, room);
		}

		return rooms;
	}

	@Override
	public String toString() {
		String rtn = "";

		for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[i].length; j++) {
				try {
					rtn += board[j][i].getKey() + " ";
				} catch (NullPointerException e) {
				}
			}
			rtn += "\n";
		}

		return rtn;
	}

	// Getters and setters
	public List<Room> getRooms() {
		List<Room> roomsList = new ArrayList<Room>(rooms.values());
		return roomsList;
	}
	
	public int getWidth() { return board.length; }
	public int getHeight(){ return board.length > 0 ? board[0].length : 0; }
	public Tile getTile(int x, int y){
		if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()){
			return board[x][y];
		}
		
		return null;
	}
}
