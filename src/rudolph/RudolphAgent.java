package rudolph;

import controller.Pair;
import controller.ViewLogger;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RudolphAgent extends Agent {
    private final int SECRET_CODE = 101010;
    private Queue<Pair<Integer,Integer>> coordRenos;
    private ViewLogger logger;
    private final boolean haveLog = false; // Flag para activar/desactivar logs
    
    @Override
    public void setup(){
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof ArrayList) {
            this.coordRenos = new LinkedList<>((ArrayList) args[0]);
        } 
        
        if (args != null && args.length > 1 && args[1] instanceof ViewLogger) {
            this.logger = (ViewLogger) args[1];
        } 
        
        System.out.println("=== Agente " + getLocalName() + " iniciado. ===");
        addBehaviour(new ReindeerBehaviour());
    } 
    
    @Override
    public void takeDown(){
        System.out.println("=== Agente " + getLocalName() + " siendo borrado. ===");
    }
    
    private class ReindeerBehaviour extends CyclicBehaviour {
        
        @Override
        public void action(){
            ACLMessage request = myAgent.blockingReceive();
            
            if (request != null){
                // Registro de RECEIVE basado en ElfTranslator
                if (haveLog){
                    logger.addLogEntry("""
                                       #RECIEVE - "%s"# Rudolph recibe de "%s" 
                                       el mensaje: "%s"
                                       """.formatted(
                       ACLMessage.getPerformative(request.getPerformative()),
                                               request.getSender().getLocalName(),
                                                        request.getContent()));
                }

                System.out.println("=== Rudolph recibe mensaje: (" + request.getContent() + ") ===");
                ACLMessage response = request.createReply();
                String logContent = "";
                
                try {
                    if (request.getPerformative() != ACLMessage.QUERY_REF){
                        throw new FIPAException("Soy Rudolph y solo acepto QUERY_REF");
                    }
                    
                    int sender_code = Integer.parseInt(request.getUserDefinedParameter("SECRET_CODE"));
                    if (sender_code != SECRET_CODE){
                        throw new FIPAException("El codigo secreto enviado no es correcto");
                    }
                    
                    if (!coordRenos.isEmpty()) {
                        System.out.println("=== Rudolph valida mensaje y responde con un reno ===");
                        response.setPerformative(ACLMessage.INFORM_REF);
                        Pair<Integer, Integer> nextReno = coordRenos.poll();
                        response.setContentObject((Serializable) nextReno);
                        logContent = nextReno.toString(); // Para el log
                    }
                    else {
                        System.out.println("=== Rudolph valida mensaje y responde que ya no hay renos ===");
                        response.setPerformative(ACLMessage.INFORM);
                        response.setContent("Ya no hay mas renos");
                        logContent = response.getContent();
                    }
                    
                } catch (FIPAException e) {
                    System.err.println("\n=== Error conocido: " + e.getMessage() + " ===\n");
                    response.setPerformative(ACLMessage.REFUSE);
                    response.setContent("Error conocido: " + e.getMessage());
                    logContent = response.getContent();
                } catch (IOException | NumberFormatException e) {
                    System.err.println("\n=== Error desconocido: " + e.getMessage() + " ===\n");
                    response.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    response.setContent("Error interno: " + e.getMessage());
                    logContent = response.getContent();
                }
                

                
                // Registro de SEND basado en ElfTranslator
                if (haveLog) {
                    // Simulación de espera
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(RudolphAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    logger.addLogEntry("""
                                       #SEND - "%s"# Rudolph envia respuesta a %s: "%s"
                                       """.formatted(
                          ACLMessage.getPerformative(response.getPerformative()),
                                                     request.getSender().getLocalName(),
                                                     logContent));
                }

                myAgent.send(response);
            }
        }
    }
}