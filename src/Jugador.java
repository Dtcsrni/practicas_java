import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Jugador extends EntidadJuego implements Movible{
    private int velocidad;

    // Constructor para inicializar el jugador
    public Jugador(int x, int y, int ancho, int alto, int velocidad) {
        super(x, y, ancho, alto);
        this.velocidad = velocidad;
    }

    @Override
    public void mover(int dx, int dy) {
        x += dx;
        y += dy;
    }

    // Método para dibujar al jugador
    @Override
    public void dibujar(Graphics g) {
        // Cuerpo principal
        g.setColor(Color.CYAN);
        g.fillRect(x, y, ancho, alto);
        // Borde blanco
        g.setColor(Color.WHITE);
        g.drawRect(x, y, ancho, alto);
        // Marca frontal para distinguir orientacion
        g.setColor(Color.RED);
        g.fillRect(x + 10, y + 4, 8, 4);
    }

    // Getters y setters para acceder y modificar los atributos de forma controlada

    public int getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(int velocidad) {
        this.velocidad = velocidad;
    }

}