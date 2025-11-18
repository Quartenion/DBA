package controller;

/**
 * Controlador principal de la aplicacion. 
 * Implementa el patron MVC (Model-View-Controller):
 * - Model: {@link World}, {@link Environment}
 * - View:  {@link MainView}
 * - Controller: esta clase.
 * 
 * Responsabilidades:
 * - Gestionar eventos de usuario (raton, botones, cierre).
 * - Coordinar actualizaciones entre modelo y vista.
 * - Cargar y reiniciar mapas.
 * - Controlar el ciclo de vida de JADE (ContainerController).
 * 
 * Importante: este controlador SOLO conoce a {@link MainView}, 
 * nunca accede directamente a subcomponentes como InfoPanelView o MatrixView.
 * Esto mantiene el acoplamiento debil y facilita el mantenimiento.
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
import model.Environment;
import model.World;
import view.MainView;

public class Controller {
    
    // === DEPENDENCIAS (inyectadas externamente) ===
    private MainView view;           // Vista principal (unica interfaz conocida)
    private World modelWorld;        // Modelo fisico del mundo (matriz de celdas)
    private final String mapsDir = "./maps/"; // Directorio de mapas
    
    private Environment model;       // Modelo logico (posicion agente/objetivo, estado)
    private ContainerController cc;  // Controlador de JADE (para iniciar/detener agentes)

    // === ESTADO INTERNO ===
    private boolean isRunning = false;    // Indica si la simulacion esta en ejecucion
    private boolean draggingAgent = false; // ¿Esta el usuario arrastrando al agente?
    private boolean draggingGoal = false;  // ¿Esta el usuario arrastrando el objetivo?

    // Offset para arrastre suave (en coordenadas de celda, no pixeles)
    // Permite que el agente/objetivo se siga al cursor con precision sub-celda.
    private double dragOffsetX = 0;  // desplazamiento vertical (fila)
    private double dragOffsetY = 0;  // desplazamiento horizontal (columna)
    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------
    /**
     * Constructor. Inicializa las referencias y registra los listeners.
     * @param view Vista principal
     * @param modelWorld Modelo del mundo fisico (mapa)
     * @param model Modelo del entorno (agente, objetivo, estado)
     * @param cc Controlador de contenedor JADE (para gestion de agentes)
     */
    public Controller(MainView view, World modelWorld, Environment model, ContainerController cc) {
        this.view = view;
        this.model = model;
        this.modelWorld = modelWorld;
        this.cc = cc;
        
        // Registra listeners de bajo nivel: raton, ventana (cierre), etc.
        createEventListeners();
        // Registra listeners de alto nivel: boton Run, selector de mapas, etc.
        createViewListeners(); 
    }

    // -------------------------------------------------------------------------
    // LISTENERS DE BAJO NIVEL (interaccion directa con la vista)
    // -------------------------------------------------------------------------
    /**
     * Crea y registra listeners para:
     * - Cierre de ventana (detiene JADE y sale)
     * - Click y arrastre del raton (movimiento de agente/objetivo)
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
                
        // Listener de raton: detecta clicks y suelta para mover agente/objetivo
        view.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Si la simulacion esta corriendo, no permitir edicion manual
                if (isRunning) return;
                
                // Coordenadas del raton en pixeles
                int mouseX = e.getX(), mouseY = e.getY();
                int cs = view.getCellSize(); // Tamaño de cada celda en pixeles
                
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
                    // Esto permite arrastrar desde cualquier punto dentro del circulo
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
                    // Redondear posicion flotante a celda entera mas cercana
                    int col = (int)(view.getAgentDoubleY() + 0.5);
                    int row = (int)(view.getAgentDoubleX() + 0.5);

                    // Validar que la celda objetivo es transitable y dentro del mapa
                    if (modelWorld.isValidPosition(row, col)) {
                        model.clearMemory();
                        model.resetEnergy();
                        // ✅ Actualizar modelo PRIMERO
                        model.setAgentPosition(row, col);
                        // ✅ Sincronizar vista flotante con modelo
                        view.setAgentDoublePos(row, col);
                    } else {
                        // Revertir a posicion actual del modelo
                        view.setAgentDoublePos(model.getAgentX(), model.getAgentY());
                    }

                    // ✅ Solo ahora se puede verificar si se alcanzo el objetivo
                    // (el modelo ya tiene la nueva posicion)
                    // Actualizar panel lateral con nueva posicion
                    view.setAgentPosition(model.getAgentX(), model.getAgentY());
                    view.setGoalReached(model.isGoalReached());
                    view.addLogEntry("Agente → (" + model.getAgentX() + ", " + model.getAgentY() + ")");
                } 
                else if (draggingGoal) {
                    // Logica analoga para el objetivo
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

        // Listener de movimiento del raton: actualiza cursor y posiciones flotantes
        view.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingAgent || draggingGoal) {
                    int cs = view.getCellSize();
                    // Calcular nueva posicion en coordenadas de celda (con offset)
                    double x = (double)e.getY() / cs - dragOffsetX; // fila
                    double y = (double)e.getX() / cs - dragOffsetY; // columna

                    // Actualizar posicion "flotante" en la vista (sin tocar el modelo aun)
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

                // Cambiar cursor a ✋ si el raton esta sobre agente u objetivo
                boolean overAgent = (row == model.getAgentX() && col == model.getAgentY());
                boolean overGoal  = (row == model.getGoalX()  && col == model.getGoalY());
                view.setCursor(overAgent || overGoal ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) 
                                                : Cursor.getDefaultCursor());
            }
        });
    }
    
    // -------------------------------------------------------------------------
    // LISTENERS DE ALTO NIVEL (interaccion con controles de la UI)
    // -------------------------------------------------------------------------
    /**
     * Registra listeners para componentes de la interfaz:
     * - Boton Run/Pause: inicia/detiene la simulacion
     * - Selector de mapas: carga un nuevo mapa
     */
    private void createViewListeners() {
        // Boton Run/Pause: alterna entre ejecutar y pausar la simulacion
        view.addRunListener(e -> {
            if (this.model.isRunning()) {
                this.model.stop(); // Detener logica del agente
            } else {
                this.model.run();  // Iniciar algoritmo de busqueda
            }
            this.isRunning = this.model.isRunning(); // Sincronizar estado local
        });
        
        view.addShowVisitedListener(e -> {
                    view.toggleShowVisited();
                    view.repaint();
                });
        
        view.addShowMemoryListener(e -> {
                    view.toggleShowMemory();
                    view.repaint();
                });
        
        // Timer para actualizar energia, posicion y estado cada 200 ms (solo cuando corre)
        javax.swing.Timer uiTimer = new javax.swing.Timer(1, e -> {
            if (isRunning) {
                // 1. Actualizar datos del panel lateral
                view.setActionsCount(model.getEnergyUsed());
                view.setAgentPosition(model.getAgentX(), model.getAgentY());
                view.setGoalReached(model.isGoalReached());

                // 2. ✅ Sincronizar posicion visual flotante con la posicion entera del modelo
                //    Esto es esencial para que el agente se vea en la celda correcta durante la ejecucion automatica
                view.setAgentDoublePos(model.getAgentX(), model.getAgentY());
                view.setGoalDoublePos(model.getGoalX(), model.getGoalY());

                // Opcional: solo repintar si hubo cambio real (mejora rendimiento)
                // view.repaint(); // setAgentDoublePos() ya hace repaint()
            }
        });
        uiTimer.start(); // ⚠️ Guarda la referencia si quieres .stop() despues

        // Inicializa y configura el selector de mapas
        initializeMapSelector();
    }
    
    /**
     * Configura el JComboBox de seleccion de mapas:
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
                java.util.Arrays.sort(maps); // Orden alfabetico
                view.setMapList(maps);       // Delegar a MainView
            } else {
                view.setMapList(new String[]{"Ningun mapa encontrado"});
            }
        } else {
            view.setMapList(new String[]{"Carpeta 'maps' no encontrada"});
        }

        // Listener: al seleccionar un mapa, reiniciar la simulacion con ese archivo
        view.addMapSelectorListener(e -> {
            String selectedMap = view.getSelectedMap();
            // Evitar procesar mensajes de error como rutas
            if (selectedMap != null &&
                !selectedMap.equals("Ningun mapa encontrado") &&
                !selectedMap.equals("Carpeta 'maps' no encontrada")) {
                restart(mapsDir + selectedMap);
            }
        });
    }
    
    // -------------------------------------------------------------------------
    // REINICIO Y CARGA DE MAPAS
    // -------------------------------------------------------------------------
    /**
     * Reinicia completamente la simulacion con un nuevo mapa.
     * - Carga el archivo de mapa
     * - Actualiza modelo fisico (World) y logico (Environment)
     * - Reinicia solo la vista del mapa (MatrixView), conservando el panel lateral
     * - Sincroniza posiciones iniciales y estado UI
     * 
     * Importante: NO recrea InfoPanelView, para preservar listeners (selector, botones).
     * Usa {@link MainView#resetMapView(WorldViewInterface)} para eso.
     */
    public void restart(String rutaArchivo) {
        // ✅ Si la simulacion esta en ejecucion, detenerla ANTES de recargar
                // ✅ Si la simulacion esta en ejecucion, detenerla ANTES de recargar
        if (isRunning) {
            this.model.stop();          // Detener el agente
            this.isRunning = false;     // Estado interno
            view.setRunButtonSelected(false); // ✅ Visualmente desmarcar el boton  
        }

        model.resetEnergy();
        model.clearMemory();
        read_map(rutaArchivo);   // Carga y actualiza modelo
        
        this.isRunning = false;  // Garantizar pausa (redundante si ya era falso, pero seguro)

        // ✅ Solo reconstruir la vista del mapa, no el panel de control
        view.resetMapView(modelWorld, model);

        // Sincronizar estado inicial
        view.setAgentDoublePos(model.getAgentX(), model.getAgentY());
        view.setGoalDoublePos(model.getGoalX(), model.getGoalY());
        view.setGoalReached(model.isGoalReached());
        view.setActionsCount(model.getEnergyUsed());
        view.addLogEntry("Mapa reiniciado. Agente: (" + model.getAgentX() + "," + model.getAgentY() + 
                         "), Objetivo: (" + model.getGoalX() + "," + model.getGoalY() + ")");
    }
    
    /**
     * Lee un archivo de mapa y actualiza el modelo fisico y logico.
     * Formato esperado:
     *   linea 1: numero de filas (R)
     *   linea 2: numero de columnas (C)
     *   R lineas siguientes: C valores enteros separados por tabulaciones
     * 
     * Valores:
     *   0 → celda transitable
     *  -1 → obstaculo (pared)
     *   >0 → costo (no usado aun)
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
            // Actualizar modelo fisico
            this.modelWorld.changeMap(newMap, rows, cols);
            // REINCIAR POSICIONES AQUI!!!--------------------------------------
            //this.model.setAgentPosition(rows-1, cols-1);
            //this.model.setGoalPosition(0,0);
            
            // MAPAS Y SUS POSICIONES PARA LA EVALUCACION
            
            // mapWithoutObstacle.txt:
            //this.model.setAgentPosition(49, 49);
            //this.model.setGoalPosition(0,0);
            // ACCIONES: 98
            // ACCIONES ESPERADAS: 98
            
            // mapHorizontal.txt:
            // this.model.setAgentPosition(49, 30);
            // this.model.setGoalPosition(0,30);
            // ACCIONES: 85
            // ACCIONES ESPERADAS: 85
            
            // mapVertical.txt:
            // this.model.setAgentPosition(25, 0);
            // this.model.setGoalPosition(25,49);
            // ACCIONES: 475
            // ACCIONES ESPERADAS: 103 -----------------------------------------
            
            // mapTriangleBig.txt:
            // this.model.setAgentPosition(49, 30);
            // this.model.setGoalPosition(0,40);
            // ACCIONES: 155
            // ACCIONES ESPERADAS: 193
            
            // mapComplex1.txt:
            // this.model.setAgentPosition(49, 30);
            // this.model.setGoalPosition(0,40);
            // ACCIONES: 369
            // ACCIONES ESPERADAS: 381
            
            // mapComplex2.txt:
            // this.model.setAgentPosition(49, 30);
            // this.model.setGoalPosition(0,40);
            // ACCIONES: 169
            // ACCIONES ESPERADAS: 107 -----------------------------------------
            
            // mapComplex3.txt:
            // this.model.setAgentPosition(17, 49);
            // this.model.setGoalPosition(15,3);
            // ACCIONES: 318
            // ACCIONES ESPERADAS: 162 -----------------------------------------
            
            // mapComplex4.txt:
            // this.model.setAgentPosition(27, 49);
            // this.model.setGoalPosition(39,15);
            // ACCIONES: 296
            // ACCIONES ESPERADAS: 178 -----------------------------------------
            
            // mapComplex5.txt:
            // this.model.setAgentPosition(49, 25);
            // this.model.setGoalPosition(37,25);
            // ACCIONES: 102
            // ACCIONES ESPERADAS: 102
            
            // mapComplex6.txt:
            // this.model.setAgentPosition(31, 24);
            // this.model.setGoalPosition(34,25);
            // ACCIONES: 286
            // ACCIONES ESPERADAS: 176 -----------------------------------------
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
    }
}