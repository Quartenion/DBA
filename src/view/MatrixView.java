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
    
    private double agentRow, agentCol;
    private double goalRow, goalCol;

    public MatrixView(WorldViewInterface matrix, Environment model) {
        this.matrix = matrix;
        this.model = model;
        initMatrix();
        setPreferredSize(new Dimension(panelSize, panelSize));
        setBackground(Color.WHITE); 
    }
    
    private void initMatrix() {
        updateCellSize();
        repaint();
    }
    
    private void updateCellSize() {
        int rows = this.matrix.getRows();
        int cols = this.matrix.getCols();
        if (rows > 0 && cols > 0) {
             this.cellSize = Math.min(getWidth() / cols, getHeight() / rows);
             if (this.cellSize < 1) this.cellSize = 1; 
        }
    }

    /**
     * Calcula el color del mapa de calor.
     */
    private Color getHeatmapColor(int visits) {
        int maxVisits = 10; 
        
        if (visits <= 1) {
            return new Color(255, 255, 180); // Amarillo base
        }

        float ratio = Math.min(1.0f, (float)(visits - 1) / maxVisits);
        int green = 255 - (int)(255 * ratio);
        green = Math.max(0, green);

        return new Color(255, green, 0);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 

        if (matrix == null) return;
        
        int rows = matrix.getRows();
        int cols = matrix.getCols();
        if (rows == 0 || cols == 0) return;

        updateCellSize();
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        Set<String> visitedCells = showVisited ? model.getVisitedCells() : null;
        Set<String> seenCells    = showMemory ? model.getSeenCells() : null;
        
        // --- 1. RELLENAR CELDAS ---
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int tileType = matrix.getTile(r, c);
                int pixelX = c * cellSize;
                int pixelY = r * cellSize;

                if (tileType == -1) {
                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(pixelX, pixelY, cellSize, cellSize);
                } else {
                    if (showVisited || showMemory) {
                        String cellKey = r + "," + c;
                        
                        // CASO 1: VISITADAS (HEATMAP + NÚMERO)
                        if (showVisited && visitedCells != null && visitedCells.contains(cellKey)) {
                            int visits = model.getVisitCount(r, c);
                            
                            // A. Fondo de color
                            g2d.setColor(getHeatmapColor(visits));
                            g2d.fillRect(pixelX, pixelY, cellSize, cellSize);
                            
                            // B. Dibujar número (Solo si cabe)
                            if (cellSize > 15) { 
                                g2d.setColor(Color.BLACK); // Texto negro
                                // Ajustar tamaño de fuente al tamaño de la celda
                                int fontSize = Math.max(10, (int)(cellSize * 0.5));
                                g2d.setFont(new Font("SansSerif", Font.BOLD, fontSize));
                                
                                String text = String.valueOf(visits);
                                FontMetrics fm = g2d.getFontMetrics();
                                
                                // Cálculo para centrar el texto perfectamente
                                int textX = pixelX + (cellSize - fm.stringWidth(text)) / 2;
                                int textY = pixelY + (cellSize - fm.getHeight()) / 2 + fm.getAscent();
                                
                                g2d.drawString(text, textX, textY);
                            }
                        } 
                        // CASO 2: MEMORIA (Solo fondo azul)
                        else if (showMemory && seenCells != null && seenCells.contains(cellKey)) {
                            g2d.setColor(new Color(220, 240, 255)); 
                            g2d.fillRect(pixelX, pixelY, cellSize, cellSize);
                        }
                    }
                }
            }
        }

        // --- 2. DIBUJAR GRID ---
        g2d.setColor(new Color(230, 230, 230)); 
        int mapHeight = rows * cellSize;
        for (int c = 0; c <= cols; c++) {
            int x = c * cellSize;
            g2d.drawLine(x, 0, x, mapHeight);
        }
        int mapWidth = cols * cellSize;
        for (int r = 0; r <= rows; r++) {
            int y = r * cellSize;
            g2d.drawLine(0, y, mapWidth, y);
        }

        // --- 3. ELEMENTOS DINÁMICOS ---
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Objetivo
        g2d.setColor(new Color(0, 200, 0));
        int goalPixelX = (int)(goalCol * cellSize); 
        int goalPixelY = (int)(goalRow * cellSize); 
        g2d.fillRect(goalPixelX + 1, goalPixelY + 1, cellSize - 2, cellSize - 2); 

        // Agente
        g2d.setColor(Color.BLUE);
        double agentSize = Math.max(2, cellSize * 0.8); 
        double offset = (cellSize - agentSize) / 2.0;
        
        int agentPixelX = (int)(agentCol * cellSize + offset);
        int agentPixelY = (int)(agentRow * cellSize + offset);
        
        g2d.fillOval(agentPixelX, agentPixelY, (int)agentSize, (int)agentSize);
    }
    
    // --- SETTERS Y GETTERS ---
    public void setAgentVisualPos(double row, double col){
        this.agentRow = row;
        this.agentCol = col;
        repaint();
    }
    
    public void setGoalVisualPos(double row, double col){
        this.goalRow = row;
        this.goalCol = col;
        repaint();
    }
    
    public void setShowVisited(boolean show) {
        this.showVisited = show;
        repaint();
    }

    public void setShowMemory(boolean show) {
        this.showMemory = show;
        repaint();
    }
    
    public void toggleShowMemory(){ 
        this.showMemory = !this.showMemory; 
        repaint(); 
    }
    
    public void toggleShowVisited(){ 
        this.showVisited = !this.showVisited; 
        repaint(); 
    }
    
    public int getCellSize() { return cellSize; }
    public double getAgentDoubleRow() { return agentRow; }
    public double getAgentDoubleCol() { return agentCol; }
    public double getGoalDoubleRow() { return goalRow; }
    public double getGoalDoubleCol() { return goalCol; }
}