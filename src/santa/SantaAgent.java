package santa;

import controller.Pair;
import controller.ViewLogger;
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
    private ViewLogger logger;
    private final boolean haveLog = false; // Flag para activar/desactivar logs

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            this.coordSanta = (Pair<Integer, Integer>) args[0];
        } else {
            System.err.println("Santa: NO RECIBE SUS COORDENADAS");
        }
        
        if (args != null && args.length > 1 && args[1] instanceof ViewLogger) {
            this.logger = (ViewLogger) args[1];
        } 
        
        System.out.println("=== Agente Santa " + getLocalName() + " iniciado en " + coordSanta + " ===");
        addBehaviour(new SantaBehaviour());
    }

    private class SantaBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg = myAgent.blockingReceive();

            if (msg != null) {
                // Registro de RECEIVE basado en ElfTranslator
                if (haveLog) {
                    logger.addLogEntry("""
                                       #RECIEVE - "%s"# Santa recibe de "%s" 
                                       el mensaje: "%s"
                                       """.formatted(
                       ACLMessage.getPerformative(msg.getPerformative()),
                                               msg.getSender().getLocalName(),
                                                        msg.getContent()));
                }

                ACLMessage reply = msg.createReply();
                String responsePayload = "";

                try {
                    String[] parts = validateAndParseMessage(msg.getContent());

                    switch (msg.getPerformative()) {
                        case ACLMessage.PROPOSE -> {
                            System.out.println("=== Santa procesando PROPOSE ===");
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
                            String senderCodeStr = msg.getUserDefinedParameter("SECRET_CODE");
                            if (senderCodeStr == null || Integer.parseInt(senderCodeStr) != SECRET_CODE) {
                                throw new FIPAException("Código secreto incorrecto o faltante");
                            }
                            if (parts.length < 3 || !KEYWORD_COORD.equals(parts[2])) {
                                throw new FIPAException("Falta la palabra clave '" + KEYWORD_COORD + "'");
                            }
                            reply.setPerformative(ACLMessage.INFORM_REF);
                            responsePayload = (coordSanta != null) ? coordSanta.toString() : "(?,?)";
                        }

                        case ACLMessage.INFORM -> {
                            System.out.println("=== Santa procesando INFORM ===");
                            if (parts.length < 3 || !KEYWORD_ARRIVED.equals(parts[2])) {
                                throw new FIPAException("Falta la palabra clave '" + KEYWORD_ARRIVED + "'");
                            }
                            reply.setPerformative(ACLMessage.INFORM);
                            responsePayload = "¡HoHoHo!";
                        }

                        default -> throw new FIPAException("Performativa no soportada: " + ACLMessage.getPerformative(msg.getPerformative()));
                    }

                    String finalContent = formatSantaResponse(responsePayload);
                    reply.setContent(finalContent);
                    System.out.println("=== Santa responde: " + finalContent);

                } catch (FIPAException e) {
                    System.err.println("Error lógico: " + e.getMessage());
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Error: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Error técnico: " + e.getMessage());
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    reply.setContent("Error interno: " + e.getMessage());
                }


                // Registro de SEND basado en ElfTranslator
                if (haveLog) {   
                    simulateThinking();
                    logger.addLogEntry("""
                                       #SEND - "%s"# Santa envia respuesta a %s: "%s"
                                       """.formatted(
                          ACLMessage.getPerformative(reply.getPerformative()),
                                                     msg.getSender().getLocalName(),
                                                     reply.getContent()));
                }

                myAgent.send(reply);
            }
        }
    }

    private String[] validateAndParseMessage(String content) throws FIPAException {
        if (content == null || content.isBlank()) throw new FIPAException("Contenido vacío");
        String[] parts = content.trim().split("\\s+");
        if (parts.length < 3) throw new FIPAException("Mensaje demasiado corto");
        if (!PREFIX_IN.equals(parts[0])) throw new FIPAException("No empieza por " + PREFIX_IN);
        if (!TARGET_NAME.equals(parts[1])) throw new FIPAException("No va dirigido a " + TARGET_NAME);
        if (!SUFFIX_IN.equals(parts[parts.length - 1])) throw new FIPAException("No termina en " + SUFFIX_IN);
        return parts;
    }

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