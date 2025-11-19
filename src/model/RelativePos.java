package model;


import java.util.Objects;

/**
 * 
 * @author adam
 * 
 * Class to store tiles
 * Mainly used by the Enviroment of the Agent
 * Its used to store in Set or Map 
 */
public class RelativePos {
    
    private final int x;
    private final int y;

    public RelativePos(int deltaX, int deltaY) {
        this.x = deltaX;
        this.y = deltaY;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    
    public RelativePos eval(int x, int y){
        return new RelativePos(x+this.x,y+this.y);
    }
    
    public RelativePos eval(RelativePos pos){
        return eval(pos.getX(), pos.getY());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelativePos that = (RelativePos) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
