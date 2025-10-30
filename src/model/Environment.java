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
    private World world;
    
    public Environment(World world) {
        this.world = world;
        
    }
    
    
    // Getters and Setters

    public int getAgentX() { return agentX; }
    public int getAgentY() { return agentY; }
    public void setAgentPosition(int x, int y) {
        System.out.println(x + " " +y);
        if (this.world.isValidPosition(x, y)) {
            this.agentX = x;
            this.agentY = y;
        }
    }

    public int getGoalX() { return goalX; }
    public int getGoalY() { return goalY; }
    public void setGoalPosition(int x, int y) {
        if (this.world.isValidPosition(x, y)) {
            this.goalX = x;
            this.goalY = y;
        }
    }
    
        // Lógica de negocio en el modelo
    public boolean isGoalReached() {
        return agentX == goalX && agentY == goalY;
    }
    
}
