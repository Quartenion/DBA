

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
import model.AgentModel;
import mundo.mundo;


public class Launcher {

    private static final String MAIN_CONTAINER = "dba_main";
    
    
    
    public static void main(String[] args) {
        
        mundo m = new mundo();
        String rutaMapa = "./maps/mapWithComplexObstacle1.txt";
        m.leer_mapa(rutaMapa);

        int[][] mapa = m.getMapa();
        /* De esta forma obtenemos la instancia de ejecucion de jade para 
           para crear nuestros propios contenedores y agentes */
        jade.core.Runtime rt = jade.core.Runtime.instance();
        
        // El profile es necesario para crear el contenedor
        Profile p  = new ProfileImpl();
        p.setParameter(Profile.CONTAINER_NAME, MAIN_CONTAINER);
        p.setParameter(Profile.MAIN, "true");
        
        // Creamos el controlador de Main container
        ContainerController cc = rt.createMainContainer(p);
        

        
        java.awt.EventQueue.invokeLater(() -> {
            // model
            AgentModel model = new AgentModel();

            // view
            mainView view = new mainView(model);

            // controller
            Controller controller = new Controller(view, model);

            view.setVisible(true);
            
            // LANZAMOS EL AGENTE
            try {
                // Creating the agent controller            
                AgentController acHW = cc.createNewAgent("Agente", AgentP2.class.getCanonicalName(), null);
                acHW.start();

            } catch (StaleProxyException ex) {
                System.getLogger(Launcher.class.getName()).log(System.Logger.Level.ERROR, 
                        (String) null, ex);
            }
        });
        

    }
    
}
