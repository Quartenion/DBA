package santa;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * 
 */
public class SantaAgent extends Agent{
    private final int secret_code = 1010;
    
    @Override
    protected void setup() {
        System.out.println("===Agente Santa " + getLocalName() + " iniciado.===");
        addBehaviour(new SantaBehaviour());
    }
    
    // Clase interna reconstruida y limpia
    private class SantaBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Esperar mensaje
            ACLMessage translatedMSG = myAgent.blockingReceive();
            
            
            if (translatedMSG != null) {
                ACLMessage reply = translatedMSG.createReply();
                try {
                    // Validación de performativa (16 = REQUEST)
                    if (translatedMSG.getPerformative() != ACLMessage.PROPOSE) {
                        throw new FIPAException("La performativa mandada no es un PROPOSE (Santa)");
                    }
                    
                    // Sacamos el contenido
                    String content = translatedMSG.getContent();
                    System.out.println("=== Santa recibe mensaje PROPOSE: (|" + content + "|)");
                    
                    // Validamos el agente
                    String[] parts = content.trim().split("\\s+"); // Split por espacios
                    
                    if (parts.length == 0) {
                         throw new FIPAException("El contenido está vacío");
                    }
                    
                    String replyContent = "";
                    
                    if ("Rakas".equals(parts[0]) && "Joulupukki".equals(parts[1]) && "Kiitos".equals(parts[parts.length -1])) {
                        System.out.println("=== Santa recibe mensaje BIEN TRADUCIDO");
                        if (ThreadLocalRandom.current().nextInt(1, 101) < 80) {
                            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            replyContent = "Hyvää joulua " + secret_code + " Nähdään pian";
                            System.out.println("=== Santa envia mensaje ACCEPT_PROPOSAL: (|" + replyContent + "|)");
                        }
                        else {
                            reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            replyContent = "Hyvää joulua reject Nähdään pian";
                            System.out.println("=== Santa envia mensaje REJECT_PROPOSAL: (|" + replyContent + "|)");
                        }
                    } else {
                        System.out.println("=== Santa recibe mensaje MAL TRADUCIDO");
                        throw new FIPAException("El mensaje no empieza por 'Rakas'");
                    }
                    
                } catch (FIPAException e) {
                    // Manejo de errores lógicos (14 = REFUSE)
                    System.err.println("\n=== Error de formato: " + e.getMessage() + " ===\n");
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Error de formato: " + e.getMessage());
                    
                } catch (Exception e) {
                    // Manejo de errores técnicos
                    System.err.println("\n=== Error desconocido: " + e.getMessage() + " ===\n");
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    reply.setContent("Error interno: " + e.getMessage());
                }
                
                // DEBUG SIMULATOR
                try {
                    System.out.println("=== Santa piensa durante 2s");
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SantaAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                myAgent.send(reply);
            }
        }
    }
}
