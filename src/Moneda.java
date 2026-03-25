import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Random;

public class Moneda extends EntidadJuego{
    private Random random;

    public Moneda(int anchoPanel, int altoPanel, int tamano){
        super(0,0, tamano, tamano);
        random = new Random();
        reposicionar(anchoPanel, altoPanel);
    }
    public void reposicionar (int anchoPanel, int altoPanel){
        x = random.nextInt(anchoPanel-ancho);
        y = random.nextInt(altoPanel-alto);
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
}
