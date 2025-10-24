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
    private final int cellSize = 40; // Tamaño de cada celda en píxeles
    
    
    public matrixView(World world){
        
        this.world = world;
        
        setLayout(new GridLayout(world.getRows(), world.getColumns()));
        setPreferredSize(new Dimension(
            world.getMap()[0].length * cellSize,
            world.getMap().length * cellSize
        ));
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
        
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                // Colo tile
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