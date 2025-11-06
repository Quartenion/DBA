package model;

import jade.core.Agent;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.TickerBehaviour;

public class AgentP2 extends Agent {
    private Environment model;
    private final int ACTION_TIME = 2000;
    private DataStore sharedDataStore; // DataStore compartido
    
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
            
            // El agente usa sus sensores para percibir el entorno
            int[] perception = model.perceive();
            int[] goalDirection = model.getDirectionToGoal();
            
            // Almacenar en datastore compartido
            getDataStore().put("perception", perception);
            getDataStore().put("goalDirection", goalDirection);
            getDataStore().put("seeComplete", true);
            
            System.out.println("\n[" + tickCount + "] SEE - Percepcion:");
            System.out.println("   UP:" + (perception[0] == 0 ? "OK" : "XX") + 
                             " DOWN:" + (perception[1] == 0 ? "OK" : "XX") + 
                             " LEFT:" + (perception[2] == 0 ? "OK" : "XX") + 
                             " RIGHT:" + (perception[3] == 0 ? "OK" : "XX"));
            System.out.println("   Distancia objetivo: DX=" + goalDirection[0] + ", DY=" + goalDirection[1]);
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
            
            // Esperar a que See haya completado
            Boolean seeComplete = (Boolean) getDataStore().get("seeComplete");
            if (seeComplete == null || !seeComplete) {
                return;
            }
            
            // Obtener percepción del datastore
            int[] perception = (int[]) getDataStore().get("perception");
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
            
            // ESTRATEGIA SIMPLE: Ir primero en X (filas), luego en Y (columnas)
            String decision = null;
            
            // Necesitamos movernos en filas?
            if (goalDirection[0] < 0 && perception[0] == 0) {
                decision = "UP";
            } else if (goalDirection[0] > 0 && perception[1] == 0) {
                decision = "DOWN";
            }
            // Si no podemos movernos en filas, intentamos columnas
            else if (goalDirection[1] < 0 && perception[2] == 0) {
                decision = "LEFT";
            } else if (goalDirection[1] > 0 && perception[3] == 0) {
                decision = "RIGHT";
            }
            // Si el camino directo esta bloqueado, buscar alternativa
            else {
                if (perception[0] == 0) decision = "UP";
                else if (perception[1] == 0) decision = "DOWN";
                else if (perception[2] == 0) decision = "LEFT";
                else if (perception[3] == 0) decision = "RIGHT";
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
            
            // Ejecutar movimiento a través del entorno
            boolean success = model.moveAgent(decision);
            
            if (success) {
                model.incrementEnergy();
                System.out.println("[" + tickCount + "] ACTION - Ejecutado: " + decision + 
                                 " | Energia: " + model.getEnergyUsed());
            } else {
                System.out.println("[" + tickCount + "] ACTION - FALLO: " + decision);
            }
            
            // Limpiar flags para el siguiente ciclo
            getDataStore().remove("seeComplete");
            getDataStore().remove("thinkComplete");
        }
    }
}