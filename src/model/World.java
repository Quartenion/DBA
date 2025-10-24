package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class World {
    private int[][] map; 

    public void read_map(String rutaArchivo) {
    try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
        int rows = Integer.parseInt(br.readLine().trim());
        int columns = Integer.parseInt(br.readLine().trim());

        map = new int[rows][columns];

        for (int i = 0; i < rows; i++) {
            String line = br.readLine();
            String[] value = line.trim().split("\\t");
            for (int j = 0; j < columns; j++) {
                map[i][j] = Integer.parseInt(value[j]);
            }
        }

    } catch (IOException | NumberFormatException e) {
        System.out.println("Error reading the file: " + e.getMessage());
    }
}


    public int[][] getMap() {
        return map;
    }
}