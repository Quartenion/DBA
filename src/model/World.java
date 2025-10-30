package model;

public class World {
    
    private int[][] map;
    private int rows;
    private int cols;
    
    public void changeMap(int[][] map, int rows, int cols){
        this.map = map;
        this.rows = rows;
        this.cols = cols;
    }

    public boolean isValidPosition(int x, int y){
        return (x >= 0 && x < rows && y >= 0 && 
                y < cols && map[x][y] != -1);
    }

    // Métodos existentes (sin cambios)
    public int getRows() { return this.rows; }
    public int getCols() { return this.cols; }
    public int[][] getMap() { return map; }
    public int getTile(int i, int j) { return this.map[i][j]; }
    public void setRows(int rows) { this.rows = rows; }
    public void setColumns(int columns) { this.cols = columns; }
    public void setMap(int[][] map) { this.map = map; }
    public boolean empty() { return (this.map == null || this.map.length == 0); }
}