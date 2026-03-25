import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Random;

public class Moneda {
    //Definimos atributos privados
    private int x;
    private int y;
    private int tamano;
    private Random random;

    public Moneda(int anchoPanel, int altoPanel, int tamano){
        this.tamano = tamano;
        this.random = new Random();
        reposicionar(anchoPanel, altoPanel);
    }
    public void reposicionar (int anchoPanel, int altoPanel){
        x = random.nextInt(anchoPanel-tamano);
        y = random.nextInt(altoPanel-tamano);
    }

    public void dibujar(Graphics g){
        //Moneda principal
        g.setColor(Color.YELLOW);
        g.fillOval(x, y, tamano, tamano);
        //Definimos borde
        g.setColor(Color.ORANGE);
        g.drawOval(x, y, tamano, tamano);
        //Punto central decorativo
        g.setColor(Color.WHITE);
        g.fillOval(x + tamano/4, y + tamano/4, tamano/2, tamano/2);
    }

    public Rectangle getBounds(){
        return new Rectangle(x, y, tamano, tamano);
    }

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public int getTamano() {
        return tamano;
    }

    
}
