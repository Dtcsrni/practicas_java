package juego.entidades;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.util.Random;

// Bonus de puntaje basico que puede recolocarse en el campo.
public class Moneda extends EntidadJuego {
    private final Random aleatorio = new Random();

    public Moneda(int anchoPanel, int altoPanel, int tamano){
        super(0, 0, tamano, tamano);
        // Arranca en una posicion visible dentro del panel.
        reposicionar(anchoPanel, altoPanel);
    }

    @Override
    public void dibujar(Graphics g){
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int pulso = (int) ((System.nanoTime() / 13_000_000L) % 6);
        g2.setColor(new Color(255, 180, 40, 45));
        g2.fillOval(x - 3 - pulso / 2, y - 3 - pulso / 2, ancho + 6 + pulso, alto + 6 + pulso);
        g2.setPaint(new GradientPaint(x, y, new Color(255, 244, 120), x, y + alto, new Color(255, 180, 35)));
        g2.fillOval(x, y, ancho, alto);
        g2.setColor(new Color(186, 108, 24));
        g2.drawOval(x, y, ancho, alto);
        g2.setColor(new Color(255, 255, 210, 180));
        g2.fillOval(x + ancho / 5, y + alto / 5, ancho / 3, alto / 3);
        g2.setColor(new Color(255, 239, 170));
        g2.drawString("$", x + ancho / 2 - 3, y + alto / 2 + 5);
        g2.dispose();
    }

    public void reposicionar(int anchoPanel, int altoPanel) {
        // Evita que la moneda aparezca cortada por los bordes.
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
       // La moneda basica no aplica efectos adicionales.
    }
}
