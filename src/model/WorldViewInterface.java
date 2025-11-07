package model;

/**
 *
 * @author adam
 */
public interface WorldViewInterface {
    public int getRows();
    public int getCols();
    public int[][] getMap();
    public int getTile(int i, int j);
    public boolean empty();
}
