import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Random;

public class Moneda extends EntidadJuego implements Recolectable{
    public Moneda(int anchoPanel, int altoPanel, int tamano){
        super(0,0, tamano, tamano);
    }

    @Override
    public void dibujar(Graphics g){
        //Moneda principal
        g.setColor(Color.YELLOW);
        g.fillOval(x, y, ancho, alto);
        //Definimos borde
        g.setColor(Color.ORANGE);
        g.drawOval(x, y, ancho, alto);
        //Punto central decorativo
        g.setColor(Color.WHITE);
        g.fillOval(x + ancho/4, y + alto/4, ancho/2, alto/2);
    }
    
    @Override
    public int getPuntos(){
        return 1;
    }
    @Override
    public void aplicarEfecto(Jugador jugador){
       //La moneda normal solo da puntos
    }
}
