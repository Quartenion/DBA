package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;


 // Panel lateral derecho de la interfaz de usuario
 // Se encarga de:
 // Mostrar estadisticas en tiempo real (energia, posicion)
 // Proveer controles de simulacion (Selector de mapas, Botones de vista, Run)
 // Visualizar un log de eventos recientes

public class InfoPanelView extends JPanel {

    // Componentes de Estado ---------------------------------------------------
    private JLabel actionsLabel;
    private JLabel agentPosLabel;
    private JLabel goalPosLabel;
    private JLabel goalReachedLabel;

    // Componentes de Control --------------------------------------------------
    private JComboBox<String> mapSelector;
    private JToggleButton showVisitedBtn;
    private JToggleButton showMemoryBtn;
    private JToggleButton runBtn;

    // Componentes de Log ------------------------------------------------------
    private DefaultListModel<String> logModel;
    private JList<String> logList;

   
     // Constructor principal. Configura el layout y los estilos base
    public InfoPanelView() {
        setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(300, 600));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "Panel de Información"));

        initComponents();
        
        // Estado inicial
        setGoalReached(false);
    }

    
     // Inicializa y organiza todos los sub-paneles
    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // 1. Panel de Estado (Posiciones y Energia)
        gbc.gridy = 0;
        add(createStatusPanel(), gbc);

        // 2. Panel de Seleccion de Mapa
        gbc.gridy = 1;
        add(createMapPanel(), gbc);

        // 3. Panel de Botones de Vista y Control
        gbc.gridy = 2;
        add(createControlsPanel(), gbc);

        // 4. Panel de Logs (Ocupa el espacio restante vertical)
        gbc.gridy = 3;
        gbc.weighty = 1.0; // Empuja hacia abajo y rellena
        gbc.fill = GridBagConstraints.BOTH;
        add(createLogPanel(), gbc);
    }

    // -------------------------------------------------------------------------
    //  METODOS PRIVADOS DE CONSTRUCCION DE UI
    // -------------------------------------------------------------------------

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Estado del Agente"));
        panel.setBackground(new Color(235, 235, 250));

        actionsLabel = new JLabel("Acciones: 0");
        agentPosLabel = new JLabel("Agente: (0,0)");
        goalPosLabel = new JLabel("Objetivo: (0,0)");
        goalReachedLabel = new JLabel("Objetivo alcanzado: false");

        // Estilos de fuente base
        Font font = new Font("SansSerif", Font.PLAIN, 12);
        actionsLabel.setFont(font);
        agentPosLabel.setFont(font);
        goalPosLabel.setFont(font);
        goalReachedLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        panel.add(actionsLabel);
        panel.add(agentPosLabel);
        panel.add(goalPosLabel);
        panel.add(goalReachedLabel);

        return panel;
    }

    private JPanel createMapPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Configuracion de Mapa"));
        panel.setBackground(new Color(235, 245, 255));

        mapSelector = new JComboBox<>();
        mapSelector.setPreferredSize(new Dimension(200, 25));
        
        panel.add(new JLabel("Seleccionar escenario:"));
        panel.add(mapSelector);

        return panel;
    }

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Vistas y Control"));
        panel.setBackground(new Color(245, 255, 245));

        showVisitedBtn = new JToggleButton("Mostrar Visitadas");
        showMemoryBtn = new JToggleButton("Mostrar Memoria");
        runBtn = new JToggleButton("RUN / PAUSE");
        
        // Resaltar el boton de Run
        runBtn.setBackground(new Color(220, 220, 220));
        runBtn.setFont(new Font("SansSerif", Font.BOLD, 12));

        panel.add(showVisitedBtn);
        panel.add(showMemoryBtn);
        panel.add(runBtn);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Registro de Acciones"));
        panel.setBackground(new Color(255, 245, 245));

        logModel = new DefaultListModel<>();
        logList = new JList<>(logModel);
        logList.setVisibleRowCount(5);
        logList.setBackground(new Color(255, 250, 240));
        logList.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(logList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------------------
    // ACTUALIZACIÓN DE INFORMACION (Setters)
    // -------------------------------------------------------------------------

    public void setActionsCount(int count) {
        actionsLabel.setText("Energía consumida: " + count);
    }

    public void setAgentPosition(int x, int y) {
        agentPosLabel.setText(String.format("Agente: (%d, %d)", x, y));
    }

    public void setGoalPosition(int x, int y) {
        goalPosLabel.setText(String.format("Objetivo: (%d, %d)", x, y));
    }

    public void setGoalReached(boolean reached) {
        if (reached) {
            goalReachedLabel.setText("¡OBJETIVO ALCANZADO!");
            goalReachedLabel.setForeground(new Color(0, 150, 0)); // Verde oscuro
        } else {
            goalReachedLabel.setText("Buscando objetivo...");
            goalReachedLabel.setForeground(Color.DARK_GRAY);
        }
    }

    // Añade una entrada al log, manteniendo un maximo de 20 registros
    public void addLogEntry(String entry) {
        logModel.add(0, entry); // Añadir al principio
        if (logModel.size() > 20) {
            logModel.remove(logModel.size() - 1); // Eliminar el mas antiguo
        }
        // Asegurar que se vea el ultimo elemento añadido (arriba)
        logList.ensureIndexIsVisible(0);
    }
    
    public void setMapList(String[] maps) {
        mapSelector.removeAllItems();
        for (String m : maps) {
            mapSelector.addItem(m);
        }
    }

    // -------------------------------------------------------------------------
    //  GESTIÓN DE LISTENERS Y ESTADO
    // -------------------------------------------------------------------------

    public void addMapSelectorListener(ActionListener listener) {
        mapSelector.addActionListener(listener);
    }

    public void addShowVisitedListener(ActionListener listener) {
        showVisitedBtn.addActionListener(listener);
    }

    public void addShowMemoryListener(ActionListener listener) {
        showMemoryBtn.addActionListener(listener);
    }
    
    public void addRunListener(ActionListener listener){
        runBtn.addActionListener(listener);
    }

    // Getters de estado de la UI ----------------------------------------------

    public String getSelectedMap() {
        return (String) mapSelector.getSelectedItem();
    }
    
    public boolean isShowVisitedSelected() {
        return showVisitedBtn.isSelected();
    }

    public boolean isShowMemorySelected() {
        return showMemoryBtn.isSelected();
    }

    // Setters de estado de la UI (para resetear vistas) -----------------------
    
    public void setRunButtonSelected(boolean selected) {
        runBtn.setSelected(selected);
        runBtn.setText(selected ? "PAUSE" : "RUN");
    }
    
    public void setShowVisitedSelected(boolean selected) {
        showVisitedBtn.setSelected(selected);
    }

    public void setShowMemorySelected(boolean selected) {
        showMemoryBtn.setSelected(selected);
    }
    
    public void setMapSelectorEnabled(boolean enabled) {
        mapSelector.setEnabled(enabled);
    }
}