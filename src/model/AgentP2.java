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
    private final int ACTION_TIME = 500;
    
    private DataStore sharedDataStore; // DataStore compartido
    
    
    // Representation of the sensors of this agent NORTH, EAST, SOUTH, WEST
    private final ArrayList<RelativePos> sensors = new ArrayList<>(List.of(new RelativePos( -1,0),
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
    
    
    //-------------------------------------------------------
    // COMPORTAMIENTO 1: VER (cada 2 segundos)
    //-------------------------------------------------------
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
            
            // Almacenar en datastore compartido
            getDataStore().put("perception", perception);
            getDataStore().put("seeComplete", true);
            
            /*
            TODO: revisar -> no entiendo mucho el proposito de esto
            System.out.println("\n[" + tickCount + "] SEE - Percepcion:");
            System.out.println("   UP:" + (perception[0] == 0 ? "OK" : "XX") + 
                             " DOWN:" + (perception[1] == 0 ? "OK" : "XX") + 
                             " LEFT:" + (perception[2] == 0 ? "OK" : "XX") + 
                             " RIGHT:" + (perception[3] == 0 ? "OK" : "XX"));
            System.out.println("   Distancia objetivo: DX=" + goalDirection[0] + ", DY=" + goalDirection[1]);
            */
            
        }
    }
    
    //-------------------------------------------------------
    // COMPORTAMIENTO 2: PENSAR (cada 2 segundos)
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
            
            
            
            // ESTRATEGIA SIMPLE: Ir primero en X (filas), luego en Y (columnas)
            String decision = null;
            
            RelativePos goalDirection = new RelativePos(model.getGoalX(), model.getGoalY());
            
            // Necesitamos movernos en filas?
            if (goalDirection.getX() < model.getAgentX() && perception.get(sensors.get(0)) == 0) {
                decision = "UP";
            } else if (goalDirection.getX() > model.getAgentX() && perception.get(sensors.get(2)) == 0) {
                decision = "DOWN";
            }
            // Si no podemos movernos en filas, intentamos columnas
            else if (goalDirection.getY() < model.getAgentY() && perception.get(sensors.get(3)) == 0) {
                decision = "LEFT";
            } else if (goalDirection.getY() > model.getAgentY() && perception.get(sensors.get(1)) == 0) {
                decision = "RIGHT";
            }
            // Si el camino directo esta bloqueado, buscar alternativa
            else {
                if (perception.get(sensors.get(0)) == 0) decision = "UP";
                else if (perception.get(sensors.get(2)) == 0) decision = "DOWN";
                else if (perception.get(sensors.get(3)) == 0) decision = "LEFT";
                else if (perception.get(sensors.get(1)) == 0) decision = "RIGHT";
                else decision = "BLOCKED"; // No hay salida
            }
            
            getDataStore().put("decision", decision);
            getDataStore().put("thinkComplete", true);
            
            System.out.println("[" + tickCount + "] THINK - Decision: " + decision);
               
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