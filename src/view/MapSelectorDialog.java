package view;

import model.World;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 *
 * @author tonni
 * 
 */
public class MapSelectorDialog extends JDialog {

    private final JList<String> list;
    private final DefaultListModel<String> listModel;
    private String selectedMap = null;

    public MapSelectorDialog(Frame owner, List<String> mapas) {
        super(owner, "Seleccionar mapa", true); // modal
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        listModel = new DefaultListModel<>();
        for (int i = 0; i < mapas.size(); i++) {
            listModel.addElement((i + 1) + ". " + mapas.get(i));
        }
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(Math.min(mapas.size(), 12));
        JScrollPane scroll = new JScrollPane(list);

        add(new JLabel("Selecciona un mapa:"), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        // Botones
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCargar = new JButton("Cargar");
        JButton btnCancelar = new JButton("Cancelar");
        btnCargar.addActionListener(e -> onCargar(mapas));
        btnCancelar.addActionListener(e -> onCancelar());

        buttons.add(btnCancelar);
        buttons.add(btnCargar);
        add(buttons, BorderLayout.SOUTH);

        // Handle window closing (treat as cancel)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancelar();
            }
        });

        // Aumentamos tamaño por defecto y permitimos redimensionar:
        // preferencia segura para mostrar listas grandes y que no quede demasiado pequeño
        setPreferredSize(new Dimension(500, 350));
        setMinimumSize(new Dimension(480, 320));
        setResizable(true);

        pack(); // usa preferred/minimum size para calcular
        setLocationRelativeTo(owner);
        setVisible(true); // bloquea hasta dispose()
    }

    // Intento de cargar el mapa seleccionado; si ok se guarda y cierra el diálogo.
    private void onCargar(List<String> mapas) {
        int idx = list.getSelectedIndex();
        if (idx < 0 || idx >= mapas.size()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, seleccione un mapa de la lista.",
                    "Mapa no seleccionado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String nombre = mapas.get(idx);
        String ruta = "./maps/" + nombre;

        // Comprobamos que el mapa carga correctamente
        World w = new World();
        w.read_map(ruta);
        if (w.empty()) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar el mapa seleccionado:\n" + nombre,
                    "Error de lectura",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Ok -> guardamos nombre y cerramos
        this.selectedMap = nombre;
        dispose();
    }

    private void onCancelar() {
        this.selectedMap = null;
        dispose();
    }

    /**
     * Devuelve el nombre de fichero seleccionado (por ejemplo "mapWithDiagonalWall.txt"),
     * o null si se canceló.
     *
     * Nota: este método retorna el valor después de que el diálogo modal haya sido cerrado.
     */
    public String getSelectedMap() {
        return selectedMap;
    }
}