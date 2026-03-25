
import java.awt.Color;
import java.awt.Graphics;
public class Obstaculo extends EntidadJuego{
    public Obstaculo(int x, int y, int ancho, int alto) {
        super(x, y, ancho, alto);
    }

    @Override
    public void dibujar(Graphics g) {
        g.setColor(new Color(120,70,15));
        g.fillRect(x, y, ancho, alto);
        g.setColor(new Color(80,40,10));
        g.drawRect(x, y, ancho, alto);
        g.setColor(new Color(160,110,50));
        g.drawLine(x, y, x+ancho, y+alto);
        g.drawLine(x+ancho, y, x, y+alto);
    }
    
}
