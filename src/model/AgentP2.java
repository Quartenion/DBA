package model;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentP2 extends Agent {
    private Environment model;
    private final int ACTION_TIME = 1;
    private String lastMove = null; // Guarda la ultima accion efectiva ("UP", "DOWN", etc.)
    
    // Sensores: 0=UP, 1=RIGHT, 2=DOWN, 3=LEFT
    private final ArrayList<RelativePos> sensors = new ArrayList<>(List.of(
            new RelativePos(-1, 0), // UP
            new RelativePos(0, 1),  // RIGHT
            new RelativePos(1, 0),  // DOWN
            new RelativePos(0, -1)  // LEFT
    ));
    
    @Override
    protected void setup(){
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof Environment) {
            this.model = (Environment) args[0];
        } else {
            throw new RuntimeException("Environment no proporcionado");
        }
        addBehaviour(new reactiveAgentBehaviour(this, ACTION_TIME));
        System.out.println("=== Agente iniciado ===");
    }
    
    @Override
    public void takeDown(){
        System.out.println("=== Terminando agente ===");
    }
    
    public boolean isRunning(){ return this.model.isRunning(); }
    public void changeState(State s){ this.model.changeState(s); }
    
    // =========================================================================
    // COMPORTAMIENTO REACTIVO (Bug Algorithm + Anti-Bucle + Anti-Reverse)
    // =========================================================================
    class reactiveAgentBehaviour extends TickerBehaviour {
        private int tickCount = 0;
        
        // --- ESTADOS ---
        private enum Mode { GREEDY, WALL_FOLLOW }
        private Mode currentMode = Mode.GREEDY;
        
        // Configuración Wall Follow
        private int wallToFollow = -1;      // Dirección relativa del muro (0=UP, etc)
        private boolean useLeftHand = true; // true=Mano Izq, false=Mano Der

        // Memoria del punto de choque
        private double hitDistance = Double.MAX_VALUE; // Distancia al goal cuando chocamos

        // ✅ Para romper bucles: recordar posición exacta del choque
        private String lastCollisionPos = null;
        private int collisionCountAtPos = 0;
        private double bestDistanceEver = Double.MAX_VALUE;

        // ✅ Posición inicial del modo pared (debugging)
        private int wallStartX = -1;
        private int wallStartY = -1;

        public reactiveAgentBehaviour(Agent a, long period){
            super(a, period);
            bestDistanceEver = Double.MAX_VALUE;
        }
        
        @Override
        public void onTick(){
            if (!((AgentP2) myAgent).isRunning()) return;
            tickCount++;
            
            // 1. PERCEPCIÓN
            ((AgentP2) myAgent).changeState(State.SEE);
            Map<RelativePos,Integer> perception = model.perceive(sensors);
            int[] goalDir = model.getDirectionToGoal(); 
            
            // Distancia Euclídea actual
            double currentDist = Math.sqrt(Math.pow(goalDir[0], 2) + Math.pow(goalDir[1], 2));

            // =============== 🐞 DEBUG: Percepción y estado básico ===============
            System.out.println("\n[TICK " + tickCount + "]");
            System.out.println("  Pos: (" + model.getAgentX() + "," + model.getAgentY() + ")");
            System.out.println("  Goal: (" + model.getGoalX() + "," + model.getGoalY() + ")");
            System.out.println("  Distancia al objetivo: " + String.format("%.3f", currentDist));
            System.out.println("  Dirección al objetivo (dY, dX): (" + goalDir[0] + ", " + goalDir[1] + ")");
            System.out.println("  Percepción (U,R,D,L): [" +
                perception.get(sensors.get(0)) + ", " +
                perception.get(sensors.get(1)) + ", " +
                perception.get(sensors.get(2)) + ", " +
                perception.get(sensors.get(3)) + "]");
            System.out.println("  Último movimiento efectivo: " + lastMove);
            System.out.println("  Modo actual: " + currentMode);
            if (currentMode == Mode.WALL_FOLLOW) {
                System.out.println("    → wallToFollow: " + wallToFollow + " (" + dirName(wallToFollow) + ")");
                System.out.println("    → useLeftHand: " + useLeftHand);
                System.out.println("    → hitDistance (al chocar): " + String.format("%.3f", hitDistance));
                System.out.println("    → bestDistanceEver: " + String.format("%.3f", bestDistanceEver));
                if (wallStartX != -1) {
                    System.out.println("    → Pared iniciada en: (" + wallStartX + "," + wallStartY + ")");
                }
            }
            // ====================================================================

            // 2. PENSAMIENTO
            ((AgentP2) myAgent).changeState(State.THINK);
            String decision = "BLOCKED";
            String logMessage = "";

            if (model.isGoalReached()) {
                System.out.println("!!! OBJETIVO ALCANZADO !!!");
                ((AgentP2) myAgent).changeState(State.STANDBY);
                model.stop();
                return;
            }

            switch (currentMode) {
                case GREEDY:
                    // Intentamos ir directo
                    String directMove = getDirectMove(goalDir, perception);
                    
                    if (directMove != null) {
                        decision = directMove;
                        logMessage = "GREEDY";
                        System.out.println("  [GREEDY] Movimiento directo posible: " + directMove);
                    } else {
                        // --- CHOQUE DETECTADO ---
                        currentMode = Mode.WALL_FOLLOW;
                        hitDistance = currentDist;

                        // ✅ Guardar posición inicial del modo pared
                        wallStartX = model.getAgentX();
                        wallStartY = model.getAgentY();

                        // 1. ¿Dónde está el muro?
                        wallToFollow = getBlockedDirection(goalDir);

                        // 2. Decidir mano INTELIGENTE:
                        useLeftHand = decideHand(wallToFollow, goalDir);

                        // ✅ Detección de bucle: ¿ya chocamos aquí antes?
                        String currentPos = model.getAgentX() + "," + model.getAgentY();
                        if (currentPos.equals(lastCollisionPos)) {
                            collisionCountAtPos++;
                        } else {
                            lastCollisionPos = currentPos;
                            collisionCountAtPos = 1;
                        }

                        // Si ya chocamos 2+ veces EN ESTA MISMA CASILLA → romper simetría
                        boolean antiLoopTriggered = false;
                        if (collisionCountAtPos >= 2 && currentDist >= bestDistanceEver - 0.5) {
                            useLeftHand = !useLeftHand; // ✅ Cambiar mano
                            antiLoopTriggered = true;
                        }

                        // Actualizar mejor distancia global
                        if (currentDist < bestDistanceEver) {
                            bestDistanceEver = currentDist;
                        }

                        logMessage = "HIT -> WALL_FOLLOW (" + (useLeftHand ? "L" : "R") + ")";
                        if (antiLoopTriggered) {
                            logMessage += " [ANTI-LOOP]";
                        }

                        // Primer movimiento de rodeo
                        decision = followWallStrict(perception);
                        
                        // ============================================================
                        // ✅ CORRECCIÓN: ANTI-REVERSE (Inercia)
                        // Evita giros de 180º al entrar en modo pared si se puede seguir recto
                        // ============================================================
                        if (lastMove != null && isOpposite(decision, lastMove)) {
                            int lastMoveIdx = getDirIndex(lastMove);
                            // Si la dirección que traíamos sigue libre, la forzamos
                            if (isFree(lastMoveIdx, perception)) {
                                System.out.println("  ⚠️ [ANTI-REVERSE] Evitando giro 180º. Forzando inercia: " + lastMove);
                                decision = lastMove;
                                // Invertimos mano para que la lógica de pared se alinee en el siguiente tick
                                useLeftHand = !useLeftHand; 
                                logMessage += " [HAND-SWAP FORCED]";
                            }
                        }
                        // ============================================================

                        // =============== 🐞 DEBUG ===============
                        System.out.println("  [GREEDY] ❌ Bloqueado → cambio a WALL_FOLLOW");
                        System.out.println("    → wallToFollow = " + wallToFollow + " (" + dirName(wallToFollow) + ")");
                        System.out.println("    → useLeftHand = " + useLeftHand + (antiLoopTriggered ? " [CAMBIADO por anti-loop]" : ""));
                        System.out.println("    → Decisión inicial de pared: " + decision);
                        // =========================================
                    }
                    break;

                case WALL_FOLLOW:
                    // === CONDICIÓN DE SALIDA (Mejorada - Lógica Bug 2) ===
                    String tryDirect = getDirectMove(goalDir, perception);

                    // Margen pequeño para evitar errores de coma flotante (0.01 o similar)
                    double EPSILON = 0.1; 

                    // CONDICIÓN:
                    // 1. Hay un movimiento directo posible (tryDirect != null)
                    // 2. Es compatible con la inercia (isCompatibleWithLastMove)
                    // 3. [NUEVO] Hemos progresado: La distancia actual es MENOR que la distancia del choque
                    boolean isCloser = currentDist < (hitDistance - EPSILON);

                    if (tryDirect != null && isCompatibleWithLastMove(tryDirect) && isCloser) {
                        currentMode = Mode.GREEDY;
                        decision = tryDirect;
                        logMessage = "LEAVE WALL -> GREEDY (dir=" + tryDirect + ") [Dist: " + String.format("%.2f", currentDist) + " < Hit: " + String.format("%.2f", hitDistance) + "]";
                        System.out.println("  [WALL] ✅ Salida válida: Camino libre Y mejor distancia.");
                    } else {
                        // LOGGING para entender por qué NO sale si ve camino libre
                        if (tryDirect != null && !isCloser) {
                            System.out.println("  [WALL] ⏳ Camino directo visto (" + tryDirect + ") pero ignorado: Distancia actual (" 
                                + String.format("%.2f", currentDist) + ") >= HitDistance (" + String.format("%.2f", hitDistance) + ")");
                        }

                        // Seguir pared
                        decision = followWallStrict(perception);
                        logMessage = "WALL (" + (useLeftHand ? "L" : "R") + ")";
                    }
                    break;
            }

            System.out.println("  → Log: [" + tickCount + "] " + logMessage + " | Dist: " + String.format("%.2f", currentDist));

            // 3. ACCIÓN
            if (!"BLOCKED".equals(decision)) {
                int oldX = model.getAgentX();
                int oldY = model.getAgentY();
                model.moveAgent(decision);
                if (oldX != model.getAgentX() || oldY != model.getAgentY()) {
                    model.incrementEnergy();
                    lastMove = decision;
                    System.out.println("  ✅ Movimiento ejecutado: " + decision + " → nueva pos: (" + model.getAgentX() + "," + model.getAgentY() + ")");
                } else {
                    System.out.println("  ⚠️ Movimiento NO efectivo (bloqueado tras moveAgent): " + decision);
                }
            } else {
                System.out.println("  ⛔ Decisión = BLOCKED");
            }
            ((AgentP2) myAgent).changeState(State.ACTION);
        }

        // ------------------------------------------------------------------------
        // LÓGICA Y HELPERS
        // ------------------------------------------------------------------------
        
        private boolean decideHand(int blockedDir, int[] goalDir) {
            int dY = goalDir[0];
            int dX = goalDir[1];
            
            boolean result;
            if (blockedDir == 0) result = dX >= 0; // UP → Left si goal a derecha
            else if (blockedDir == 2) result = dX >= 0; // DOWN → Left si goal a derecha
            else if (blockedDir == 1) result = dY < 0;  // RIGHT → Left si goal arriba
            else if (blockedDir == 3) result = dY >= 0; // LEFT → Right si goal abajo
            else result = true;
            
            return result;
        }

        private String followWallStrict(Map<RelativePos,Integer> perception) {
            String[] dirs = {"UP", "RIGHT", "DOWN", "LEFT"};
            int turnOut = useLeftHand ? 3 : 1; // (-1) Girar hacia afuera
            int turnIn  = useLeftHand ? 1 : 3; // (+1) Girar hacia adentro

            // 1. REGLA DEL HUECO (Esquina Convexa)
            if (isFree(wallToFollow, perception)) {
                String move = dirs[wallToFollow];
                wallToFollow = (wallToFollow + turnIn) % 4;
                return move;
            }
            
            // 2. REGLA DE AVANCE (Pared recta)
            int moveDir = (wallToFollow + turnOut) % 4;
            if (isFree(moveDir, perception)) {
                return dirs[moveDir];
            }
            
            // 3. REGLA DEL CALLEJÓN (Esquina Cóncava)
            wallToFollow = moveDir; 
            return "BLOCKED";
        }

        // Helpers
        private boolean isFree(int dirIndex, Map<RelativePos,Integer> perception) {
            return perception.get(sensors.get(dirIndex)) == 0;
        }

        private String getDirectMove(int[] goalDir, Map<RelativePos, Integer> perception) {
            int dY = goalDir[0];
            int dX = goalDir[1];
            List<String> candidates = new ArrayList<>();

            if (dY < 0 && isFree(0, perception)) candidates.add("UP");
            if (dY > 0 && isFree(2, perception)) candidates.add("DOWN");
            if (dX < 0 && isFree(3, perception)) candidates.add("LEFT");
            if (dX > 0 && isFree(1, perception)) candidates.add("RIGHT");

            if (candidates.isEmpty()) return null;
            
            // Si solo hay una opción, no hay nada que pensar
            if (candidates.size() == 1) return candidates.get(0);

            // Ahora siempre decidirá matemáticamente cuál eje reducir
            String choice;
            if (Math.abs(dY) >= Math.abs(dX)) {
                // Prioridad Vertical
                if (dY < 0) choice = "UP";
                else if (dY > 0) choice = "DOWN";
                else if (dX < 0) choice = "LEFT"; // Si vertical bloqueado, intenta horizontal
                else choice = "RIGHT";
            } else {
                // Prioridad Horizontal
                if (dX < 0) choice = "LEFT";
                else if (dX > 0) choice = "RIGHT";
                else if (dY < 0) choice = "UP"; // Si horizontal bloqueado, intenta vertical
                else choice = "DOWN";
            }
            
            // Verificar que la elección elegida es válida (está en candidatos)
            // Esto es importante porque la lógica de arriba podría elegir una dir bloqueada
            // si su eje principal está bloqueado pero el secundario no.
            if (candidates.contains(choice)) {
                return choice;
            } else {
                // Si la mejor opción matemática está bloqueada, devuelve la primera disponible
                return candidates.get(0);
            }
        }
        
        private int getBlockedDirection(int[] goalDir) {
            int dY = goalDir[0]; int dX = goalDir[1];
            int result = (Math.abs(dY) > Math.abs(dX)) ? (dY < 0 ? 0 : 2) : (dX > 0 ? 1 : 3);
            return result;
        }
        
        private boolean isCompatibleWithLastMove(String move) {
            if (lastMove == null) return true;
            // Reutilizamos isOpposite para simplificar
            return !isOpposite(move, lastMove);
        }

        private String dirName(int idx) {
            String[] names = {"UP", "RIGHT", "DOWN", "LEFT"};
            return (idx >= 0 && idx < 4) ? names[idx] : "???";
        }

        // ✅ NUEVOS MÉTODOS AUXILIARES PARA EL FIX
        private boolean isOpposite(String dir1, String dir2) {
            if (dir1 == null || dir2 == null) return false;
            if (dir1.equals("UP") && dir2.equals("DOWN")) return true;
            if (dir1.equals("DOWN") && dir2.equals("UP")) return true;
            if (dir1.equals("LEFT") && dir2.equals("RIGHT")) return true;
            if (dir1.equals("RIGHT") && dir2.equals("LEFT")) return true;
            return false;
        }

        private int getDirIndex(String dir) {
            switch (dir) {
                case "UP": return 0;
                case "RIGHT": return 1;
                case "DOWN": return 2;
                case "LEFT": return 3;
                default: return -1;
            }
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
