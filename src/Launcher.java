import view.MainView;
import controller.Controller;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import model.AgentP3;
import model.Environment;
import model.World;
import model.WorldPerceptionInterface;


import elfo.ElfTranslatorAgent;
import santa.SantaAgent;


public class Launcher {

    private static final String MAIN_CONTAINER = "dba_main";

    public static void main(String[] args) {
        
        
        java.awt.EventQueue.invokeLater(() -> {
            
            // -----------------------------------------------------------------
            // CREAMOS EL MUNDO
            // -----------------------------------------------------------------
            // Create an empty world
            World world = new World();
            
            WorldPerceptionInterface worldPerceptionInterface = 
                                            new WorldPerceptionInterface(world);
            
            // Create the agent model
            Environment environment = new Environment(worldPerceptionInterface);

            
            // -----------------------------------------------------------------
            // LANZAMOS LOS AGENTES
            // -----------------------------------------------------------------
            // De esta forma obtenemos la instancia de ejecucion de jade para 
            // para crear nuestros propios contenedores y agentes 
            jade.core.Runtime rt = jade.core.Runtime.instance();

            // El profile es necesario para crear el contenedor
            Profile p  = new ProfileImpl();
            p.setParameter(Profile.CONTAINER_NAME, MAIN_CONTAINER);
            p.setParameter(Profile.MAIN, "true");

            // Creamos el controlador de Main container
            ContainerController cc = rt.createMainContainer(p);
            

            try {
                // Creamos los agentes (Enviroment is only sent to AgentP3)
                AgentController acHW = cc.createNewAgent("AgentP3", 
                                        AgentP3.class.getCanonicalName(), 
                                        new Object[]{environment});
                
                AgentController acE = cc.createNewAgent("ElfTranslator", 
                                        ElfTranslatorAgent.class.getCanonicalName(), 
                                        null);
                
                AgentController acS = cc.createNewAgent("Santa", 
                                        SantaAgent.class.getCanonicalName(), 
                                        null);
                /*
                AgentController acR = cc.createNewAgent("AgentP3", 
                                        AgentP3.class.getCanonicalName(), 
                                        new Object[]{environment});
                */
                
                // Lanzamos los agentes ElfTranslator - Santa - Rudolph - AgentP3
                acE.start();
                acS.start();
                // acR.start();
                acHW.start();

            } catch (StaleProxyException ex) {
                System.getLogger(Launcher.class.getName()).log(System.Logger.Level.ERROR,
                        (String) null, ex);
            }

            //------------------------------------------------------------------
            // view (Se le pasa mundo, pero recibe un objeto de la interfaz)
            MainView view = new MainView(world, environment);
            
            // controller
            Controller controller = new Controller(view, world, environment, cc);
            controller.restart("./maps/mapWithoutObstacle.txt");
            

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
}
