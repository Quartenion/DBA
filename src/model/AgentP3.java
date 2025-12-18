package model;

import controller.Pair;
import controller.ViewLogger;
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

/**
 * Agente Reactivo-Comunicativo P3.
 */
public class AgentP3 extends Agent {

    private Environment model;
    private final int ACTION_TIME = 10; 
    private String lastMove = null;      
    private int secret_code = 0;
    
    private final String TRASLATION_ID = "traslation_elf_";
    private final String SANTA_MSG_ID  = "santa_";
    private final String RUDOLPH_REINDEER_ID = "reindeer_";
    
    private ViewLogger logger;
    
    private final ArrayList<RelativePos> sensors = new ArrayList<>(List.of(
            new RelativePos(-1, 0), // UP
            new RelativePos(0, 1),  // RIGHT
            new RelativePos(1, 0),  // DOWN
            new RelativePos(0, -1)  // LEFT
    ));
    
    @Override
    protected void setup(){
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof Environment) {
            this.model = (Environment) args[0];
        } else {
            throw new RuntimeException("Environment no proporcionado");
        }
        
        if (args.length > 2) {
            this.logger = (ViewLogger) args[2];
            addBehaviour(new startSearchBehaviour());
            addBehaviour(new reactiveAgentBehaviourP3(this, ACTION_TIME));
        }
        else {
            addBehaviour(new reactiveAgentBehaviour(this, ACTION_TIME));
        }
        
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
            msg = receive();
        }
        System.out.println("Cola de mensajes vaciada.");
    }
    
    public boolean isRunning(){ return this.model.isRunning(); }
    public void changeState(State s){ this.model.changeState(s); }

    /**
     * Función centralizada para gestionar logs de comunicación y esperas.
     */
    public void logAndSleep(String type, String performative, String target, String motive, String content, int waitMillis) {
        String action = type.equalsIgnoreCase("SEND") ? "envia mensaje a" : "recibe de";
        String logEntry = String.format("#%s - \"%s\"# AgentP3 %s \"%s\" %s: \"%s\"",
                type, performative, action, target, motive, content);
        
        logger.addLogEntry(logEntry);
        
        if (waitMillis > 0) {
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException ex) {
                Logger.getLogger(AgentP3.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    class startSearchBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            System.out.println("===Comienza el startSearch del AgentP3===");
            
            ACLMessage msg_translation = new ACLMessage(ACLMessage.REQUEST);
            msg_translation.addReceiver(new AID("ElfTranslator", AID.ISLOCALNAME)); 
            msg_translation.setConversationId(TRASLATION_ID);
            msg_translation.setContent("Bro propuesta a comenzar ejecución de rescate de renos, en plan");
            
            logAndSleep("SEND", ACLMessage.getPerformative(msg_translation.getPerformative()), 
                    "ElfTranslator", "para traducir", msg_translation.getContent(), 3000);
            
            myAgent.send(msg_translation);
            
            ACLMessage response_translate = myAgent.blockingReceive();
            logAndSleep("RECIEVE", ACLMessage.getPerformative(response_translate.getPerformative()), 
                    response_translate.getSender().getLocalName(), "el mensaje", response_translate.getContent(), 0);
            
            try {             
                if (!response_translate.getConversationId().equals(TRASLATION_ID)){
                    throw new FIPAException("Error de ID en traducción");
                }

                ACLMessage msg_propose_to_santa = new ACLMessage(ACLMessage.PROPOSE);
                msg_propose_to_santa.addReceiver(new AID("Santa", AID.ISLOCALNAME)); 
                msg_propose_to_santa.setConversationId(SANTA_MSG_ID);
                msg_propose_to_santa.setContent(response_translate.getContent());
                
                logAndSleep("SEND", ACLMessage.getPerformative(msg_propose_to_santa.getPerformative()), 
                        "Santa", "para buscar a los renos", msg_propose_to_santa.getContent(), 3000);
                
                myAgent.send(msg_propose_to_santa);
                
                ACLMessage response_propose_to_santa = myAgent.blockingReceive();
                logAndSleep("RECIEVE", ACLMessage.getPerformative(response_propose_to_santa.getPerformative()), 
                        response_propose_to_santa.getSender().getLocalName(), "el mensaje", response_propose_to_santa.getContent(), 0);
                
                if(response_propose_to_santa.getPerformative() != ACLMessage.ACCEPT_PROPOSAL) {
                    throw new FIPAException("Santa ha rechazado la propuesta");
                }
                
                msg_translation.setContent(response_propose_to_santa.getContent());
                logAndSleep("SEND", ACLMessage.getPerformative(msg_translation.getPerformative()), 
                        "ElfTranslator", "para traducir", msg_translation.getContent(), 3000);
                
                myAgent.send(msg_translation);
                response_translate = myAgent.blockingReceive();

                logAndSleep("RECIEVE", ACLMessage.getPerformative(response_translate.getPerformative()), 
                        response_translate.getSender().getLocalName(), "el mensaje", response_translate.getContent(), 3000);
                
                String[] parts = response_translate.getContent().trim().split("\\s+");
                secret_code = Integer.parseInt(parts[1]);
                
            } catch (FIPAException e){
                System.err.println("Error en la ejecución: " + e.getMessage());
                model.changeState(State.END_WORLD);
            }
        }
    }

    class reactiveAgentBehaviourP3 extends TickerBehaviour {
        private int tickCount = 0;
        private enum Mode { GREEDY, WALL_FOLLOW }
        private Mode currentMode = Mode.GREEDY;
        private int wallToFollow = -1;      
        private boolean useLeftHand = true; 
        private double hitDistance = Double.MAX_VALUE; 
        private String lastCollisionPos = null;
        private int collisionCountAtPos = 0;
        private int wallStartX = -1, wallStartY = -1;

        public reactiveAgentBehaviourP3(Agent a, long period){
            super(a, period);
        }

        private Pair<Integer, Integer> getNextReindeer() throws InterruptedException {
            ACLMessage requestReindeer = new ACLMessage(ACLMessage.QUERY_REF);
            requestReindeer.addReceiver(new AID("Rudolph", AID.ISLOCALNAME)); 
            requestReindeer.setConversationId(RUDOLPH_REINDEER_ID);
            requestReindeer.addUserDefinedParameter("SECRET_CODE", Integer.toString(secret_code));
            requestReindeer.setContent("No tengo goal, dame el proximo reno");
            
            logAndSleep("SEND", ACLMessage.getPerformative(requestReindeer.getPerformative()), 
                    "Rudolph", "para obtener un nuevo reno", requestReindeer.getContent(), 1000);
                
            myAgent.send(requestReindeer);
            ACLMessage responseReindeer = myAgent.blockingReceive();
            
            Pair<Integer,Integer> coord = new Pair<>(-1,-1);
            String displayContent = responseReindeer.getContent();

            if (responseReindeer.getPerformative() == ACLMessage.INFORM_REF){
                try {
                    coord = (Pair<Integer,Integer>) responseReindeer.getContentObject();
                    displayContent = coord.toString();
                } catch (UnreadableException ex) {
                    Logger.getLogger(AgentP3.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            logAndSleep("RECIEVE", ACLMessage.getPerformative(responseReindeer.getPerformative()), 
                    responseReindeer.getSender().getLocalName(), "el mensaje", displayContent, 500);
            
            return coord;
        }
        
        private Pair<Integer, Integer> pedirCoordenadasSanta() {
            ACLMessage msgElf = new ACLMessage(ACLMessage.REQUEST);
            msgElf.addReceiver(new AID("ElfTranslator", AID.ISLOCALNAME));
            msgElf.setContent("Bro coordenadas en plan");
            
            logAndSleep("SEND", ACLMessage.getPerformative(msgElf.getPerformative()), 
                    "ElfTranslator", "para traducir", msgElf.getContent(), 3000);
                
            myAgent.send(msgElf);
            ACLMessage translation = myAgent.blockingReceive();
            logAndSleep("RECIEVE", ACLMessage.getPerformative(translation.getPerformative()), 
                    translation.getSender().getLocalName(), "el mensaje", translation.getContent(), 0);
            
            ACLMessage msgSanta = new ACLMessage(ACLMessage.QUERY_REF);
            msgSanta.addReceiver(new AID("Santa", AID.ISLOCALNAME));
            msgSanta.setContent(translation.getContent());
            msgSanta.addUserDefinedParameter("SECRET_CODE", Integer.toString(secret_code)); 
            
            logAndSleep("SEND", ACLMessage.getPerformative(msgSanta.getPerformative()), 
                    "Santa", "para obtener sus coordenadas", msgSanta.getContent(), 3000);
                
            myAgent.send(msgSanta);
            ACLMessage reply = myAgent.blockingReceive();
            
            if (reply != null) {
                logAndSleep("RECIEVE", ACLMessage.getPerformative(reply.getPerformative()), 
                        reply.getSender().getLocalName(), "el mensaje", reply.getContent(), 0);
            
                msgElf.setContent(reply.getContent());
                logAndSleep("SEND", ACLMessage.getPerformative(msgElf.getPerformative()), 
                        "ElfTranslator", "para traducir", msgElf.getContent(), 3000);
                
                myAgent.send(msgElf);
                ACLMessage translated = myAgent.blockingReceive();
                logAndSleep("RECIEVE", ACLMessage.getPerformative(translated.getPerformative()), 
                        translated.getSender().getLocalName(), "el mensaje", translated.getContent(), 2000);
                
                return extraerCoordenadas(translated.getContent());
            }   
            return null; 
        }
        
        private void confirmarLlegadaSanta() {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(new AID("ElfTranslator", AID.ISLOCALNAME));
            msg.setContent("Bro llegado en plan");

            logAndSleep("SEND", ACLMessage.getPerformative(msg.getPerformative()), 
                    "ElfTranslator", "para traducir", msg.getContent(), 3000);
            
            myAgent.send(msg);
            ACLMessage translation = myAgent.blockingReceive();
            
            if (translation != null) {
                logAndSleep("RECIEVE", ACLMessage.getPerformative(translation.getPerformative()), 
                        translation.getSender().getLocalName(), "el mensaje", translation.getContent(), 2000);

                ACLMessage msgSanta = new ACLMessage(ACLMessage.INFORM);
                msgSanta.addReceiver(new AID("Santa", AID.ISLOCALNAME));
                msgSanta.setContent(translation.getContent());

                logAndSleep("SEND", ACLMessage.getPerformative(msgSanta.getPerformative()), 
                        "Santa", "para confirmarle la llegada", msgSanta.getContent(), 2000);
            
                myAgent.send(msgSanta);
                ACLMessage reply = myAgent.blockingReceive();
                
                logAndSleep("RECIEVE", ACLMessage.getPerformative(reply.getPerformative()), 
                        reply.getSender().getLocalName(), "el mensaje", reply.getContent(), 0);

                model.setSantaConfirmedReached(true);
            }
        }
	private Pair<Integer, Integer> extraerCoordenadas(String message) {
            try {
            	Pattern p = Pattern.compile("-?\\d+");
            	Matcher m = p.matcher(message);
                Integer r = null, c = null;
            	if (m.find()) r = Integer.valueOf(m.group());
            	if (m.find()) c = Integer.valueOf(m.group());
           	if (r != null && c != null) return new Pair<>(r, c);
            } catch (NumberFormatException e) { }
            
            return null;
    	}
        
        @Override
        public void onTick(){
            boolean isRunning = ((AgentP3) myAgent).isRunning();
            
            // DETECCIÓN DE REINICIO (Flanco de subida) P2
            // Si ahora estamos corriendo, pero en el ultimo tick estábamos parados,
            // significa que el usuario le ha dado a "RUN" de nuevo (posiblemente mapa nuevo).
            if (model.isMapChanged()) {
                resetMemory();
                model.setMapChanged(false);
            }
            
            if (model.isSantaConfirmedReached()) {
                return;
            }
            
            // APLICAMOS CAMBIOS DE P3
            // Si esto se cumple significa que no hay GOAL, solicitamos nuevo
            if (model.getGoalR() < 0 || model.getGoalC() < 0){

                // El caso de que no se haya llegado a santa aun
                if (!model.isSantaReached()) {
                    try {
                        Pair<Integer,Integer> reindeerCoord = getNextReindeer();
                        
                        // Recibimos coordenada valida, hacemos set de goal
                        if (reindeerCoord.first > 0 && reindeerCoord.second > 0){
                            model.setGoalPosition(reindeerCoord.first,
                                    reindeerCoord.second);
                        }
                        else {
                            // La coordenada no es valida, o es que no hay mas renos
                            // No contemplamos que la coordenada sea invalida
                            // Preguntamos a santa directamente
                            Pair<Integer,Integer> santaCoord = pedirCoordenadasSanta();
                            model.setGoalPosition(santaCoord.first,
                                    santaCoord.second);
                            // Colocamos esto a true y la proxima vez que llegue al goal
                            // terminara la ejecucion
                            model.setSantaReachedTrue();
                        }
                    } catch (InterruptedException ex) {
                        System.getLogger(AgentP3.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                    }
                }
                else {
                    // El caso de que ya se haya llegado a santa 
                    // Confirmamos la llegada y ya deberia de pararse todo
                    // desde el controller
                    confirmarLlegadaSanta();
                    return;
                }
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