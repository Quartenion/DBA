package view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JPanel;
import model.Environment;
import model.World;
import java.util.Set;

public class matrixView extends JPanel {
    
    private World world;
    private Environment model;
    private int panelSize = 800;
    private int cellSize;

    // Solo para renderizado "flotante"
    private double agentFloatX, agentFloatY;
    private double goalFloatX, goalFloatY;

    private boolean isRunning = false;
    private boolean draggingAgent = false;
    private boolean draggingGoal = false;
    
    // Referencia a infoPanelView para leer estado de toggles
    private infoPanelView infoPanel;
    
    // Listeners: notifican coordenadas propuestas
    private java.util.function.BiConsumer<Integer, Integer> agentPositionListener;
    private java.util.function.BiConsumer<Integer, Integer> goalPositionListener;
    private Runnable positionChangedListener; // para repaint/log
    private java.util.function.Consumer<String> logListener;

    public matrixView(World world, Environment model) {
        this.world = world;
        this.model = model;
        syncFloatPositions();
        setPreferredSize(new Dimension(panelSize, panelSize));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isRunning) return;
                int mouseX = e.getX(), mouseY = e.getY();
                int margin = cellSize / 4;
                int radius = (cellSize - margin) / 2;

                int agentCenterX = (int)(agentFloatY * cellSize + cellSize/2);
                int agentCenterY = (int)(agentFloatX * cellSize + cellSize/2);
                if ((mouseX - agentCenterX)*(mouseX - agentCenterX) + 
                    (mouseY - agentCenterY)*(mouseY - agentCenterY) <= radius*radius) {
                    draggingAgent = true;
                    return;
                }

                int goalCenterX = (int)(goalFloatY * cellSize + cellSize/2);
                int goalCenterY = (int)(goalFloatX * cellSize + cellSize/2);
                if ((mouseX - goalCenterX)*(mouseX - goalCenterX) + 
                    (mouseY - goalCenterY)*(mouseY - goalCenterY) <= radius*radius) {
                    draggingGoal = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingAgent) {
                    int col = (int)(agentFloatY + 0.5);
                    int row = (int)(agentFloatX + 0.5);
                    if (agentPositionListener != null) {
                        agentPositionListener.accept(row, col);
                    }
                } else if (draggingGoal) {
                    int col = (int)(goalFloatY + 0.5);
                    int row = (int)(goalFloatX + 0.5);
                    if (goalPositionListener != null) {
                        goalPositionListener.accept(row, col);
                    }
                }
                draggingAgent = false;
                draggingGoal = false;
                setCursor(Cursor.getDefaultCursor());
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingAgent) {
                    agentFloatX = (double)e.getY() / cellSize - 0.5;
                    agentFloatY = (double)e.getX() / cellSize - 0.5;
                    repaint();
                } else if (draggingGoal) {
                    goalFloatX = (double)e.getY() / cellSize - 0.5;
                    goalFloatY = (double)e.getX() / cellSize - 0.5;
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (isRunning) return;
                int mouseX = e.getX(), mouseY = e.getY();
                int margin = cellSize / 4;
                int radius = (cellSize - margin) / 2;

                int agentCenterX = (int)(agentFloatY * cellSize + cellSize/2);
                int agentCenterY = (int)(agentFloatX * cellSize + cellSize/2);
                int goalCenterX = (int)(goalFloatY * cellSize + cellSize/2);
                int goalCenterY = (int)(goalFloatX * cellSize + cellSize/2);

                boolean overAgent = (mouseX - agentCenterX)*(mouseX - agentCenterX) + 
                                    (mouseY - agentCenterY)*(mouseY - agentCenterY) <= radius*radius;
                boolean overGoal = (mouseX - goalCenterX)*(mouseX - goalCenterX) + 
                                   (mouseY - goalCenterY)*(mouseY - goalCenterY) <= radius*radius;

                setCursor(overAgent || overGoal ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) 
                                                : Cursor.getDefaultCursor());
            }
        });
    }
    
    public void initMatrix(){
        syncFloatPositions();
        this.cellSize = Math.min(panelSize / world.getRows(), panelSize / world.getCols());
        repaint();
    }

    public void syncWithModel() {
        syncFloatPositions();
        if (positionChangedListener != null) positionChangedListener.run();
        repaint();
    }

    private void syncFloatPositions() {
        this.agentFloatX = model.getAgentX();
        this.agentFloatY = model.getAgentY();
        this.goalFloatX = model.getGoalX();
        this.goalFloatY = model.getGoalY();
    }

    // Setters para listeners
    
    public void setAgentPositionListener(java.util.function.BiConsumer<Integer, Integer> listener) {
        this.agentPositionListener = listener;
    }

    public void setGoalPositionListener(java.util.function.BiConsumer<Integer, Integer> listener) {
        this.goalPositionListener = listener;
    }

    public void setPositionChangedListener(Runnable listener) {
        this.positionChangedListener = listener;
    }

    public void setLogListener(java.util.function.Consumer<String> listener) {
        this.logListener = listener;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }
    
    // Método para conectar con el infoPanel
    public void setInfoPanel(infoPanelView infoPanel) {
        this.infoPanel = infoPanel;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (world.empty()) return;

        int rows = world.getRows();
        int cols = world.getCols();
        this.cellSize = Math.min(panelSize / rows, panelSize / cols);
        
        // Obtener memoria del modelo
        Set<String> visitedCells = model.getVisitedCells();
        Set<String> seenCells = model.getSeenCells();
        
        // Comprobar estado de los toggles
        boolean showVisited = infoPanel != null && infoPanel.isShowVisitedSelected();
        boolean showMemory = infoPanel != null && infoPanel.isShowMemorySelected();

        // Dibujar el mapa
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                String cellKey = i + "," + j;
                Color color;
                
                // Prioridad de colores:
                // 1. Obstáculos (negro)
                // 2. Visitadas (amarillo claro) - si el toggle está activo
                // 3. Vistas (azul muy claro) - si el toggle está activo
                // 4. No exploradas (blanco)
                
                if (world.getTile(i, j) == -1) {
                    color = Color.BLACK;
                } else if (showVisited && visitedCells.contains(cellKey)) {
                    // Amarillo claro para celdas visitadas
                    color = new Color(255, 255, 150);
                } else if (showMemory && seenCells.contains(cellKey)) {
                    // Azul muy claro para celdas vistas
                    color = new Color(200, 220, 255);
                } else {
                    // Blanco para celdas no exploradas
                    color = Color.WHITE;
                }
                
                g.setColor(color);
                g.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);
                g.setColor(Color.BLACK);
                g.drawRect(j * cellSize, i * cellSize, cellSize, cellSize);
            }
        }

        // ✅ Dibujar OBJETIVO desde posición flotante (para arrastre suave)
        g.setColor(Color.GREEN);
        g.fillRect((int)(goalFloatY * cellSize), (int)(goalFloatX * cellSize), cellSize, cellSize);

        // ✅ Dibujar AGENTE desde posición flotante (para arrastre suave)
        g.setColor(Color.BLUE);
        int margin = cellSize / 4;
        g.fillOval((int)(agentFloatY * cellSize + margin/2), 
                   (int)(agentFloatX * cellSize + margin/2), 
                   cellSize - margin, cellSize - margin);
    }
}