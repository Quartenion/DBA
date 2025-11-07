/**
 *
 * @author Group_7_DBA
 */

import view.MainView;
import controller.Controller;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import model.AgentP2;
import model.Environment;
import model.World;

import java.util.List;

public class Launcher {

    private static final String MAIN_CONTAINER = "dba_main";

    public static void main(String[] args) {
        
        
        java.awt.EventQueue.invokeLater(() -> {
            
            // -------------------------
            // LANZAMOS EL MUNDO
            // -------------------------
            // Create an empty world
            World world = new World();
            
            // Create the agent model
            Environment environment = new Environment();

            
            // -------------------------
            // LANZAMOS EL AGENTE
            // -------------------------
            /* De esta forma obtenemos la instancia de ejecucion de jade para 
            para crear nuestros propios contenedores y agentes */
            jade.core.Runtime rt = jade.core.Runtime.instance();

            // El profile es necesario para crear el contenedor
            Profile p  = new ProfileImpl();
            p.setParameter(Profile.CONTAINER_NAME, MAIN_CONTAINER);
            p.setParameter(Profile.MAIN, "true");

            // Creamos el controlador de Main container
            ContainerController cc = rt.createMainContainer(p);
            AgentController acHW = null;

            try {
                // Crear y lanzar el agente
                acHW = cc.createNewAgent("Agente", AgentP2.class.getCanonicalName(), 
                                        new Object[]{environment});
     

            } catch (StaleProxyException ex) {
                System.getLogger(Launcher.class.getName()).log(System.Logger.Level.ERROR,
                        (String) null, ex);
            }

            //------------------------------------------------------------------
            // view (Se le pasa mundo, pero recibe un objeto de la interfaz)
            MainView view = new MainView(world);
            
            // controller
            Controller controller = new Controller(view, world, environment, cc, acHW);
            
            controller.read_map("./maps/mapWithoutObstacle.txt");
            

            // Si la lectura por alguna razón no cargó el mapa, mostramos error y salimos.
            if (world.empty()) {
                javax.swing.JOptionPane.showMessageDialog(null,
                        "Error al cargar el mapa inicial",
                        "Error de carga",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            view.setVisible(true);

        });
    }

    /**
     * Método para listar los archivos de mapas en la carpeta especificada
     */
    private static List<String> obtenerMapas(String carpeta) {
        java.io.File dir = new java.io.File(carpeta);
        java.util.List<String> mapas = new java.util.ArrayList<>();
        if (dir.exists() && dir.isDirectory()) {
            for (java.io.File f : dir.listFiles()) {
                if (f.isFile() && f.getName().endsWith(".txt")) {
                    mapas.add(f.getName());
                }
            }
        }
        return mapas;
    }
}
