package view;

import java.awt.*;
import javax.swing.JPanel;
import model.World;

/**
 *
 * @author adam
 * 
 * This view represents the matrix of the world
 * 
 */
public class matrixView extends JPanel{
    
    private World world;
    private int panelSize = 800; // Tamaño total fijo del panel (en píxeles)
    private int cellSize; // Tamaño de cada celda en píxeles
    
    public matrixView(World world){
        
        this.world = world;
        
        // Calculamos el tamaño de la celda según el número de filas y columnas
        int rows = world.getRows();
        int cols = world.getColumns();
        this.cellSize = Math.min(panelSize / rows, panelSize / cols);

        // Mantenemos el panel cuadrado, del tamaño fijo
        setLayout(new GridLayout(world.getRows(), world.getColumns()));
        setPreferredSize(new Dimension(panelSize, panelSize));
    }
            
    // Repaint method (clear of the map)
    public void setMatriz(int[][] newMatrix) {
        this.world.setMap(newMatrix);
        repaint(); // Solicita redibujar
    }    
    
    /**
     * 
     * @param g 
     * 
     * This function draws in the panel of matrix, the layout is grid so it paints
     * with the correct size calculated to have one cell for each number of the
     * matrix.
     */
    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        if (this.world.empty()) return;
        
        int rows = this.world.getRows();
        int columns = this.world.getColumns();
        
        // Volvemos a ajustar por si cambia el tamaño de la matriz
        this.cellSize = Math.min(panelSize / rows, panelSize / columns);
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                // Color del tile
                Color color = getColorForValue(this.world.getTile(i,j));
                g.setColor(color);
                g.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);

                // Stroke
                g.setColor(Color.BLACK);
                g.drawRect(j * cellSize, i * cellSize, cellSize, cellSize);
            }
        }       
    }

    private Color getColorForValue(int tile) {
        Color color = Color.GRAY;
        
        switch (tile) {
            case  0 -> color = Color.WHITE;
            case -1 -> color = Color.RED;
        }
        
        return color;
    }   
}
