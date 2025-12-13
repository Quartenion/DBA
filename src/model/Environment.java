package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;


 // Representa el entorno y el estado interno del agente
 // NOTA DE CONCURRENCIA: Esta clase es accedida tanto por el hilo de ejecucion
 // del agente (escritura) como por el hilo de Swing (lectura para pintado)
 // Por eso, los metodos criticos de acceso a memoria y posicion estan marcados como 
 // synchronized para evitar ConcurrentModificationException
 
public class Environment {
    
    // Variables de Posicion 
    private int agentX;
    private int agentY;
    private int goalX;
    private int goalY;
    
    // Estado y Metricas 
    private int energyUsed = 0;
    private State state = State.STANDBY;
    private boolean run = false;
    
    // Dependencias
    private final WorldPerceptionInterface world;
    
    // Memoria del Agente (Recursos Compartidos) 
    private final Set<String> visitedCells;
    private final Set<String> seenCells;
    private final Map<String, Integer> visitCountMap; 
    private boolean mapChanged = false;

    public Environment(WorldPerceptionInterface w) {
        this.visitedCells = new HashSet<>();
        this.seenCells = new HashSet<>();
        this.visitCountMap = new HashMap<>();
        this.world = w;
    }
    
    // -------------------------------------------------------------------------
    // CONTROL DE EJECUCIÓN
    // -------------------------------------------------------------------------
    
    public synchronized void stop() { if (run) run = false; }
    public synchronized void run()  { if (!run) run = true; }
    public synchronized boolean isRunning() { return run; }
    
    public void changeState(State s) {
        this.state = s;
    }

    // -------------------------------------------------------------------------
    // GETTERS Y SETTERS DE POSICIÓN 
    // -------------------------------------------------------------------------

    public synchronized int getAgentX() { return agentX; }
    public synchronized int getAgentY() { return agentY; }
    
    
    // Actualiza la posicion del agente y registra automaticamente la visita
   
    public synchronized void setAgentPosition(int x, int y) {
        this.agentX = x;
        this.agentY = y;
        // Actualiza memoria inmediatamente
        addVisitedCell(x, y);
        addSeenCell(x, y);
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
    
    public synchronized int[] getDirectionToGoal() {
        return new int[]{goalX - agentX, goalY - agentY};
    }
    
    // -------------------------------------------------------------------------
    // GESTION DE MEMORIA 
    // -------------------------------------------------------------------------
    
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
    
    // -------------------------------------------------------------------------
    // MAPA DE CALOR (MIGAJAS)
    // -------------------------------------------------------------------------
    
    public synchronized void incrementVisitCount(int x, int y) {
        String key = x + "," + y;
        visitCountMap.put(key, visitCountMap.getOrDefault(key, 0) + 1);
    }

    public synchronized int getVisitCount(int x, int y) {
        return visitCountMap.getOrDefault(x + "," + y, 0);
    }

    // -------------------------------------------------------------------------
    // SENSORES Y ACCIONES
    // -------------------------------------------------------------------------

    public Map<RelativePos, Integer> perceive(ArrayList<RelativePos> sensors) {
        // Percieve lee de world por lo que no necesita sincronizarse
        Map<RelativePos, Integer> perception = 
                        world.percieve(new RelativePos(agentX, agentY), sensors);
        
        for (RelativePos sensor : sensors) {
            int tile = perception.get(sensor);
            RelativePos pos = sensor.eval(new RelativePos(agentX, agentY));
            // Si no es muro exterior/fuera de rango (-2), se marca como visto
            if (tile != -2) {
                addSeenCell(pos.getX(), pos.getY()); 
            }
        }
   
        return perception;
    }
    
    public void moveAgent(String direction) {
        int newX = agentX;
        int newY = agentY;
        
        switch(direction.toUpperCase()) {
            case "UP"    -> newX = agentX - 1;
            case "DOWN"  -> newX = agentX + 1;
            case "LEFT"  -> newY = agentY - 1;
            case "RIGHT" -> newY = agentY + 1;
        }
        
        this.setAgentPosition(newX, newY);
        
        incrementVisitCount(newX, newY);

        // System.out.println("Agente movido a: (" + agentX + ", " + agentY + ")"); 
    }

    public synchronized int getEnergyUsed() { return energyUsed; }
    public synchronized void incrementEnergy() { energyUsed++; }
    public synchronized void resetEnergy() { energyUsed = 0; }
    
    // PARA EL ANTIGUO COMPORTAMIENTO
    public synchronized boolean[] getVisitedDirections() {
        boolean[] visited = new boolean[4];
        // Asumiendo que agentX es fila y agentY es columna
        visited[0] = hasVisited(agentX - 1, agentY); // UP
        visited[1] = hasVisited(agentX, agentY + 1); // RIGHT
        visited[2] = hasVisited(agentX + 1, agentY); // DOWN
        visited[3] = hasVisited(agentX, agentY - 1); // LEFT
        return visited;
    }

    public synchronized int[] getVisitCounts() {
        int[] counts = new int[4];
        counts[0] = getVisitCount(agentX - 1, agentY); // UP
        counts[1] = getVisitCount(agentX, agentY + 1); // RIGHT
        counts[2] = getVisitCount(agentX + 1, agentY); // DOWN
        counts[3] = getVisitCount(agentX, agentY - 1); // LEFT
        return counts;
    }
    
    // DETECCION DE CAMBIO DE MAPA

    public boolean isMapChanged() {
        return mapChanged;
    }

    public void setMapChanged(boolean mapChanged) {
        this.mapChanged = mapChanged;
    }
}