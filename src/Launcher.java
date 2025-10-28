/**
 *
 * @author Group_7_DBA
 */

import view.mainView;
import view.MapSelectorDialog;
import controller.Controller;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import model.AgentP2;
import model.AgentMemory;
import model.World;

import java.util.List;

public class Launcher {

    private static final String MAIN_CONTAINER = "dba_main";

    public static void main(String[] args) {

        // Listamos mapas en ./maps/
        List<String> mapasDisponibles = obtenerMapas("./maps/");

        if (mapasDisponibles.isEmpty()) {
            System.out.println("No hay mapas disponibles en la carpeta ./maps/");
            return;
        }

        // Mostramos diálogo modal para seleccionar mapa (bloqueante)
        MapSelectorDialog selector = new MapSelectorDialog(null, mapasDisponibles);
        String seleccionado = selector.getSelectedMap(); // Devuelve nombre de fichero o null si cancelado

        if (seleccionado == null) {
            // Usuario canceló o no seleccionó nada -> salimos
            System.out.println("Selección de mapa cancelada. Saliendo.");
            return;
        }

        String rutaMapa = "./maps/" + seleccionado;
        System.out.println("Has seleccionado: " + rutaMapa);

        // -------------------------
        // INTERFAZ GRÁFICA
        // -------------------------
        java.awt.EventQueue.invokeLater(() -> {

            // Lectura del mapa
            World world = new World();
            world.read_map(rutaMapa);

            // Si la lectura por alguna razón no cargó el mapa, mostramos error y salimos.
            if (world.empty()) {
                javax.swing.JOptionPane.showMessageDialog(null,
                        "Error al cargar el mapa seleccionado: " + rutaMapa,
                        "Error de carga",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }

            // model
            AgentMemory model = new AgentMemory();

            // view
            mainView view = new mainView(model, world);

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

            try {
                // Creating the agent controller            
                AgentController acHW = cc.createNewAgent("Agente", AgentP2.class.getCanonicalName(), null);
                acHW.start();

            } catch (StaleProxyException ex) {
                System.getLogger(Launcher.class.getName()).log(System.Logger.Level.ERROR,
                        (String) null, ex);
            }

            // controller
            Controller controller = new Controller(view, model, cc);

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