package model;

public class World implements WorldViewInterface {
    
    private int[][] map;
    private int rows;
    private int cols;
    
    // Only used by controller
    public void changeMap(int[][] map, int rows, int cols){
        this.map = map;
        this.rows = rows;
        this.cols = cols;
    }
    
    // Only used by controller
    public boolean isValidPosition(int x, int y){
        return (x >= 0 && x < rows && y >= 0 && 
                y < cols && map[x][y] != -1);
    }
    
    // Getters for the view
    @Override
    public int getRows() { return this.rows; }
    @Override
    public int getCols() { return this.cols; }
    @Override
    public int[][] getMap() { return map; }
    @Override
    public int getTile(int i, int j) { return this.map[i][j]; }
    @Override
    public boolean empty() { return (this.map == null || this.map.length == 0); }
}