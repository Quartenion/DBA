package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author adam
 * 
 * All the methods are synchronized because the agent works in the JADE
 * thread and can access all the methods, and the controller runs in his own
 * thread and it can access all the methods too
 * 
 * 
 * MOVEMENT PIPELINE
 * 
 * SEE
 * -> agent starts on one tile
 * -> register that tile and all the adyacent tiles
 * 
 * THINK
 * 
 *
 * 
 * ACTION
 * 
 *  
 */


public class Environment {
    
    private int agentX;
    private int agentY;
    private int goalX;
    private int goalY;
    
    private int energyUsed = 0;
    
    private State state = State.STANDBY;
    
    private WorldPerceptionInterface world;
    
    // Celdas visitadas (donde el agente ha ESTADO físicamente)
    private Set<String> visitedCells;
    
    // Celdas vistas (que el agente ha PERCIBIDO con sus sensores)
    private Set<String> seenCells;
    
    // Activates or deactivates the agent
    private boolean run = false;

    public Environment(WorldPerceptionInterface w) {
        this.visitedCells = new HashSet<>();
        this.seenCells = new HashSet<>();
        this.world = w;
    }
    
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
        
        this.agentX = x;
        this.agentY = y;
        // Registrar como visitada
        addVisitedCell(x, y);
        
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
    
    // ============================================
    // GESTIÓN DE MEMORIA
    // ============================================
    
    
    // Cambia el estado interno del agente
    public void changeState(State s){
        this.state = s;
    }
    
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
       visitCountMap.clear();
    }
    
    // ============================================
    // SENSORES DEL AGENTE
    // ============================================

    /**
     * El agente percibe las 4 direcciones cardinales
     * @param sensors -> Relative positions from pos to represent the sensors
     * @return Map {RelativePos: } where 0=free, -1=block
     */
    public Map<RelativePos,Integer> perceive(ArrayList<RelativePos> sensors) {
        Map<RelativePos,Integer> perception = 
                        world.percieve(new RelativePos(agentX,agentY), sensors);
        
        // ADD PERCEPTION
        addSeenCell(agentX, agentY);
        
        for (int i = 0; i < sensors.size(); i++){
            int tile = perception.get(sensors.get(i));
            RelativePos pos = sensors.get(i).eval(new RelativePos(agentX,agentY));
            if (tile != -2){
                addSeenCell(pos.getX(), pos.getY());
            }
        }
   
        return perception;
    }
    
    /**
     * Registra todas las celdas adyacentes como "vistas"
     * DEPRECATED
     */
    /*
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

    */
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
     */
    public void moveAgent(String direction) {
        int newX = agentX;
        int newY = agentY;
        
        switch(direction.toUpperCase()) {
            case "UP" -> newX = agentX - 1;
            case "DOWN" -> newX = agentX + 1;
            case "LEFT" -> newY = agentY - 1;
            case "RIGHT" -> newY = agentY + 1;
            default -> {
                newX = agentX;
                newY = agentY;
            }
        }
        
        // TODO ELIMINAR
        // Validar y ejecutar movimiento

        agentX = newX;
        agentY = newY;

        // Registrar como visitada
        addVisitedCell(newX, newY);
        
        // Incrementar contador de visitas
        incrementVisitCount(newX, newY);

        System.out.println("Agente movido a: (" + agentX + ", " + agentY + ")");

    }

    public int getEnergyUsed() { return energyUsed; }

    public void incrementEnergy() { energyUsed++; }
    
    // Reiniciar energía
    public void resetEnergy() { energyUsed = 0; }
    
    /**
    * Obtiene información sobre qué direcciones han sido visitadas
    * @return array [UP visitado, DOWN visitado, LEFT visitado, RIGHT visitado]
    *         donde true = ya visitada, false = no visitada
    */
   public boolean[] getVisitedDirections() {
       boolean[] visited = new boolean[4];

       // UP (fila - 1)
       visited[0] = hasVisited(agentX - 1, agentY);

       // DOWN (fila + 1)
       visited[1] = hasVisited(agentX + 1, agentY);

       // LEFT (columna - 1)
       visited[2] = hasVisited(agentX, agentY - 1);

       // RIGHT (columna + 1)
       visited[3] = hasVisited(agentX, agentY + 1);

       return visited;
   }

   /**
    * Cuenta cuántas veces se ha visitado una celda
    * (Para implementación avanzada de backtracking)
    */
   private java.util.Map<String, Integer> visitCountMap = new java.util.HashMap<>();

   public void incrementVisitCount(int x, int y) {
       String key = x + "," + y;
       visitCountMap.put(key, visitCountMap.getOrDefault(key, 0) + 1);
   }

   public int getVisitCount(int x, int y) {
       return visitCountMap.getOrDefault(x + "," + y, 0);
   }

   /**
    * Obtiene el conteo de visitas de las 4 direcciones
    * @return array [UP count, DOWN count, LEFT count, RIGHT count]
    */
   public int[] getVisitCounts() {
       int[] counts = new int[4];

       // UP
       counts[0] = getVisitCount(agentX - 1, agentY);

       // DOWN
       counts[1] = getVisitCount(agentX + 1, agentY);

       // LEFT
       counts[2] = getVisitCount(agentX, agentY - 1);

       // RIGHT
       counts[3] = getVisitCount(agentX, agentY + 1);

       return counts;
   }
}
