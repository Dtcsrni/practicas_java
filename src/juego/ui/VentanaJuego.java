package juego.ui;

import javax.swing.JFrame;

// Ventana principal que aloja el panel del juego.
public class VentanaJuego extends JFrame {
    public VentanaJuego(){
        // Definicion de caracteristicas de la ventana.
        setTitle("Proyecto Juego ");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Evita redimensionar para conservar proporciones del layout.
        setResizable(false);
        // Crea e inserta el panel principal del juego.
        PanelJuego panel = new PanelJuego();
        // Agrega el panel al frame.
        add(panel);
        // Ajusta tamaño del frame al tamaño preferido del panel.
        pack();
        // Centra ventana en pantalla.
        setLocationRelativeTo(null);
        // Hace visible la UI.
        setVisible(true);
        // Solicita foco para capturar eventos de teclado.
        panel.requestFocusInWindow();
    }
}
