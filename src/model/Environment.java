package model;

/**
 *
 * @author adam
 */
public class Environment {
    
    private int agentX;
    private int agentY;
    private int goalX;
    private int goalY;
    
    // Activates or deactivates the 
    private boolean run = false;
    
    // Getters and Setters

    public int getAgentX() { return agentX; }
    public int getAgentY() { return agentY; }
    
    /**
     * @param x
     * @param y 
     * @pre position is valid in the world
     */
    public void setAgentPosition(int x, int y) {
        System.out.println(x + " " +y);
        this.agentX = x;
        this.agentY = y;
    }

    public int getGoalX() { return goalX; }
    public int getGoalY() { return goalY; }
        
    /**
     * @param x
     * @param y 
     * @pre position is valid in the world
     */
    public void setGoalPosition(int x, int y) {
        this.goalX = x;
        this.goalY = y;  
    }
    
        // Lógica de negocio en el modelo
    public boolean isGoalReached() {
        return agentX == goalX && agentY == goalY;
    }
    
}
