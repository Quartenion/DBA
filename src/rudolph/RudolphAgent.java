package rudolph;


import controller.Pair;
import elfo.ElfTranslatorAgent;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RUDOLPH AGENT
 * 
 * -> Solo acepta QUERY-REF
 * -> Devuelve REFUSE en caso de no ser QUERY-REF y de que el codigo este mal
 * -> Extrae el codigo a traves de userDefinedParameter "SECRET_CODE"
 * -> Se la pela el contenido del mensaje
 * 
 * 
 * @param -> coordRenos : ArrayList<Pair<Integer,Integer>>
 */
public class RudolphAgent extends Agent {
    private final int SECRET_CODE = 101010;
    private Queue<Pair<Integer,Integer>> coordRenos;
    
    
    @Override
    public void setup(){
        // Recuperamos los argumentos para recibir el listado de coordenadas
        // de los renos. Tiene que ser 
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof ArrayList) {
            this.coordRenos = new LinkedList<>((ArrayList) args[0]);
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
            // Esperamos mensajes QUERY-REF
            ACLMessage request = myAgent.blockingReceive();
            
            // Verificamos que hay mensaje
            if (request != null){
                
                System.out.println("=== Rudolph recibe mensaje REQUEST: (" 
                                               + request.getContent() + ") ===");
                // Creamos una reply vacia al mensaje
                ACLMessage response = request.createReply();
                
                try {
                    // Validamos que sea un QUERY-REF y que sea SECRET_CODE
                    if (request.getPerformative() != ACLMessage.QUERY_REF){
                        throw new FIPAException("Soy Rudolph y solo acepto QUERY_REF");
                    }
                    
                    // Extraemos el codigo del mensaje
                    int sender_code = Integer.parseInt(
                                request.getUserDefinedParameter("SECRET_CODE"));
                    
                    if ( sender_code != SECRET_CODE){
                        throw new FIPAException("El codigo secreto enviado no es correcto");
                    }
                    
                    
                    // Una vez terminada la validacion mandamos el siguiente reno
                    if (!coordRenos.isEmpty()) {
                        // Añadimos la siguiente coordenada a la respuesta
                        System.out.println("=== Rudolph valida mensaje y responde con un reno ===");
                        response.setPerformative(ACLMessage.INFORM_REF);
                        response.setContentObject((Serializable) coordRenos.poll());
                        
                        // El objeto se recibe tal que asi:
                        // Pair<Integer, Integer> coordRecibida = (Pair<Integer, Integer>) msg.getContentObject();
                    }
                    else {
                        // Si tenemos que no hay mandamos eso mismo con un INFORM
                        System.out.println("=== Rudolph valida mensaje y responde que ya no hay renos ===");
                        response.setPerformative(ACLMessage.INFORM);
                        response.setContent("Ya no hay mas renos");
                    }
                    
                    // En este punto ya está 
                    
                } catch (FIPAException e) {
                    // Manejo de errores lógicos 
                    System.err.println("\n=== Error conocido: " + e.getMessage() + " ===\n");
                    response.setPerformative(ACLMessage.REFUSE);
                    response.setContent("Error conocido: " + e.getMessage());

                } catch (Exception e) {
                    // Manejo de errores técnicos
                    System.err.println("\n=== Error desconocido: " + e.getMessage() + " ===\n");
                    response.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    response.setContent("Error interno: " + e.getMessage());
                }
                
                // DEBUG SIMULATOR
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ElfTranslatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                myAgent.send(response);
            }
        }
    }
}
