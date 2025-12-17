// REFACTORIZACION GEMINI (EN MI OPINION ESTA MEJOR PERO SE PUEDE REVERTIR AL ORIGINAL)
// Basicamente comprueba cada performativa (en el switch) y mete en la variable 
// responsePayload el mensaje que va entre PREFIX_OUT <responsePayload> SUFFIX_OUT
// Ademas, antes de todo valida si el mensaje recibido es valido con el metodo validateAndParseMessage

package santa;

import controller.Pair;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ThreadLocalRandom;

public class SantaAgent extends Agent {

    // --- CONSTANTES (Vocabulario) ---
    private static final int SECRET_CODE = 101010;
    private static final String PREFIX_IN = "Rakas";
    private static final String TARGET_NAME = "Joulupukki";
    private static final String SUFFIX_IN = "Kiitos";
    
    private static final String PREFIX_OUT = "Hyvää joulua";
    private static final String SUFFIX_OUT = "Nähdään pian";
    
    // Palabras clave especificas de cada accion
    private static final String KEYWORD_COORD = "coordenadas";
    private static final String KEYWORD_ARRIVED = "llegado";

    // Estado del agente
    private Pair<Integer, Integer> coordSanta;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            this.coordSanta = (Pair<Integer, Integer>) args[0];
        } else {
            System.err.println("Santa: NO RECIBE SUS COORDENADAS");
        }
        System.out.println("=== Agente Santa " + getLocalName() + " iniciado en " + coordSanta + " ===");
        addBehaviour(new SantaBehaviour());
    }

    private class SantaBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg = myAgent.blockingReceive();

            if (msg != null) {
                ACLMessage reply = msg.createReply();
                String responsePayload = "";

                try {
                    // 1. Pre-validacion comun para TODOS los mensajes
                    // Esto lanza FIPAException si falla, saltando al catch directamente
                    String[] parts = validateAndParseMessage(msg.getContent());

                    // 2. Logica especifica segun performativa
                    switch (msg.getPerformative()) {
                        
                        case ACLMessage.PROPOSE -> {
                            System.out.println("=== Santa procesando PROPOSE ===");
                            // Logica de aceptacion aleatoria (80%)
                            if (ThreadLocalRandom.current().nextInt(1, 101) < 80) {
                                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                responsePayload = String.valueOf(SECRET_CODE);
                            } else {
                                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                responsePayload = "reject";
                            }
                        }

                        case ACLMessage.QUERY_REF -> {
                            System.out.println("=== Santa procesando QUERY_REF ===");
                            
                            // Validar parametro de usuario (Codigo secreto)
                            String senderCodeStr = msg.getUserDefinedParameter("SECRET_CODE");
                            if (senderCodeStr == null || Integer.parseInt(senderCodeStr) != SECRET_CODE) {
                                throw new FIPAException("Código secreto incorrecto o faltante");
                            }

                            // Validar palabra clave especifica "coordenadas"
                            if (parts.length < 3 || !KEYWORD_COORD.equals(parts[2])) {
                                System.out.println("MENSAJE QUE SE ESTA ENVIANDO: " + msg.getContent());
                                throw new FIPAException("Falta la palabra clave '" + KEYWORD_COORD + "'");
                            }

                            reply.setPerformative(ACLMessage.INFORM_REF);
                            // Aqui enviamos las coordenadas
                            responsePayload = (coordSanta != null) ? coordSanta.toString() : "(?,?)";
                        }

                        case ACLMessage.INFORM -> {
                            System.out.println("=== Santa procesando INFORM ===");
                            
                            // Validar palabra clave específica "llegado"
                            if (parts.length < 3 || !KEYWORD_ARRIVED.equals(parts[2])) {
                                throw new FIPAException("Falta la palabra clave '" + KEYWORD_ARRIVED + "'");
                            }
                            
                            reply.setPerformative(ACLMessage.INFORM);
                            responsePayload = "¡HoHoHo!";
                        }

                        default -> throw new FIPAException("Performativa no soportada: " + ACLMessage.getPerformative(msg.getPerformative()));
                    }

                    // 3. Construccion final del mensaje 
                    // Solo llegamos aquí si no hubo excepciones
                    String finalContent = formatSantaResponse(responsePayload);
                    reply.setContent(finalContent);
                    System.out.println("=== Santa responde: " + finalContent);

                } catch (FIPAException e) {
                    System.err.println("Error lógico: " + e.getMessage());
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Error: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Error técnico: " + e.getMessage());
                    e.printStackTrace();
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    reply.setContent("Error interno: " + e.getMessage());
                }

                // Simulación de pensamiento
                simulateThinking();
                myAgent.send(reply);
            }
        }
    }

    // --- METODOS AUXILIARES PARA LIMPIEZA DE CODIGO ---

    
     // Valida que el mensaje cumpla la estructura "Rakas Joulupukki ... Kiitos"
     // Devuelve el array de palabras si es correcto, o lanza excepcion si no
     
    private String[] validateAndParseMessage(String content) throws FIPAException {
        if (content == null || content.isBlank()) {
            throw new FIPAException("Contenido vacío");
        }
        String[] parts = content.trim().split("\\s+");
        
        // Estructura minima: Rakas Joulupukki [algo] Kiitos (mínimo 3 o 4 palabras según lógica estricta)
        if (parts.length < 3) {
            throw new FIPAException("Mensaje demasiado corto");
        }
        
        // Validacion estricta del protocolo
        if (!PREFIX_IN.equals(parts[0])) throw new FIPAException("No empieza por " + PREFIX_IN);
        if (!TARGET_NAME.equals(parts[1])) throw new FIPAException("No va dirigido a " + TARGET_NAME);
        if (!SUFFIX_IN.equals(parts[parts.length - 1])) throw new FIPAException("No termina en " + SUFFIX_IN);

        return parts;
    }

    // Envuelve el contenido en la frase estándar de Santa:
    // "Hyvää joulua [CONTENIDO] Nähdään pian"
    private String formatSantaResponse(String coreContent) {
        return PREFIX_OUT + " " + coreContent + " " + SUFFIX_OUT;
    }

    private void simulateThinking() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SantaAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

/* CODIGO ESCRITO A MANO
package santa;

import controller.Pair;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ThreadLocalRandom;

public class SantaAgent extends Agent{
    private final int SECRET_CODE = 101010;
    private Pair<Integer,Integer> coordSanta;
    
    @Override
    protected void setup() {
        // Recuperamos los argumentos para recibir las coordenadas
        // de Santa. 
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            this.coordSanta = (Pair<Integer, Integer>) args[0];
        } else {
            System.err.println("Santa: NO RECIBE SUS COORDENADAS");
        }
        System.out.println("===Agente Santa " + getLocalName() + 
                           " iniciado en " + coordSanta + "===");
        addBehaviour(new SantaBehaviour());
    }
    
    // Clase interna reconstruida y limpia
    private class SantaBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Esperar mensaje
            ACLMessage translatedMSG = myAgent.blockingReceive();    
            
            if (translatedMSG != null) {
                // Creamos una reply vacia al mensaje
                ACLMessage reply = translatedMSG.createReply();
                try {
                    // Validación de performativa (16 = REQUEST)
                    // Switch con los diferentes ACLMessage
                    switch(translatedMSG.getPerformative()) {
                        case(ACLMessage.PROPOSE) -> {
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
                                System.out.println("=== Santa recibe mensaje PROPOSE BIEN TRADUCIDO");
                                if (ThreadLocalRandom.current().nextInt(1, 101) < 80) {
                                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    replyContent = "Hyvää joulua " + SECRET_CODE + " Nähdään pian";
                                    System.out.println("=== Santa envia mensaje ACCEPT_PROPOSAL: (|" + replyContent + "|)");
                                }
                                else {
                                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                    replyContent = "Hyvää joulua reject Nähdään pian";
                                    System.out.println("=== Santa envia mensaje REJECT_PROPOSAL: (|" + replyContent + "|)");
                                }
                            } else {
                                System.out.println("=== Santa recibe mensaje PROPOSE MAL TRADUCIDO");
                                throw new FIPAException("El mensaje no empieza por 'Rakas'");
                            }
                        }
                        
                        case(ACLMessage.QUERY_REF) -> {
                            // Extraemos el codigo del mensaje
                            int sender_code = Integer.parseInt(
                                translatedMSG.getUserDefinedParameter("SECRET_CODE"));
                    
                            if ( sender_code != SECRET_CODE){
                                throw new FIPAException("El codigo secreto enviado no es correcto");
                            }
                            
                            // Sacamos el contenido
                            String content = translatedMSG.getContent();
                            System.out.println("=== Santa recibe mensaje QUERY_REF: (|" + content + "|)");

                            // Validamos el agente
                            String[] parts = content.trim().split("\\s+"); // Split por espacios
                            
                            if (parts.length == 0) {
                                 throw new FIPAException("El contenido está vacío");
                            }
                            
                            String replyContent = "";
                            
                             if ("Rakas".equals(parts[0]) && "Joulupukki".equals(parts[1]) && "coordenadas".equals(parts[2]) && "Kiitos".equals(parts[parts.length -1])) {
                                System.out.println("=== Santa recibe mensaje QUERY-REF BIEN TRADUCIDO");
                                reply.setPerformative(ACLMessage.INFORM_REF);
                                replyContent = "Hyvää joulua " + coordSanta.toString() + " Nähdään pian";
                                System.out.println("=== Santa envia mensaje INFORM_REF: (|" + replyContent + "|)");
                            } else {
                                System.out.println("=== Santa recibe mensaje QUERY_REF MAL TRADUCIDO");
                                throw new FIPAException("El mensaje no empieza por 'Rakas'");
                            }
                        }
                        
                        case(ACLMessage.INFORM) -> {
                            // Sacamos el contenido
                            String content = translatedMSG.getContent();
                            System.out.println("=== Santa recibe mensaje INFORM: (|" + content + "|)");

                            // Validamos el agente
                            String[] parts = content.trim().split("\\s+"); // Split por espacios
                            
                            if (parts.length == 0) {
                                 throw new FIPAException("El contenido está vacío");
                            }
                            
                            String replyContent = "";
                            
                             if ("Rakas".equals(parts[0]) && "Joulupukki".equals(parts[1]) && "llegado".equals(parts[2]) && "Kiitos".equals(parts[parts.length -1])) {
                                System.out.println("=== Santa recibe mensaje INFORM BIEN TRADUCIDO");
                                reply.setPerformative(ACLMessage.INFORM);
                                replyContent = "Hyvää joulua ¡HoHoHo! Nähdään pian";
                                System.out.println("=== Santa envia mensaje INFORM: (|" + replyContent + "|)");
                            } else {
                                System.out.println("=== Santa recibe mensaje INFORM MAL TRADUCIDO");
                                throw new FIPAException("El mensaje no empieza por 'Rakas'");
                            }
                        }
                        
                        default -> {
                            throw new FIPAException("La performativa mandada no es ni PROPOSE ni QUERY-REF ni INFORM (Santa)");
                        }
                            
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
*/