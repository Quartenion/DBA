package model;

import java.util.HashSet;
import java.util.Set;

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
    
    // Celdas visitadas (donde el agente ha ESTADO físicamente)
    private Set<String> visitedCells;
    
    // Celdas vistas (que el agente ha PERCIBIDO con sus sensores)
    private Set<String> seenCells;
    
    public Environment(World world) {
        this.world = world;
        this.visitedCells = new HashSet<>();
        this.seenCells = new HashSet<>();
    }
    
    
    // Getters and Setters
    public int getAgentX() { return agentX; }
    public int getAgentY() { return agentY; }
    public void setAgentPosition(int x, int y) {
        System.out.println(x + " " +y);
        if (this.world.isValidPosition(x, y)) {
            this.agentX = x;
            this.agentY = y;
            // Registrar como visitada
            addVisitedCell(x, y);
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
    // GESTIÓN DE MEMORIA
    // ============================================
    
    //Añade una celda a la memoria de visitadas
    public void addVisitedCell(int x, int y) { visitedCells.add(x + "," + y); }
    
    // Añade una celda a la memoria de vistas
    public void addSeenCell(int x, int y) { seenCells.add(x + "," + y); }
    
    
    // Comprueba si una celda ha sido visitada
    public boolean hasVisited(int x, int y) { return visitedCells.contains(x + "," + y); }
    
    // Comprueba si una celda ha sido vista
    public boolean hasSeen(int x, int y) { return seenCells.contains(x + "," + y); }
    
    
    // Obtiene todas las celdas visitadas
    public Set<String> getVisitedCells() { return new HashSet<>(visitedCells); }
    
    // Obtiene todas las celdas vistas
    public Set<String> getSeenCells() { return new HashSet<>(seenCells); }
    
    // Reinicia la memoria (útil al cargar nuevo mapa)
    public void clearMemory() {
        visitedCells.clear();
        seenCells.clear();
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
        
        // IMPORTANTE: Registrar las celdas percibidas en la memoria
        registerSeenCells();
        
        return perception;
    }
    
    /**
     * Registra todas las celdas adyacentes como "vistas"
     */
    private void registerSeenCells() {
        // La celda actual siempre se ve
        addSeenCell(agentX, agentY);
        
        // Registrar las 4 direcciones cardinales
        // UP
        if (agentX - 1 >= 0 && agentX - 1 < world.getRows()) {
            addSeenCell(agentX - 1, agentY);
        }
        // DOWN
        if (agentX + 1 >= 0 && agentX + 1 < world.getRows()) {
            addSeenCell(agentX + 1, agentY);
        }
        // LEFT
        if (agentY - 1 >= 0 && agentY - 1 < world.getCols()) {
            addSeenCell(agentX, agentY - 1);
        }
        // RIGHT
        if (agentY + 1 >= 0 && agentY + 1 < world.getCols()) {
            addSeenCell(agentX, agentY + 1);
        }
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
            
            // Registrar como visitada
            addVisitedCell(newX, newY);
            
            System.out.println("Agente movido a: (" + agentX + ", " + agentY + ")");
            return true;
        }
        
        System.out.println("Movimiento invalido: " + direction);
        return false;
    }

    public int getEnergyUsed() { return energyUsed; }

    public void incrementEnergy() { energyUsed++; }
    
    // Reiniciar energía
    public void resetEnergy() { energyUsed = 0; }
}