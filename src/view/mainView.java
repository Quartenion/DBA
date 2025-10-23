package view;

import javax.swing.*;
import java.awt.*;
import model.AgentModel;

/**
 * Ventana principal que muestra la matriz del mundo y la información del agente.
 */
public class mainView extends JFrame {
    
    // Model
    private AgentModel model;
    
    // View elements
    private matrixView matrixPanel;
    private infoPanelView infoPanel;
    
    /**
     * @param model modelo del agente
     * @param mapa matriz del mundo (0 = libre, 1 = pared)
     */
    public mainView(AgentModel model, int[][] mapa) {
        this.model = model;
        
        setTitle("Simulación de Mundo - Agente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        initComponents(mapa);
        
        pack();
        setLocationRelativeTo(null);
    }
    
    private void initComponents(int[][] mapa) {
        // Crear componentes
        this.matrixPanel = new matrixView(mapa);
        this.infoPanel = new infoPanelView();

        // Añadir al layout
        add(matrixPanel, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.EAST);
    }

    // Getter para acceder al panel del mundo
    public matrixView getMatrixPanel() {
        return matrixPanel;
    }
}
