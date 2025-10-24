package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class World {
    
    private int[][] map;
    private int rows;
    private int columns;
    
    // Estado del mundo: posiciones del agente y objetivo
    private int agentX;
    private int agentY;
    private int goalX;
    private int goalY;

    public void read_map(String rutaArchivo) {
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            setRows(Integer.parseInt(br.readLine().trim()));
            setColumns(Integer.parseInt(br.readLine().trim()));

            int[][] newMap = new int[getRows()][getColumns()];
            for (int i = 0; i < getRows(); i++) {
                String linea = br.readLine();
                String[] valores = linea.trim().split("\\t");
                for (int j = 0; j < getColumns(); j++) {
                    newMap[i][j] = Integer.parseInt(valores[j]);
                }
            }
            setMap(newMap);
            initializeDefaultPositions();
        } 
        catch (IOException | NumberFormatException e) {
            System.out.println("Error reading the file: " + e.getMessage());
        }
    }

    private void initializeDefaultPositions() {
        this.agentX = rows - 1; // esquina inferior derecha
        this.agentY = columns - 1;
        this.goalX = 0;         // esquina superior izquierda
        this.goalY = 0;
    }

    // Getters y setters para posiciones
    public int getAgentX() { return agentX; }
    public int getAgentY() { return agentY; }
    public void setAgentPosition(int x, int y) {
        if (x >= 0 && x < rows && y >= 0 && y < columns && getTile(x, y) != -1) {
            this.agentX = x;
            this.agentY = y;
        }
    }

    public int getGoalX() { return goalX; }
    public int getGoalY() { return goalY; }
    public void setGoalPosition(int x, int y) {
        if (x >= 0 && x < rows && y >= 0 && y < columns && getTile(x, y) != -1) {
            this.goalX = x;
            this.goalY = y;
        }
    }

    // Lógica de negocio en el modelo
    public boolean isGoalReached() {
        return agentX == goalX && agentY == goalY;
    }

    // Métodos existentes (sin cambios)
    public int getRows() { return this.rows; }
    public int getColumns() { return this.columns; }
    public int[][] getMap() { return map; }
    public int getTile(int i, int j) { return this.map[i][j]; }
    public void setRows(int rows) { this.rows = rows; }
    public void setColumns(int columns) { this.columns = columns; }
    public void setMap(int[][] map) { this.map = map; }
    public boolean empty() { return (this.map == null || this.map.length == 0); }
}