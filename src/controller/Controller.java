package controller;

/**
 * Controlador principal de la aplicacion. 
 * - Lee configuraciones desde 'posiciones.txt'.
 * - Gestiona la carga de mapas y la simulacion.
 * - Respeta MVC.
 */
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import model.Environment;
import model.World;
import view.MainView;

public class Controller {
    
    // - DEPENDENCIAS ----------------------------------------------------------
    private MainView view;            
    private World modelWorld;         
    private final String mapsDir = "./maps/"; 
    private final String configFilePath = "./maps/posiciones.txt";
    private final String configFileP3Path = "./maps/Coordenadas.txt";
    
    private Environment model;        
    private ContainerController cc;   

    // - ESTADO INTERNO --------------------------------------------------------
    private boolean isRunning = false;    
    private boolean draggingAgent = false; 
    private boolean draggingGoal = false;  

    private double dragOffsetRow = 0; 
    private double dragOffsetCol = 0; 
    
    // Cache de configuraciones de mapas: NombreArchivo -> Configuracion
    private Map<String, MapConfig> mapConfigs = new HashMap<>();

    // Clase interna para guardar la config de un mapa
    private static class MapConfig {
        int agentX, agentY; // Fila, Columna (Ojo: en el txt X es Columna, Y es Fila)
        int goalX, goalY;   // Ajustaremos según tu formato: Agent-x (col), Agent-y (row)
        
        public MapConfig(int ax, int ay, int gx, int gy) {
            this.agentX = ax; // Columna
            this.agentY = ay; // Fila
            this.goalX = gx;  // Columna
            this.goalY = gy;  // Fila
        }
    }
    
    // Clase interna que extiende de la configuración de los mapas de la P2
    // y sirve para añadir los flags e info necesaria para la P3
    // Se puede inicializar el goal a -1 y se irán cambiando por los de los
    // renos
    private static class MapConfigP3 extends MapConfig {
        ArrayList<Pair<Integer,Integer>> coordRenos = new ArrayList<>();
        Pair<Integer,Integer> coordSanta;
        
        public MapConfigP3(int ax, int ay, int gx, int gy, 
                Pair<Integer,Integer> coordSanta, ArrayList<Pair<Integer,Integer>> coordRenos){
            
            super(ax,ay,gx,gy);
            
            this.coordSanta = coordSanta;
            this.coordRenos = coordRenos;
        }
    }
    
    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------
    public Controller(MainView view, World modelWorld, Environment model, ContainerController cc) {
        this.view = view;
        this.model = model;
        this.modelWorld = modelWorld;
        this.cc = cc;
        
        // 1. Cargar configuraciones desde el txt
        loadMapConfigurations();
        loadP3configurations();
        
        createEventListeners();
        createViewListeners(); 
    }

    
    /**
     * Lee líneas del buffer hasta encontrar una línea que no está vacía
     */
    private static String readNextTextLine(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                return line;
            }
        }
        return null;
    }
    
    /**
     * Lee el archivo posiciones.txt y carga los datos en memoria
     */
    private void loadMapConfigurations() {
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            System.err.println("AVISO: No se encontro " + configFilePath + ". Se usaran posiciones por defecto: GOAL: (0,0), AGENT: (fil-1, col-1).");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line = br.readLine(); // Descarta la cabecera del archivo (lee la primera linea y pasa a la siguiente)
            
            while ((line = br.readLine()) != null) {
                // Formato: Tipo mapa | fichero | Agent-x | Agent-y | Goal-x | Goal-y | Energia
                // Separador: Tabulador (\t) 
                String[] parts = line.trim().split("\\t+"); // Separa el string por tabuladores
                
                if (parts.length >= 6) {
                    String filename = parts[1].trim(); // Coge el nombre del archivo
                    try {
                        // NOTA: En el txt: "Agent-x" (columna) es parts[2], "Agent-y" (fila) es parts[3]
                        int ax = Integer.parseInt(parts[2].trim()); // Columna (X)
                        int ay = Integer.parseInt(parts[3].trim()); // Fila (Y)
                        int gx = Integer.parseInt(parts[4].trim()); // Columna (X)
                        int gy = Integer.parseInt(parts[5].trim()); // Fila (Y)
                        
                        mapConfigs.put(filename, new MapConfig(ax, ay, gx, gy));
                        // System.out.println("Config cargada para: " + filename); // Debug
                    } catch (NumberFormatException e) {
                        System.err.println("Error parseando linea de config: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo " + configFilePath + ": " + e.getMessage());
        }
    }
    
    /**
     * Lee el archivo de Coordenadas.txt
     * 
     * ARCHIVO DE CONFIGURACIÓN PARA LA PRÁCTICA 3
     * Contiene toda la información de: 
     *  coordenadas de los renos
     *  coordenadas de santa
     *  coordenadas iniciales
     * 
     * Para cada uno de los mapas relevantes
     * 
     * Hacer que el programa compruebe primero si es uno de los mapas de la P2
     * en caso de que no esté en la configuración el mapa que mire en la estructura
     * que genera esta función.
     * 
     */
    private void loadP3configurations() {
        File configFile = new File(configFileP3Path);
        if (!configFile.exists()) {
            System.err.println("AVISO: No se encontro " + configFileP3Path + ". que es la configuración para la P3, se cierra el programa");
            closeApp();
        }
        
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line;

            // Encontramos mapa
            while ((line = readNextTextLine(br)) != null) {
                
                // Sacamos filename
                String filename = line.trim();
                System.out.println("Processing Map: " + filename);

                // Quitamos la cabecera de los renos
                readNextTextLine(br); 

                // Leemos las coordenadas de los renos renos
                ArrayList<Pair<Integer, Integer>> renosTemp = new ArrayList<>();
                while ((line = br.readLine()) != null && !line.trim().isEmpty()) {
                    // Sacamos la línea
                    String cleanLine = line.replace("*", "").trim();
                    String[] partes = cleanLine.split(",");
                    
                    // Añadimos
                    renosTemp.add(new Pair<>(
                        Integer.valueOf(partes[0].trim()), 
                        Integer.valueOf(partes[1].trim())
                    ));
                }

                // Consumimos cabecera santa
                readNextTextLine(br); 
                
                // Leemos la coordenada de santa
                Pair<Integer,Integer> coordSanta = new Pair<Integer,Integer>(-1,-1);
                line = readNextTextLine(br);
                if (line != null) {
                    String cleanLine = line.replace("*", "").trim();
                    String[] partes = cleanLine.split(",");
                    coordSanta.first  = Integer.valueOf(partes[0].trim());
                    coordSanta.second = Integer.valueOf(partes[1].trim());
                }
                
                
                // Consumismos cabecera agente
                readNextTextLine(br);
                
                // Leemos la coordenada de agente
                Integer ax = -2; // junk value
                Integer ay = -2; // junk value
                line = readNextTextLine(br);
                if (line != null) {
                    String cleanLine = line.replace("*", "").trim();
                    String[] partes = cleanLine.split(",");
                    ax = Integer.valueOf(partes[0].trim());
                    ay = Integer.valueOf(partes[1].trim());
                }
                
                // Creamos la configuración asociada al filename
                MapConfigP3 config = new MapConfigP3(ax, ay, -1, -1, coordSanta, renosTemp);
                System.out.println(" -> Loaded: " + filename + " | Agent: " + ax + "," + ay 
                                            + " | Reindeers: " + renosTemp.size());
                
                // Colocamos configuración en el map
                mapConfigs.put(filename, config);
            }
            
        } catch (IOException e) {
            System.err.println("Error leyendo " + configFilePath + ": " + e.getMessage());
            closeApp();
        }
        
    }


    // -------------------------------------------------------------------------
    // LISTENERS DE BAJO NIVEL (Mouse & Window)
    // -------------------------------------------------------------------------
    private void createEventListeners() {
        // ---------------------------------------------------------------------
        // 1. LISTENER DE VENTANA (Cierre de la aplicación)
        // ---------------------------------------------------------------------
        view.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    // Cierra el ContainerController de JADE
                    // para evitar que queden procesos huerfanos en memoria
                    if (cc != null) cc.kill(); 
                } catch (ControllerException ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            }
        });
                
        // Listener de click directo al Panel del mapa
        view.addMapMouseListener(new MouseAdapter() {
            @Override
            
            // Se ejecuta cuando el usuario presiona el botón del raton
            // Detecta si se ha hecho clic sobre el Agente o el Objetivo para iniciar el arrastre
            
            public void mousePressed(MouseEvent e) {
                // Si la simulacion esta corriendo, bloqueamos la interaccion para no romper la logica
                if (isRunning) return;
                
                int cs = view.getCellSize(); // Tamaño de cada celda en pixeles
                // Convertimos coordenadas de pantalla a coordenadas de matriz (fil, col)
                int mouseRow = e.getY() / cs;
                int mouseCol = e.getX() / cs;
                
                // VERIFICAR SI EL CLIC FUE SOBRE EL AGENTE
                if (mouseRow == model.getAgentX() && mouseCol == model.getAgentY()) {
                    draggingAgent = true;
                    // Calculamos el desplazamiento para que, al arrastrar, el objeto 
                    // no salte al centro del raton, sino que mantenga la posicion relativa del clicí
                    // es una tonteria pero se ve visualmente un poco mejor
                    dragOffsetRow = (double)e.getY() / cs - model.getAgentX();
                    dragOffsetCol = (double)e.getX() / cs - model.getAgentY();
                    return;
                }
                
                // VERIFICAR SI EL CLIC FUE SOBRE EL GOAL
                if (mouseRow == model.getGoalX() && mouseCol == model.getGoalY()) {
                    draggingGoal = true;
                    dragOffsetRow = (double)e.getY() / cs - model.getGoalX();
                    dragOffsetCol = (double)e.getX() / cs - model.getGoalY();
                }
            }
            
            @Override
            
            // Se ejecuta cuando el usuario suelta el boton del raton
            // Aqui se valida la nueva posicion y se actualiza el modelo logico
         
            public void mouseReleased(MouseEvent e) {
                // LOGICA PARA SOLTAR AL AGENTE
                if (draggingAgent) {
                    // Redondeamos la posicion visual al entero mas cercano
                    // para saber en que celda exacta cayo
                    int targetRow = (int)(view.getAgentDoubleRow() + 0.5);
                    int targetCol = (int)(view.getAgentDoubleCol() + 0.5);
                    
                    // Si la celda es valida (no es una pared, esta dentro de limites)
                    if (modelWorld.isValidPosition(targetRow, targetCol)) {
                        model.clearMemory(); // Reiniciamos la memoria del agente (se borra de su memoria las casillas visitadas y las vistas)
                        model.resetEnergy(); // Reiniciamos el numero de acciones que ha realizadoú
                        model.setAgentPosition(targetRow, targetCol); // Actualiza la logica
                        view.setAgentDoublePos(targetRow, targetCol); // Ajusta visualmente el agente al centro de la celda
                        view.addLogEntry("Agente movido a (" + targetRow + ", " + targetCol + ")");
                    } else {
                        // Se devuelve el agente a su posicion si la nueva áno es valida
                        view.setAgentDoublePos(model.getAgentX(), model.getAgentY());
                    }
                } 
                // LOGICA PARA SOLTAR EL OBJETIVO (IGUAL QUE LA LOGICA DEL AGENTE)
                else if (draggingGoal) {
                    int targetRow = (int)(view.getGoalDoubleRow() + 0.5);
                    int targetCol = (int)(view.getGoalDoubleCol() + 0.5);

                    if (modelWorld.isValidPosition(targetRow, targetCol)) {
                        model.setGoalPosition(targetRow, targetCol);
                        view.setGoalDoublePos(targetRow, targetCol);
                        view.addLogEntry("Objetivo movido a (" + targetRow + ", " + targetCol + ")");
                    } else {
                        view.setGoalDoublePos(model.getGoalX(), model.getGoalY());
                    }
                }
                
                // Actualizar paneles de informacion si hubo cambios
                if (draggingAgent || draggingGoal) {
                    updateUIInfos();
                }
                // Se resetean los estados
                draggingAgent = false;
                draggingGoal = false;
                view.repaint();
            }
        });

        // ---------------------------------------------------------------------
        // 3. LISTENER DE MOVIMIENTO (Feedback visual y Hover)
        // ---------------------------------------------------------------------
        view.addMapMouseMotionListener(new MouseMotionAdapter() {
            
            // Se ejecuta mientras se arrastra el raton con el boton presionado
            // Actualiza la posicion visual flotante (double) para que el movimiento se vea suave
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingAgent || draggingGoal) {
                    int cs = view.getCellSize();
                    
                    // Calculamos la nueva posicion exacta basada en pixeles, restando el offset inicial
                    double newRow = (double)e.getY() / cs - dragOffsetRow;
                    double newCol = (double)e.getX() / cs - dragOffsetCol;

                    if (draggingAgent) {
                        view.setAgentDoublePos(newRow, newCol);
                    } else if (draggingGoal) {
                        view.setGoalDoublePos(newRow, newCol);
                    }
                    // Se ejecuta constantemente para ver el movimiento suave
                    view.repaint();
                }
            }
            
            
            // Se ejecuta cuando el raton se mueve 
            // Sirve para cambiar la imagen del cursor para indicar que se puede seleccionar
            @Override
            public void mouseMoved(MouseEvent e) {
                if (isRunning) return; // No mostrar cursor de mano si la simulacion corre
                int cs = view.getCellSize();
                int row = e.getY() / cs;
                int col = e.getX() / cs;
                
                // Comprobamos si el raton esta sobre el Agente o el Goal
                boolean overAgent = (row == model.getAgentX() && col == model.getAgentY());
                boolean overGoal  = (row == model.getGoalX()  && col == model.getGoalY());
                
                // Cambiamos el cursor a MANO si esta sobre algo interactivo
                view.setCursor(overAgent || overGoal ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) 
                                                     : Cursor.getDefaultCursor());
            }
        });
    }
    
    // -------------------------------------------------------------------------
    // LISTENERS DE ALTO NIVEL (UI Controls)
    // -------------------------------------------------------------------------
    private void createViewListeners() {
        // ---------------------------------------------------------------------
        // 1. BOTÓN RUN / STOP (Control de la Simulación)
        // ---------------------------------------------------------------------
        
        view.addRunListener(e -> {
            // Actua como un toggle,
            // si el agente ya esta corriendo lo paramos y viceversa
            if (this.model.isRunning()) {
                this.model.stop();
            } else {
                this.model.run();
            }
            // Sincronizamos el estado local del controlador con el del modelo
            this.isRunning = this.model.isRunning();
            
            // Si corre, deshabilita el selector de mapas(false). Si para, habilita (true).
            view.setMapSelectorEnabled(!this.isRunning);
        });
        
        // ---------------------------------------------------------------------
        // 2. INTERRUPTORES VISUALES (Layers del mapa)
        // ---------------------------------------------------------------------
        
        // Boton para mostrar/ocultar las migajas que va dejando en cada celda que visita
        view.addShowVisitedListener(e -> {
            view.toggleShowVisited();
            view.repaint();
        });
        
        // Boton para mostrar/ocultar la vision del agente
        view.addShowMemoryListener(e -> {
            view.toggleShowMemory();
            view.repaint();
        });
        
        // ---------------------------------------------------------------------
        // 3. BUCLE DE REFRESCO DE LA UI 
        // ---------------------------------------------------------------------
        // Usamos un Timer para comprobar el estado del modelo periodicamente
        javax.swing.Timer uiTimer = new javax.swing.Timer(40, e -> {
            // Solo actualizamos la vista si la simulacion esta activa
            if (isRunning) {
                // A) ACTUALIZACION DE DATOS
                // Actualizar paneles de informacion
                updateUIInfos();
                
                // Sincroniza la posicion visual del agente y meta con la logica
                view.setAgentDoublePos(model.getAgentX(), model.getAgentY());
                view.setGoalDoublePos(model.getGoalX(), model.getGoalY());

                // B) COMPROBAR SI SE ALCANZO EL GOAL
                // Detiene la logica del agente y del controlador
                if (model.isGoalReached()) {
                    this.model.stop();
                    this.isRunning = false;
                    view.setRunButtonSelected(false);
                    view.addLogEntry("¡OBJETIVO ALCANZADO! - Stop");
                    view.setMapSelectorEnabled(true);
                }
            }
        });
        uiTimer.start(); 
        
        // Inicializar la logica del desplegable de seleccion de mapas
        initializeMapSelector();
    }
    
    private void updateUIInfos() {
        view.setActionsCount(model.getEnergyUsed());
        view.setAgentPosition(model.getAgentX(), model.getAgentY());
        view.setGoalReached(model.isGoalReached());
        view.setGoalPosition(model.getGoalX(), model.getGoalY());
    }
    
    private void initializeMapSelector() {
        File dir = new File(mapsDir);
        if (dir.exists() && dir.isDirectory()) {
            String[] maps = dir.list((d, name) -> name.endsWith(".txt"));
            if (maps != null && maps.length > 0) {
                java.util.Arrays.sort(maps);
                view.setMapList(maps);       
            } else {
                view.setMapList(new String[]{"Ningun mapa encontrado"});
            }
        } else {
            view.setMapList(new String[]{"Carpeta 'maps' no encontrada"});
        }

        view.addMapSelectorListener(e -> {
            String selectedMap = view.getSelectedMap();
            if (selectedMap != null && !selectedMap.contains("no encontrada")) {
                restart(mapsDir + selectedMap);
                model.setMapChanged(true);
            }
        });
    }
    
    // -------------------------------------------------------------------------
    // REINICIO Y CARGA DE MAPAS
    // Reinicia la simulacion cargando un nuevo mapa (o recargando el actual)
    // Se encarga de coordinar la parada segura del agente, la carga de datos
    // y la regeneracion de la interfaz grafica
    // -------------------------------------------------------------------------
    
    public void restart(String rutaArchivo) {
        // ---------------------------------------------------------------------
        // 1. PARADA DE SEGURIDAD
        // ---------------------------------------------------------------------
        // Si el agente esta corriendo, lo detenemos antes de cambiar el mapa  
        if (isRunning) {
            this.model.stop();           
            this.isRunning = false;      
        }
        
        // ---------------------------------------------------------
        // 2. RESETEO DEL ESTADO LOGICO (MODELO)
        // ---------------------------------------------------------
        model.resetEnergy();
        model.clearMemory();
        
        // Cargar logica del mapa
        read_map(rutaArchivo);   
        
        // Asegura que la simulacion comience en estado pausado
        this.isRunning = false;  

        // ---------------------------------------------------------
        // 3. RECONSTRUCCIÓN DE LA VISTA (VIEW)
        // ---------------------------------------------------------
        // Pide a la vista que tire el panel del mapa viejo y cree uno nuevo 
        // con las nuevas dimensiones (filas/columnas) leidas en read_map()
        view.resetMapView(modelWorld, model);

        // Resetea el estado de los botones visualmente (desmarca los botones)
        view.resetToggles(); 

        // ---------------------------------------------------------
        // 4. SINCRONIZACIÓN VISUAL
        // ---------------------------------------------------------
        // Alineamos los sprites visuales (Agente y Goal) con las coordenadas 
        // iniciales que acabamos de cargar en el modelo.
        view.setAgentDoublePos(model.getAgentX(), model.getAgentY());
        view.setGoalDoublePos(model.getGoalX(), model.getGoalY());
        
        // Actualizar paneles de informacion
        updateUIInfos();
        
        // Añade un log 
        String mapName = new File(rutaArchivo).getName();
        view.addLogEntry("Mapa cargado: " + mapName);
    }
    
    
    
    // Lee un archivo de texto plano para reconstruir el entorno
    // Formato esperado:
    // - Linea 1: Numero de filas
    // - Linea 2: Numero de columnas
    // - Resto: Matriz de valores separados por tabuladores.
    
    private void read_map(String rutaArchivo) {
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            // -----------------------------------------------------------------
            // 1. PARSEO DE LA ESTRUCTURA DEL MAPA
            // -----------------------------------------------------------------
            // Lee las dos primeras lineas para saber el tamaño de la matriz
            int rows = Integer.parseInt(br.readLine().trim());
            int cols = Integer.parseInt(br.readLine().trim());

            // Crea la matriz temporal para leer los obstaculos (0=libre, 1=pared)
            int[][] newMap = new int[rows][cols];
            for (int i = 0; i < rows; i++) {
                String linea = br.readLine();
                String[] valores = linea.trim().split("\\t");
                for (int j = 0; j < cols; j++) {
                    newMap[i][j] = Integer.parseInt(valores[j]);
                }
            }
            
            // Enviamos la estructura fisica (paredes/huecos) al modelo del Mundo
            this.modelWorld.changeMap(newMap, rows, cols);
            
            // -----------------------------------------------------------------
            // 2. DETERMINAR POSICIONES DE INICIO Y META
            // -----------------------------------------------------------------
            File file = new File(rutaArchivo);
            String fileName = file.getName();
            
            // Valores por defecto (por si no hay configuracion en posiciones.txt)
            int startR = rows - 1, startC = cols - 1; // Esquina inferior derecha
            int goalR = 0, goalC = 0;                 // Esquina superior izquierda

            // Buscamos en nuestro mapa de configuraciones cargado previamente
            if (mapConfigs.containsKey(fileName)) {
                MapConfig cfg = mapConfigs.get(fileName);
                // OJO: En el txt: X = Columna, Y = Fila.
                // setAgentPosition(FILA, COLUMNA)
                startR = cfg.agentY; // Fila
                startC = cfg.agentX; // Columna
                goalR = cfg.goalY;   // Fila
                goalC = cfg.goalX;   // Columna
            } else {
                System.out.println("Configuracion no encontrada para " + fileName + ". Usando defaults.");
            }

            // Aplicamos las posiciones al entorno logico del Agente
            this.model.setAgentPosition(startR, startC);
            this.model.setGoalPosition(goalR, goalC);
            
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error al leer el archivo de mapa: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
        
    /**
     * Used to close the app when santa is a 
     * scumbag or when the config is not right
     */
    private void closeApp() {
        try {
                // Cierra el ContainerController de JADE
                // para evitar que queden procesos huerfanos en memoria
                if (cc != null) cc.kill(); 
            } catch (ControllerException ex) {
                ex.printStackTrace();
            }
        view.dispose();
        System.exit(0);
    }
    
}