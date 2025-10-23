package mundo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class mundo {
    private int[][] mapa; 

    public void leer_mapa(String rutaArchivo) {
    try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
        int filas = Integer.parseInt(br.readLine().trim());
        int columnas = Integer.parseInt(br.readLine().trim());

        mapa = new int[filas][columnas];

        for (int i = 0; i < filas; i++) {
            String linea = br.readLine();
            String[] valores = linea.trim().split("\\t");
            for (int j = 0; j < columnas; j++) {
                mapa[i][j] = Integer.parseInt(valores[j]);
            }
        }

    } catch (IOException | NumberFormatException e) {
        System.out.println("Error al leer el archivo: " + e.getMessage());
    }
}


    public int[][] getMapa() {
        return mapa;
    }
}