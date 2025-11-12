package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import model.WorldViewInterface;

public class MainView extends JFrame {
    
    private MatrixView matrixPanel  = null;
    private InfoPanelView infoPanel = null;
    
    public MainView(WorldViewInterface world) {
        setTitle("Simulación de Mundo - Agente");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        // Inicialización inicial
        initComponents(world);
    }
    
    /**
     * Inicializa la interfaz completa (solo al inicio).
     */
    public void initComponents(WorldViewInterface world) {
        if (this.matrixPanel != null) remove(this.matrixPanel);
        if (this.infoPanel != null)   remove(this.infoPanel);
        
        this.matrixPanel = new MatrixView(world);
        this.infoPanel = new InfoPanelView();

        add(this.matrixPanel, BorderLayout.CENTER);
        add(this.infoPanel, BorderLayout.EAST);
        pack();
        setLocationRelativeTo(null);
    }
    
    /**
     * Reinicia SOLO la vista del mapa (conserva InfoPanelView y sus listeners).
     */
    public void resetMapView(WorldViewInterface world) {
        if (this.matrixPanel != null) {
            remove(this.matrixPanel);
        }
        this.matrixPanel = new MatrixView(world);
        add(this.matrixPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
    
    // Setters delegados
    public void setAgentDoublePos(double x, double y) {
        this.matrixPanel.setAgentDoublePos(x, y);
    }
    
    public void setGoalDoublePos(double x, double y) {
        this.matrixPanel.setGoalDoublePos(x, y);
    }
    
    public void addLogEntry(String log) {
        this.infoPanel.addLogEntry(log);
    }
    
    public void setGoalReached(boolean reached) {
        this.infoPanel.setGoalReached(reached);
    }
    
    public void setActionsCount(int energy) {
        this.infoPanel.setActionsCount(energy);
    }
    
    public void setAgentPosition(int x, int y) {
        this.infoPanel.setAgentPosition(x, y);
    }
    
    public void setGoalPosition(int x, int y) {
        this.infoPanel.setGoalPosition(x, y);
    }
    
    // Getters
    public int getCellSize() {
        return matrixPanel.getCellSize();
    }
    
    public double getAgentDoubleX() {
        return matrixPanel.getAgentDoubleX();
    }
    
    public double getAgentDoubleY() {
        return matrixPanel.getAgentDoubleY();
    }
    
    public double getGoalDoubleX() {
        return matrixPanel.getGoalDoubleX();
    }
    
    public double getGoalDoubleY() {
        return matrixPanel.getGoalDoubleY();
    }
    
    // Delegación para el selector de mapas (encapsulación)
    public void setMapList(String[] maps) {
        if (infoPanel != null) infoPanel.setMapList(maps);
    }
    
    public void addMapSelectorListener(ActionListener listener) {
        if (infoPanel != null) infoPanel.addMapSelectorListener(listener);
    }
    
    public String getSelectedMap() {
        return (infoPanel != null) ? infoPanel.getSelectedMap() : null;
    }
    
    // Delegación para botón Run
    public void addRunListener(ActionListener listener) {
        if (infoPanel != null) infoPanel.addRunListener(listener);
    }
    
    public void setRunButtonSelected(boolean selected) {
        if (infoPanel != null) {
            infoPanel.setRunButtonSelected(selected);
        }
    }
}