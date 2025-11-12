package controller;

/**
 * Controlador principal de la aplicación. 
 * Implementa el patrón MVC (Model-View-Controller):
 * - Model: {@link World}, {@link Environment}
 * - View:  {@link MainView}
 * - Controller: esta clase.
 * 
 * Responsabilidades:
 * - Gestionar eventos de usuario (ratón, botones, cierre).
 * - Coordinar actualizaciones entre modelo y vista.
 * - Cargar y reiniciar mapas.
 * - Controlar el ciclo de vida de JADE (ContainerController).
 * 
 * Importante: este controlador SOLO conoce a {@link MainView}, 
 * nunca accede directamente a subcomponentes como InfoPanelView o MatrixView.
 * Esto mantiene el acoplamiento débil y facilita el mantenimiento.
 * 
 * @author Group_7_DBA
 */
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import model.Environment;
import model.World;
import view.MainView;

public class Controller {
    
    // === DEPENDENCIAS (inyectadas externamente) ===
    private MainView view;           // Vista principal (única interfaz conocida)
    private World modelWorld;        // Modelo físico del mundo (matriz de celdas)
    private final String mapsDir = "./maps/"; // Directorio de mapas
    
    private Environment model;       // Modelo lógico (posición agente/objetivo, estado)
    private ContainerController cc;  // Controlador de JADE (para iniciar/detener agentes)

    // === ESTADO INTERNO ===
    private boolean isRunning = false;    // Indica si la simulación está en ejecución
    private boolean draggingAgent = false; // ¿Está el usuario arrastrando al agente?
    private boolean draggingGoal = false;  // ¿Está el usuario arrastrando el objetivo?

    // Offset para arrastre suave (en coordenadas de celda, no píxeles)
    // Permite que el agente/objetivo se siga al cursor con precisión sub-celda.
    private double dragOffsetX = 0;  // desplazamiento vertical (fila)
    private double dragOffsetY = 0;  // desplazamiento horizontal (columna)
    
    // ------------------------------------------------------------------------
    // CONSTRUCTOR
    // ------------------------------------------------------------------------
    /**
     * Constructor. Inicializa las referencias y registra los listeners.
     * @param view Vista principal
     * @param modelWorld Modelo del mundo físico (mapa)
     * @param model Modelo del entorno (agente, objetivo, estado)
     * @param cc Controlador de contenedor JADE (para gestión de agentes)
     */
    public Controller(MainView view, World modelWorld, Environment model, ContainerController cc) {
        this.view = view;
        this.model = model;
        this.modelWorld = modelWorld;
        this.cc = cc;
        
        // Registra listeners de bajo nivel: ratón, ventana (cierre), etc.
        createEventListeners();
        // Registra listeners de alto nivel: botón Run, selector de mapas, etc.
        createViewListeners(); 
    }

    // ------------------------------------------------------------------------
    // LISTENERS DE BAJO NIVEL (interacción directa con la vista)
    // ------------------------------------------------------------------------
    /**
     * Crea y registra listeners para:
     * - Cierre de ventana (detiene JADE y sale)
     * - Click y arrastre del ratón (movimiento de agente/objetivo)
     */
    private void createEventListeners() {
        // Listener para el cierre de la ventana: mata el contenedor JADE y cierra la app
        view.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    if (cc != null) cc.kill(); // Detiene todos los agentes y comportamientos
                } catch (ControllerException ex) {
                    ex.printStackTrace();
                }
                System.exit(0); // Sale de la JVM
            }
        });
                
        // Listener de ratón: detecta clicks y suelta para mover agente/objetivo
        view.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Si la simulación está corriendo, no permitir edición manual
                if (isRunning) return;
                
                // Coordenadas del ratón en píxeles
                int mouseX = e.getX(), mouseY = e.getY();
                int cs = view.getCellSize(); // Tamaño de cada celda en píxeles
                
                // Convertir a coordenadas de celda (fila, columna)
                int agentCol = (int)(mouseX / cs);
                int agentRow = (int)(mouseY / cs);
                int goalCol  = (int)(mouseX / cs);
                int goalRow  = (int)(mouseY / cs);

                // Detectar si se hizo clic sobre el agente o el objetivo
                boolean clickedAgent = (agentRow == model.getAgentX() && agentCol == model.getAgentY());
                boolean clickedGoal  = (goalRow  == model.getGoalX()  && goalCol  == model.getGoalY());

                if (clickedAgent) {
                    draggingAgent = true;
                    // Calcular offset relativo: diferencia entre cursor y centro de la celda
                    // Esto permite arrastrar desde cualquier punto dentro del círculo
                    dragOffsetX = (double)mouseY / cs - model.getAgentX(); // en filas
                    dragOffsetY = (double)mouseX / cs - model.getAgentY(); // en columnas
                    return;
                }
                if (clickedGoal) {
                    draggingGoal = true;
                    dragOffsetX = (double)mouseY / cs - model.getGoalX();
                    dragOffsetY = (double)mouseX / cs - model.getGoalY();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                // Al soltar, finalizar el arrastre y actualizar modelo y vista
                if (draggingAgent) {
                    // Redondear posición flotante a celda entera más cercana
                    int col = (int)(view.getAgentDoubleY() + 0.5);
                    int row = (int)(view.getAgentDoubleX() + 0.5);

                    // Validar que la celda objetivo es transitable y dentro del mapa
                    if (modelWorld.isValidPosition(row, col)) {
                        // ✅ Actualizar modelo PRIMERO
                        model.setAgentPosition(row, col);
                        // ✅ Sincronizar vista flotante con modelo
                        view.setAgentDoublePos(row, col);
                    } else {
                        // Revertir a posición actual del modelo
                        view.setAgentDoublePos(model.getAgentX(), model.getAgentY());
                    }

                    // ✅ Solo ahora se puede verificar si se alcanzó el objetivo
                    // (el modelo ya tiene la nueva posición)
                    // Actualizar panel lateral con nueva posición
                    view.setAgentPosition(model.getAgentX(), model.getAgentY());
                    view.setGoalReached(model.isGoalReached());
                    view.addLogEntry("Agente → (" + model.getAgentX() + ", " + model.getAgentY() + ")");
                } 
                else if (draggingGoal) {
                    // Lógica análoga para el objetivo
                    int col = (int)(view.getGoalDoubleY() + 0.5);
                    int row = (int)(view.getGoalDoubleX() + 0.5);

                    if (modelWorld.isValidPosition(row, col)) {
                        model.setGoalPosition(row, col);
                        view.setGoalDoublePos(row, col);
                    } else {
                        view.setGoalDoublePos(model.getGoalX(), model.getGoalY());
                    }

                    view.setGoalPosition(model.getGoalX(), model.getGoalY());
                    view.setGoalReached(model.isGoalReached());
                    view.addLogEntry("Objetivo → (" + model.getGoalX() + ", " + model.getGoalY() + ")");
                }

                // Resetear estado de arrastre
                draggingAgent = false;
                draggingGoal = false;
                dragOffsetX = 0;
                dragOffsetY = 0;
                view.setCursor(Cursor.getDefaultCursor());
                view.repaint();
            }
        });

        // Listener de movimiento del ratón: actualiza cursor y posiciones flotantes
        view.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingAgent || draggingGoal) {
                    int cs = view.getCellSize();
                    // Calcular nueva posición en coordenadas de celda (con offset)
                    double x = (double)e.getY() / cs - dragOffsetX; // fila
                    double y = (double)e.getX() / cs - dragOffsetY; // columna

                    // Actualizar posición "flotante" en la vista (sin tocar el modelo aún)
                    if (draggingAgent) {
                        view.setAgentDoublePos(x, y);
                    } else if (draggingGoal) {
                        view.setGoalDoublePos(x, y);
                    }
                    view.repaint(); // Redibujar para mostrar arrastre en tiempo real
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (isRunning) return;
                int cs = view.getCellSize();
                int col = e.getX() / cs;
                int row = e.getY() / cs;

                // Cambiar cursor a ✋ si el ratón está sobre agente u objetivo
                boolean overAgent = (row == model.getAgentX() && col == model.getAgentY());
                boolean overGoal  = (row == model.getGoalX()  && col == model.getGoalY());
                view.setCursor(overAgent || overGoal ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) 
                                                : Cursor.getDefaultCursor());
            }
        });
    }
    
    // ------------------------------------------------------------------------
    // LISTENERS DE ALTO NIVEL (interacción con controles de la UI)
    // ------------------------------------------------------------------------
    /**
     * Registra listeners para componentes de la interfaz:
     * - Botón Run/Pause: inicia/detiene la simulación
     * - Selector de mapas: carga un nuevo mapa
     */
    private void createViewListeners() {
        // Botón Run/Pause: alterna entre ejecutar y pausar la simulación
        view.addRunListener(e -> {
            if (this.model.isRunning()) {
                this.model.stop(); // Detener lógica del agente
            } else {
                this.model.run();  // Iniciar algoritmo de búsqueda
            }
            this.isRunning = this.model.isRunning(); // Sincronizar estado local
        });
        
        // Timer para actualizar energía, posicion y estado cada 200 ms (solo cuando corre)
        javax.swing.Timer uiTimer = new javax.swing.Timer(1, e -> {
            if (isRunning) {
                // 1. Actualizar datos del panel lateral
                view.setActionsCount(model.getEnergyUsed());
                view.setAgentPosition(model.getAgentX(), model.getAgentY());
                view.setGoalReached(model.isGoalReached());

                // 2. ✅ Sincronizar posición visual flotante con la posición entera del modelo
                //    Esto es esencial para que el agente se vea en la celda correcta durante la ejecución automática
                view.setAgentDoublePos(model.getAgentX(), model.getAgentY());
                view.setGoalDoublePos(model.getGoalX(), model.getGoalY());

                // Opcional: solo repintar si hubo cambio real (mejora rendimiento)
                // view.repaint(); // setAgentDoublePos() ya hace repaint()
            }
        });
        uiTimer.start(); // ⚠️ Guarda la referencia si quieres .stop() después

        // Inicializa y configura el selector de mapas
        initializeMapSelector();
    }
    
    /**
     * Configura el JComboBox de selección de mapas:
     * 1. Escanea la carpeta ./maps/ en busca de archivos .txt
     * 2. Los muestra ordenados en el combo
     * 3. Asocia un listener para recargar el mapa seleccionado
     */
    private void initializeMapSelector() {
        java.io.File dir = new java.io.File(mapsDir);
        if (dir.exists() && dir.isDirectory()) {
            // Filtrar solo archivos .txt
            String[] maps = dir.list((d, name) -> name.endsWith(".txt"));
            if (maps != null && maps.length > 0) {
                java.util.Arrays.sort(maps); // Orden alfabético
                view.setMapList(maps);       // Delegar a MainView
            } else {
                view.setMapList(new String[]{"Ningún mapa encontrado"});
            }
        } else {
            view.setMapList(new String[]{"Carpeta 'maps' no encontrada"});
        }

        // Listener: al seleccionar un mapa, reiniciar la simulación con ese archivo
        view.addMapSelectorListener(e -> {
            String selectedMap = view.getSelectedMap();
            // Evitar procesar mensajes de error como rutas
            if (selectedMap != null &&
                !selectedMap.equals("Ningún mapa encontrado") &&
                !selectedMap.equals("Carpeta 'maps' no encontrada")) {
                restart(mapsDir + selectedMap);
            }
        });
    }
    
    // ------------------------------------------------------------------------
    // REINICIO Y CARGA DE MAPAS
    // ------------------------------------------------------------------------
    /**
     * Reinicia completamente la simulación con un nuevo mapa.
     * - Carga el archivo de mapa
     * - Actualiza modelo físico (World) y lógico (Environment)
     * - Reinicia solo la vista del mapa (MatrixView), conservando el panel lateral
     * - Sincroniza posiciones iniciales y estado UI
     * 
     * Importante: NO recrea InfoPanelView, para preservar listeners (selector, botones).
     * Usa {@link MainView#resetMapView(WorldViewInterface)} para eso.
     */
    public void restart(String rutaArchivo) {
        // ✅ Si la simulación está en ejecución, detenerla ANTES de recargar
                // ✅ Si la simulación está en ejecución, detenerla ANTES de recargar
        if (isRunning) {
            this.model.stop();          // Detener el agente
            this.isRunning = false;     // Estado interno
            view.setRunButtonSelected(false); // ✅ Visualmente desmarcar el botón  
        }

        read_map(rutaArchivo);   // Carga y actualiza modelo
        model.resetEnergy();
        
        this.isRunning = false;  // Garantizar pausa (redundante si ya era falso, pero seguro)

        // ✅ Solo reconstruir la vista del mapa, no el panel de control
        view.resetMapView(modelWorld);

        // Sincronizar estado inicial
        view.setAgentDoublePos(model.getAgentX(), model.getAgentY());
        view.setGoalDoublePos(model.getGoalX(), model.getGoalY());
        view.setGoalReached(model.isGoalReached());
        view.setActionsCount(model.getEnergyUsed());
        view.addLogEntry("Mapa reiniciado. Agente: (" + model.getAgentX() + "," + model.getAgentY() + 
                         "), Objetivo: (" + model.getGoalX() + "," + model.getGoalY() + ")");
    }
    
    /**
     * Lee un archivo de mapa y actualiza el modelo físico y lógico.
     * Formato esperado:
     *   línea 1: número de filas (R)
     *   línea 2: número de columnas (C)
     *   R líneas siguientes: C valores enteros separados por tabulaciones
     * 
     * Valores:
     *   0 → celda transitable
     *  -1 → obstáculo (pared)
     *   >0 → costo (no usado aún)
     * 
     * Posiciones iniciales fijas:
     *   Agente: (R-1, C-1) — esquina inferior derecha
     *   Objetivo: (0, 0) — esquina superior izquierda
     */
    private void read_map(String rutaArchivo) {
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            int rows = Integer.parseInt(br.readLine().trim());
            int cols = Integer.parseInt(br.readLine().trim());

            int[][] newMap = new int[rows][cols];
            for (int i = 0; i < rows; i++) {
                String linea = br.readLine();
                String[] valores = linea.trim().split("\\t");
                for (int j = 0; j < cols; j++) {
                    newMap[i][j] = Integer.parseInt(valores[j]);
                }
            }
            // Actualizar modelo físico
            this.modelWorld.changeMap(newMap, rows, cols);
            // Reiniciar posiciones en modelo lógico
            this.model.setAgentPosition(rows - 1, cols - 1);
            this.model.setGoalPosition(0, 0);
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
    }
}