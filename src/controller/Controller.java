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
import java.util.HashMap;
import java.util.Map;
import model.Environment;
import model.World;
import view.MainView;

public class Controller {
    
    // === DEPENDENCIAS ===
    private MainView view;            
    private World modelWorld;         
    private final String mapsDir = "./maps/"; 
    private final String configFilePath = "./maps/posiciones.txt"; // Archivo de configuracion
    
    private Environment model;        
    private ContainerController cc;   

    // === ESTADO INTERNO ===
    private boolean isRunning = false;    
    private boolean draggingAgent = false; 
    private boolean draggingGoal = false;  

    private double dragOffsetRow = 0; 
    private double dragOffsetCol = 0; 
    
    // Cache de configuraciones de mapas: NombreArchivo -> Configuracion
    private Map<String, MapConfig> mapConfigs = new HashMap<>();

    // Clase interna para guardar la config de un mapa
    private static class MapConfig {
        int agentX, agentY; // Fila, Columna (Ojo: en tu txt X es Columna, Y es Fila, o al revés?)
        int goalX, goalY;   // Ajustaremos según tu formato: Agent-x (col), Agent-y (row)
        
        // Según tu ejemplo: "Agent-x  Agent-y".
        // En tu código previo usabas: setAgentPosition(row, col).
        // IMPORTANTE: En 'posiciones.txt':
        // mapVertical.txt -> Agent-x: 0, Agent-y: 25.
        // Tu switch anterior: startR = 25 (Y), startC = 0 (X).
        // CONCLUSIÓN: En el txt, la columna "Agent-x" es la coordenada X (Columna)
        // y "Agent-y" es la coordenada Y (Fila).
        
        public MapConfig(int ax, int ay, int gx, int gy) {
            this.agentX = ax; // Columna
            this.agentY = ay; // Fila
            this.goalX = gx;  // Columna
            this.goalY = gy;  // Fila
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
        
        createEventListeners();
        createViewListeners(); 
    }

    /**
     * Lee el archivo posiciones.txt y carga los datos en memoria.
     */
    private void loadMapConfigurations() {
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            System.err.println("⚠️ AVISO: No se encontró " + configFilePath + ". Se usarán posiciones por defecto.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line = br.readLine(); // Leer cabecera y descartarla
            
            while ((line = br.readLine()) != null) {
                // Formato: Tipo mapa | fichero | Agent-x | Agent-y | Goal-x | Goal-y | Energia
                // Separador: Tabulador (\t) o espacios multiples
                String[] parts = line.trim().split("\\t+"); // Split por tabuladores
                
                if (parts.length >= 6) {
                    String filename = parts[1].trim();
                    try {
                        // NOTA: En tu txt: "Agent-x" (columna) es parts[2], "Agent-y" (fila) es parts[3]
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

    // -------------------------------------------------------------------------
    // LISTENERS DE BAJO NIVEL (Mouse & Window)
    // -------------------------------------------------------------------------
    private void createEventListeners() {
        view.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
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
            public void mousePressed(MouseEvent e) {
                if (isRunning) return;
                
                int cs = view.getCellSize();
                int mouseRow = e.getY() / cs;
                int mouseCol = e.getX() / cs;

                if (mouseRow == model.getAgentX() && mouseCol == model.getAgentY()) {
                    draggingAgent = true;
                    dragOffsetRow = (double)e.getY() / cs - model.getAgentX();
                    dragOffsetCol = (double)e.getX() / cs - model.getAgentY();
                    return;
                }
                
                if (mouseRow == model.getGoalX() && mouseCol == model.getGoalY()) {
                    draggingGoal = true;
                    dragOffsetRow = (double)e.getY() / cs - model.getGoalX();
                    dragOffsetCol = (double)e.getX() / cs - model.getGoalY();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingAgent) {
                    int targetRow = (int)(view.getAgentDoubleRow() + 0.5);
                    int targetCol = (int)(view.getAgentDoubleCol() + 0.5);

                    if (modelWorld.isValidPosition(targetRow, targetCol)) {
                        model.clearMemory();
                        model.resetEnergy();
                        model.setAgentPosition(targetRow, targetCol);
                        view.setAgentDoublePos(targetRow, targetCol);
                        view.addLogEntry("Agente movido a (" + targetRow + ", " + targetCol + ")");
                    } else {
                        view.setAgentDoublePos(model.getAgentX(), model.getAgentY());
                    }
                } 
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

                if (draggingAgent || draggingGoal) {
                    updateUIInfos();
                }
                draggingAgent = false;
                draggingGoal = false;
                view.setCursor(Cursor.getDefaultCursor());
                view.repaint();
            }
        });

        // Listener de movimiento directo al Panel del mapa
        view.addMapMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingAgent || draggingGoal) {
                    int cs = view.getCellSize();
                    
                    double newRow = (double)e.getY() / cs - dragOffsetRow;
                    double newCol = (double)e.getX() / cs - dragOffsetCol;

                    if (draggingAgent) {
                        view.setAgentDoublePos(newRow, newCol);
                    } else if (draggingGoal) {
                        view.setGoalDoublePos(newRow, newCol);
                    }
                    view.repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (isRunning) return;
                int cs = view.getCellSize();
                int row = e.getY() / cs;
                int col = e.getX() / cs;

                boolean overAgent = (row == model.getAgentX() && col == model.getAgentY());
                boolean overGoal  = (row == model.getGoalX()  && col == model.getGoalY());
                
                view.setCursor(overAgent || overGoal ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) 
                                                     : Cursor.getDefaultCursor());
            }
        });
    }
    
    // -------------------------------------------------------------------------
    // LISTENERS DE ALTO NIVEL (UI Controls)
    // -------------------------------------------------------------------------
    private void createViewListeners() {
        view.addRunListener(e -> {
            if (this.model.isRunning()) {
                this.model.stop();
            } else {
                this.model.run();
            }
            this.isRunning = this.model.isRunning();
        });
        
        view.addShowVisitedListener(e -> {
            view.toggleShowVisited();
            view.repaint();
        });
        
        view.addShowMemoryListener(e -> {
            view.toggleShowMemory();
            view.repaint();
        });
        
        javax.swing.Timer uiTimer = new javax.swing.Timer(40, e -> {
            if (isRunning) {
                // 1. Actualizar UI con el estado actual
                updateUIInfos();
                view.setAgentDoublePos(model.getAgentX(), model.getAgentY());
                view.setGoalDoublePos(model.getGoalX(), model.getGoalY());

                // 2. COMPROBAR SI SE ALCANZÓ EL OBJETIVO
                if (model.isGoalReached()) {
                    this.model.stop();
                    this.isRunning = false;
                    view.setRunButtonSelected(false);
                    view.addLogEntry("¡OBJETIVO ALCANZADO! - Stop");
                }
            }
        });
        uiTimer.start(); 

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
            }
        });
    }
    
    // -------------------------------------------------------------------------
    // REINICIO Y CARGA DE MAPAS
    // -------------------------------------------------------------------------
    public void restart(String rutaArchivo) {
        if (isRunning) {
            this.model.stop();           
            this.isRunning = false;      
        }

        model.resetEnergy();
        model.clearMemory();
        
        // Cargar lógica del mapa
        read_map(rutaArchivo);   
        
        this.isRunning = false;  

        // 1. Actualizar vista (sin destruir el panel)
        view.resetMapView(modelWorld, model);

        // 2. Resetear botones
        view.resetToggles(); 

        // 3. Sincronizar posiciones
        view.setAgentDoublePos(model.getAgentX(), model.getAgentY());
        view.setGoalDoublePos(model.getGoalX(), model.getGoalY());
        
        updateUIInfos();
        
        String mapName = new File(rutaArchivo).getName();
        view.addLogEntry("Mapa cargado: " + mapName);
    }
    
    private void read_map(String rutaArchivo) {
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            // 1. Leer dimensiones
            int rows = Integer.parseInt(br.readLine().trim());
            int cols = Integer.parseInt(br.readLine().trim());

            // 2. Leer matriz
            int[][] newMap = new int[rows][cols];
            for (int i = 0; i < rows; i++) {
                String linea = br.readLine();
                String[] valores = linea.trim().split("\\t");
                for (int j = 0; j < cols; j++) {
                    newMap[i][j] = Integer.parseInt(valores[j]);
                }
            }
            
            // Actualizar Modelo Físico
            this.modelWorld.changeMap(newMap, rows, cols);
            
            // Determinar posiciones iniciales (AUTOMATIZADO)
            File file = new File(rutaArchivo);
            String fileName = file.getName();
            
            int startR = rows - 1, startC = cols - 1; // Default: abajo-derecha
            int goalR = 0, goalC = 0;                 // Default: arriba-izquierda

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
                System.out.println("⚠️ Configuración no encontrada para " + fileName + ". Usando defaults.");
            }

            // 3. Aplicar al Modelo Lógico
            this.model.setAgentPosition(startR, startC);
            this.model.setGoalPosition(goalR, goalC);
            
            // === SIMULAR VISIÓN INICIAL ===
            int[] dr = {-1, 1, 0, 0};
            int[] dc = {0, 0, -1, 1};
            
            for (int k = 0; k < 4; k++) {
                int nr = startR + dr[k];
                int nc = startC + dc[k];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    this.model.addSeenCell(nr, nc);
                }
            }
            
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error al leer el archivo de mapa: " + e.getMessage());
            e.printStackTrace();
        }
    }
}