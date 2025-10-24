package view;

import javax.swing.*;
import java.awt.*;
import model.AgentMemory;
import model.World;

public class mainView extends JFrame {
    
    private AgentMemory model;
    private matrixView matrixPanel;
    private infoPanelView infoPanel;
    
    public mainView(AgentMemory model, World world) {
        this.model = model;
        setTitle("Simulación de Mundo - Agente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        initComponents(world);
        pack();
        setLocationRelativeTo(null);
    }
    
    private void initComponents(World world) {
        this.matrixPanel = new matrixView(world);
        this.infoPanel = new infoPanelView();

        add(this.matrixPanel, BorderLayout.CENTER);
        add(this.infoPanel, BorderLayout.EAST);
    }

    public matrixView getMatrixPanel() {
        return matrixPanel;
    }
    
    public infoPanelView getInfoPanel() {
        return infoPanel;
    }
}