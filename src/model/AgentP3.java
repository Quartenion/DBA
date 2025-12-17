//
package model;

import controller.Pair;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import jade.domain.FIPAException;
import jade.lang.acl.UnreadableException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Agente Reactivo P2.
// Implementa un algoritmo de navegacion tipo Bug Algorithm
// con mejoras heuristicas para evitar bucles infinitos y movimientos redundantes

/**
 * Agente Reactivo-Comunicativo P3.
 * 
 * Comportamiento ahora:
 *  One shot behaviour que haga una función que sea traducir la solicitud para jugar
 *  Luego el comportamiento envía PROPOSE a santa y en caso de ser
 *  aceptado empieza el bucle de ejecución, que consiste en que 
 *  se pregunta a rudolph el siguiente reno, se recoge y se pone que es la goal,
 *  se va hasta allí y se repite el bucle.
 *  Cuando están todos los renos se coloca una variable y se hace
 *  comunicación con santa.
 *  Se vuelve al ticker behaviour para ir a santa y se termina la ejecución
 *  
 *  
 * 
 */
 
public class AgentP3 extends Agent {

    private Environment model;
    private final int ACTION_TIME = 50; // Tiempo entre ticks
    private String lastMove = null;      // Guarda la ultima accion efectiva ("UP", "DOWN", etc...)
                                         // Sirve para evitar que se realice una accion completamente opuesta
    // Codigo secreto que inicia como 0 y toma el valor que le de santa
    private int secret_code = 0;
    
    // Usamos IDs únicos de comunicación para cada uno de los agentes ya
    // que solo tendrá una vía abierta al mismo tiempo con cada uno de ellos
    private final String TRASLATION_ID = "traslation_elf_";
    private final String SANTA_MSG_ID  = "santa_";
    private final String RUDOLPH_REINDEER_ID = "reindeer_";
    
    
    
    // Configuración de sensores: 0=UP, 1=RIGHT, 2=DOWN, 3=LEFT
    private final ArrayList<RelativePos> sensors = new ArrayList<>(List.of(
            new RelativePos(-1, 0), // UP
            new RelativePos(0, 1),  // RIGHT
            new RelativePos(1, 0),  // DOWN
            new RelativePos(0, -1)  // LEFT
    ));
    
    @Override
    protected void setup(){
        // 1. Recuperacion de argumentos (El entorno)
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof Environment) {
            this.model = (Environment) args[0];
        } else {
            throw new RuntimeException("Environment no proporcionado");
        }
        
        // 2. Añadir comportamientos
        if (args.length > 1) {
            addBehaviour(new startSearchBehaviour());
            addBehaviour(new reactiveAgentBehaviourP3(this, ACTION_TIME));
        }
        else {            
            addBehaviour(new reactiveAgentBehaviour(this, ACTION_TIME));
        }
        
        
        // 3. Limpieza de cola de mensajes (esto no evita si había uno llegando)
        // Cubre la mayoría de los casos, si fueramos prudentes lo que habría que 
        // hacer es pasarle ID único al agente y trabajar con eso, no hace falta
        flushMessageQueue();
        
        System.out.println("=== SetUp completado agente AgentP3 ===");
    }
    
    @Override
    public void takeDown(){
        System.out.println("=== Terminando agente AgentP3 ===");
    }
    
    public void flushMessageQueue() {
    ACLMessage msg = receive();
    while (msg != null) {
        // Simplemente los leemos y no hacemos nada con ellos
        // para sacarlos de la cola interna del agente.
        msg = receive();
    }
    System.out.println("Cola de mensajes vaciada.");
}
    
    // Metodos puente para acceder al modelo -----------------------------------
    public boolean isRunning(){ return this.model.isRunning(); }
    public void changeState(State s){ this.model.changeState(s); }
    
    
     
    /**
     * NEW : startSearchBehaviour OneShotBehaviour 
     * 
     * @brief Este comportamiento se añade al principio de la ejecución
     *        para así realizar la primera traducción y comunicación
     *        con Santa para empezar la ejecución.
     */
    class startSearchBehaviour extends OneShotBehaviour {
        
        @Override
        public void action() {
            System.out.println("===Comienza el startSearch del AgentP3===");
            
            ACLMessage msg_translation = new ACLMessage(ACLMessage.REQUEST);
            msg_translation.addReceiver(new AID("ElfTranslator", AID.ISLOCALNAME)); 
            msg_translation.setConversationId(TRASLATION_ID);
            msg_translation.setContent("Bro propuesta a comenzar ejecución "
                                               + "de rescate de renos, en plan");
            myAgent.send(msg_translation);
            System.out.println("Enviado REQUEST de traducir\n");
            
            ACLMessage response_translate = myAgent.blockingReceive();
            
            System.out.println("Recibido RESPONSE de traducir\n");
            
            try {             
                // Administración de errores
                if (!response_translate.getConversationId().equals(TRASLATION_ID)){
                    throw new FIPAException("Por alguna razón el AgentP3 recibe"
                            + " un mensaje de otro ID cuando espera la traducción " 
                            + response_translate.getContent());
                }

                if (response_translate.getPerformative() != ACLMessage.INFORM){
                    throw new FIPAException("Agente P3 no ha recibido INFORM, probablemente"
                            + " haya recibido un refuse por error en el formato del mensaje: " 
                            + response_translate.getContent());
                }
                
                // DEBUG SIMULATOR
                Thread.sleep(1000);
                
                System.out.println("\n\nMensaje verificado por agente: " + response_translate.getContent() + " procedemos a enviarselo a SANTA");
                ACLMessage msg_propose_to_santa = new ACLMessage(ACLMessage.PROPOSE);
                msg_propose_to_santa.addReceiver(new AID("Santa", AID.ISLOCALNAME)); 
                msg_propose_to_santa.setConversationId(SANTA_MSG_ID);
                msg_propose_to_santa.setContent(response_translate.getContent());
                myAgent.send(msg_propose_to_santa);
                System.out.println("Enviado PROPOSE de cosigo secreto\n");
                
                ACLMessage response_propose_to_santa = myAgent.blockingReceive();
                
                System.out.println("Recibido RESPONSE de SANTA\n");
                
                if(response_propose_to_santa.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    System.out.println("Recibido RESPONSE de SANTA ACCEPT_PROPOSAL. EMPEZANDO\n");
                } else {
                    throw new FIPAException("Recibido RESPONSE de SANTA REJECT_PROPOSAL. FINALIZANDO\n");
                }
                
                // Ahora traducimos el mensaje de santa para obtener el codigo
                msg_translation.setContent(response_propose_to_santa.getContent());
                myAgent.send(msg_translation);
                
                response_translate = myAgent.blockingReceive();
                
                // Administración de errores
                if (!response_translate.getConversationId().equals(TRASLATION_ID)){
                    throw new FIPAException("Por alguna razón el AgentP3 recibe"
                            + " un mensaje de otro ID cuando espera la traducción (2) " 
                            + response_translate.getContent());
                }

                if (response_translate.getPerformative() != ACLMessage.INFORM){
                    throw new FIPAException("Agente P3 no ha recibido INFORM, probablemente"
                            + " haya recibido un refuse por error en el formato del mensaje: (2) " 
                            + response_translate.getContent());
                }
                
                // Si todo ha ido bien el codigo
                String[] parts = response_translate.getContent().trim().split("\\s+");
                
                System.out.println("\n\nEl codigo secreto (AGENTE) es: " + parts[1]);
                
                secret_code = Integer.parseInt(parts[1]);
                
                // Ya tenemos el codigo somos felices, termina este comportamiento
                // y puede empezar la ejecucion
                
            } catch (FIPAException e){
                System.err.println("Error en la ejecución: " + e.getMessage());
                model.changeState(State.END_WORLD); // Terminamos la ejecución
            } catch (InterruptedException ex) {
                Logger.getLogger(AgentP3.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    
    
    
    
    
    
    /**
     * AUN NO HA SIDO MODIFICADO
     * COPIA DE reactiveAgentBehaviour
     */
    class reactiveAgentBehaviourP3 extends TickerBehaviour {
        private int tickCount = 0;
        
        // MAQUINA DE ESTADOS --------------------------------------------------
        private enum Mode { GREEDY, WALL_FOLLOW }
        private Mode currentMode = Mode.GREEDY;
        
        // MEMORIA WALL FOLLOW -------------------------------------------------
        private int wallToFollow = -1;      // Direccion relativa del muro (0=UP, etc)
        private boolean useLeftHand = true; // Direccion en la que sigue el muro (con la mano izda o la dcha)
        private double hitDistance = Double.MAX_VALUE; // Distancia al goal al momento del choque

        // HEURISTICAS ANTI-BUCLE ----------------------------------------------
        private String lastCollisionPos = null;
        private int collisionCountAtPos = 0;

        // DEBUGGING -----------------------------------------------------------
        private int wallStartX = -1;
        private int wallStartY = -1;

        public reactiveAgentBehaviourP3(Agent a, long period){
            super(a, period);
        }

        // FUNCIONES  DE COMUNICACION
        
        // FUNCIONES RUDOLPH
        /**
         * @brief Makes the necessary communication with Rudolph to get
         *        reindeers.
         * 
         * @returns A pair with the row and col of the next reindeer, in case
         *          there is no more reindeers, it returns (-1, -1)
         */
        private Pair<Integer, Integer> getNextReindeer() {
           
            // Preparamos el mensaje de conversación para Rudolph
            ACLMessage requestReindeer = new ACLMessage(ACLMessage.QUERY_REF);
            requestReindeer.addReceiver(new AID("Rudolph", AID.ISLOCALNAME)); 
            requestReindeer.setConversationId(RUDOLPH_REINDEER_ID);
            
            // Importante añadir cabecera el codigo
            requestReindeer.addUserDefinedParameter("SECRET_CODE", Integer.toString(secret_code));
            
            // Content es indiferente
            requestReindeer.setContent("No tengo goal, dame el proximo reno");
            
            System.out.println("\nAgentP3 manda a Rudolph mensaje de peticion de reno\n");
            
            // Mandamos mensaje
            myAgent.send(requestReindeer);
            
            // Esperamos por la respuesta de Rudolph 
            ACLMessage responseReindeer = myAgent.blockingReceive();
            
            System.out.println("\nAgentP3 recibe respuesta, en teoría por Rudolph\n");
            
            try {
                // Administración de errores
                if (!responseReindeer.getConversationId().equals(RUDOLPH_REINDEER_ID)){
                    throw new FIPAException("Por alguna razón el AgentP3 recibe"
                            + " un mensaje de otro ID cuando espera un reno respuesta " 
                            + responseReindeer.getContent());
                }

                if (responseReindeer.getPerformative() != ACLMessage.INFORM_REF 
                    || responseReindeer.getPerformative() != ACLMessage.INFORM){
                    throw new FIPAException("Agente P3 no ha recibido INFORM o INFORM_REF, probablemente"
                            + " haya recibido un refuse por error en el formato del mensaje: " 
                            + responseReindeer.getContent());
                }
                
                // DEBUG SIMULATOR
                Thread.sleep(1000);
            }
            catch (FIPAException e){
                System.err.println("Error en la ejecución: " + e.getMessage());
                model.changeState(State.END_WORLD); // Terminamos la ejecución
            } catch (InterruptedException ex) {
                Logger.getLogger(AgentP3.class.getName()).log(Level.SEVERE, null, ex);
                model.changeState(State.END_WORLD); // Terminamos la ejecución
            }
            
            // En este punto deberíamos de tener la respuesta validada, por lo que 
            // falta recoger el nuevo Pair
            
            Pair<Integer,Integer> coord = new Pair<>(-1,-1);
            
            // En caso de que sea INFORM_REF significa que hay más renos
            if (responseReindeer.getPerformative() == ACLMessage.INFORM_REF){
                try {
                    coord = (Pair<Integer,Integer>) responseReindeer.getContentObject();
                } catch (UnreadableException ex) {
                    Logger.getLogger(AgentP3.class.getName()).log(Level.SEVERE, null, ex);
                    System.err.println("\nNo se ha podido leer bien el objeto coordenada del reno\n");
                }
            }
            
            return coord;
        }
        
        
        // FUNCIONES SANTA (Quiero terminar)
        private Pair<Integer, Integer> extraerCoordenadas(String message) { // ESTA FUNCION SIRVE TAMBIEN PARA LOS RENOS
            try {
                // Regex para buscar numeros enteros
                Pattern p = Pattern.compile("-?\\d+");
                Matcher m = p.matcher(message);

                Integer r = null;
                Integer c = null;

                if (m.find()) r = Integer.valueOf(m.group()); // Primer numero encontrado
                if (m.find()) c = Integer.valueOf(m.group()); // Segundo numero encontrado

                if (r != null && c != null) {
                    return new Pair<>(r, c);
                }
            } catch (Exception e) {
                System.err.println("Error parseando coordenadas de [SANTA]: " + e.getMessage());
            }
            return null;
        }
        
        private Pair<Integer, Integer> pedirCoordenadasSanta() {
            System.out.println("=== Preguntando coordenadas a Santa ===");
            
            // Le enviamos el mensaje al ElfoTraductor
            ACLMessage msgElf = new ACLMessage(ACLMessage.REQUEST);
            msgElf.addReceiver(new AID("ElfTranslator", AID.ISLOCALNAME));
            msgElf.setContent("Bro coordenadas en plan");
            
            myAgent.send(msgElf);
            
            ACLMessage translation = myAgent.blockingReceive();
            
            // Le enviamos el mensaje al Santa
            ACLMessage msgSanta = new ACLMessage(ACLMessage.QUERY_REF);
            msgSanta.addReceiver(new AID("Santa", AID.ISLOCALNAME));
            msgSanta.setContent(translation.getContent());
            msgSanta.addUserDefinedParameter("SECRET_CODE", Integer.toString(secret_code)); 
            
            myAgent.send(msgSanta);
            
            ACLMessage reply = myAgent.blockingReceive();
            
            if (reply != null) {
                // Le enviamos el mensaje al ElfoTraductor
                msgElf.addReceiver(new AID("ElfTranslator", AID.ISLOCALNAME));
                msgElf.setContent(reply.getContent());

                myAgent.send(msgElf);

                ACLMessage translated = myAgent.blockingReceive();
     
                System.out.println("=== Respuesta de [SANTA] para las [COORDENADAS] recibida: " + translated.getContent());
                // 4. Parsear las coordenadas del mensaje "Bro (x,y) en plan"
                // Buscamos cualquier secuencia de digitos
                return extraerCoordenadas(translated.getContent());
            }   
            
            return null; 
        }
        
        private void confirmarLlegadaSanta() {
            System.out.println("=== Confirmando llegada a [SANTA] ===");

            // 1. Preparar mensaje
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID("ElfTranslator", AID.ISLOCALNAME));
            msg.setContent("Bro llegado en plan");
            msg.addUserDefinedParameter("SECRET_CODE", Integer.toString(secret_code));

            // 2. Enviar
            myAgent.send(msg);

            // 3. Espera bloqueante de confirmacion 
            ACLMessage reply = myAgent.blockingReceive();

            if (reply != null) {
                System.out.println("=== [SANTA] confirma recepcion: " + reply.getContent());
            }
        }
        
        
        @Override
        public void onTick(){
            boolean isRunning = ((AgentP3) myAgent).isRunning();
            
            // DETECCIÓN DE REINICIO (Flanco de subida)
            // Si ahora estamos corriendo, pero en el ultimo tick estábamos parados,
            // significa que el usuario le ha dado a "RUN" de nuevo (posiblemente mapa nuevo).
            if (model.isMapChanged()) {
                resetMemory();
                model.setMapChanged(false);
            }

            // Si no está corriendo, salimos
            if (!isRunning) return;
            
            
            tickCount++;
            
            // -----------------------------------------------------------------
            // 1. PERCEPCION
            // -----------------------------------------------------------------
            ((AgentP3) myAgent).changeState(State.SEE);
            
            Map<RelativePos,Integer> perception = model.perceive(sensors);
            int[] goalDir = model.getDirectionToGoal(); 
            double currentDist = Math.sqrt(Math.pow(goalDir[0], 2) + Math.pow(goalDir[1], 2));
            
            // DEBUG
            printPerceptionDebug(perception, goalDir, currentDist);

            // -----------------------------------------------------------------
            // 2. PENSAMIENTO (DECISION)
            // -----------------------------------------------------------------
            ((AgentP3) myAgent).changeState(State.THINK);
            
            String decision = "BLOCKED";
            String logMessage = ""; // Variable para traza de depuracion

            // A. Verificar si hemos llegado
            if (model.isGoalReached()) {
                System.out.println("!!! OBJETIVO ALCANZADO !!!");
                ((AgentP3) myAgent).changeState(State.STANDBY);
                model.stop();
                return;
            }

            // B. Maquina de Estados (GREEDY vs WALL_FOLLOW)
            switch (currentMode) {
                case GREEDY:
                    // Intento de movimiento directo hacia el objetivo
                    String directMove = getDirectMove(goalDir, perception);
                    
                    if (directMove != null) {
                        // CAMINO LIBRE ----------------------------------------
                        decision = directMove;
                        logMessage = "GREEDY";
                        System.out.println("  [GREEDY] Movimiento directo posible: " + directMove);
                    } else {
                        // CHOQUE DETECTADO (Inicio de Wall Follow) ------------
                        currentMode = Mode.WALL_FOLLOW;
                        hitDistance = currentDist;
                        
                        // Guardar datos para debug
                        wallStartX = model.getAgentR();
                        wallStartY = model.getAgentC();

                        // 1. Identificar donde esta el obstaculo
                        wallToFollow = getBlockedDirection(goalDir);

                        // 2. Heuristica: Decidir mano inicial (Izquierda o Derecha)
                        useLeftHand = decideHand(wallToFollow, goalDir);

                        // 3. Heuristica: Anti-Bucle (Detectar colisiones repetidas en el mismo punto)
                        boolean antiLoopTriggered = checkAntiLoopHeuristic(currentDist);

                        // Registro de log
                        logMessage = "HIT -> WALL_FOLLOW (" + (useLeftHand ? "L" : "R") + ")";
                        if (antiLoopTriggered) logMessage += " [ANTI-LOOP]";

                        // 4. Obtener primer movimiento de rodeo
                        decision = followWallStrict(perception);
                        
                        // 5. Heuristica: Anti-Reverse 
                        // Si la decision de pared nos hace dar la vuelta (180º) pero podiamos seguir recto:
                        if (lastMove != null && isOpposite(decision, lastMove)) {
                            int lastMoveIdx = getDirIndex(lastMove);
                            if (isFree(lastMoveIdx, perception)) {
                                System.out.println("[ANTI-REVERSE] Evitando giro 180º. Forzando inercia: " + lastMove);
                                decision = lastMove;
                                useLeftHand = !useLeftHand; // Invertir mano para alinear logica en siguiente tick
                                logMessage += " [HAND-SWAP FORCED]";
                            }
                        }
                        
                        // Log detallado del cambio de modo
                        printHitDebug(decision, antiLoopTriggered);
                    }
                    break;

                case WALL_FOLLOW:
                    // Verificar condicion de salida (Bug Algorithm Logic)
                    String tryDirect = getDirectMove(goalDir, perception);
                    double EPSILON = 0.1; 
                    
                    // Condicion de salida: 
                    // 1. Existe camino directo
                    // 2. Es compatible con inercia (no es giro 180º)
                    // 3. Estamos mas cerca del objetivo que cuando chocamos (Progreso real).
                    boolean isCloser = currentDist < (hitDistance - EPSILON);

                    if (tryDirect != null && isCompatibleWithLastMove(tryDirect) && isCloser) {
                        // SALIDA DE PARED -------------------------------------
                        currentMode = Mode.GREEDY;
                        decision = tryDirect;
                        logMessage = "LEAVE WALL -> GREEDY (dir=" + tryDirect + ") [Dist: " + String.format("%.2f", currentDist) + " < Hit: " + String.format("%.2f", hitDistance) + "]";
                        System.out.println("  [WALL] ✅ Salida valida: Camino libre Y mejor distancia.");
                    } else {
                        // CONTINUAR PARED -------------------------------------
                        if (tryDirect != null && !isCloser) {
                            System.out.println("  [WALL] Camino directo visto (" + tryDirect + ") pero ignorado: Distancia actual >= HitDistance");
                        }

                        decision = followWallStrict(perception);
                        logMessage = "  WALL (" + (useLeftHand ? "L" : "R") + ")";
                    }
                    break;
            }

            // Imprimir resumen de decision
            printDecisionLog(logMessage, currentDist);

            // =================================================================
            // 3. ACCION
            // =================================================================
            if (!"BLOCKED".equals(decision)) {
                int oldX = model.getAgentR();
                int oldY = model.getAgentC();
                
                model.moveAgent(decision);
                
                // Verificacion de efectividad del movimiento
                if (oldX != model.getAgentR() || oldY != model.getAgentC()) {
                    model.incrementEnergy();
                    lastMove = decision;
                    System.out.println("  ✅ Movimiento ejecutado: " + decision + " -> nueva pos: (" + model.getAgentR() + "," + model.getAgentC() + ")");
                } else {
                    System.out.println("  Movimiento NO efectivo (bloqueado tras moveAgent): " + decision);
                }
            } else {
                System.out.println("  Decision = BLOCKED");
            }
            
            ((AgentP3) myAgent).changeState(State.ACTION);
        }

        // =========================================================================
        // LOGICA DE ALGORITMOS Y HELPERS
        // =========================================================================
        
         // Gestiona la logica Anti-Loop: cuenta colisiones en el mismo punto
         // Si se repiten y no hay mejora global de distancia, cambia la mano
        private boolean checkAntiLoopHeuristic(double currentDist) {
            String currentPos = model.getAgentR() + "," + model.getAgentC();
            
            if (currentPos.equals(lastCollisionPos)) {
                collisionCountAtPos++;
            } else {
                lastCollisionPos = currentPos;
                collisionCountAtPos = 1;
            }

            // Si ya chocamos 2+ veces EN ESTA MISMA CASILLA -> romper simetria
            if (collisionCountAtPos >= 2) {
                useLeftHand = !useLeftHand; // Cambiar mano
                return true;
            }

            return false;
        }
        
        private boolean decideHand(int blockedDir, int[] goalDir) {
            int dY = goalDir[0]; 
            int dX = goalDir[1]; 

            switch (blockedDir) {
                case 0: // Bloqueado ARRIBA
                    // LHR lleva a Derecha. RHR lleva a Izquierda.
                    // Si el objetivo esta a la derecha (dX > 0), quiero LHR.
                    return dX >= 0;

                case 1: // Bloqueado DERECHA
                    // LHR lleva a Abajo. RHR lleva a Arriba.
                    // Si el objetivo esta Abajo (dY > 0), quiero LHR.
                    return dY > 0;

                case 2: // Bloqueado ABAJO
                    // LHR lleva a Izquierda. RHR lleva a Derecha.
                    // Si el objetivo esta a la Izquierda (dX < 0), quiero LHR.
                    return dX <= 0;

                case 3: // Bloqueado IZQUIERDA
                    // LHR lleva a Arriba. RHR lleva a Abajo.
                    // Si el objetivo esta Arriba (dY < 0), quiero LHR.
                    return dY < 0;
            }

            return true;
        }

        private String followWallStrict(Map<RelativePos,Integer> perception) {
            String[] dirs = {"UP", "RIGHT", "DOWN", "LEFT"};
            int turnOut = useLeftHand ? 1 : 3; // (-1 mod 4) Girar hacia afuera
            int turnIn  = useLeftHand ? 3 : 1; // (+1 mod 4) Girar hacia adentro

            // 1. REGLA DEL HUECO (Esquina Convexa): Intentar girar hacia la pared
            if (isFree(wallToFollow, perception)) {
                String move = dirs[wallToFollow];
                wallToFollow = (wallToFollow + turnIn) % 4; // Recalibrar muro
                return move;
            }
            
            // 2. REGLA DE AVANCE (Pared recta): Intentar seguir recto
            int moveDir = (wallToFollow + turnOut) % 4;
            if (isFree(moveDir, perception)) {
                return dirs[moveDir];
            }
            
            // 3. REGLA DEL CALLEJON (Esquina Concava): Girar sobre si mismo
            wallToFollow = moveDir; // El muro ahora esta al frente
            return "BLOCKED";
        }

        private String getDirectMove(int[] goalDir, Map<RelativePos, Integer> perception) {
            int dY = goalDir[0];
            int dX = goalDir[1];
            List<String> candidates = new ArrayList<>();

            if (dY < 0 && isFree(0, perception)) candidates.add("UP");
            if (dY > 0 && isFree(2, perception)) candidates.add("DOWN");
            if (dX < 0 && isFree(3, perception)) candidates.add("LEFT");
            if (dX > 0 && isFree(1, perception)) candidates.add("RIGHT");

            if (candidates.isEmpty()) return null;
            if (candidates.size() == 1) return candidates.get(0);

            // Decision puramente matematica para reducir el eje mayor
            String choice;
            if (Math.abs(dY) >= Math.abs(dX)) {
                // Prioridad Vertical
                if (dY < 0) choice = "UP";
                else if (dY > 0) choice = "DOWN";
                else if (dX < 0) choice = "LEFT"; 
                else choice = "RIGHT";
            } else {
                // Prioridad Horizontal
                if (dX < 0) choice = "LEFT";
                else if (dX > 0) choice = "RIGHT";
                else if (dY < 0) choice = "UP";
                else choice = "DOWN";
            }
            
            // Validar disponibilidad
            if (candidates.contains(choice)) {
                return choice;
            } else {
                return candidates.get(0);
            }
        }
        
        // --- HELPERS BASICOS ---

        private boolean isFree(int dirIndex, Map<RelativePos,Integer> perception) {
            return perception.get(sensors.get(dirIndex)) == 0;
        }
        
        private int getBlockedDirection(int[] goalDir) {
            int dY = goalDir[0]; int dX = goalDir[1];
            // Retorna 0, 1, 2 o 3
            return (Math.abs(dY) > Math.abs(dX)) ? (dY < 0 ? 0 : 2) : (dX > 0 ? 1 : 3);
        }
        
        private boolean isCompatibleWithLastMove(String move) {
            if (lastMove == null) return true;
            return !isOpposite(move, lastMove);
        }

        private boolean isOpposite(String dir1, String dir2) {
            if (dir1 == null || dir2 == null) return false;
            if (dir1.equals("UP") && dir2.equals("DOWN")) return true;
            if (dir1.equals("DOWN") && dir2.equals("UP")) return true;
            if (dir1.equals("LEFT") && dir2.equals("RIGHT")) return true;
            if (dir1.equals("RIGHT") && dir2.equals("LEFT")) return true;
            return false;
        }

        private int getDirIndex(String dir) {
            switch (dir) {
                case "UP": return 0;
                case "RIGHT": return 1;
                case "DOWN": return 2;
                case "LEFT": return 3;
                default: return -1;
            }
        }
        
        private String dirName(int idx) {
            String[] names = {"UP", "RIGHT", "DOWN", "LEFT"};
            return (idx >= 0 && idx < 4) ? names[idx] : "???";
        }

        // ---------------------------------------------------------------------
        // SECCION DE DEPURACION (MWTODOS EXTRAIDOS)
        // ---------------------------------------------------------------------

        // Imprime el bloque grande de informacion al inicio de cada Tick
        private void printPerceptionDebug(Map<RelativePos,Integer> perception, int[] goalDir, double currentDist) {
            System.out.println("\n[TICK " + tickCount + "]");
            System.out.println("  Pos: (" + model.getAgentR() + "," + model.getAgentC() + ")");
            System.out.println("  Goal: (" + model.getGoalR() + "," + model.getGoalC() + ")");
            System.out.println("  Distancia al objetivo: " + String.format("%.3f", currentDist));
            System.out.println("  Direccion al objetivo (dY, dX): (" + goalDir[0] + ", " + goalDir[1] + ")");
            System.out.println("  Percepcion (U,R,D,L): [" +
                perception.get(sensors.get(0)) + ", " +
                perception.get(sensors.get(1)) + ", " +
                perception.get(sensors.get(2)) + ", " +
                perception.get(sensors.get(3)) + "]");
            System.out.println("  Ultimo movimiento efectivo: " + lastMove);
            System.out.println("  Modo actual: " + currentMode);
            
            if (currentMode == Mode.WALL_FOLLOW) {
                System.out.println("    -> wallToFollow: " + wallToFollow + " (" + dirName(wallToFollow) + ")");
                System.out.println("    -> useLeftHand: " + useLeftHand);
                System.out.println("    -> hitDistance (al chocar): " + String.format("%.3f", hitDistance));
                if (wallStartX != -1) {
                    System.out.println("    -> Pared iniciada en: (" + wallStartX + "," + wallStartY + ")");
                }
            }
        }

        // Imprime informacion especifica cuando ocurre un choque y entra en modo pared
        private void printHitDebug(String decision, boolean antiLoopTriggered) {
            System.out.println("  [GREEDY] ❌ Bloqueado -> cambio a WALL_FOLLOW");
            System.out.println("    -> wallToFollow = " + wallToFollow + " (" + dirName(wallToFollow) + ")");
            System.out.println("    -> useLeftHand = " + useLeftHand + (antiLoopTriggered ? " [CAMBIADO por anti-loop]" : ""));
            System.out.println("    -> Decision inicial de pared: " + decision);
        }

        // Imprime la linea de resumen de decision al final de la fase de pensamiento
        private void printDecisionLog(String logMessage, double currentDist) {
            System.out.println("  -> Log: [" + tickCount + "] " + logMessage + " | Dist: " + String.format("%.2f", currentDist));
        }
        
        // Reinicia todas las variables de estado y heuristicas
        private void resetMemory() {
            System.out.println("--- DETECTADO CAMBIO DE ENTORNO: RESETEANDO MEMORIA AGENTE P2 (dont concern comm)---");
            
            // 1. Reiniciar contadores visuales
            this.tickCount = 0;
            
            // 2. Reiniciar Maquina de Estados
            this.currentMode = Mode.GREEDY;
            
            // 3. Reiniciar Memoria Wall Follow
            this.wallToFollow = -1;
            this.useLeftHand = true;
            this.hitDistance = Double.MAX_VALUE; 
            
            // 4. Reiniciar Heuristicas Anti-Bucle
            this.lastCollisionPos = null;
            this.collisionCountAtPos = 0;
            
            // 5. Reiniciar Debugging
            this.wallStartX = -1;
            this.wallStartY = -1;
            
            // 6. Reiniciar variable de la clase externa (AgentP2)
            AgentP3.this.lastMove = null; 
        }        
    }
    
    
    //--------------------------------------------------------------------------
    // Comportamiento ciclico del agente
    // UNTOUCHED BY P3
    // Estructura: PERCEPCION -> PENSAMIENTO  -> ACCION
    class reactiveAgentBehaviour extends TickerBehaviour {
        
        private int tickCount = 0;
        
        // MAQUINA DE ESTADOS --------------------------------------------------
        private enum Mode { GREEDY, WALL_FOLLOW }
        private Mode currentMode = Mode.GREEDY;
        
        // MEMORIA WALL FOLLOW -------------------------------------------------
        private int wallToFollow = -1;      // Direccion relativa del muro (0=UP, etc)
        private boolean useLeftHand = true; // Direccion en la que sigue el muro (con la mano izda o la dcha)
        private double hitDistance = Double.MAX_VALUE; // Distancia al goal al momento del choque

        // HEURISTICAS ANTI-BUCLE ----------------------------------------------
        private String lastCollisionPos = null;
        private int collisionCountAtPos = 0;

        // DEBUGGING -----------------------------------------------------------
        private int wallStartX = -1;
        private int wallStartY = -1;

        public reactiveAgentBehaviour(Agent a, long period){
            super(a, period);
        }
        
        @Override
        public void onTick(){
            boolean isRunning = ((AgentP3) myAgent).isRunning();
            
            // DETECCIÓN DE REINICIO (Flanco de subida)
            // Si ahora estamos corriendo, pero en el ultimo tick estábamos parados,
            // significa que el usuario le ha dado a "RUN" de nuevo (posiblemente mapa nuevo).
            if (model.isMapChanged()) {
                resetMemory();
                model.setMapChanged(false);
            }

            // Si no está corriendo, salimos
            if (!isRunning) return;
            
            
            tickCount++;
            
            // -----------------------------------------------------------------
            // 1. PERCEPCION
            // -----------------------------------------------------------------
            ((AgentP3) myAgent).changeState(State.SEE);
            
            Map<RelativePos,Integer> perception = model.perceive(sensors);
            int[] goalDir = model.getDirectionToGoal(); 
            double currentDist = Math.sqrt(Math.pow(goalDir[0], 2) + Math.pow(goalDir[1], 2));
            
            // DEBUG
            printPerceptionDebug(perception, goalDir, currentDist);

            // -----------------------------------------------------------------
            // 2. PENSAMIENTO (DECISION)
            // -----------------------------------------------------------------
            ((AgentP3) myAgent).changeState(State.THINK);
            
            String decision = "BLOCKED";
            String logMessage = ""; // Variable para traza de depuracion

            // A. Verificar si hemos llegado
            if (model.isGoalReached()) {
                System.out.println("!!! OBJETIVO ALCANZADO !!!");
                ((AgentP3) myAgent).changeState(State.STANDBY);
                model.stop();
                return;
            }

            // B. Maquina de Estados (GREEDY vs WALL_FOLLOW)
            switch (currentMode) {
                case GREEDY:
                    // Intento de movimiento directo hacia el objetivo
                    String directMove = getDirectMove(goalDir, perception);
                    
                    if (directMove != null) {
                        // CAMINO LIBRE ----------------------------------------
                        decision = directMove;
                        logMessage = "GREEDY";
                        System.out.println("  [GREEDY] Movimiento directo posible: " + directMove);
                    } else {
                        // CHOQUE DETECTADO (Inicio de Wall Follow) ------------
                        currentMode = Mode.WALL_FOLLOW;
                        hitDistance = currentDist;
                        
                        // Guardar datos para debug
                        wallStartX = model.getAgentR();
                        wallStartY = model.getAgentC();

                        // 1. Identificar donde esta el obstaculo
                        wallToFollow = getBlockedDirection(goalDir);

                        // 2. Heuristica: Decidir mano inicial (Izquierda o Derecha)
                        useLeftHand = decideHand(wallToFollow, goalDir);

                        // 3. Heuristica: Anti-Bucle (Detectar colisiones repetidas en el mismo punto)
                        boolean antiLoopTriggered = checkAntiLoopHeuristic(currentDist);

                        // Registro de log
                        logMessage = "HIT -> WALL_FOLLOW (" + (useLeftHand ? "L" : "R") + ")";
                        if (antiLoopTriggered) logMessage += " [ANTI-LOOP]";

                        // 4. Obtener primer movimiento de rodeo
                        decision = followWallStrict(perception);
                        
                        // 5. Heuristica: Anti-Reverse 
                        // Si la decision de pared nos hace dar la vuelta (180º) pero podiamos seguir recto:
                        if (lastMove != null && isOpposite(decision, lastMove)) {
                            int lastMoveIdx = getDirIndex(lastMove);
                            if (isFree(lastMoveIdx, perception)) {
                                System.out.println("[ANTI-REVERSE] Evitando giro 180º. Forzando inercia: " + lastMove);
                                decision = lastMove;
                                useLeftHand = !useLeftHand; // Invertir mano para alinear logica en siguiente tick
                                logMessage += " [HAND-SWAP FORCED]";
                            }
                        }
                        
                        // Log detallado del cambio de modo
                        printHitDebug(decision, antiLoopTriggered);
                    }
                    break;

                case WALL_FOLLOW:
                    // Verificar condicion de salida (Bug Algorithm Logic)
                    String tryDirect = getDirectMove(goalDir, perception);
                    double EPSILON = 0.1; 
                    
                    // Condicion de salida: 
                    // 1. Existe camino directo
                    // 2. Es compatible con inercia (no es giro 180º)
                    // 3. Estamos mas cerca del objetivo que cuando chocamos (Progreso real).
                    boolean isCloser = currentDist < (hitDistance - EPSILON);

                    if (tryDirect != null && isCompatibleWithLastMove(tryDirect) && isCloser) {
                        // SALIDA DE PARED -------------------------------------
                        currentMode = Mode.GREEDY;
                        decision = tryDirect;
                        logMessage = "LEAVE WALL -> GREEDY (dir=" + tryDirect + ") [Dist: " + String.format("%.2f", currentDist) + " < Hit: " + String.format("%.2f", hitDistance) + "]";
                        System.out.println("  [WALL] ✅ Salida valida: Camino libre Y mejor distancia.");
                    } else {
                        // CONTINUAR PARED -------------------------------------
                        if (tryDirect != null && !isCloser) {
                            System.out.println("  [WALL] Camino directo visto (" + tryDirect + ") pero ignorado: Distancia actual >= HitDistance");
                        }

                        decision = followWallStrict(perception);
                        logMessage = "  WALL (" + (useLeftHand ? "L" : "R") + ")";
                    }
                    break;
            }

            // Imprimir resumen de decision
            printDecisionLog(logMessage, currentDist);

            // =================================================================
            // 3. ACCION
            // =================================================================
            if (!"BLOCKED".equals(decision)) {
                int oldX = model.getAgentR();
                int oldY = model.getAgentC();
                
                model.moveAgent(decision);
                
                // Verificacion de efectividad del movimiento
                if (oldX != model.getAgentR() || oldY != model.getAgentC()) {
                    model.incrementEnergy();
                    lastMove = decision;
                    System.out.println("  ✅ Movimiento ejecutado: " + decision + " -> nueva pos: (" + model.getAgentR() + "," + model.getAgentC() + ")");
                } else {
                    System.out.println("  Movimiento NO efectivo (bloqueado tras moveAgent): " + decision);
                }
            } else {
                System.out.println("  Decision = BLOCKED");
            }
            
            ((AgentP3) myAgent).changeState(State.ACTION);
        }

        // =========================================================================
        // LOGICA DE ALGORITMOS Y HELPERS
        // =========================================================================
        
         // Gestiona la logica Anti-Loop: cuenta colisiones en el mismo punto
         // Si se repiten y no hay mejora global de distancia, cambia la mano
        private boolean checkAntiLoopHeuristic(double currentDist) {
            String currentPos = model.getAgentR() + "," + model.getAgentC();
            
            if (currentPos.equals(lastCollisionPos)) {
                collisionCountAtPos++;
            } else {
                lastCollisionPos = currentPos;
                collisionCountAtPos = 1;
            }

            // Si ya chocamos 2+ veces EN ESTA MISMA CASILLA -> romper simetria
            if (collisionCountAtPos >= 2) {
                useLeftHand = !useLeftHand; // Cambiar mano
                return true;
            }

            return false;
        }
        
        private boolean decideHand(int blockedDir, int[] goalDir) {
            int dY = goalDir[0]; 
            int dX = goalDir[1]; 

            switch (blockedDir) {
                case 0: // Bloqueado ARRIBA
                    // LHR lleva a Derecha. RHR lleva a Izquierda.
                    // Si el objetivo esta a la derecha (dX > 0), quiero LHR.
                    return dX >= 0;

                case 1: // Bloqueado DERECHA
                    // LHR lleva a Abajo. RHR lleva a Arriba.
                    // Si el objetivo esta Abajo (dY > 0), quiero LHR.
                    return dY > 0;

                case 2: // Bloqueado ABAJO
                    // LHR lleva a Izquierda. RHR lleva a Derecha.
                    // Si el objetivo esta a la Izquierda (dX < 0), quiero LHR.
                    return dX <= 0;

                case 3: // Bloqueado IZQUIERDA
                    // LHR lleva a Arriba. RHR lleva a Abajo.
                    // Si el objetivo esta Arriba (dY < 0), quiero LHR.
                    return dY < 0;
            }

            return true;
        }

        private String followWallStrict(Map<RelativePos,Integer> perception) {
            String[] dirs = {"UP", "RIGHT", "DOWN", "LEFT"};
            int turnOut = useLeftHand ? 1 : 3; // (-1 mod 4) Girar hacia afuera
            int turnIn  = useLeftHand ? 3 : 1; // (+1 mod 4) Girar hacia adentro

            // 1. REGLA DEL HUECO (Esquina Convexa): Intentar girar hacia la pared
            if (isFree(wallToFollow, perception)) {
                String move = dirs[wallToFollow];
                wallToFollow = (wallToFollow + turnIn) % 4; // Recalibrar muro
                return move;
            }
            
            // 2. REGLA DE AVANCE (Pared recta): Intentar seguir recto
            int moveDir = (wallToFollow + turnOut) % 4;
            if (isFree(moveDir, perception)) {
                return dirs[moveDir];
            }
            
            // 3. REGLA DEL CALLEJON (Esquina Concava): Girar sobre si mismo
            wallToFollow = moveDir; // El muro ahora esta al frente
            return "BLOCKED";
        }

        private String getDirectMove(int[] goalDir, Map<RelativePos, Integer> perception) {
            int dY = goalDir[0];
            int dX = goalDir[1];
            List<String> candidates = new ArrayList<>();

            if (dY < 0 && isFree(0, perception)) candidates.add("UP");
            if (dY > 0 && isFree(2, perception)) candidates.add("DOWN");
            if (dX < 0 && isFree(3, perception)) candidates.add("LEFT");
            if (dX > 0 && isFree(1, perception)) candidates.add("RIGHT");

            if (candidates.isEmpty()) return null;
            if (candidates.size() == 1) return candidates.get(0);

            // Decision puramente matematica para reducir el eje mayor
            String choice;
            if (Math.abs(dY) >= Math.abs(dX)) {
                // Prioridad Vertical
                if (dY < 0) choice = "UP";
                else if (dY > 0) choice = "DOWN";
                else if (dX < 0) choice = "LEFT"; 
                else choice = "RIGHT";
            } else {
                // Prioridad Horizontal
                if (dX < 0) choice = "LEFT";
                else if (dX > 0) choice = "RIGHT";
                else if (dY < 0) choice = "UP";
                else choice = "DOWN";
            }
            
            // Validar disponibilidad
            if (candidates.contains(choice)) {
                return choice;
            } else {
                return candidates.get(0);
            }
        }
        
        // --- HELPERS BASICOS ---

        private boolean isFree(int dirIndex, Map<RelativePos,Integer> perception) {
            return perception.get(sensors.get(dirIndex)) == 0;
        }
        
        private int getBlockedDirection(int[] goalDir) {
            int dY = goalDir[0]; int dX = goalDir[1];
            // Retorna 0, 1, 2 o 3
            return (Math.abs(dY) > Math.abs(dX)) ? (dY < 0 ? 0 : 2) : (dX > 0 ? 1 : 3);
        }
        
        private boolean isCompatibleWithLastMove(String move) {
            if (lastMove == null) return true;
            return !isOpposite(move, lastMove);
        }

        private boolean isOpposite(String dir1, String dir2) {
            if (dir1 == null || dir2 == null) return false;
            if (dir1.equals("UP") && dir2.equals("DOWN")) return true;
            if (dir1.equals("DOWN") && dir2.equals("UP")) return true;
            if (dir1.equals("LEFT") && dir2.equals("RIGHT")) return true;
            if (dir1.equals("RIGHT") && dir2.equals("LEFT")) return true;
            return false;
        }

        private int getDirIndex(String dir) {
            switch (dir) {
                case "UP": return 0;
                case "RIGHT": return 1;
                case "DOWN": return 2;
                case "LEFT": return 3;
                default: return -1;
            }
        }
        
        private String dirName(int idx) {
            String[] names = {"UP", "RIGHT", "DOWN", "LEFT"};
            return (idx >= 0 && idx < 4) ? names[idx] : "???";
        }

        // ---------------------------------------------------------------------
        // SECCION DE DEPURACION (MWTODOS EXTRAIDOS)
        // ---------------------------------------------------------------------

        // Imprime el bloque grande de informacion al inicio de cada Tick
        private void printPerceptionDebug(Map<RelativePos,Integer> perception, int[] goalDir, double currentDist) {
            System.out.println("\n[TICK " + tickCount + "]");
            System.out.println("  Pos: (" + model.getAgentR() + "," + model.getAgentC() + ")");
            System.out.println("  Goal: (" + model.getGoalR() + "," + model.getGoalC() + ")");
            System.out.println("  Distancia al objetivo: " + String.format("%.3f", currentDist));
            System.out.println("  Direccion al objetivo (dY, dX): (" + goalDir[0] + ", " + goalDir[1] + ")");
            System.out.println("  Percepcion (U,R,D,L): [" +
                perception.get(sensors.get(0)) + ", " +
                perception.get(sensors.get(1)) + ", " +
                perception.get(sensors.get(2)) + ", " +
                perception.get(sensors.get(3)) + "]");
            System.out.println("  Ultimo movimiento efectivo: " + lastMove);
            System.out.println("  Modo actual: " + currentMode);
            
            if (currentMode == Mode.WALL_FOLLOW) {
                System.out.println("    -> wallToFollow: " + wallToFollow + " (" + dirName(wallToFollow) + ")");
                System.out.println("    -> useLeftHand: " + useLeftHand);
                System.out.println("    -> hitDistance (al chocar): " + String.format("%.3f", hitDistance));
                if (wallStartX != -1) {
                    System.out.println("    -> Pared iniciada en: (" + wallStartX + "," + wallStartY + ")");
                }
            }
        }

        // Imprime informacion especifica cuando ocurre un choque y entra en modo pared
        private void printHitDebug(String decision, boolean antiLoopTriggered) {
            System.out.println("  [GREEDY] ❌ Bloqueado -> cambio a WALL_FOLLOW");
            System.out.println("    -> wallToFollow = " + wallToFollow + " (" + dirName(wallToFollow) + ")");
            System.out.println("    -> useLeftHand = " + useLeftHand + (antiLoopTriggered ? " [CAMBIADO por anti-loop]" : ""));
            System.out.println("    -> Decision inicial de pared: " + decision);
        }

        // Imprime la linea de resumen de decision al final de la fase de pensamiento
        private void printDecisionLog(String logMessage, double currentDist) {
            System.out.println("  -> Log: [" + tickCount + "] " + logMessage + " | Dist: " + String.format("%.2f", currentDist));
        }
        
        // Reinicia todas las variables de estado y heuristicas
        private void resetMemory() {
            System.out.println("--- DETECTADO CAMBIO DE ENTORNO: RESETEANDO MEMORIA AGENTE ---");
            
            // 1. Reiniciar contadores visuales
            this.tickCount = 0;
            
            // 2. Reiniciar Maquina de Estados
            this.currentMode = Mode.GREEDY;
            
            // 3. Reiniciar Memoria Wall Follow
            this.wallToFollow = -1;
            this.useLeftHand = true;
            this.hitDistance = Double.MAX_VALUE; 
            
            // 4. Reiniciar Heuristicas Anti-Bucle
            this.lastCollisionPos = null;
            this.collisionCountAtPos = 0;
            
            // 5. Reiniciar Debugging
            this.wallStartX = -1;
            this.wallStartY = -1;
            
            // 6. Reiniciar variable de la clase externa (AgentP2)
            AgentP3.this.lastMove = null; 
        }
    }
}
//

/* !!! DEPRECATED
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
    private final int ACTION_TIME = 1;
    // private DataStore sharedDataStore; // DataStore compartido
    
    
    // Representation of the sensors of this agent NORTH, EAST, SOUTH, WEST
    //                                             UP,  ,RIGHT, DOWN, LEFT
    private final ArrayList<RelativePos> sensors = new ArrayList<>(List.of(
                                                    new RelativePos( -1,0),
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
        
        addBehaviour(new reactiveAgentBehaviour(this,ACTION_TIME));
        
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
    
    
    
    class reactiveAgentBehaviour extends TickerBehaviour {
        private int tickCount = 0;
        
        public reactiveAgentBehaviour(Agent a, long period){
            super(a, period);
        }
        
        
        //-BEHAVIOUR------------------------------------------------------------
        @Override
        public void onTick(){
            tickCount++;
            
            // If its stopped
            if (!((AgentP2) myAgent).isRunning()){
                return;
            }
            
            //-SEE--------------------------------------------------------------
            // The controller will detect the change to edit the UI
            ((AgentP2) myAgent).changeState(State.SEE);
            
            
            // Method percieve -> Stores the info in the Enviroment
            Map<RelativePos,Integer> perception = model.perceive(sensors);
            int [] goalDirection = model.getDirectionToGoal();
            
            
            //-THINK------------------------------------------------------------
            // The controller will detect the change to edit the UI
            ((AgentP2) myAgent).changeState(State.THINK);
            
            
            String decision = "";
            // Comprobar si ya llegamos al objetivo
            if (model.isGoalReached()) {
                System.out.println("[" + tickCount + "] THINK - OBJETIVO ALCANZADO!");
                System.out.println("[" + tickCount + "] ACTION - Agente detenido - Objetivo alcanzado");
                System.out.println(">>> Energia total consumida: " + model.getEnergyUsed());
                ((AgentP2) myAgent).changeState(State.STANDBY);
                model.stop();
            }
            else{
                // Consultar memoria de celdas visitadas
                boolean[] visited = model.getVisitedDirections();
                int[] visitCounts = model.getVisitCounts();

                 // ESTRATEGIA CON PRIORIDADES
                decision = decideMovementWithMemory(perception, goalDirection, visited, visitCounts);
                System.out.println("[" + tickCount + "] THINK - Decision final: " + decision);
            }
            
            
            //-ACTION-----------------------------------------------------------
            if ("BLOCKED".equals(decision)) {
                System.out.println("[" + tickCount + "] ACTION - BLOQUEADO - No hay movimientos validos!");
                ((AgentP2) myAgent).changeState(State.STANDBY);
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
            
            
            
            if (model.isGoalReached()){
            }
            
            // The controller will detect the change to edit the UI
            ((AgentP2) myAgent).changeState(State.ACTION);

        }
        
        
        
        //-HEURISTICA-----------------------------------------------------------
        // Decide el mejor movimiento considerando:
        // 1. Dirección del objetivo
        // 2. Celdas visitadas (evitarlas si es posible)
        // 3. Backtracking inteligente si es necesario

        private String decideMovementWithMemory(Map<RelativePos,Integer> perception, int[] goalDirection, 
                                               boolean[] visited, int[] visitCounts) {

            // PRIORIDAD 1: Intentar ir directo al objetivo por celda NO visitada
            String directMove = tryDirectUnvisitedMove(perception, goalDirection, visited);
            if (directMove != null) {
                System.out.println("   -> Estrategia: Movimiento directo no visitado");
                return directMove;
            }

            // PRIORIDAD 2: Explorar celdas NO visitadas (aunque no sea directo al objetivo)
            String unexploredMove = tryUnexploredMove(perception, visited);
            if (unexploredMove != null) {
                System.out.println("   -> Estrategia: Exploracion de celda nueva");
                return unexploredMove;
            }

            // PRIORIDAD 3: Backtracking - ir a la celda menos visitada
            String backtrackMove = tryBacktracking(perception, visited, visitCounts);
            if (backtrackMove != null) {
                System.out.println("   -> Estrategia: Backtracking (celda menos visitada)");
                return backtrackMove;
            }

            // PRIORIDAD 4: Completamente bloqueado
            System.out.println("   -> Estrategia: BLOQUEADO - Sin opciones");
            return "BLOCKED";
        }
        

        
        // ESTRATEGIA 1: Intenta moverse directo al objetivo por celda no visitada
         
        private String tryDirectUnvisitedMove(Map<RelativePos,Integer> perception, 
                                        int[] goalDirection, boolean[] visited) {
            // Priorizar movimiento en filas (vertical)
            if (goalDirection[0] < 0 && perception.get(sensors.get(0)) == 0 && !visited[0]) {
                return "UP";
            }
            if (goalDirection[0] > 0 && perception.get(sensors.get(2)) == 0 && !visited[2]) {
                return "DOWN";
            }
            // Luego movimiento en columnas (horizontal)
            if (goalDirection[1] < 0 && perception.get(sensors.get(3)) == 0 && !visited[3]) {
                return "LEFT";
            }
            if (goalDirection[1] > 0 && perception.get(sensors.get(1)) == 0 && !visited[1]) {
                return "RIGHT";
            }

            return null; // No hay camino directo no visitado
        }

        
        //ESTRATEGIA 2: Explora cualquier celda NO visitada
        // (Útil cuando el camino directo esta bloqueado pero hay alternativas)
        private String tryUnexploredMove(Map<RelativePos,Integer> perception, 
                                                boolean[] visited) {
            String[] directions = {"UP", "RIGHT", "DOWN", "LEFT"};

            for (int i = 0; i < 4; i++) {
                if (perception.get(sensors.get(i)) == 0 && !visited[i]) {
                    return directions[i];
                }
            }

            return null; // Todas las celdas libres ya fueron visitadas
        }

        
        //ESTRATEGIA 3: Backtracking - elige la celda MENOS visitada
        //(Para salir de callejones sin salida)
        
        private String tryBacktracking(Map<RelativePos,Integer>
                            perception, boolean[] visited, int[] visitCounts) {
            String[] directions = {"UP", "RIGHT", "DOWN", "LEFT"};

            String bestDirection = null;
            int minVisits = Integer.MAX_VALUE;

            for (int i = 0; i < 4; i++) {
                // Solo considerar celdas libres
                if (perception.get(sensors.get(i)) == 0) {
                    if (visitCounts[i] < minVisits) {
                        minVisits = visitCounts[i];
                        bestDirection = directions[i];
                    }
                }
            }

            if (bestDirection != null) {
                System.out.println("   -> Backtracking: eligiendo " + bestDirection + 
                                 " (visitada " + minVisits + " veces)");
            }

            return bestDirection;
        }
    }
}

*/
