package controller;

/**
 *
 * @author Group_7_DBA
 */


import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import model.Environment;
import model.World;
import view.mainView;

public class Controller {
    
    private mainView view;
    private World modelWorld;
    private final String mapsDir = "./maps/";
    
    private Environment model;
    private ContainerController cc;
    
    public Controller(mainView view, World modelWorld, Environment model, ContainerController cc){
        this.view = view;
        this.model = model;
        this.modelWorld = modelWorld;
        this.cc = cc;
        
        createEventListeners();
        setupViewListeners();
        initializeMapSelector();
        initializeViewToggles();
        
        // Sincronizar vista con modelo inicial
        syncViewWithModel();
    }
    
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
    }
    // Functions to control the app
    private void createEventListeners(){
        
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
    }
    
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
    
    
    public void read_map(String rutaArchivo) {
        
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

    private void loadMap(String mapName) {
        try {
            read_map(mapsDir + mapName);
            syncViewWithModel();
            view.getInfoPanel().setActionsCount(0);
            // El log se añade dentro de syncViewWithModel() vía positionChangedListener
        } catch (Exception ex) {
            ex.printStackTrace();
            view.getInfoPanel().addLogEntry("Error cargando el mapa: " + mapName);
        }
    }
    
    

    // Sincroniza toda la vista con el modelo
    private void syncViewWithModel() {
        view.getMatrixPanel().syncWithModel(); // Esto dispara positionChangedListener
    }
}
