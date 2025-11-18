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
    
    
    
    // Reemplaza toda la clase interna reactiveAgentBehaviour con esto:
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

        public reactiveAgentBehaviour(Agent a, long period){
            super(a, period);
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
                    } else {
                        // --- CHOQUE DETECTADO ---
                        currentMode = Mode.WALL_FOLLOW;
                        hitDistance = currentDist; // Guardamos referencia: "Tengo que mejorar esto"
                        
                        // 1. ¿Dónde está el muro?
                        wallToFollow = getBlockedDirection(goalDir);
                        
                        // 2. Decidir mano INTELIGENTE:
                        // Si el objetivo está a mi derecha, uso mano IZQUIERDA (para ir hacia la derecha)
                        // Si el objetivo está a mi izquierda, uso mano DERECHA
                        useLeftHand = decideHand(wallToFollow, goalDir);
                        
                        // Primer movimiento de rodeo
                        decision = followWallStrict(perception);
                        logMessage = "HIT -> WALL_FOLLOW (" + (useLeftHand ? "L" : "R") + ")";
                    }
                    break;

                case WALL_FOLLOW:
                    // CONDICIÓN DE SALIDA:
                    // 1. ¿Estamos más cerca que cuando empezamos a rodear este obstáculo?
                    // 2. ¿Podemos movernos hacia el objetivo?
                    String tryDirect = getDirectMove(goalDir, perception);
                    
                    // Usamos un margen de 1.0 para asegurar que realmente hemos avanzado
                    if (tryDirect != null && currentDist < (hitDistance - 0.5)) {
                        currentMode = Mode.GREEDY;
                        decision = tryDirect;
                        logMessage = "LEAVE WALL -> GREEDY";
                    } else {
                        // SEGUIR PARED ESTRICTAMENTE (Geometría pura, sin memoria de visitas)
                        decision = followWallStrict(perception);
                        
                        // Mini-fix: Si nos encerramos en un bucle infinito perfecto (raro pero posible),
                        // aquí podrías añadir una comprobación de visitas > 10, pero 
                        // normalmente el Bug Algorithm sale solo.
                        logMessage = "WALL (" + (useLeftHand?"L":"R") + ")";
                    }
                    break;
            }

            System.out.println("[" + tickCount + "] " + logMessage + " | Dist: " + String.format("%.2f", currentDist));

            // 3. ACCIÓN
            if (!"BLOCKED".equals(decision)) {
                int oldX = model.getAgentX();
                int oldY = model.getAgentY();
                model.moveAgent(decision);
                if (oldX != model.getAgentX() || oldY != model.getAgentY()) {
                    model.incrementEnergy();
                }
            }
            ((AgentP2) myAgent).changeState(State.ACTION);
        }

        // ------------------------------------------------------------------------
        // LÓGICA DETERMINISTA
        // ------------------------------------------------------------------------

        /**
         * Decide qué mano usar para rodear el obstáculo más rápido.
         * Lógica: Queremos ir "hacia" el objetivo rodeando el muro.
         */
        private boolean decideHand(int blockedDir, int[] goalDir) {
            int dY = goalDir[0]; // Vertical (-Arriba, +Abajo)
            int dX = goalDir[1]; // Horizontal (-Izquierda, +Derecha)
            
            // blockedDir: 0:UP, 1:RIGHT, 2:DOWN, 3:LEFT
            
            // Ejemplo: Quiero ir UP (0) y choco.
            if (blockedDir == 0) {
                // Si el objetivo está a la derecha (dX > 0), quiero ir a la derecha.
                // Poner la mano IZQUIERDA en el muro de arriba me hace caminar hacia la derecha.
                if (dX >= 0) return true; // Left Hand
                return false; // Right Hand
            }
            if (blockedDir == 2) { // DOWN
                if (dX >= 0) return false; // Right Hand (Muro derecha -> voy derecha?? No, espera)
                // Si miro abajo (muro), mano derecha en muro -> voy a la izquierda.
                // Si goal está a la derecha, quiero ir izquierda? No.
                // Si goal derecha, quiero ir derecha. Mano Izq en muro abajo -> voy derecha.
                return true; // Left Hand
            }
            if (blockedDir == 1) { // RIGHT
                // Goal Arriba (dY < 0). Mano Izq en muro derecha -> Voy arriba.
                if (dY < 0) return true; 
                return false;
            }
            if (blockedDir == 3) { // LEFT
                // Goal Arriba (dY < 0). Mano Der en muro izquierda -> Voy arriba.
                if (dY < 0) return false;
                return true;
            }
            return true; // Default
        }

        /**
         * Sigue el muro geométricamente.
         * NO mira celdas visitadas. Solo mira sensores.
         */
        private String followWallStrict(Map<RelativePos,Integer> perception) {
            String[] dirs = {"UP", "RIGHT", "DOWN", "LEFT"};
            
            // Definir giros según la mano
            // Indice aumenta = Giro Derecha (Clockwise)
            // Indice disminuye = Giro Izquierda (Counter-Clockwise)
            
            int turnOut = useLeftHand ? 3 : 1; // (-1) Girar hacia afuera (buscar hueco)
            int turnIn  = useLeftHand ? 1 : 3; // (+1) Girar hacia adentro (chocamos)
            
            // 1. REGLA DEL HUECO (Esquina Convexa)
            // Si la celda donde debería estar mi mano (wallToFollow) está LIBRE,
            // significa que el muro se acabó o dobló. Debo girar y "abrazar" la esquina.
            if (isFree(wallToFollow, perception)) {
                // Moverse hacia el hueco
                String move = dirs[wallToFollow];
                // Actualizar referencia: Ahora el muro está "detrás" del giro relativo
                wallToFollow = (wallToFollow + turnIn) % 4; 
                return move;
            }
            
            // 2. REGLA DE AVANCE (Pared recta)
            // Intentar avanzar paralelo al muro.
            // Dirección de avance = Muro + 90 grados (hacia fuera de la mano)
            int moveDir = (wallToFollow + turnOut) % 4;
            
            if (isFree(moveDir, perception)) {
                return dirs[moveDir];
            }
            
            // 3. REGLA DEL CALLEJÓN (Esquina Cóncava)
            // No puedo seguir la pared (hay muro enfrente).
            // El obstáculo de enfrente se convierte en mi nuevo muro de referencia.
            wallToFollow = moveDir; 
            
            // Devolvemos BLOCKED para rotar mentalmente en este tick.
            return "BLOCKED";
        }

        // Helpers
        private boolean isFree(int dirIndex, Map<RelativePos,Integer> perception) {
            return perception.get(sensors.get(dirIndex)) == 0;
        }

        private String getDirectMove(int[] goalDir, Map<RelativePos,Integer> perception) {
             int dY = goalDir[0]; int dX = goalDir[1];
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
        
        private int getBlockedDirection(int[] goalDir) {
             int dY = goalDir[0]; int dX = goalDir[1];
            if (Math.abs(dY) > Math.abs(dX)) return (dY < 0) ? 0 : 2;
            else return (dX > 0) ? 1 : 3;
        }
    }
}