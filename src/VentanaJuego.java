import javax.swing.JFrame;
public class VentanaJuego extends JFrame {
    public VentanaJuego(){
        //Definicion de caracteristicas de la ventana
        setTitle("Proyecto Juego -Base Grafica");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Impide redimensionar la ventana manualmente
        setResizable(false);
        //Creación de objeto panel de tipo PanelJuego y su instanciación
        PanelJuego panel = new PanelJuego();
        //Agregamos el panel a la ventana
        add(panel);
        //Ajusta automaticamente el tamaño de la ventana al tamaño del panel
        pack();
        //Centra la ventana en la pantalla
        setLocationRelativeTo(null);
        //Hace visible la ventana
        setVisible(true);
        panel.requestFocus();
    }
}
