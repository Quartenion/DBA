package view;

import javax.swing.*;
import java.awt.*;

import model.AgentP2;
import model.AgentMemory;
import model.World;

/**
 * Ventana principal que muestra la matriz del mundo y la información del agente.
 */
public class mainView extends JFrame {
    
    // Model
    private AgentMemory model;
    
    // View elements
    private matrixView matrixPanel;
    private infoPanelView infoPanel;
    
    /**
     * @param model its the memory of the Agent
     * @param world its the physical world that the agent inhabits
     */
    public mainView(AgentMemory model, World world){
        
        // Save the model
        this.model = model;
        
        setTitle("Simulación de Mundo - Agente");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        
        initComponents(world);
        
        pack();
        setLocationRelativeTo(null);
    }
    
    // Initial components
    private void initComponents(World world) {
        // Function to create the initial components
        
        this.matrixPanel = new matrixView(world);

        this.infoPanel = new infoPanelView();
        
        add(this.matrixPanel, BorderLayout.CENTER);
        //add(this.infoPanel, BorderLayout.EAST);
    }
}
