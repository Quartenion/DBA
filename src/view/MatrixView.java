package view;

import java.awt.*;
import javax.swing.JPanel;
import model.WorldViewInterface;

public class MatrixView extends JPanel {
    
    private final WorldViewInterface matrix;
    private final int panelSize = 800;
    private int cellSize;
    
    // Solo para renderizado "flotante"
    private double agentDoubleX, agentDoubleY;
    private double goalDoubleX, goalDoubleY;


    public MatrixView(WorldViewInterface matrix) {
        this.matrix = matrix;
        initMatrix();
        setPreferredSize(new Dimension(panelSize, panelSize));
    }
    
    private void initMatrix(){
        this.cellSize = Math.min(panelSize / this.matrix.getRows(), 
                                 panelSize / this.matrix.getCols());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (matrix.empty()) return;
        
        this.cellSize = Math.min(panelSize / this.matrix.getRows(), 
                                 panelSize / this.matrix.getCols());

        // Dibujar el mapa
        for (int i = 0; i < this.matrix.getRows(); i++) {
            for (int j = 0; j < this.matrix.getCols(); j++) {
                Color color = switch(matrix.getTile(i, j)) {
                    case 0 -> Color.WHITE;
                    case -1 -> Color.BLACK;
                    default -> Color.LIGHT_GRAY;
                };
                g.setColor(color);
                g.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);
                g.setColor(Color.BLACK);
                g.drawRect(j * cellSize, i * cellSize, cellSize, cellSize);
            }
        }

        // ✅ Dibujar OBJETIVO desde posición flotante (para arrastre suave)
        g.setColor(Color.GREEN);
        g.fillRect((int)(goalDoubleY * cellSize), (int)(goalDoubleX * cellSize), 
                                                          cellSize, cellSize);

        // ✅ Dibujar AGENTE desde posición flotante (para arrastre suave)
        g.setColor(Color.BLUE);
        int margin = cellSize / 4;
        g.fillOval((int)(agentDoubleY * cellSize + margin/2), 
                   (int)(agentDoubleX * cellSize + margin/2), 
                   cellSize - margin, cellSize - margin);
    }
    
    
    // Setters
    public void setAgentDoublePos(double x, double y){
        this.agentDoubleX = x;
        this.agentDoubleY = y;
    }
    
    public void setGoalDoublePos(double x, double y){
        this.goalDoubleX = x;
        this.goalDoubleY = y;
    }
    
    // Getters
    public int getPanelSize() {
        return panelSize;
    }
    public int getCellSize() {
        return cellSize;
    }
    public double getAgentDoubleX() {
        return agentDoubleX;
    }
    public double getAgentDoubleY() {
        return agentDoubleY;
    }
    public double getGoalDoubleX() {
        return goalDoubleX;
    }
    public double getGoalDoubleY() {
        return goalDoubleY;
    }
}