package view;

import javax.swing.*;
import java.awt.*;
import model.WorldViewInterface;

public class MainView extends JFrame {
    
    private MatrixView matrixPanel;
    private InfoPanelView infoPanel;
    
    public MainView(WorldViewInterface world) {
        setTitle("Simulación de Mundo - Agente");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        
        initComponents(world);
        pack();
        setLocationRelativeTo(null);
    }
    
    private void initComponents(WorldViewInterface world) {
        this.matrixPanel = new MatrixView(world);
        this.infoPanel = new InfoPanelView();

        add(this.matrixPanel, BorderLayout.CENTER);
        add(this.infoPanel, BorderLayout.EAST);
    }
    
    public void initMatrix(){
        this.matrixPanel.initMatrix();
    }

    public MatrixView getMatrixPanel() {
        return matrixPanel;
    }
    
    public InfoPanelView getInfoPanel() {
        return infoPanel;
    }
    
    // Setters
    public void setAgentDoublePos(double x, double y){
        this.matrixPanel.setAgentDoublePos(x, y);
    }
    
    public void setGoalDoublePos(double x, double y){
        this.matrixPanel.setGoalDoublePos(x,y);
    }
    
    // Getters
    public int getPanelSize() {
        return matrixPanel.getPanelSize();
    }
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
}
