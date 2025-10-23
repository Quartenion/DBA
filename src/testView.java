import javax.swing.*;
import java.awt.*;

public class testView extends JPanel {
    private int filas;
    private int columnas;
    private int tamanoCelda;

    public testView(int filas, int columnas, int tamanoCelda) {
        this.filas = filas;
        this.columnas = columnas;
        this.tamanoCelda = tamanoCelda;
        setPreferredSize(new Dimension(columnas * tamanoCelda, filas * tamanoCelda));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Dibujar rejilla
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                int x = j * tamanoCelda;
                int y = i * tamanoCelda;
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(x, y, tamanoCelda, tamanoCelda);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, tamanoCelda, tamanoCelda);
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Matriz de Celdas");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        testView panel = new testView(10, 10, 40);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
