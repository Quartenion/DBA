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
    private int energyUsed = 0;

    
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
    
    
    // ============================================
    // SENSORES DEL AGENTE
    // ============================================

    /**
     * El agente percibe las 4 direcciones cardinales
     * @return array [UP, DOWN, LEFT, RIGHT] donde 0=libre, -1=bloqueado
     */
    public int[] perceive() {
        int[] perception = new int[4];

        // UP (fila - 1)
        perception[0] = isValidMovement(agentX - 1, agentY) ? 0 : -1;

        // DOWN (fila + 1)
        perception[1] = isValidMovement(agentX + 1, agentY) ? 0 : -1;

        // LEFT (columna - 1)
        perception[2] = isValidMovement(agentX, agentY - 1) ? 0 : -1;

        // RIGHT (columna + 1)
        perception[3] = isValidMovement(agentX, agentY + 1) ? 0 : -1;

        return perception;
    }

    private boolean isValidMovement(int x, int y) {
        return world.isValidPosition(x, y);
    }

    // ============================================
    // INFORMACIÓN SOBRE EL OBJETIVO
    // ============================================

    /**
     * Calcula la dirección hacia el objetivo
     * @return [deltaX, deltaY] - distancia en filas y columnas
     */
    public int[] getDirectionToGoal() {
        return new int[]{goalX - agentX, goalY - agentY};
    }

    // ============================================
    // ACCIONES DEL AGENTE
    // ============================================
    
    /**
     * Mueve al agente en la dirección indicada
     * @param direction "UP", "DOWN", "LEFT", "RIGHT"
     * @return true si el movimiento fue exitoso
     */
    public boolean moveAgent(String direction) {
        int newX = agentX;
        int newY = agentY;

        switch(direction.toUpperCase()) {
            case "UP":
                newX = agentX - 1;
                break;
            case "DOWN":
                newX = agentX + 1;
                break;
            case "LEFT":
                newY = agentY - 1;
                break;
            case "RIGHT":
                newY = agentY + 1;
                break;
            default:
                return false;
        }

        // Validar y ejecutar movimiento
        if (world.isValidPosition(newX, newY)) {
            agentX = newX;
            agentY = newY;
            System.out.println("Agente movido a: (" + agentX + ", " + agentY + ")");
            return true;
        }

        System.out.println("Movimiento inválido: " + direction);
        return false;
    }

    public int getEnergyUsed() {
        return energyUsed;
    }

    public void incrementEnergy() {
        energyUsed++;
    }
}
