/**
 *
 * @author Group_7_DBA
 */

import view.mainView;
import controller.Controller;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import model.AgentP2;
import model.AgentMemory;
import model.World;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Launcher {

    private static final String MAIN_CONTAINER = "dba_main";

    public static void main(String[] args) {

        /* De esta forma obtenemos la instancia de ejecucion de jade para 
           crear nuestros propios contenedores y agentes */
        jade.core.Runtime rt = jade.core.Runtime.instance();

        // El profile es necesario para crear el contenedor
        Profile p  = new ProfileImpl();
        p.setParameter(Profile.CONTAINER_NAME, MAIN_CONTAINER);
        p.setParameter(Profile.MAIN, "true");

        // Creamos el controlador de Main container
        ContainerController cc = rt.createMainContainer(p);

        // -------------------------
        // INTERFAZ GRÁFICA
        // -------------------------
        java.awt.EventQueue.invokeLater(() -> {

            // Lectura del mapa
            World world = new World();
            world.read_map("./maps/mapWithoutObstacle.txt");

            // model para la memoria del agente
            AgentMemory model = new AgentMemory();

            // view
            mainView view = new mainView(model, world);

            // controller
            Controller controller = new Controller(view, world);

            // Mostrar ventana
            view.setVisible(true);

            // -------------------------
            // LANZAMOS EL AGENTE
            // -------------------------
            try {
                // Crear y lanzar el agente
                AgentController acHW = cc.createNewAgent("Agente", AgentP2.class.getCanonicalName(), null);
                acHW.start();

            } catch (StaleProxyException ex) {
                System.getLogger(Launcher.class.getName()).log(System.Logger.Level.ERROR,
                        (String) null, ex);
            }
        });
    }

    /**
     * Método para listar los archivos de mapas en la carpeta especificada
     */
    private static List<String> obtenerMapas(String carpeta) {
        File dir = new File(carpeta);
        List<String> mapas = new ArrayList<>();
        if (dir.exists() && dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                if (f.isFile() && f.getName().endsWith(".txt")) {
                    mapas.add(f.getName());
                }
            }
        }
        return mapas;
    }
}
