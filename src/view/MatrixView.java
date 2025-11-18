package view;

import java.awt.*;
import java.util.Set;
import javax.swing.JPanel;
import model.WorldViewInterface;
import model.Environment;

public class MatrixView extends JPanel {
    
    private final WorldViewInterface matrix;
    private final Environment model;
    private final int panelSize = 800;
    private int cellSize;
    
    private boolean showVisited = false;
    private boolean showMemory  = false;
    
    // Solo para renderizado "flotante"
    private double agentDoubleX, agentDoubleY;
    private double goalDoubleX, goalDoubleY;


    public MatrixView(WorldViewInterface matrix, Environment model) {
        this.matrix = matrix;
        this.model = model;
        initMatrix();
        setPreferredSize(new Dimension(panelSize, panelSize));
    }
    
    private void initMatrix() {
        int rows = this.matrix.getRows();
        int cols = this.matrix.getCols();
        // Evitar division por cero si el mundo aun no esta inicializado
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
        
        // Obtener memoria del modelo
        Set<String> visitedCells = model.getVisitedCells();
        Set<String> seenCells    = model.getSeenCells();
        

        // Dibujar el mapa (como antes)
        for (int i = 0; i < matrix.getRows(); i++) {
            for (int j = 0; j < matrix.getCols(); j++) {
                String cellKey = i + "," + j;
                Color color;
                
                
                // Prioridad de colores:
                // 1. Obstaculos (negro)
                // 2. Visitadas (amarillo claro) - si el toggle esta activo
                // 3. Vistas (azul muy claro) - si el toggle esta activo
                // 4. No exploradas (blanco)
                
                switch(matrix.getTile(i, j)){
                    case 0 ->   {
                                if (showVisited && visitedCells.contains(cellKey)){
                                    color = new Color(255, 255, 150);
                                }
                                else if (showMemory && seenCells.contains(cellKey)){
                                    color = new Color(200,220,255);
                                }
                                else{
                                    color = Color.WHITE;
                                }
                                }
                    case -1 -> color = Color.BLACK;
                    default -> color = Color.LIGHT_GRAY;
                    
                }

                g.setColor(color);
                g.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);
                g.setColor(Color.BLACK);
                g.drawRect(j * cellSize, i * cellSize, cellSize, cellSize);

            }
        }

        // ✅ Dibujar OBJETIVO (mejor como circulo centrado)
        g.setColor(Color.GREEN);
        g.fillRect((int)(goalDoubleY * cellSize), (int)(goalDoubleX * cellSize), 
                                                            cellSize, cellSize);

        // ✅ Dibujar AGENTE (circulo centrado)
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
    
    public void toggleShowMemory(){
        this.showMemory = !this.showMemory;
    }
    
    public void toggleShowVisited(){
        this.showVisited = !this.showVisited;
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