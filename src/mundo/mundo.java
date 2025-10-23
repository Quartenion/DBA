package mundo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class mundo {
    private int[][] map;
    private int rows;
    private int columns;

    public void leer_mapa(String rutaArchivo) {
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

    } catch (IOException | NumberFormatException e) {
        System.out.println("Error al leer el archivo: " + e.getMessage());
      }
    }
    
    public int getRows(){
        return this.rows;
    }
    
    public int getColumns(){
        return this.columns;
    }
    
    public int[][] getMap() {
        return map;
    }
    
    public void setRows(int rows){
        this.rows = rows;
    }
    public void setColumns(int columns){
        this.columns = columns;
    }
    public void setMap(int[][] map){
        this.map = map;
    }
}