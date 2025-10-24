package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Panel lateral que muestra información del agente y controles.
 * Incluye:
 * - Nº de acciones realizadas
 * - Posición x,y del agente
 * - Posición x,y del objetivo
 * - Selector de mapas
 * - Boolean objetivo alcanzado
 * - Toggle buttons para mostrar diferentes vistas
 * - Log de últimas 5 acciones
 */
public class infoPanelView extends JPanel {

    // Labels
    private JLabel actionsLabel;
    private JLabel agentPosLabel;
    private JLabel goalPosLabel;
    private JLabel goalReachedLabel;

    // Selector de mapa
    private JComboBox<String> mapSelector;

    // Toggle buttons de vistas
    private JToggleButton showVisitedBtn;
    private JToggleButton showMemoryBtn;

    // Log de acciones
    private DefaultListModel<String> logModel;
    private JList<String> logList;

    public infoPanelView() {
        setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(300, 600));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Panel Info"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        gbc.gridx = 0;

        // Sección de estado del agente
        JPanel statusPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Estado Agente"));
        statusPanel.setBackground(new Color(235, 235, 250));

        actionsLabel = new JLabel("Acciones: 0");
        agentPosLabel = new JLabel("Agente: (0,0)");
        goalPosLabel = new JLabel("Objetivo: (0,0)");
        goalReachedLabel = new JLabel("Objetivo alcanzado: false");

        statusPanel.add(actionsLabel);
        statusPanel.add(agentPosLabel);
        statusPanel.add(goalPosLabel);
        statusPanel.add(goalReachedLabel);

        gbc.gridy = 0;
        add(statusPanel, gbc);

        // Selector de mapa
        JPanel mapPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        mapPanel.setBorder(BorderFactory.createTitledBorder("Mapa"));
        mapPanel.setBackground(new Color(235, 245, 255));

        mapSelector = new JComboBox<>();
        mapSelector.setPreferredSize(new Dimension(200, 25));
        mapPanel.add(new JLabel("Seleccionar mapa:"));
        mapPanel.add(mapSelector);

        gbc.gridy++;
        add(mapPanel, gbc);

        // Toggle buttons de vistas
        JPanel viewsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        viewsPanel.setBorder(BorderFactory.createTitledBorder("Vistas"));
        viewsPanel.setBackground(new Color(245, 255, 245));

        showVisitedBtn = new JToggleButton("Mostrar visitadas");
        showMemoryBtn = new JToggleButton("Mostrar memoria");

        viewsPanel.add(showVisitedBtn);
        viewsPanel.add(showMemoryBtn);

        gbc.gridy++;
        add(viewsPanel, gbc);

        // Log de últimas 5 acciones
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Últimas acciones"));
        logPanel.setBackground(new Color(255, 245, 245));

        logModel = new DefaultListModel<>();
        logList = new JList<>(logModel);
        logList.setVisibleRowCount(5);
        logList.setBackground(new Color(255, 250, 240));
        logList.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(logList);
        scrollPane.setPreferredSize(new Dimension(250, 100));

        logPanel.add(scrollPane, BorderLayout.CENTER);

        gbc.gridy++;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        add(logPanel, gbc);
        
        setGoalReached(false);
    }

    // ---------------------------
    // Métodos para actualizar info
    // ---------------------------

    public void setActionsCount(int count) {
        actionsLabel.setText("Acciones: " + count);
    }

    public void setAgentPosition(int x, int y) {
        agentPosLabel.setText("Agente: (" + x + "," + y + ")");
    }

    public void setGoalPosition(int x, int y) {
        goalPosLabel.setText("Objetivo: (" + x + "," + y + ")");
    }

    public void setGoalReached(boolean reached) {
        goalReachedLabel.setText("Objetivo alcanzado: " + reached);
        goalReachedLabel.setForeground(reached ? Color.GREEN.darker() : Color.RED.darker());
    }

    public void setMapList(String[] maps) {
        mapSelector.removeAllItems();
        for (String m : maps) {
            mapSelector.addItem(m);
        }
    }

    public void addMapSelectorListener(ActionListener listener) {
        mapSelector.addActionListener(listener);
    }

    public void addLogEntry(String entry) {
        logModel.add(0, entry); // añadimos arriba
        if (logModel.size() > 5) {
            logModel.remove(logModel.size() - 1); // mantenemos solo 5
        }
    }

    public void addShowVisitedListener(ActionListener listener) {
        showVisitedBtn.addActionListener(listener);
    }

    public void addShowMemoryListener(ActionListener listener) {
        showMemoryBtn.addActionListener(listener);
    }

    public boolean isShowVisitedSelected() {
        return showVisitedBtn.isSelected();
    }

    public boolean isShowMemorySelected() {
        return showMemoryBtn.isSelected();
    }

    public String getSelectedMap() {
        return (String) mapSelector.getSelectedItem();
    }
}