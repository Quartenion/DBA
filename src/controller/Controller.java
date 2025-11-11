package controller;

/**
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
    
    private MainView view;
    private World modelWorld;
    private final String mapsDir = "./maps/";
    
    private Environment model;
    private ContainerController cc;
    
    private boolean isRunning = false;
    private boolean draggingAgent = false;
    private boolean draggingGoal = false;

    // Listeners: notifican coordenadas propuestas
    private java.util.function.BiConsumer<Integer, Integer> agentPositionListener;
    private java.util.function.BiConsumer<Integer, Integer> goalPositionListener;
    
    /**
     * @brief The constructor doesnt do anything crazy, only creates the listeners
     *        so it doesnt create any problem.
     * @param view Vista principal, que luego encapsula las otras vistas
     * @param modelWorld Modelo del mundo fisico
     * @param model Modelo del entorno del agente
     * @param cc Modelo del contenedor del agente (Para luego parar la ejecucion)
     */
    public Controller(MainView view, World modelWorld, Environment model, 
                            ContainerController cc){
        
        this.view = view;
        this.model = model;
        this.modelWorld = modelWorld;
        this.cc = cc;
        
        createEventListeners();
        //initializeMapSelector();
        //initializeViewToggles();
        
    }


    //--------------------------------------------------------------------------
    // APP LISTENERS -> Only executes one time at the start
    /*
    private void setupViewListeners() {
        // Escuchar intentos de cambio de posición del agente
        view.getMatrixPanel().setAgentPositionListener((row, col) -> {
            this.model.setAgentPosition(row, col);
            syncViewWithModel();
        });

        // Escuchar intentos de cambio de posición del objetivo
        view.getMatrixPanel().setGoalPositionListener((row, col) -> {
            this.model.setGoalPosition(row, col);
            syncViewWithModel();
        });

        // Listener para actualizar log y panel de info cuando cambia la posición
        view.getMatrixPanel().setPositionChangedListener(() -> {
            view.getInfoPanel().setAgentPosition(this.model.getAgentX(), this.model.getAgentY());
            view.getInfoPanel().setGoalPosition(this.model.getGoalX(), this.model.getGoalY());
            view.getInfoPanel().setGoalReached(this.model.isGoalReached());
            view.getInfoPanel().addLogEntry(
                "Agente: (" + this.model.getAgentX() + "," + this.model.getAgentY() + 
                ") | Objetivo: (" + this.model.getGoalX() + "," + this.model.getGoalY() + ")"
            );
        });

        // ============================================
        // TIMER PARA ACTUALIZAR LA VISTA AUTOMÁTICAMENTE
        // ============================================
        javax.swing.Timer timer = new javax.swing.Timer(500, e -> {
            syncViewWithModel();
            view.getInfoPanel().setActionsCount(this.model.getEnergyUsed());
        });
        timer.start();
    }
    */
    // Functions to control the app
    private void createEventListeners(){
        
        // Listener para cerrar la aplicacion y parar la ejecucion de jade
        view.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // 1. Close JADE
                try {
                    if (cc != null) {
                        cc.kill(); // Kill the container and everything
                    }
                } catch (ControllerException ex) {
                    ex.printStackTrace();
                }

                // 2. Close APP
                System.exit(0); 
            }
        });
                
        // Listener para el click del raton
        view.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isRunning) return;
                int mouseX = e.getX(), mouseY = e.getY();
                int margin = view.getCellSize() / 4;
                int radius = (view.getCellSize() - margin) / 2;

                int agentCenterX = (int)(view.getAgentDoubleY() * view.getCellSize() + view.getCellSize()/2);
                int agentCenterY = (int)(view.getAgentDoubleX() * view.getCellSize() + view.getCellSize()/2);
                if ((mouseX - agentCenterX)*(mouseX - agentCenterX) + 
                    (mouseY - agentCenterY)*(mouseY - agentCenterY) <= radius*radius) {
                    draggingAgent = true;
                    return;
                }

                int goalCenterX = (int)(view.getGoalDoubleY() * view.getCellSize() + view.getCellSize()/2);
                int goalCenterY = (int)(view.getGoalDoubleX() * view.getCellSize() + view.getCellSize()/2);
                if ((mouseX - goalCenterX)*(mouseX - goalCenterX) + 
                    (mouseY - goalCenterY)*(mouseY - goalCenterY) <= radius*radius) {
                    draggingGoal = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingAgent) {
                    int col = (int)(view.getAgentDoubleY() + 0.5);
                    int row = (int)(view.getAgentDoubleX() + 0.5);
                    if (agentPositionListener != null) {
                        agentPositionListener.accept(row, col);
                    }
                } else if (draggingGoal) {
                    int col = (int)(view.getGoalDoubleY() + 0.5);
                    int row = (int)(view.getGoalDoubleX() + 0.5);
                    if (goalPositionListener != null) {
                        goalPositionListener.accept(row, col);
                    }
                }
                draggingAgent = false;
                draggingGoal = false;
                view.setCursor(Cursor.getDefaultCursor());
                view.repaint();
            }
        });

        // Listener para el arrastre del raton
        view.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingAgent) {
                    view.setAgentDoublePos((double)e.getX() / view.getCellSize() - 0.5,
                                          (double)e.getY() / view.getCellSize() - 0.5);
                    view.repaint();
                } else if (draggingGoal) {
                    view.setGoalDoublePos((double)e.getX() / view.getCellSize() - 0.5,
                                          (double)e.getY() / view.getCellSize() - 0.5);
                    view.repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (isRunning) return;
                int mouseX = e.getX(), mouseY = e.getY();
                int margin = view.getCellSize() / 4;
                int radius = (view.getCellSize() - margin) / 2;

                int agentCenterX = (int)(view.getAgentDoubleY() * view.getCellSize() + view.getCellSize()/2);
                int agentCenterY = (int)(view.getAgentDoubleX() * view.getCellSize() + view.getCellSize()/2);
                int goalCenterX = (int)(view.getGoalDoubleY() * view.getCellSize() + view.getCellSize()/2);
                int goalCenterY = (int)(view.getGoalDoubleX() * view.getCellSize() + view.getCellSize()/2);

                boolean overAgent = (mouseX - agentCenterX)*(mouseX - agentCenterX) + 
                                    (mouseY - agentCenterY)*(mouseY - agentCenterY) <= radius*radius;
                boolean overGoal = (mouseX - goalCenterX)*(mouseX - goalCenterX) + 
                                   (mouseY - goalCenterY)*(mouseY - goalCenterY) <= radius*radius;

                view.setCursor(overAgent || overGoal ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) 
                                                : Cursor.getDefaultCursor());
                view.repaint();
            }
        });
    }
    
    
    // Each time the app is restarted this is executed
    // Add here all the listeners for the buttons of info panel
    private void createViewListeners(){
        // Listener para boton de pausa
        view.addRunListener(e -> {
           if (this.model.isRunning())
               this.model.stop();
           else
               this.model.run();           
        });
        
        
    }
    
    
    
    // This funcion is needed each time the map is changed, so a listener
    // in the map selector is needed to launch this function.
    // its also needed to launch this after the constructor to have a map
    public void restart(String rutaArchivo){
        read_map(rutaArchivo);
        this.view.initComponents(modelWorld);
        syncDoublePositions();
        createViewListeners();
    }
    
    // Private function to read a map, its only used in restart
    // This is the only function to modify the physical world
    // pre: modelWorld and model is freed
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
            this.modelWorld.changeMap(newMap, rows, cols);
            this.model.setAgentPosition(rows-1, cols-1);
            this.model.setGoalPosition(0,0);
        } 
        catch (IOException | NumberFormatException e) {
            System.out.println("Error reading the file: " + e.getMessage());
        }
    }

    
    
    // SETTERS
    // Sincroniza la posicion para mover el agente con el cursor
    private void syncDoublePositions() {
        this.view.setAgentDoublePos(model.getAgentX(), model.getAgentY());
        this.view.setGoalDoublePos(model.getGoalX(), model.getGoalY());
    }

    // GETTERS
    public int getAgentX(){
        return model.getAgentX();
    }
    public int getAgentY(){
        return model.getAgentY();
    }
    public int getGoalX(){
        return model.getGoalX();
    }
    public int getGoalY(){
        return model.getGoalY();
    }
    
    
    
    
    // Cargar el mapa a partir de una archivo CORREGIR
    /* Deprecated
    private void loadMap(String mapName) {
        try {
            read_map(mapsDir + mapName);

            // Limpiar memoria y energía al cargar nuevo mapa
            this.model.clearMemory();
            this.model.resetEnergy();
            this.model.resetEnergy();

            syncViewWithModel();
            view.getInfoPanel().setActionsCount(0);
            // El log se añade dentro de syncViewWithModel() vía positionChangedListener
        } catch (Exception ex) {
            ex.printStackTrace();
            view.getInfoPanel().addLogEntry("Error cargando el mapa: " + mapName);
        }
    }
    */
    /*
    // Sincroniza toda la vista con el modelo
    private void syncViewWithModel() {
        // view.getMatrixPanel().syncWithModel(); // Esto dispara positionChangedListener
    }
    
    // Sincroniza las posiciones flotantes con el modelo
    public void syncWithModel() {
        //if (positionChangedListener != null) 
            //positionChangedListener.run();
        //repaint();
    }
    */
    
    // FUNCIONES DE CREACION Y DESTRUCCION DE LOS MAPAS
    /* deprecated
    private void initializeMapSelector() {
        File dir = new File(mapsDir);
        if (dir.exists() && dir.isDirectory()) {
            String[] maps = dir.list((d, name) -> name.endsWith(".txt"));
            if (maps != null) {
                view.getInfoPanel().setMapList(maps);
            }
        }

        view.getInfoPanel().addMapSelectorListener(e -> {
            String selectedMap = view.getInfoPanel().getSelectedMap();
            if (selectedMap != null) {
                loadMap(selectedMap);
            }
        });
    }
    
    private void initializeViewToggles() {
        view.getInfoPanel().addShowVisitedListener(e -> view.getMatrixPanel().repaint());
        view.getInfoPanel().addShowMemoryListener(e -> view.getMatrixPanel().repaint());
    }
    
    */
}
