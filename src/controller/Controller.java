package controller;

import javax.swing.*;
import java.io.File;
import java.awt.event.ActionListener;
import model.World;
import view.mainView;

public class Controller {
    
    private mainView view;
    private World model;
    private final String mapsDir = "./maps/";
    
    public Controller(mainView view, World model) {
        this.view = view;
        this.model = model;
        
        setupViewListeners();
        initializeMapSelector();
        initializeViewToggles();
        
        // Sincronizar vista con modelo inicial
        syncViewWithModel();
    }
    
    private void setupViewListeners() {
        // Escuchar intentos de cambio de posición del agente
        view.getMatrixPanel().setAgentPositionListener((row, col) -> {
            model.setAgentPosition(row, col);
            syncViewWithModel();
        });

        // Escuchar intentos de cambio de posición del objetivo
        view.getMatrixPanel().setGoalPositionListener((row, col) -> {
            model.setGoalPosition(row, col);
            syncViewWithModel();
        });

        // Listener para actualizar log y panel de info cuando cambia la posición
        view.getMatrixPanel().setPositionChangedListener(() -> {
            view.getInfoPanel().setAgentPosition(model.getAgentX(), model.getAgentY());
            view.getInfoPanel().setGoalPosition(model.getGoalX(), model.getGoalY());
            view.getInfoPanel().setGoalReached(model.isGoalReached());
            view.getInfoPanel().addLogEntry(
                "Agente: (" + model.getAgentX() + "," + model.getAgentY() + 
                ") | Objetivo: (" + model.getGoalX() + "," + model.getGoalY() + ")"
            );
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

    private void loadMap(String mapName) {
        try {
            model.read_map(mapsDir + mapName);
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