package mundo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class mundo {
    private int[][] mapa; 

    public void leer_mapa(String rutaArchivo) {
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            System.out.println("Leyendo mapa desde: " + rutaArchivo);

            // Leer las dimensiones del mapa
            int filas = Integer.parseInt(br.readLine().trim());
            int columnas = Integer.parseInt(br.readLine().trim());
            System.out.println("Dimensiones del mapa: " + filas + "x" + columnas);

            // Inicializar la matriz
            mapa = new int[filas][columnas];

            // Leer cada línea del mapa
            for (int i = 0; i < filas; i++) {
                String linea = br.readLine();
                if (linea == null) {
                    System.out.println("Archivo más corto de lo esperado en la fila " + i);
                    break;
                }
                System.out.println("Línea leída: " + linea);

                String[] valores = linea.trim().split("\\t"); // separar por tabulación
                for (int j = 0; j < columnas; j++) {
                    mapa[i][j] = Integer.parseInt(valores[j]);
                }
            }

            System.out.println("Mapa leído correctamente.");

        } catch (IOException e) {
            System.out.println("Error al leer el archivo: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Formato de número incorrecto en el archivo: " + e.getMessage());
        }
    }

    public int[][] getMapa() {
        return mapa;
    }
}