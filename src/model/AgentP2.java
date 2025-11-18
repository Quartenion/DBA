package model;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agente reactivo hibrido:
 * 1. Busqueda greedy cuando se puede mover directamente hacia el objetivo
 * 2. Seguimiento de paredes (algoritmo Bug) al encontrar un obstaculo
 */
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
