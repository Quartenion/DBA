package elfo;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElfTranslatorAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("===Agente Elfo " + getLocalName() + " iniciado.===");
        // Añadimos el comportamiento que has recuperado
        addBehaviour(new TranslatorBehaviour());
    }

    // Clase interna reconstruida y limpia
    private class TranslatorBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            // Esperar mensaje
            ACLMessage translateMSG = myAgent.blockingReceive();
            
            if (translateMSG != null) {
                ACLMessage translateReply = translateMSG.createReply();
                
                try {
                    // Validación de performativa (16 = REQUEST)
                    if (translateMSG.getPerformative() != ACLMessage.REQUEST) {
                        throw new FIPAException("La performativa mandada no es un REQUEST (ElfTranslator)");
                    }
                    
                    // Sacamos el contenido
                    String content = translateMSG.getContent();
                    System.out.println("=== ElfTranslator recibe mensaje REQUEST: (" + content + ") ===");
                    
                    // Validamos el agente
                    String[] parts = content.trim().split("\\s+"); // Split por espacios
                    
                    if (parts.length == 0) {
                         throw new FIPAException("El contenido está vacío");
                    }

                    String replyContent = "";

                    switch (parts[0]) {
                        case "Bro" -> {
                            // Verifica estructura: "Bro ... en plan"
                            if (parts.length >= 3 && "en".equals(parts[parts.length - 2]) && "plan".equals(parts[parts.length - 1])) {
                                translateReply.setPerformative(ACLMessage.INFORM);
                                
                                // Lógica Regex recuperada
                                replyContent = content.replaceFirst("^\\S+", "Rakas Joulupukki");
                                replyContent = replyContent.replaceAll("\\S+\\s+\\S+\\s+\\S+$", "Kiitos"); 
                                // OJO: He ajustado el regex final porque "en plan" son dos palabras, no una.
                                
                                translateReply.setContent(replyContent);
                            } else {
                                throw new FIPAException("Estructura incorrecta para 'Bro... en plan'");
                            }
                        }

                        case "Hyvää" -> {
                            // Verifica estructura: "Hyvää jolua ... Nähdään pian"
                            // (Asegúrate de ajustar los índices según tu lógica exacta)
                            if (parts.length >= 4 && "jolua".equals(parts[1])
                                && "Nähdään".equals(parts[parts.length-2])
                                && "pian".equals(parts[parts.length - 1])) {
                                
                                translateReply.setPerformative(ACLMessage.INFORM);
                                
                                // Lógica Regex recuperada
                                replyContent = content.replaceFirst("^\\S+\\s+\\S+", "Bro");
                                replyContent = replyContent.replaceAll("\\S+\\s+\\S+$", "en plan");
                                
                                translateReply.setContent(replyContent);
                            } else {
                                throw new FIPAException("Estructura incorrecta para finlandés");
                            }
                        }

                        default -> {
                            throw new FIPAException("El mensaje no empieza por 'Bro' ni 'Hyvää'");
                        }
                    }
                    // ===============================================

                } catch (FIPAException e) {
                    // Manejo de errores lógicos (14 = REFUSE)
                    System.err.println("\n=== Error de formato: " + e.getMessage() + " ===\n");
                    translateReply.setPerformative(ACLMessage.REFUSE);
                    translateReply.setContent("Error de formato: " + e.getMessage());
                    
                } catch (Exception e) {
                    // Manejo de errores técnicos
                    System.err.println("\n=== Error desconocido: " + e.getMessage() + " ===\n");
                    translateReply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    translateReply.setContent("Error interno: " + e.getMessage());
                }

                // DEBUG SIMULATOR
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ElfTranslatorAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
               
                // Enviar respuesta
                myAgent.send(translateReply);
            }
        }
    }
}