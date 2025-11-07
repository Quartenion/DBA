package model;

/**
 *
 * @author adam
 * 
 * All the methods are synchronized because the agent works in the JADE
 * thread and can access all the methods, and the controller runs in his own
 * thread and it can access all the methods too
 */
public class Environment {
    
    private int agentX;
    private int agentY;
    private int goalX;
    private int goalY;
    
    // Activates or deactivates the agent
    private boolean run = false;
    
    public synchronized void stop(){if (run)  run = false;}
    public synchronized void run() {if (!run) run = true; }
    public synchronized boolean isRunning() {return run;}
    
    // Getters and Setters

    public synchronized int getAgentX() { return agentX; }
    public synchronized int getAgentY() { return agentY; }
    
    /**
     * @param x
     * @param y 
     * @pre position is valid in the world
     */
    public synchronized void setAgentPosition(int x, int y) {
        System.out.println(x + " " +y);
        this.agentX = x;
        this.agentY = y;
    }

    public synchronized int getGoalX() { return goalX; }
    public synchronized int getGoalY() { return goalY; }
        
    /**
     * @param x
     * @param y 
     * @pre position is valid in the world
     */
    public synchronized void setGoalPosition(int x, int y) {
        this.goalX = x;
        this.goalY = y;  
    }
    
    // Lógica de negocio en el modelo
    public synchronized boolean isGoalReached() {
        return agentX == goalX && agentY == goalY;
    }
    
    
    
    
}
