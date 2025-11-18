//
package model;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//

/**
 * Agente reactivo hibrido:
 * 1. Busqueda greedy cuando se puede mover directamente hacia el objetivo
 * 2. Seguimiento de paredes (algoritmo Bug) al encontrar un obstaculo
 */
//
public class AgentP2 extends Agent {

    private Environment world;
    
    private final int ACTION_TIME = 10;
    
    // Indices: 0:UP, 1:RIGHT, 2:DOWN, 3:LEFT
    private final ArrayList<RelativePos> sensors = new ArrayList<>(List.of(
        new RelativePos(-1, 0),
        new RelativePos( 0, 1),
        new RelativePos( 1, 0),
        new RelativePos( 0,-1)
    ));
    
    @Override
    protected void setup() {
        Object[] args = getArguments();
        
        if (args != null && args.length > 0 && args[0] instanceof Environment) {
            this.world = (Environment) args[0];
        } else {
            throw new RuntimeException("El agente necesita una instancia de Environment como argumento");
        }
        
        addBehaviour(new ReactiveBehaviour(this, ACTION_TIME));
        
        System.out.println("=== Agente " + getLocalName() + " iniciado ===");
    }
    
    @Override
    public void takeDown() {
        System.out.println("=== Agente " + getLocalName() + " terminado ===");
    }
        
    public boolean isRunning() {
        return this.world.isRunning();
    }
    
    public void changeState(State estado) {
        this.world.changeState(estado);
    }
    
    //Comportamiento principal del agente
    class ReactiveBehaviour extends TickerBehaviour {
        
        private int ticks = 0;
        
        // Modos de navegacion
        private enum Mode { GREEDY, WALL_FOLLOW }
        private Mode currentMode = Mode.GREEDY;
        
        // Variables para seguimiento de paredes
        private int wallDirection = -1;      // Direccion relativa de la pared (0..3)
        private boolean leftHand = true;     // Heuristica de mano izquierda/derecha
        
        // Distancia al objetivo al momento de encontrar la pared
        private double hitDistance = Double.MAX_VALUE; 

        public ReactiveBehaviour(Agent agente, long period) {
            super(agente, period);
        }
        
        @Override
        public void onTick() {
            if (!((AgentP2) myAgent).isRunning()) return;
            
            ticks++;
            
            // -----------------------------------------------------------------
            // FASE DE PERCEPCION
            // -----------------------------------------------------------------
            ((AgentP2) myAgent).changeState(State.SEE);
            
            Map<RelativePos, Integer> perception = world.perceive(sensors);
            int[] goalDir = world.getDirectionToGoal(); 
            
            // Calculamos distancia euclidiana al objetivo
            double currentDist = Math.sqrt(Math.pow(goalDir[0], 2) + Math.pow(goalDir[1], 2));

            // -----------------------------------------------------------------
            // FASE DE DECISION (THINK)
            // -----------------------------------------------------------------
            ((AgentP2) myAgent).changeState(State.THINK);
            
            String action = "BLOCKED";
            String modeLog = "";

            // Comprobamos si se ha alcanzado el objetivo
            if (world.isGoalReached()) {
                System.out.println("!!! OBJETIVO ALCANZADO !!!");
                ((AgentP2) myAgent).changeState(State.STANDBY);
                world.stop();
                return;
            }

            switch (currentMode) {
                case GREEDY:
                    // Intentamos movernos directamente hacia el objetivo
                    String directMove = getDirectMove(goalDir, perception);
                    
                    if (directMove != null) {
                        action = directMove;
                        modeLog = "GREEDY";
                    } else {
                        // Obstaculo detectado: activamos modo Wall Following
                        currentMode = Mode.WALL_FOLLOW;
                        hitDistance = currentDist; // Guardamos distancia al impactar
                        
                        wallDirection = getBlockedDirection(goalDir);
                        leftHand = decideHand(wallDirection, goalDir);
                        
                        action = followWall(perception);
                        modeLog = "HIT -> WALL_FOLLOW (" + (leftHand ? "LEFT" : "RIGHT") + ")";
                    }
                    break;

                case WALL_FOLLOW:
                    // Podemos volver al modo GREEDY??
                    String tryDirect = getDirectMove(goalDir, perception);
                    
                    if (tryDirect != null && currentDist < (hitDistance - 0.5)) {
                        currentMode = Mode.GREEDY;
                        action = tryDirect;
                        modeLog = "LEAVE WALL -> GREEDY";
                    } else {
                        // Seguimos rodeando la pared
                        action = followWall(perception);
                        modeLog = "WALL (" + (leftHand ? "LEFT" : "RIGHT") + ")";
                    }
                    break;
            }

            // Debug
            System.out.println("[" + ticks + "] " + modeLog + " | Dist: " + String.format("%.2f", currentDist));

            // -----------------------------------------------------------------
            // FASE DE ACCION
            // -----------------------------------------------------------------
            if (!"BLOCKED".equals(action)) {
                int oldX = world.getAgentX();
                int oldY = world.getAgentY();
                
                world.moveAgent(action);
                
                if (oldX != world.getAgentX() || oldY != world.getAgentY()) {
                    world.incrementEnergy();
                }
            }
            
            ((AgentP2) myAgent).changeState(State.ACTION);
        }

        // ---------------------------------------------------------------------
        // METODOS AUXILIARES
        // ---------------------------------------------------------------------

        // Decide que mano usar para seguir la pared
        private boolean decideHand(int blockedDir, int[] goalDir) {
            int dY = goalDir[0];
            int dX = goalDir[1];
            
            if (blockedDir == 0) return dX >= 0;
            if (blockedDir == 2) return dX < 0;
            if (blockedDir == 1) return dY < 0;
            if (blockedDir == 3) return dY >= 0;
            return true;
        }

        // Algoritmo de seguimiento estricto de paredes (solo sensores locales)
        private String followWall(Map<RelativePos, Integer> perception) {
            String[] dirs = {"UP", "RIGHT", "DOWN", "LEFT"};
            int turnOut = leftHand ? 3 : 1;
            int turnIn  = leftHand ? 1 : 3;
            
            // 1. Verificar apertura (esquina convexa)
            if (isFree(wallDirection, perception)) {
                String move = dirs[wallDirection];
                wallDirection = (wallDirection + turnIn) % 4;
                return move;
            }
            
            // 2. Moverse paralelo a la pared
            int moveDir = (wallDirection + turnOut) % 4;
            if (isFree(moveDir, perception)) return dirs[moveDir];
            
            // 3. Esquina ciega (concava)
            wallDirection = moveDir;
            return "BLOCKED";
        }

        // Comprueba si una celda relativa esta libre
        private boolean isFree(int index, Map<RelativePos, Integer> perception) {
            return perception.get(sensors.get(index)) == 0;
        }

        // Movimiento greedy hacia el objetivo
        private String getDirectMove(int[] goalDir, Map<RelativePos, Integer> perception) {
            int dY = goalDir[0]; 
            int dX = goalDir[1];
            
            if (Math.abs(dY) >= Math.abs(dX)) {
                if (dY < 0 && isFree(0, perception)) return "UP";
                if (dY > 0 && isFree(2, perception)) return "DOWN";
                if (dX < 0 && isFree(3, perception)) return "LEFT";
                if (dX > 0 && isFree(1, perception)) return "RIGHT";
            } else {
                if (dX < 0 && isFree(3, perception)) return "LEFT";
                if (dX > 0 && isFree(1, perception)) return "RIGHT";
                if (dY < 0 && isFree(0, perception)) return "UP";
                if (dY > 0 && isFree(2, perception)) return "DOWN";
            }
            return null;
        }
        
        // Determina la direccion bloqueada respecto al objetivo
        private int getBlockedDirection(int[] goalDir) {
            int dY = goalDir[0]; 
            int dX = goalDir[1];
            if (Math.abs(dY) > Math.abs(dX)) return (dY < 0) ? 0 : 2;
            else return (dX > 0) ? 1 : 3;
        }
    }
}
//

/* !!! DEPRECATED
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
    private final int ACTION_TIME = 1;
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
        // Decide el mejor movimiento considerando:
        // 1. Dirección del objetivo
        // 2. Celdas visitadas (evitarlas si es posible)
        // 3. Backtracking inteligente si es necesario

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
        

        
        // ESTRATEGIA 1: Intenta moverse directo al objetivo por celda no visitada
         
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

        
        //ESTRATEGIA 2: Explora cualquier celda NO visitada
        // (Útil cuando el camino directo está bloqueado pero hay alternativas)
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

        
        //ESTRATEGIA 3: Backtracking - elige la celda MENOS visitada
        //(Para salir de callejones sin salida)
        
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
}

*/
