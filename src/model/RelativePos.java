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
    
    private final int r;
    private final int c;

    public RelativePos(int deltaR, int deltaC) {
        this.r = deltaR;
        this.c = deltaC;
    }

    public int getX() { return r; }
    public int getY() { return c; }

    
    public RelativePos eval(int x, int y){
        return new RelativePos(x+this.r,y+this.c);
    }
    
    public RelativePos eval(RelativePos pos){
        return eval(pos.getX(), pos.getY());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelativePos that = (RelativePos) o;
        return r == that.r && c == that.c;
    }

    @Override
    public int hashCode() {
        return Objects.hash(r, c);
    }
}
