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
    private JLabel reindeerProgressLabel;

    // Componentes de Control --------------------------------------------------
    private JComboBox<String> mapSelector;
    private JToggleButton showVisitedBtn;
    private JToggleButton showMemoryBtn;
    private JToggleButton runBtn;

    // Componentes de Log ------------------------------------------------------
    private JTextArea logArea;

   
     // Constructor principal. Configura el layout y los estilos base
    public InfoPanelView() {
        setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(600, 600));
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
        reindeerProgressLabel = new JLabel("Renos:  0/0");

        // Estilos de fuente base
        Font font = new Font("SansSerif", Font.PLAIN, 12);
        actionsLabel.setFont(font);
        agentPosLabel.setFont(font);
        goalPosLabel.setFont(font);
        goalReachedLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        reindeerProgressLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        panel.add(actionsLabel);
        panel.add(agentPosLabel);
        panel.add(goalPosLabel);
        panel.add(goalReachedLabel);
        panel.add(reindeerProgressLabel);

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

        // 1. CAMBIO: Usamos JTextArea en lugar de JList
        logArea = new JTextArea();

        // 2. CONFIGURACIÓN CLAVE para que el texto baje y no se corte
        logArea.setLineWrap(true);        // Activa el ajuste de línea automático
        logArea.setWrapStyleWord(true);   // Ajusta por palabras completas (no corta a mitad de palabra)
        logArea.setEditable(false);       // Importante: que el usuario no pueda borrar el log

        // 3. Estilos (Los mismos que tenías)
        logArea.setBackground(new Color(255, 250, 240));
        logArea.setFont(new Font("Arial", Font.PLAIN, 14));

        // Margen interno para que el texto no pegue con el borde
        logArea.setMargin(new java.awt.Insets(5, 5, 5, 5)); 

        JScrollPane scrollPane = new JScrollPane(logArea);
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
    public void addLogEntry(String mensaje) {
        // insert pone el texto en la posición 0 (el principio)
        logArea.insert(mensaje + "\n--------------------\n", 0);

        // Mover el scroll al principio
        logArea.setCaretPosition(0);
    }
    
    public void clearLogEntry() {
        logArea.setText("");
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
    
    public void setReindeerProgress(int found, int total) {
        if (total > 0) {
            reindeerProgressLabel.setText("🦌 Renos:  " + found + "/" + total);
            reindeerProgressLabel.setVisible(true);
        } else {
            reindeerProgressLabel.setVisible(false);
        }
    }
}