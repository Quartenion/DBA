package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap; // Import necesario para el mapa de visitas

/**
 * Modelo del entorno.
 * CORRECCIÓN: Se han sincronizado los métodos de acceso a las colecciones
 * (visitedCells, seenCells, visitCountMap) para evitar ConcurrentModificationException
 * cuando la vista pinta mientras el agente se mueve.
 */
public class Environment {
    
    private int agentX;
    private int agentY;
    private int goalX;
    private int goalY;
    
    private int energyUsed = 0;
    
    private State state = State.STANDBY;
    
    private WorldPerceptionInterface world;
    
    // Memoria del agente
    private Set<String> visitedCells;
    private Set<String> seenCells;
    private Map<String, Integer> visitCountMap; // Inicializarlo en el constructor
    
    private boolean run = false;

    public Environment(WorldPerceptionInterface w) {
        this.visitedCells = new HashSet<>();
        this.seenCells = new HashSet<>();
        this.visitCountMap = new HashMap<>(); // Inicialización importante
        this.world = w;
    }
    
    public synchronized void stop(){if (run)  run = false;}
    public synchronized void run() {if (!run) run = true; }
    public synchronized boolean isRunning() {return run;}
    
    // --- Getters and Setters Sincronizados ---

    public synchronized int getAgentX() { return agentX; }
    public synchronized int getAgentY() { return agentY; }
    
    public synchronized void setAgentPosition(int x, int y) {
        this.agentX = x;
        this.agentY = y;
        // Al ser métodos internos sincronizados, podemos llamarlos aquí
        addVisitedCell(x, y);
        addSeenCell(agentX, agentY);
    }

    public synchronized int getGoalX() { return goalX; }
    public synchronized int getGoalY() { return goalY; }
        
    public synchronized void setGoalPosition(int x, int y) {
        this.goalX = x;
        this.goalY = y;  
    }
    
    public synchronized boolean isGoalReached() {
        return agentX == goalX && agentY == goalY;
    }
    
    // ============================================
    // GESTION DE MEMORIA (CRÍTICO: SYNCHRONIZED)
    // ============================================
    
    public void changeState(State s){
        this.state = s;
    }
    
    // === AQUÍ ESTABA EL ERROR: FALTABA SYNCHRONIZED ===
    
    public synchronized void addVisitedCell(int x, int y) { 
        visitedCells.add(x + "," + y); 
    }
    
    public synchronized void addSeenCell(int x, int y) { 
        seenCells.add(x + "," + y); 
    }
    
    public synchronized boolean hasVisited(int x, int y) { 
        return visitedCells.contains(x + "," + y); 
    }
    
    public synchronized boolean hasSeen(int x, int y) { 
        return seenCells.contains(x + "," + y); 
    }
    
    // Estos métodos creaban copias (new HashSet) mientras el otro hilo escribía.
    // Al poner synchronized, bloqueamos la escritura mientras copiamos.
    public synchronized Set<String> getVisitedCells() { 
        return new HashSet<>(visitedCells); 
    }
    
    public synchronized Set<String> getSeenCells() { 
        return new HashSet<>(seenCells); 
    }
    
    public synchronized void clearMemory() {
       visitedCells.clear();
       seenCells.clear();
       visitCountMap.clear();
    }
    
    // ============================================
    // GESTION DE CONTEO DE VISITAS (SYNCHRONIZED)
    // ============================================
    
    public synchronized void incrementVisitCount(int x, int y) {
        String key = x + "," + y;
        visitCountMap.put(key, visitCountMap.getOrDefault(key, 0) + 1);
    }

    public synchronized int getVisitCount(int x, int y) {
        return visitCountMap.getOrDefault(x + "," + y, 0);
    }

    // ============================================
    // SENSORES DEL AGENTE
    // ============================================

    public Map<RelativePos,Integer> perceive(ArrayList<RelativePos> sensors) {
        // perceive del world suele ser de solo lectura, no necesita sync estricto
        // a menos que el mundo cambie dinámicamente (obstáculos móviles)
        Map<RelativePos,Integer> perception = 
                        world.percieve(new RelativePos(agentX,agentY), sensors);
        
        for (int i = 0; i < sensors.size(); i++){
            int tile = perception.get(sensors.get(i));
            RelativePos pos = sensors.get(i).eval(new RelativePos(agentX,agentY));
            if (tile != -2){
                addSeenCell(pos.getX(), pos.getY()); // Llama al método synchronized
            }
        }
   
        return perception;
    }
    
    // ============================================
    // INFORMACION SOBRE EL OBJETIVO
    // ============================================

    public synchronized int[] getDirectionToGoal() {
        return new int[]{goalX - agentX, goalY - agentY};
    }

    // ============================================
    // ACCIONES DEL AGENTE
    // ============================================

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
        
        // setAgentPosition ya es synchronized
        this.setAgentPosition(newX, newY);
        
        // incrementVisitCount ya es synchronized
        incrementVisitCount(newX, newY);

        System.out.println("Agente movido a: (" + agentX + ", " + agentY + ")");
    }

    public synchronized int getEnergyUsed() { return energyUsed; }
    public synchronized void incrementEnergy() { energyUsed++; }
    public synchronized void resetEnergy() { energyUsed = 0; }
    
   public synchronized boolean[] getVisitedDirections() {
       boolean[] visited = new boolean[4];
       visited[0] = hasVisited(agentX - 1, agentY); // UP
       visited[1] = hasVisited(agentX, agentY + 1); // RIGHT
       visited[2] = hasVisited(agentX + 1, agentY); // DOWN
       visited[3] = hasVisited(agentX, agentY - 1); // LEFT
       return visited;
   }

   public synchronized int[] getVisitCounts() {
       int[] counts = new int[4];
       counts[0] = getVisitCount(agentX - 1, agentY);
       counts[1] = getVisitCount(agentX, agentY + 1);
       counts[2] = getVisitCount(agentX + 1 , agentY);
       counts[3] = getVisitCount(agentX, agentY - 1);
       return counts;
   }
}