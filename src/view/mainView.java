package view;

import javax.swing.*;
import java.awt.*;
import model.Environment;
import model.World;

public class mainView extends JFrame {
    
    private Environment model;
    private matrixView matrixPanel;
    private infoPanelView infoPanel;
    
    public mainView(Environment model, World world) {
        this.model = model;
        setTitle("Simulación de Mundo - Agente");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        
        initComponents(world);
        pack();
        setLocationRelativeTo(null);
    }
    
    private void initComponents(World world) {
        this.matrixPanel = new matrixView(world, this.model);
        this.infoPanel = new infoPanelView();
        
        // Conectar matrixPanel con infoPanel
        this.matrixPanel.setInfoPanel(this.infoPanel);

        add(this.matrixPanel, BorderLayout.CENTER);
        add(this.infoPanel, BorderLayout.EAST);
    }
    
    public void initMatrix(){
        this.matrixPanel.initMatrix();
    }

    public matrixView getMatrixPanel() {
        return matrixPanel;
    }
    
    public infoPanelView getInfoPanel() {
        return infoPanel;
    }
}