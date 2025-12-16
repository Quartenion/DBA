package model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import controller.Pair;
import java.util.List;


 // Representa el entorno y el estado interno del agente
 // NOTA DE CONCURRENCIA: Esta clase es accedida tanto por el hilo de ejecucion
 // del agente (escritura) como por el hilo de Swing (lectura para pintado)
 // Por eso, los metodos criticos de acceso a memoria y posicion estan marcados como 
 // synchronized para evitar ConcurrentModificationException
 
public class Environment {
    
    // Variables de Posicion 
    private int agentR;
    private int agentC;
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
    
    
    // Para manejar listeners
    private PropertyChangeSupport support = null;
    
    // Objetivos
    private Queue<Pair<Integer, Integer>> goalQueue;
    private int totalGoals = 0;
    private int goalsReached = 0;

    public Environment(WorldPerceptionInterface w) {
        this.visitedCells = new HashSet<>();
        this.seenCells = new HashSet<>();
        this.visitCountMap = new HashMap<>();
        this.world = w;
        
        this.goalQueue = new LinkedList<>();
        
        this.support = new PropertyChangeSupport(this);
    }
    
    // -------------------------------------------------------------------------
    // CONTROL DE EJECUCIÓN
    // -------------------------------------------------------------------------
    
    public synchronized void stop() { if (run) run = false; }
    public synchronized void run()  { if (!run) run = true; }
    public synchronized boolean isRunning() { return run; }
    
    // Cambiar el estado
    public void changeState(State s) {
        State oldState = this.state;
        this.state = s;
        
        // Lanzamos el cambio a los que escuchan
        support.firePropertyChange("state", oldState, this.state);
    }
    
    // Método para que el Controller se pueda suscribir y hacer patrón
    // observer
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    // -------------------------------------------------------------------------
    // GETTERS Y SETTERS DE POSICIÓN 
    // -------------------------------------------------------------------------

    public synchronized int getAgentX() { return agentR; }
    public synchronized int getAgentY() { return agentC; }
    
    
    // Actualiza la posicion del agente y registra automaticamente la visita
   
    public synchronized void setAgentPosition(int r, int c) {
        this.agentR = r;
        this.agentC = c;
        // Actualiza memoria inmediatamente
        addVisitedCell(r, c);
        addSeenCell(r, c);
    }

    public synchronized int getGoalX() { return goalX; }
    public synchronized int getGoalY() { return goalY; }
        
    public synchronized void setGoalPosition(int x, int y) {
        this.goalX = x;
        this.goalY = y;  
    }
    
    public synchronized boolean isGoalReached() {
        return agentR == goalX && agentC == goalY;
    }
    
    public synchronized int[] getDirectionToGoal() {
        return new int[]{goalX - agentR, goalY - agentC};
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
                        world.percieve(new RelativePos(agentR, agentC), sensors);
        
        for (RelativePos sensor : sensors) {
            int tile = perception.get(sensor);
            RelativePos pos = sensor.eval(new RelativePos(agentR, agentC));
            // Si no es muro exterior/fuera de rango (-2), se marca como visto
            if (tile != -2) {
                addSeenCell(pos.getX(), pos.getY()); 
            }
        }
   
        return perception;
    }
    
    public void moveAgent(String direction) {
        int newX = agentR;
        int newY = agentC;
        
        switch(direction.toUpperCase()) {
            case "UP"    -> newX = agentR - 1;
            case "DOWN"  -> newX = agentR + 1;
            case "LEFT"  -> newY = agentC - 1;
            case "RIGHT" -> newY = agentC + 1;
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
        visited[0] = hasVisited(agentR - 1, agentC); // UP
        visited[1] = hasVisited(agentR, agentC + 1); // RIGHT
        visited[2] = hasVisited(agentR + 1, agentC); // DOWN
        visited[3] = hasVisited(agentR, agentC - 1); // LEFT
        return visited;
    }

    public synchronized int[] getVisitCounts() {
        int[] counts = new int[4];
        counts[0] = getVisitCount(agentR - 1, agentC); // UP
        counts[1] = getVisitCount(agentR, agentC + 1); // RIGHT
        counts[2] = getVisitCount(agentR + 1, agentC); // DOWN
        counts[3] = getVisitCount(agentR, agentC - 1); // LEFT
        return counts;
    }
    
    // DETECCION DE CAMBIO DE MAPA

    public boolean isMapChanged() {
        return mapChanged;
    }

    public void setMapChanged(boolean mapChanged) {
        this.mapChanged = mapChanged;
    }
    
    // -------------------------------------------------------------------------
    // GOALS - DBA3
    // -------------------------------------------------------------------------
    
    public synchronized int getGoalsReached() { return goalsReached; }
    public synchronized int getTotalGoals() { return totalGoals; }
    
    /**
     * 
     * 
    * Inicializa la cola de objetivos con las coordenadas de los renos
    * @param reindeerCoords Lista de coordenadas (row, col)
    */
   public synchronized void initializeGoals(ArrayList<Pair<Integer, Integer>> reindeerCoords) {
       goalQueue.clear();
       goalsReached = 0;

       for (Pair<Integer, Integer> coord : reindeerCoords) {
           goalQueue.offer(coord); // offer() == add(), pero no da Exception cuando falla, devuelve false
       }

       totalGoals = goalQueue.size();

       // Establecer el primer goal
       if (!goalQueue.isEmpty()) {
           Pair<Integer, Integer> firstGoal = goalQueue.peek(); // peek() toma el primer valor, pero no lo elimina
           setGoalPosition(firstGoal.first, firstGoal.second);
       }
   }

   /**
    * Marca el goal actual como alcanzado y carga el siguiente
    * @return true si había más goals, false si era el último
    */
   public synchronized boolean moveToNextGoal() {
       if (!goalQueue.isEmpty()) {
           goalQueue.poll(); // poll() == peek(), pero si lo elimina
           goalsReached++;

           if (!goalQueue.isEmpty()) {
               Pair<Integer, Integer> nextGoal = goalQueue.peek();
               setGoalPosition(nextGoal.first, nextGoal.second);
               return true;
           }
       }
       return false; // No hay más goals
   }

   /**
    * @return true si quedan renos por encontrar
    */
   public synchronized boolean hasMoreGoals() {
       return !goalQueue.isEmpty();
   }

   /**
    * @return Lista de goals pendientes (para visualización)
    */
   public synchronized List<Pair<Integer, Integer>> getPendingGoals() {
       return new ArrayList<>(goalQueue);
   }
}