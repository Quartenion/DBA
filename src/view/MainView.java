package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;       // Import necesario
import java.awt.event.MouseMotionListener; // Import necesario
import model.Environment;
import model.WorldViewInterface;

public class MainView extends JFrame {
    
    private MatrixView matrixPanel  = null;
    private InfoPanelView infoPanel = null;
    
    public MainView(WorldViewInterface world, Environment model) {
        setTitle("Simulacion de Mundo - Agente");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        setLayout(new BorderLayout());
        initComponents(world, model);
    }
    
    public void initComponents(WorldViewInterface world, Environment model) {
        if (this.matrixPanel != null) remove(this.matrixPanel);
        if (this.infoPanel != null)   remove(this.infoPanel);
        
        // Creamos los paneles UNA sola vez
        this.matrixPanel = new MatrixView(world, model);
        this.infoPanel = new InfoPanelView();
        
        add(this.matrixPanel, BorderLayout.CENTER);
        add(this.infoPanel, BorderLayout.EAST);
        
        pack();
        setLocationRelativeTo(null);
    }
    
    /**
     * Actualiza la vista. YA NO DESTRUYE EL PANEL.
     * Al mantener el mismo objeto JPanel, conservamos los MouseListeners.
     */
    public void resetMapView(WorldViewInterface world, Environment model) {
        // Como MatrixView lee del 'world' por referencia, 
        // solo necesitamos forzar el repintado y revalidado.
        if (this.matrixPanel != null) {
            this.matrixPanel.revalidate();
            this.matrixPanel.repaint();
        }
    }
    
    /**
     * Resetea todos los botones y la visualización a su estado inicial (false).
     */
    public void resetToggles() {
        // 1. Resetear botones visuales (InfoPanel)
        if (infoPanel != null) {
            infoPanel.setRunButtonSelected(false);
            infoPanel.setShowVisitedSelected(false);
            infoPanel.setShowMemorySelected(false);
        }

        // 2. Resetear lógica de pintado (MatrixView)
        if (matrixPanel != null) {
            matrixPanel.setShowVisited(false);
            matrixPanel.setShowMemory(false);
        }
    }

    // === NUEVOS MÉTODOS PARA ARREGLAR EL CLICK ===
    // Delegamos los listeners directamente al PANEL DE DIBUJO, no a la ventana.
    // Esto hace que (0,0) sea la esquina del mapa, ignorando la barra de titulo.
    public void addMapMouseListener(MouseListener l) {
        if (matrixPanel != null) matrixPanel.addMouseListener(l);
    }

    public void addMapMouseMotionListener(MouseMotionListener l) {
        if (matrixPanel != null) matrixPanel.addMouseMotionListener(l);
    }
    
    // Delegados de coordenadas
    public void setAgentDoublePos(double row, double col) {
        this.matrixPanel.setAgentVisualPos(row, col);
    }
    
    public void setGoalDoublePos(double row, double col) {
        this.matrixPanel.setGoalVisualPos(row, col);
    }
    
    public double getAgentDoubleRow() { return matrixPanel.getAgentDoubleRow(); }
    public double getAgentDoubleCol() { return matrixPanel.getAgentDoubleCol(); }
    
    public double getGoalDoubleRow() { return matrixPanel.getGoalDoubleRow(); }
    public double getGoalDoubleCol() { return matrixPanel.getGoalDoubleCol(); }
    
    public int getCellSize() {
        return matrixPanel.getCellSize();
    }

    // --- Delegados al InfoPanel ---
    public void addLogEntry(String log) { this.infoPanel.addLogEntry(log); }
    public void setGoalReached(boolean reached) { this.infoPanel.setGoalReached(reached); }
    public void setActionsCount(int energy) { this.infoPanel.setActionsCount(energy); }
    public void setAgentPosition(int x, int y) { this.infoPanel.setAgentPosition(x, y); }
    public void setGoalPosition(int x, int y) { this.infoPanel.setGoalPosition(x, y); }
    public void addShowVisitedListener(ActionListener l) { this.infoPanel.addShowVisitedListener(l); }
    public void addShowMemoryListener(ActionListener l) { this.infoPanel.addShowMemoryListener(l); }
    public void setMapList(String[] maps) { if (infoPanel != null) infoPanel.setMapList(maps); }
    public void addMapSelectorListener(ActionListener l) { if (infoPanel != null) infoPanel.addMapSelectorListener(l); }
    public String getSelectedMap() { return (infoPanel != null) ? infoPanel.getSelectedMap() : null; }
    public void addRunListener(ActionListener l) { if (infoPanel != null) infoPanel.addRunListener(l); }
    public void setRunButtonSelected(boolean s) { if (infoPanel != null) infoPanel.setRunButtonSelected(s); }
    
    public void toggleShowMemory(){ this.matrixPanel.toggleShowMemory(); }
    public void toggleShowVisited(){ this.matrixPanel.toggleShowVisited(); }
}