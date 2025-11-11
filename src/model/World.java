package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class World implements WorldViewInterface, WorldPerceptionInterface {
    
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
    
    // Getters for the view (WorldViewInterface)
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
    
    
    // Perception (WorldPerceptionInterface) TODO: percieve()
    /**
     * @param pos currentPos of the element
     * @param sensors sensors that indicate the elements to percieve
     * @return map with each sensor with a tile, in case it was valid
     */
    @Override
    public Map<RelativePos,Integer> percieve(RelativePos pos, ArrayList<RelativePos> sensors){
        Map<RelativePos, Integer> result = new HashMap<>();
       
        // Loop the sensors
        for (int i = 0; i < sensors.size(); i++){
            // Obtain the pos to look at
            RelativePos res = sensors.get(i).eval(pos);
            
            // Check if its valid (within the size of the matrix)
            if ((res.getX() < getRows() && res.getX() >= 0) && 
                (res.getY() < getCols() && res.getY() >= 0)){
                // Add the value in the map
                result.put(sensors.get(i), map[res.getX()][res.getY()]);
            }
            else{
                // Add -2 -> indicates that its not valid
                result.put(sensors.get(i), -2);
            }
        }
        
        // Add the tile of the current pos in the result too
        result.put(pos, map[pos.getX()][pos.getY()]);
        
        return result;
    }
}