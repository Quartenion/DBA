package view;

/**
 *
 * @author Group_7_DBA
 */


import javax.swing.*;
import java.awt.*;

import model.AgentP2;
import model.AgentModel;


public class mainView extends JFrame{
    
    // Model
    private AgentModel model;
    
    // View elements (we can create our own classes)
    private JLabel labelContador;
    private JButton btnIncrementar;
    private JButton btnReiniciar;
    
    /**
     * Constructor of the main view
     * Sets the main configuration of the main JFrame
     * Adds the other views (matrix of the field and the data of the agent)
     */
    public mainView(AgentModel model){
        
        // Save the model
        this.model = model;
        
        
        setTitle("Prueba de vista"); // Title
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout()); // Layout to order things
        
        initComponents();
        
        
        
        pack(); // Sets the size needed for the containing elements
        setLocationRelativeTo(null); // Sets the frame in the center of the screen
    }
    
    private void initComponents() {
        labelContador = new JLabel("Contador: 0", SwingConstants.CENTER);
        labelContador.setFont(new Font("Monospaced", Font.BOLD, 24));

        btnIncrementar = new JButton("Incrementar");
        btnReiniciar = new JButton("Reiniciar");

        JPanel panelBotones = new JPanel(new FlowLayout());
        panelBotones.add(btnIncrementar);
        panelBotones.add(btnReiniciar);

        add(labelContador, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);
    }
    
    //---------------Getters and Setters for the controller---------------------
    
    public JButton getBtnIncrementar()  { return btnIncrementar;    }
    public JButton getBtnReiniciar()    { return btnReiniciar;      }

        
    public void setContador(int valor) {
        labelContador.setText("Contador: " + valor);
    }
}
