// src/modelo/DatosApp.java
package model;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;

public class AgentP2 extends Agent {
    private Environment model;
    private final int ACTION_TIME = 2000;
    
    @Override
    protected void setup(){
        
        // Add the behaviours
        addBehaviour(new SeeBehaviour(this, ACTION_TIME));
        addBehaviour(new ThinkBehaviour(this, ACTION_TIME));
        addBehaviour(new ActionBehaviour(this, ACTION_TIME));   
    }
    
    @Override
    public void takeDown(){
        System.out.println("Terminating agent...");
    }
    
    
    
    //-------------------------------------------------------
    // The see behaviour uses the sensors
    // To update its memory with new possible states,
    // if there are any
    class SeeBehaviour extends TickerBehaviour {
                
        public SeeBehaviour(Agent a, long period){
            super(a, period);
        }
        
        @Override
        public void onTick(){
            System.out.println("\neco");
        }
    }
    
    // The think behaviour contains the logic
    // To calculate the value of each posible state
    class ThinkBehaviour extends TickerBehaviour {
        
        public ThinkBehaviour(Agent a, long period){
            super(a, period);
        }
        
        @Override
        public void onTick(){
            System.out.println("\neco2");
        }
        
    }
    
    // The action behaviour, previusly the
    // possible movements have been assigned 
    // a value so it can decide.
    class ActionBehaviour extends TickerBehaviour {
                
        public ActionBehaviour(Agent a, long period){
            super(a, period);
        }
        
        @Override
        public void onTick(){
            System.out.println("\neco3");
        }
    }
}