package juego.ui;

import javax.swing.JFrame;

// Ventana principal que contiene el panel de juego.
public class VentanaJuego extends JFrame {
    public VentanaJuego(){
        // Configuracion base de la ventana.
        setTitle("La Canchita - Futbol Callejero (Java)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Mantiene una resolucion fija para preservar el layout.
        setResizable(false);
        // Construye el panel principal.
        PanelJuego panel = new PanelJuego();
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        // El foco inicial va al panel para capturar teclado.
        panel.requestFocusInWindow();
    }
}
