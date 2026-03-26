package juego.entidades;

import java.awt.Graphics;
import java.awt.Rectangle;

// Base comun para cualquier objeto dibujable con posicion rectangular.
public abstract class EntidadJuego {
    protected int x;
    protected int y;
    protected int ancho;
    protected int alto;
    public EntidadJuego(int x, int y, int ancho, int alto) {
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;
    }

    public Rectangle getBounds() {
        // Colision rectangular simple usada en toda la simulacion.
        return new Rectangle(x, y, ancho, alto);
    }

    public int getPuntos() {
        return 0;
    }

    public abstract void dibujar(Graphics g);

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public int getAncho() {
        return ancho;
    }
    public int getAlto() {
        return alto;
    }
    public void setX(int x) {
        this.x = x;
    }
    public void setY(int y) {
        this.y = y;
    }
}
