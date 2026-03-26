package juego.entidades;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Random;

// Recolectable base: suma puntos y puede reposicionarse aleatoriamente.
public class Moneda extends EntidadJuego {
    private final Random aleatorio = new Random();

    public Moneda(int anchoPanel, int altoPanel, int tamano){
        super(0, 0, tamano, tamano);
        // Se ubica en una posicion valida inicial.
        reposicionar(anchoPanel, altoPanel);
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

    public void reposicionar(int anchoPanel, int altoPanel) {
        // Garantiza que no salga parcialmente de pantalla.
        int maxX = Math.max(0, anchoPanel - ancho);
        int maxY = Math.max(0, altoPanel - alto);
        x = aleatorio.nextInt(maxX + 1);
        y = aleatorio.nextInt(maxY + 1);
    }
    
    @Override
    public int getPuntos(){
        return 1;
    }
    public void aplicarEfecto(Jugador jugador){
       // La moneda normal solo da puntos (sin efecto extra).
    }
}
