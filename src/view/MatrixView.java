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
    
    private void initMatrix() {
        int rows = this.matrix.getRows();
        int cols = this.matrix.getCols();
        // Evitar división por cero si el mundo aún no está inicializado
        if (rows <= 0 || cols <= 0) {
            this.cellSize = 1; // valor por defecto seguro
        } else {
            this.cellSize = Math.min(panelSize / rows, panelSize / cols);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);  // ✅ Primero limpia el fondo

        if (matrix == null || matrix.empty() || cellSize <= 0) return;

        // Recalcular cellSize (seguro)
        this.cellSize = Math.min(getWidth() / matrix.getCols(), getHeight() / matrix.getRows());

        // Dibujar el mapa (como antes)
        for (int i = 0; i < matrix.getRows(); i++) {
            for (int j = 0; j < matrix.getCols(); j++) {
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

        // ✅ Dibujar OBJETIVO (mejor como círculo centrado)
        g.setColor(Color.GREEN);
        int goalSize = cellSize - cellSize / 6;
        int goalX = (int)(goalDoubleY * cellSize + cellSize / 2 - goalSize / 2);
        int goalY = (int)(goalDoubleX * cellSize + cellSize / 2 - goalSize / 2);
        g.fillRect((int)(goalDoubleY * cellSize), (int)(goalDoubleX * cellSize), 
                                                            cellSize, cellSize);

        // ✅ Dibujar AGENTE (círculo centrado)
        g.setColor(Color.BLUE);
        int agentSize = cellSize - cellSize / 3;
        int agentX = (int)(agentDoubleY * cellSize + cellSize / 2 - agentSize / 2);
        int agentY = (int)(agentDoubleX * cellSize + cellSize / 2 - agentSize / 2);
        g.fillOval(agentX, agentY, agentSize, agentSize);
    }
    
    
    // Setters
    public void setAgentDoublePos(double x, double y){
        this.agentDoubleX = x;
        this.agentDoubleY = y;
        repaint();
    }
    
    public void setGoalDoublePos(double x, double y){
        this.goalDoubleX = x;
        this.goalDoubleY = y;
        repaint();
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