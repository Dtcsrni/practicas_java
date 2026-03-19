import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;
public class PanelJuego extends JPanel {
    //Constructor que define las caracteristicas del panel
    public PanelJuego() {
        setPreferredSize(new Dimension(640, 320));
        setBackground(Color.BLACK);
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.drawString("Base grafica del juego lista", 20, 20);
    }
}
