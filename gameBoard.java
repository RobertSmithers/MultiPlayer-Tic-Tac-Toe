import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class gameBoard implements Serializable{
	private static final long serialVersionUID = 1L;
	private int[][] board = new int[3][3];
	
	public gameBoard(){
	}
	
	public int[][] getBoard() {
		return board;
	}
	
	public void printBoard(){
		System.out.println();
		System.out.println(" TIC TAC TOE");
		System.out.println("*************");
		System.out.println("| "+this.board[0][0]+" | "+this.board[0][1]+" | "+this.board[0][2]+" |");
		System.out.println("*************");
		System.out.println("| "+this.board[1][0]+" | "+this.board[1][1]+" | "+this.board[1][2]+" |");
		System.out.println("*************");
		System.out.println("| "+this.board[2][0]+" | "+this.board[2][1]+" | "+this.board[2][2]+" |");
		System.out.println("*************");
	}
	
	//if full, and no winner => draw (tie game)
	public boolean boardFull(){
		for (int i=0; i<board.length;i++)
			for (int j=0; j<board[0].length; j++)
				if (board[i][j]==0) return false;
		return true;
	}
	
	//used by both server and client
	public void update(int row, int col, int x){
		this.board[row][col] = x;
	}
	
	public void printSlots(){
		List<String> slots = new ArrayList<String>();
		for (int i=0;i<board.length;i++)
			for (int j=0; j<board[0].length; j++)
				if (board[i][j]==0) slots.add((i+1) + "" + (j+1));
		System.out.println(slots);
	}
	
	public boolean validSlot(int pickedSlot) {
		List<Integer> slots = new ArrayList<Integer>();
		for (int i=0;i<board.length;i++)
			for (int j=0; j<board[0].length; j++)
				if (board[i][j]==0) slots.add(i*10 + j);

		if (slots.contains(pickedSlot))
			return true;
		else 
			return false;
	}
	
	//did server OR client win
	public boolean checkWin(){
		return checkRows() || checkCols() || checkDiags(); 
	}
	
	
	public boolean checkRows() {
		return checkArrForWin(board[0]) || checkArrForWin(board[1]) || checkArrForWin(board[2]);
	}

	public boolean checkCols(){
		int[] col0 = getCol(board, 0),
			col1 = getCol(board, 1),
			col2 = getCol(board, 2);
		return checkArrForWin(col0) || checkArrForWin(col1) || checkArrForWin(col2);
	}

	public boolean checkDiags(){
		int[] diag1 = {board[0][0], board[1][1], board[2][2]},
			diag2 = {board[0][2], board[1][1], board[2][0]};
		return checkArrForWin(diag1) || checkArrForWin(diag2);
	}

	private boolean checkArrForWin(int[] arr) {
		return (sum(arr) == 15 && !contains(arr, 0));
	}

	private int[] getCol(int[][] arr, int n) {
		int[] cols = new int[arr[0].length];
		for(int row = 0; row < arr[0].length; row++)
			cols[row] = arr[row][n];
		return cols;
	}

	private int sum(int[] rowCol) {
		int sum = 0;
		for (int val : rowCol) sum += val;
		return sum;
	}

	private boolean contains(int[] arr, int val) {
		for (int v : arr)
			if (v == val) return true;
		return false;
	}
	
	//checks for valid spots in the board
	//used by the server
	public ArrayList<Integer> validSpots(){
		ArrayList<Integer> choice = new ArrayList<>();
		for (int i=0;i<board.length;i++)
			for (int j=0; j<board[0].length; j++)
				if (board[i][j]==0) choice.add(i*10 + j);
		return choice;
	}

	public String getBoardIndices() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) {
				String val = Integer.toString(i+1) + Integer.toString(j+1);
				String border = "";
				if (j != 2) {
					border = "|";
				}

				// check if board cell is filled
				if (board[i][j] != 0) {
					val = "XX";
				}

				sb.append("  " + val + "  " + border);
			}
			sb.append("\n");
			if (i != 2) {
				sb.append("-".repeat(20) + "\n");
			}
		}
		return sb.toString();
	}
}

