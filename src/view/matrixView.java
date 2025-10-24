package view;

import java.awt.Dimension;
import java.awt.GridLayout;
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
}
