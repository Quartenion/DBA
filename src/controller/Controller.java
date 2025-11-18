package controller;

/**
 * Controlador principal de la aplicacion. 
 * Implementa el patron MVC (Model-View-Controller).
 * * CORRECCIONES:
 * 1. Uso explícito de Row (Fila/Y) y Col (Columna/X).
 * 2. Switch case para carga de mapas.
 * 3. Timer a 40ms.
 * 4. Reset de botones UI al cambiar de mapa.
 * * @author Group_7_DBA
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
import model.Environment;
import model.World;
import view.MainView;

public class Controller {
    
    // === DEPENDENCIAS ===
    private MainView view;            
    private World modelWorld;         
    private final String mapsDir = "./maps/"; 
    
    private Environment model;        
    private ContainerController cc;   

    // === ESTADO INTERNO ===
    private boolean isRunning = false;    
    private boolean draggingAgent = false; 
    private boolean draggingGoal = false;  

    private double dragOffsetRow = 0; 
    private double dragOffsetCol = 0; 
    
    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------
    public Controller(MainView view, World modelWorld, Environment model, ContainerController cc) {
        this.view = view;
        this.model = model;
        this.modelWorld = modelWorld;
        this.cc = cc;
        
        createEventListeners();
        createViewListeners(); 
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
                    // Detener lógica del agente
                    this.model.stop();
                    
                    // Actualizar estado del controlador (esto desbloquea el ratón)
                    this.isRunning = false;
                    
                    // Visualmente "soltar" el botón Run
                    view.setRunButtonSelected(false);
                    
                    // Opcional: Mensaje de log
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

        // 2. NUEVO: Resetear todos los botones (Toggle buttons a false)
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
            
            // Determinar posiciones iniciales
            File file = new File(rutaArchivo);
            String fileName = file.getName();
            
            int startR = rows - 1, startC = cols - 1;
            int goalR = 0, goalC = 0;

            // Lógica de posiciones hardcodeadas para las pruebas
            switch (fileName) {
                case "mapVertical.txt":
                    startR = 25; startC = 0;
                    goalR = 25;  goalC = 49;
                    break;
                case "mapComplex3.txt":
                    startR = 17; startC = 49;
                    goalR = 15;  goalC = 3;
                    break;
                case "mapComplex4.txt":
                    startR = 27; startC = 49;
                    goalR = 39;  goalC = 15;
                    break;
                case "mapComplex5.txt":
                    startR = 49; startC = 25;
                    goalR = 37;  goalC = 25;
                    break;
                case "mapComplex6.txt":
                    startR = 31; startC = 24;
                    goalR = 34;  goalC = 25;
                    break;
                case "mapHorizontal.txt":
                    startR = 49; startC = 30;
                    goalR = 0;   goalC = 30;
                    break;
                case "mapTriangleBig.txt":
                case "mapComplex1.txt":
                case "mapComplex2.txt":
                    startR = 49; startC = 30;
                    goalR = 0;   goalC = 40;
                    break;
                case "mapWithoutObstacle.txt":
                    startR = 49; startC = 49;
                    goalR = 0;   goalC = 0;
                    break;
                default:
                    startR = rows - 1; startC = cols - 1;
                    goalR = 0; goalC = 0;
                    break;
            }

            // 3. Aplicar al Modelo Lógico
            this.model.setAgentPosition(startR, startC);
            this.model.setGoalPosition(goalR, goalC);
            
            // === NUEVO: SIMULAR VISIÓN INICIAL ===
            // Como el agente está parado, forzamos manualmente la visión 
            // de las celdas adyacentes (Up, Down, Left, Right)
            int[] dr = {-1, 1, 0, 0};
            int[] dc = {0, 0, -1, 1};
            
            for (int k = 0; k < 4; k++) {
                int nr = startR + dr[k];
                int nc = startC + dc[k];
                
                // Si la celda adyacente está dentro del mapa, la añadimos a la memoria visual
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    this.model.addSeenCell(nr, nc);
                }
            }
            // =====================================
            
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error al leer el archivo de mapa: " + e.getMessage());
            e.printStackTrace();
        }
    }
}