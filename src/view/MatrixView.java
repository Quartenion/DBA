package view;

import java.awt.*;
import java.util.Set;
import javax.swing.JPanel;
import model.WorldViewInterface;
import model.Environment;


// Panel grafico que renderiza la cuadricula, el agente, el objetivo y las migajas

public class MatrixView extends JPanel {
    
    private final WorldViewInterface matrix;
    private final Environment model;
    
    private final int panelSize = 800; 
    private int cellSize;
    
    // Flags de visualizacion
    private boolean showVisited = false;
    private boolean showMemory  = false;
    
    // Posiciones para movimiento suave
    private double agentRow, agentCol;
    private double goalRow, goalCol;
    
    // Posiciones de renos y Santa (solo para visualización - Controller las configura)
    private java.util.ArrayList<controller.Pair<Integer, Integer>> reindeerPositions = null;
    private controller.Pair<Integer, Integer> santaPosition = null;

    public MatrixView(WorldViewInterface matrix, Environment model) {
        this.matrix = matrix;
        this.model = model;
        
        setPreferredSize(new Dimension(panelSize, panelSize));
        setBackground(Color.WHITE); 
        
        // Inicialización
        updateCellSize();
    }
    
    
    // Recalcula el tamaño de la celda basandose en el tamaño actual del panel
    private void updateCellSize() {
        int rows = this.matrix.getRows();
        int cols = this.matrix.getCols();
        if (rows > 0 && cols > 0) {
             this.cellSize = Math.min(getWidth() / cols, getHeight() / rows);
             if (this.cellSize < 1) this.cellSize = 1; 
        }
    }


     // Calcula el color del mapa de calor basado en el numero de visitas
     // Interpola entre amarillo (pocas visitas) y rojo (muchas visitas)
    private Color getHeatmapColor(int visits) {
        int maxVisits = 10; // Umbral para el rojo maximo
        
        if (visits <= 1) {
            return new Color(255, 255, 180); 
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

        Set<String> visitedCells = showVisited ? model.getVisitedCells() : null;
        Set<String> seenCells    = showMemory ? model.getSeenCells() : null;
        
        // 1. DIBUJAR CELDAS Y CAPAS DE INFORMACIÓN ----------------------------
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int tileType = matrix.getTile(r, c);
                int pixelX = c * cellSize;
                int pixelY = r * cellSize;

                if (tileType == -1) { // Muro
                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(pixelX, pixelY, cellSize, cellSize);
                } else {
                    drawInformationOverlay(g2d, r, c, pixelX, pixelY, visitedCells, seenCells);
                }
            }
        }

        // 2. DIBUJAR GRID (REJILLA) -------------------------------------------
        drawGrid(g2d, rows, cols);

        // 3. DIBUJAR RENOS Y SANTA (P3) - solo visualización ----------------
        drawReindeerAndSanta(g2d);

        // 4. DIBUJAR ELEMENTOS DINÁMICOS (Agente y Objetivo) ------------------
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //Mas nitido
        drawEntities(g2d);
    }
    
    
    // Dibuja las capas de visitados (migajas) o memoria (seen)
    
    private void drawInformationOverlay(Graphics2D g2d, int r, int c, int px, int py, Set<String> visited, Set<String> seen) {
        String cellKey = r + "," + c;
        
        if ((showVisited || showMemory)) {
            // PRIORIDAD 1: VISITADAS (HEATMAP + NUMERO)
            if (showVisited && visited != null && visited.contains(cellKey)) {
                int visits = model.getVisitCount(r, c);
                
                // A. Fondo de color
                g2d.setColor(getHeatmapColor(visits));
                g2d.fillRect(px, py, cellSize, cellSize);
                
                // B. Dibujar numero (Solo si la celda es suficientemente grande)
                if (cellSize > 15) { 
                    g2d.setColor(Color.BLACK);
                    int fontSize = Math.max(10, (int)(cellSize * 0.5));
                    g2d.setFont(new Font("SansSerif", Font.BOLD, fontSize));
                    
                    String text = String.valueOf(visits);
                    FontMetrics fm = g2d.getFontMetrics();
                    
                    // Centrar texto
                    int textX = px + (cellSize - fm.stringWidth(text)) / 2;
                    int textY = py + (cellSize - fm.getHeight()) / 2 + fm.getAscent();
                    
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //Mas nitido
                    g2d.drawString(text, textX, textY);
                }
            } 
            // PRIORIDAD 2: MEMORIA (Solo fondo azul claro)
            else if (showMemory && seen != null && seen.contains(cellKey)) {
                g2d.setColor(new Color(220, 240, 255)); 
                g2d.fillRect(px, py, cellSize, cellSize);
            }
        }
    }
    
    private void drawGrid(Graphics2D g2d, int rows, int cols) {
        g2d.setColor(new Color(230, 230, 230)); 
        int mapHeight = rows * cellSize;
        int mapWidth = cols * cellSize;
        
        // Líneas verticales
        for (int c = 0; c <= cols; c++) {
            int x = c * cellSize;
            g2d.drawLine(x, 0, x, mapHeight);
        }
        // Líneas horizontales
        for (int r = 0; r <= rows; r++) {
            int y = r * cellSize;
            g2d.drawLine(0, y, mapWidth, y);
        }
    }
    
    private void drawReindeerAndSanta(Graphics2D g2d) {
        // Dibujar renos (Marrón)
        if (reindeerPositions != null && !reindeerPositions.isEmpty()) {
            g2d.setColor(new Color(139, 69, 19)); // Marrón
            for (controller.Pair<Integer, Integer> pos : reindeerPositions) {
                int row = pos.first;
                int col = pos.second;
                
                int pixelX = col * cellSize;
                int pixelY = row * cellSize;
                
                // Dibujar rectángulo marrón
                g2d.fillRect(pixelX + 2, pixelY + 2, cellSize - 4, cellSize - 4);
                
                // Dibujar emoji 🦌 si el tamaño lo permite
                if (cellSize > 20) {
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("SansSerif", Font.BOLD, (int)(cellSize * 0.6)));
                    FontMetrics fm = g2d.getFontMetrics();
                    String emoji = "🦌";
                    int textX = pixelX + (cellSize - fm.stringWidth(emoji)) / 2;
                    int textY = pixelY + (cellSize - fm.getHeight()) / 2 + fm.getAscent();
                    g2d.drawString(emoji, textX, textY);
                }
            }
        }
        
        // Dibujar Santa (Rojo con emoji)
        if (santaPosition != null) {
            int row = santaPosition.first;
            int col = santaPosition.second;
            
            int pixelX = col * cellSize;
            int pixelY = row * cellSize;
            
            // Dibujar rectángulo rojo
            g2d.setColor(new Color(220, 20, 60)); // Rojo
            g2d.fillRect(pixelX + 2, pixelY + 2, cellSize - 4, cellSize - 4);
            
            // Dibujar emoji 🎅 si el tamaño lo permite
            if (cellSize > 20) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.BOLD, (int)(cellSize * 0.6)));
                FontMetrics fm = g2d.getFontMetrics();
                String emoji = "🎅";
                int textX = pixelX + (cellSize - fm.stringWidth(emoji)) / 2;
                int textY = pixelY + (cellSize - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(emoji, textX, textY);
            }
        }
    }
    
    private void drawEntities(Graphics2D g2d) {
        // Objetivo (Verde)
        g2d.setColor(new Color(0, 200, 0));
        int goalPixelX = (int)(goalCol * cellSize); 
        int goalPixelY = (int)(goalRow * cellSize); 
        g2d.fillRect(goalPixelX + 1, goalPixelY + 1, cellSize - 2, cellSize - 2); 

        // Agente (Azul - Círculo)
        g2d.setColor(Color.BLUE);
        double agentSize = Math.max(2, cellSize * 0.8); 
        double offset = (cellSize - agentSize) / 2.0;
        
        int agentPixelX = (int)(agentCol * cellSize + offset);
        int agentPixelY = (int)(agentRow * cellSize + offset);
        
        g2d.fillOval(agentPixelX, agentPixelY, (int)agentSize, (int)agentSize);
    }
    
    // -------------------------------------------------------------------------
    // GETTERS Y SETTERS 
    // -------------------------------------------------------------------------
    
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
    
    public void toggleShowMemory() { 
        this.showMemory = !this.showMemory; 
        repaint(); 
    }
    
    public void toggleShowVisited() { 
        this.showVisited = !this.showVisited; 
        repaint(); 
    }
    
    public void setReindeerPositions(java.util.ArrayList<controller.Pair<Integer, Integer>> positions) {
        this.reindeerPositions = positions;
        repaint();
    }
    
    public void setSantaPosition(controller.Pair<Integer, Integer> position) {
        this.santaPosition = position;
        repaint();
    }
    
    public int getCellSize() { return cellSize; }
    public double getAgentDoubleRow() { return agentRow; }
    public double getAgentDoubleCol() { return agentCol; }
    public double getGoalDoubleRow() { return goalRow; }
    public double getGoalDoubleCol() { return goalCol; }
}