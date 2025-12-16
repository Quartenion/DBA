package controller;

/**
 * Pair class
 */
public class Pair<T0, T1> {
    public T0 first;
    public T1 second;
    
    public Pair(T0 first, T1 second) {
        this.first  = first;
        this.second = second;
    }
    
    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) obj;
        return first.equals(pair. first) && second.equals(pair.second);
    }
    
    @Override
    public int hashCode() {
        return 31 * first.hashCode() + second.hashCode();
    }
}