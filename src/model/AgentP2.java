package model;

import jade.core.Agent;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.TickerBehaviour;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentP2 extends Agent {
    // Enviroment of the agent to store all the data
    private Environment model;
    // Time for the tickers
    private final int ACTION_TIME = 1000;
    // private DataStore sharedDataStore; // DataStore compartido
    
    
    // Representation of the sensors of this agent NORTH, EAST, SOUTH, WEST
    //                                             UP,  ,RIGHT, DOWN, LEFT
    private final ArrayList<RelativePos> sensors = new ArrayList<>(List.of(
                                                    new RelativePos( -1,0),
                                                    new RelativePos( 0, 1),
                                                    new RelativePos( 1, 0),
                                                    new RelativePos( 0,-1)
                                                    ));
    
    @Override
    protected void setup(){
        Object[] args = getArguments();
        
        if (args != null && args.length > 0 && args[0] instanceof Environment) {
            this.model = (Environment) args[0];
        } else {
            throw new RuntimeException("Environment no proporcionado al agente");
        }
        
        addBehaviour(new reactiveAgentBehaviour(this,ACTION_TIME));
        
        /*
        !! DEPRECATED
        // Crear un DataStore compartido para todos los behaviours
        this.sharedDataStore = new DataStore();
        
        // Crear los behaviours con delays escalonados
        SeeBehaviour seeBehaviour = new SeeBehaviour(this, ACTION_TIME);
        ThinkBehaviour thinkBehaviour = new ThinkBehaviour(this, ACTION_TIME);
        ActionBehaviour actionBehaviour = new ActionBehaviour(this, ACTION_TIME);
        
        // Asignar el mismo DataStore a todos
        seeBehaviour.setDataStore(sharedDataStore);
        thinkBehaviour.setDataStore(sharedDataStore);
        actionBehaviour.setDataStore(sharedDataStore);
         
        
        // Añadir los behaviours con delays para que se ejecuten en orden
        addBehaviour(seeBehaviour);
        addBehaviour(thinkBehaviour);
        addBehaviour(actionBehaviour);
        */
        
        System.out.println("=== Agente iniciado correctamente ===");
    }
    
    @Override
    public void takeDown(){
        System.out.println("=== Terminando agente ===");
    }
    
    public boolean isRunning(){
        return this.model.isRunning();
    }
    
    public void changeState(State s){
        this.model.changeState(s);
    }
    
    
    
    class reactiveAgentBehaviour extends TickerBehaviour {
        private int tickCount = 0;
        
        public reactiveAgentBehaviour(Agent a, long period){
            super(a, period);
        }
        
        
        //-BEHAVIOUR------------------------------------------------------------
        @Override
        public void onTick(){
            tickCount++;
            
            // If its stopped
            if (!((AgentP2) myAgent).isRunning()){
                return;
            }
            
            //-SEE--------------------------------------------------------------
            // The controller will detect the change to edit the UI
            ((AgentP2) myAgent).changeState(State.SEE);
            
            
            // Method percieve -> Stores the info in the Enviroment
            Map<RelativePos,Integer> perception = model.perceive(sensors);
            int [] goalDirection = model.getDirectionToGoal();
            
            
            //-THINK------------------------------------------------------------
            // The controller will detect the change to edit the UI
            ((AgentP2) myAgent).changeState(State.THINK);
            
            
            String decision = "";
            // Comprobar si ya llegamos al objetivo
            if (model.isGoalReached()) {
                System.out.println("[" + tickCount + "] THINK - OBJETIVO ALCANZADO!");
                System.out.println("[" + tickCount + "] ACTION - Agente detenido - Objetivo alcanzado");
                System.out.println(">>> Energia total consumida: " + model.getEnergyUsed());
                ((AgentP2) myAgent).changeState(State.STANDBY);
                model.stop();
            }
            else{
                // Consultar memoria de celdas visitadas
                boolean[] visited = model.getVisitedDirections();
                int[] visitCounts = model.getVisitCounts();

                 // ESTRATEGIA CON PRIORIDADES
                decision = decideMovementWithMemory(perception, goalDirection, visited, visitCounts);
                System.out.println("[" + tickCount + "] THINK - Decision final: " + decision);
            }
            
            
            //-ACTION-----------------------------------------------------------
            if ("BLOCKED".equals(decision)) {
                System.out.println("[" + tickCount + "] ACTION - BLOQUEADO - No hay movimientos validos!");
                ((AgentP2) myAgent).changeState(State.STANDBY);
                return;
            }
            
            
            int oldX = model.getAgentX();
            int oldY = model.getAgentY();
            // Ejecutar movimiento a través del entorno
            model.moveAgent(decision);
            
            if (oldX != model.getAgentX() || oldY != model.getAgentY()) {
                model.incrementEnergy();
                System.out.println("[" + tickCount + "] ACTION - Ejecutado: " + decision + 
                                 " | Energia: " + model.getEnergyUsed());
            } else {
                System.out.println("[" + tickCount + "] ACTION - FALLO: " + decision);
            }
            
            
            
            if (model.isGoalReached()){
            }
            
            // The controller will detect the change to edit the UI
            ((AgentP2) myAgent).changeState(State.ACTION);

        }
        
        
        
        //-HEURISTICA-----------------------------------------------------------
        /**
         * Decide el mejor movimiento considerando:
         * 1. Dirección del objetivo
         * 2. Celdas visitadas (evitarlas si es posible)
         * 3. Backtracking inteligente si es necesario
         */
        private String decideMovementWithMemory(Map<RelativePos,Integer> perception, int[] goalDirection, 
                                               boolean[] visited, int[] visitCounts) {

            // PRIORIDAD 1: Intentar ir directo al objetivo por celda NO visitada
            String directMove = tryDirectUnvisitedMove(perception, goalDirection, visited);
            if (directMove != null) {
                System.out.println("   -> Estrategia: Movimiento directo no visitado");
                return directMove;
            }

            // PRIORIDAD 2: Explorar celdas NO visitadas (aunque no sea directo al objetivo)
            String unexploredMove = tryUnexploredMove(perception, visited);
            if (unexploredMove != null) {
                System.out.println("   -> Estrategia: Exploracion de celda nueva");
                return unexploredMove;
            }

            // PRIORIDAD 3: Backtracking - ir a la celda menos visitada
            String backtrackMove = tryBacktracking(perception, visited, visitCounts);
            if (backtrackMove != null) {
                System.out.println("   -> Estrategia: Backtracking (celda menos visitada)");
                return backtrackMove;
            }

            // PRIORIDAD 4: Completamente bloqueado
            System.out.println("   -> Estrategia: BLOQUEADO - Sin opciones");
            return "BLOCKED";
        }
        

        /**
         * ESTRATEGIA 1: Intenta moverse directo al objetivo por celda no visitada
         */
        private String tryDirectUnvisitedMove(Map<RelativePos,Integer> perception, 
                                        int[] goalDirection, boolean[] visited) {
            // Priorizar movimiento en filas (vertical)
            if (goalDirection[0] < 0 && perception.get(sensors.get(0)) == 0 && !visited[0]) {
                return "UP";
            }
            if (goalDirection[0] > 0 && perception.get(sensors.get(2)) == 0 && !visited[2]) {
                return "DOWN";
            }

            // Luego movimiento en columnas (horizontal)
            if (goalDirection[1] < 0 && perception.get(sensors.get(3)) == 0 && !visited[3]) {
                return "LEFT";
            }
            if (goalDirection[1] > 0 && perception.get(sensors.get(1)) == 0 && !visited[1]) {
                return "RIGHT";
            }

            return null; // No hay camino directo no visitado
        }

        /**
         * ESTRATEGIA 2: Explora cualquier celda NO visitada
         * (Útil cuando el camino directo está bloqueado pero hay alternativas)
         */
        private String tryUnexploredMove(Map<RelativePos,Integer> perception, 
                                                boolean[] visited) {
            String[] directions = {"UP", "RIGHT", "DOWN", "LEFT"};

            for (int i = 0; i < 4; i++) {
                if (perception.get(sensors.get(i)) == 0 && !visited[i]) {
                    return directions[i];
                }
            }

            return null; // Todas las celdas libres ya fueron visitadas
        }

        /**
         * ESTRATEGIA 3: Backtracking - elige la celda MENOS visitada
         * (Para salir de callejones sin salida)
         */
        private String tryBacktracking(Map<RelativePos,Integer>
                            perception, boolean[] visited, int[] visitCounts) {
            String[] directions = {"UP", "RIGHT", "DOWN", "LEFT"};

            String bestDirection = null;
            int minVisits = Integer.MAX_VALUE;

            for (int i = 0; i < 4; i++) {
                // Solo considerar celdas libres
                if (perception.get(sensors.get(i)) == 0) {
                    if (visitCounts[i] < minVisits) {
                        minVisits = visitCounts[i];
                        bestDirection = directions[i];
                    }
                }
            }

            if (bestDirection != null) {
                System.out.println("   -> Backtracking: eligiendo " + bestDirection + 
                                 " (visitada " + minVisits + " veces)");
            }

            return bestDirection;
        }
    }
    
    
    
    
    
    
    
    
    // !! DEPRECATED
    //-------------------------------------------------------
    // COMPORTAMIENTO 1: VER (cada 2 segundos)
    //-------------------------------------------------------
    /*
    {
    
    class SeeBehaviour extends TickerBehaviour {
        
        private int tickCount = 0;
        
        public SeeBehaviour(Agent a, long period){
            super(a, period);
        }
        
        @Override
        public void onTick(){
            tickCount++;
            
            // If its stopped
            if (!((AgentP2) myAgent).isRunning()) {
                return; 
            }
            
            // The controller will detect the change to edit the UI
            ((AgentP2) myAgent).changeState(State.SEE);
            
            // Method percieve -> Stores the info in the Enviroment
            Map<RelativePos,Integer> perception = model.perceive(sensors);
            int [] goalDirection = model.getDirectionToGoal();
            
                
            // Almacenar en datastore compartido
            getDataStore().put("perception", perception);
            getDataStore().put("seeComplete", true);
            getDataStore().put("goalDirection", goalDirection);
            
            
            /*
            TODO: revisar -> no entiendo mucho el proposito de esto
            System.out.println("\n[" + tickCount + "] SEE - Percepcion:");
            System.out.println("   UP:" + (perception[0] == 0 ? "OK" : "XX") + 
                             " DOWN:" + (perception[1] == 0 ? "OK" : "XX") + 
                             " LEFT:" + (perception[2] == 0 ? "OK" : "XX") + 
                             " RIGHT:" + (perception[3] == 0 ? "OK" : "XX"));
            System.out.println("   Distancia objetivo: DX=" + goalDirection[0] + ", DY=" + goalDirection[1]);
            */
           /*
        }
    }
    */
    
    /*
    //-------------------------------------------------------
    // COMPORTAMIENTO 2: PENSAR (cada 2 segundos)
    // Estrategia mejorada con memoria de celdas visitadas
    //-------------------------------------------------------
    class ThinkBehaviour extends TickerBehaviour {
        
        private int tickCount = 0;

        public ThinkBehaviour(Agent a, long period){
            super(a, period);
        }

        @Override
        public void onTick(){
            tickCount++;
            // If its stopped
            if (!((AgentP2) myAgent).isRunning()) {
                return;
            }
            
            // The controller will detect the change to edit the UI
            ((AgentP2) myAgent).changeState(State.THINK);
           
            // Esperar a que See haya completado
            Boolean seeComplete = (Boolean) getDataStore().get("seeComplete");
            if (seeComplete == null || !seeComplete) {
                return;
            }
            // Evitamos que si este ticker llegase a ejecucion de nuevo sin el resto
            // terminar el ciclo, no se llegue a repetir de nuevo
            getDataStore().put("seeComplete", false);
            
            // Obtener percepción del datastore
            Map<RelativePos,Integer> perception = 
                     (Map<RelativePos,Integer>) getDataStore().get("perception");
            
            
            int[] goalDirection = (int[]) getDataStore().get("goalDirection");

            if (perception == null || goalDirection == null) {
                System.out.println("[" + tickCount + "] THINK - Esperando datos de percepcion...");
                return;
            }

            // Comprobar si ya llegamos al objetivo
            if (goalDirection[0] == 0 && goalDirection[1] == 0) {
                System.out.println("[" + tickCount + "] THINK - OBJETIVO ALCANZADO!");
                getDataStore().put("decision", "STOP");
                getDataStore().put("thinkComplete", true);
                return;
            }

            // Consultar memoria de celdas visitadas
            boolean[] visited = model.getVisitedDirections();
            int[] visitCounts = model.getVisitCounts();

            
            System.out.println("[" + tickCount + "] THINK - Analizando opciones:");
            System.out.println("   Percepcion: UP=" + (perception[0] == 0 ? "Libre" : "Bloq") + 
                             " DOWN=" + (perception[1] == 0 ? "Libre" : "Bloq") + 
                             " LEFT=" + (perception[2] == 0 ? "Libre" : "Bloq") + 
                             " RIGHT=" + (perception[3] == 0 ? "Libre" : "Bloq"));
            System.out.println("   Visitadas:  UP=" + visited[0] + 
                             " DOWN=" + visited[1] + 
                             " LEFT=" + visited[2] + 
                             " RIGHT=" + visited[3]);
            
            
            // ESTRATEGIA CON PRIORIDADES
            String decision = decideMovementWithMemory(perception, goalDirection, visited, visitCounts);

            getDataStore().put("decision", decision);
            getDataStore().put("thinkComplete", true);
            
            System.out.println("[" + tickCount + "] THINK - Decision final: " + decision);
        }
            
        private String decideMovementWithMemory(Map<RelativePos,Integer> perception, int[] goalDirection, 
                                               boolean[] visited, int[] visitCounts) {

            // PRIORIDAD 1: Intentar ir directo al objetivo por celda NO visitada
            String directMove = tryDirectUnvisitedMove(perception, goalDirection, visited);
            if (directMove != null) {
                System.out.println("   -> Estrategia: Movimiento directo no visitado");
                return directMove;
            }

            // PRIORIDAD 2: Explorar celdas NO visitadas (aunque no sea directo al objetivo)
            String unexploredMove = tryUnexploredMove(perception, visited);
            if (unexploredMove != null) {
                System.out.println("   -> Estrategia: Exploracion de celda nueva");
                return unexploredMove;
            }

            // PRIORIDAD 3: Backtracking - ir a la celda menos visitada
            String backtrackMove = tryBacktracking(perception, visited, visitCounts);
            if (backtrackMove != null) {
                System.out.println("   -> Estrategia: Backtracking (celda menos visitada)");
                return backtrackMove;
            }

            // PRIORIDAD 4: Completamente bloqueado
            System.out.println("   -> Estrategia: BLOQUEADO - Sin opciones");
            return "BLOCKED";
        }
        

        private String tryDirectUnvisitedMove(Map<RelativePos,Integer> perception, 
                                        int[] goalDirection, boolean[] visited) {
            // Priorizar movimiento en filas (vertical)
            if (goalDirection[0] < 0 && perception.get(sensors.get(0)) == 0 && !visited[0]) {
                return "UP";
            }
            if (goalDirection[0] > 0 && perception.get(sensors.get(2)) == 0 && !visited[1]) {
                return "DOWN";
            }

            // Luego movimiento en columnas (horizontal)
            if (goalDirection[1] < 0 && perception.get(sensors.get(3)) == 0 && !visited[2]) {
                return "LEFT";
            }
            if (goalDirection[1] > 0 && perception.get(sensors.get(1)) == 0 && !visited[3]) {
                return "RIGHT";
            }

            return null; // No hay camino directo no visitado
        }

        private String tryUnexploredMove(Map<RelativePos,Integer> perception, 
                                                boolean[] visited) {
            String[] directions = {"UP", "RIGHT", "DOWN", "LEFT"};

            for (int i = 0; i < 4; i++) {
                if (perception.get(sensors.get(i)) == 0 && !visited[i]) {
                    return directions[i];
                }
            }

            return null; // Todas las celdas libres ya fueron visitadas
        }

        private String tryBacktracking(Map<RelativePos,Integer>
                            perception, boolean[] visited, int[] visitCounts) {
            String[] directions = {"UP", "RIGHT", "DOWN", "LEFT"};

            String bestDirection = null;
            int minVisits = Integer.MAX_VALUE;

            for (int i = 0; i < 4; i++) {
                // Solo considerar celdas libres
                if (perception.get(sensors.get(i)) == 0) {
                    if (visitCounts[i] < minVisits) {
                        minVisits = visitCounts[i];
                        bestDirection = directions[i];
                    }
                }
            }

            if (bestDirection != null) {
                System.out.println("   -> Backtracking: eligiendo " + bestDirection + 
                                 " (visitada " + minVisits + " veces)");
            }

            return bestDirection;
        }
    }

    
    //-------------------------------------------------------
    // COMPORTAMIENTO 3: ACTUAR (cada 2 segundos)
    //-------------------------------------------------------
    class ActionBehaviour extends TickerBehaviour {
        
        private int tickCount = 0;
        
        public ActionBehaviour(Agent a, long period){
            super(a, period);
        }
        
        @Override
        public void onTick(){
            tickCount++;
            // If its stopped
            if (!((AgentP2) myAgent).isRunning()) {
                return; 
            }
            
            
            // Esperar a que Think haya completado
            Boolean thinkComplete = (Boolean) getDataStore().get("thinkComplete");
            if (thinkComplete == null || !thinkComplete) {
                return;
            }
            
            String decision = (String) getDataStore().get("decision");
            
            if (decision == null) {
                System.out.println("[" + tickCount + "] ACTION - Sin decision aun");
                return;
            }
            
            getDataStore().put("thinkComplete", false);
            
            
            if ("STOP".equals(decision)) {
                System.out.println("[" + tickCount + "] ACTION - Agente detenido - Objetivo alcanzado");
                System.out.println(">>> Energia total consumida: " + model.getEnergyUsed());
                // Limpiar flags
                getDataStore().remove("seeComplete");
                getDataStore().remove("thinkComplete");
                return;
            }
            
            if ("BLOCKED".equals(decision)) {
                System.out.println("[" + tickCount + "] ACTION - BLOQUEADO - No hay movimientos validos!");
                getDataStore().remove("seeComplete");
                getDataStore().remove("thinkComplete");
                return;
            }
            
            
            int oldX = model.getAgentX();
            int oldY = model.getAgentY();
            // Ejecutar movimiento a través del entorno
            model.moveAgent(decision);
            
            if (oldX != model.getAgentX() || oldY != model.getAgentY()) {
                model.incrementEnergy();
                System.out.println("[" + tickCount + "] ACTION - Ejecutado: " + decision + 
                                 " | Energia: " + model.getEnergyUsed());
            } else {
                System.out.println("[" + tickCount + "] ACTION - FALLO: " + decision);
            }
            
            // Limpiar flags para el siguiente ciclo
            getDataStore().remove("seeComplete");
            getDataStore().remove("thinkComplete");
            
            
            if (model.isGoalReached()){
                ((AgentP2) myAgent).changeState(State.STANDBY);
                model.stop();
            }
            
            // The controller will detect the change to edit the UI
            ((AgentP2) myAgent).changeState(State.ACTION);
        }
        
    }
    }
    */
}