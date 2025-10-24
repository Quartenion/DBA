package controller;

/**
 *
 * @author Group_7_DBA
 */


import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import javax.swing.*;
import model.AgentP2;
import model.AgentMemory;
import view.mainView;

public class Controller {
    
    private mainView view;
    private AgentMemory model;
    private ContainerController cc;
    
    public Controller(mainView view, AgentMemory model, ContainerController cc){
        this.view = view;
        this.model = model;
        this.cc = cc;
        
        createEventListeners();
        
        /*
        // Here we establish the events listeners
        this.view.getBtnIncrementar().addActionListener(e -> {
            model.incrementar();
            view.setContador(model.getContador()); // ← El controlador actualiza la vista
        });

        this.view.getBtnReiniciar().addActionListener(e -> {
            model.reiniciar();
            view.setContador(model.getContador());
        });
        
        this.view.setContador(model.getContador());
        */
    }
    
    // Functions to control the app
    private void createEventListeners(){
        
        view.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // 1. Close JADE
                try {
                    if (cc != null) {
                        cc.kill(); // Kill the container and everything
                    }
                } catch (ControllerException ex) {
                    ex.printStackTrace();
                }

                // 2. Close APP
                System.exit(0); 
            }
        });
    }
}
