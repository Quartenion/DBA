

/**
 *
 * @author Group_7_DBA
 */


import view.mainView;
import controller.Controller;
import model.Example;


public class Launcher {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> {
            // model
            Example model = new Example();

            // view
            mainView view = new mainView(model);

            // controller
            Controller controller = new Controller(view, model);

            view.setVisible(true);
        });
    }
    
}
