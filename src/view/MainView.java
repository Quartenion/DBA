package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;       
import java.awt.event.MouseMotionListener; 
import model.Environment;
import model.WorldViewInterface;


 // Ventana principal de la aplicación.
 // Actua como contenedor para la vista de la matriz (MatrixView) y el panel de informacion (InfoPanelView)
 // Utiliza un patron de delegacion para pasar comandos desde el Controlador hacia los paneles especificos

public class MainView extends JFrame {
    
    private MatrixView matrixPanel  = null;
    private InfoPanelView infoPanel = null;
    
    public MainView(WorldViewInterface world, Environment model) {
        setTitle("Simulación de Mundo - Agente");
        // DO_NOTHING_ON_CLOSE permite que el controlador maneje el cierre 
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        setLayout(new BorderLayout());
        
        initComponents(world, model);
    }
    
    
     // Inicializa o reinicializa los componentes de la interfaz.
     
    public void initComponents(WorldViewInterface world, Environment model) {
        // Limpieza preventiva si se llamara mas de una vez
        if (this.matrixPanel != null) remove(this.matrixPanel);
        if (this.infoPanel != null)   remove(this.infoPanel);
        
        this.matrixPanel = new MatrixView(world, model);
        this.infoPanel = new InfoPanelView();
        
        add(this.matrixPanel, BorderLayout.CENTER);
        add(this.infoPanel, BorderLayout.EAST);
        
        pack(); // Ajustar tamaño de ventana al contenido
        setLocationRelativeTo(null); // Centrar en pantalla
    }
    
     // Actualiza la vista sin destruir el objeto JPanel
     // Mantiene vivos los MouseListeners del controlador
    
    public void resetMapView(WorldViewInterface world, Environment model) {
        // Al ser MatrixView quien tiene la referencia a world, basta con
        // notificarle que debe repintarse si los datos cambiaron 
        if (this.matrixPanel != null) {
            this.matrixPanel.revalidate(); // Recalcular layout si cambiaron dimensiones
            this.matrixPanel.repaint();    // Volver a pintar píxeles
        }
    }
    
   
     // Resetea todos los botones y la visualización a su estado inicial (apagado/false).
     // Útil cuando se carga un mapa nuevo.
     
    public void resetToggles() {
        // 1. Resetear botones visuales (InfoPanel)
        if (infoPanel != null) {
            infoPanel.setRunButtonSelected(false);
            infoPanel.setShowVisitedSelected(false);
            infoPanel.setShowMemorySelected(false);
        }

        // 2. Resetear logica de pintado (MatrixView)
        if (matrixPanel != null) {
            matrixPanel.setShowVisited(false);
            matrixPanel.setShowMemory(false);
        }
    }

    // -------------------------------------------------------------------------
    // DELEGACIÓN DE EVENTOS DE RATON (MOUSE LISTENERS)
    // -------------------------------------------------------------------------
    // Se agregan al panel interno (MatrixView) para que las coordenadas (0,0)
    // sean relativas al area de dibujo y no a la esquina de la ventana con borde.
    
    public void addMapMouseListener(MouseListener l) {
        if (matrixPanel != null) matrixPanel.addMouseListener(l);
    }

    public void addMapMouseMotionListener(MouseMotionListener l) {
        if (matrixPanel != null) matrixPanel.addMouseMotionListener(l);
    }
    
    // -------------------------------------------------------------------------
    //  POSICIONAMIENTO Y VISUALIZACION
    // -------------------------------------------------------------------------
    
    // GETTERS Y SETTERS
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
    
    // TOGGLES
    public void toggleShowMemory(){ this.matrixPanel.toggleShowMemory(); }
    public void toggleShowVisited(){ this.matrixPanel.toggleShowVisited(); }
    
    // VISUALIZACIÓN P3 (renos y Santa)
    public void setReindeerPositions(java.util.ArrayList<controller.Pair<Integer, Integer>> positions) {
        this.matrixPanel.setReindeerPositions(positions);
    }
    
    public void setSantaPosition(controller.Pair<Integer, Integer> position) {
        this.matrixPanel.setSantaPosition(position);
    }

    // -------------------------------------------------------------------------
    //  PANEL DE INFORMACIÓN (InfoPanel)
    // -------------------------------------------------------------------------
    
    // SETTERS Y FUNCIONES PARA ACTUALIZAR EL PANEL
    public void addLogEntry(String log) { this.infoPanel.addLogEntry(log); }
    public void clearLogEntry(){this.infoPanel.clearLogEntry();}
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
    public void setMapSelectorEnabled(boolean enabled) {
        if (infoPanel != null) {
            infoPanel.setMapSelectorEnabled(enabled);
        }
    }
    
    public void setReindeerProgress(int found, int total) {
        if (infoPanel != null) {
            infoPanel.setReindeerProgress(found, total);
        }
    }
}