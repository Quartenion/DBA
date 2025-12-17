package controller;

import view.MainView;

public class ViewLogger {
    MainView view;
    
    public ViewLogger(MainView view) {
        this.view = view;
    }
    
    public synchronized void addLogEntry(String log) {
        this.view.addLogEntry(log);
    }
    
    //funcion synchronized: limpia el log
    public synchronized void clearLogEntry() {
        this.view.clearLogEntry();
    }
}
