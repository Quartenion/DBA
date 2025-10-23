package controller;

/**
 *
 * @author Group_7_DBA
 */


import javax.swing.*;
import model.Example;
import view.mainView;

public class Controller {
    
    private mainView view;
    private Example model;
    
    public Controller(mainView view, Example model){
        this.view = view;
        this.model = model;
        
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
    }
    
    // Functions to control the app
    
}
