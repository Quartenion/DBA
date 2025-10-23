import mundo.mundo;

public class Main {
    public static void main(String[] args) {
        mundo m = new mundo();
        String rutaMapa = "/Users/juanra/Downloads/maps/mapWithComplexObstacle1.txt";
        m.leer_mapa(rutaMapa);

        int[][] mapa = m.getMapa();
        if (mapa != null) {
            for (int i = 0; i < mapa.length; i++) {
                for (int j = 0; j < mapa[i].length; j++) {
                    System.out.print(mapa[i][j] + "\t");
                }
                System.out.println();
            }
        } else {
            System.out.println("No se pudo leer el mapa.");
        }
    }
}