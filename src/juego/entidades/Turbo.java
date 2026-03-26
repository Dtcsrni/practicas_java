package juego.entidades;

import java.awt.Color;
import java.awt.Graphics;

// Bonus temporal de velocidad para el jugador principal.
public class Turbo extends EntidadJuego {
    private boolean activo;

    public Turbo() {
        // Comienza oculto hasta que el motor lo genere.
        super(-100, -100, 24, 24);
        this.activo = false;
    }

    public void activar(int x, int y) {
        // Entra al campo y queda listo para recogerse.
        setX(x);
        setY(y);
        this.activo = true;
    }

    public void desactivar() {
        // Lo retira del campo.
        this.activo = false;
        setX(-100);
        setY(-100);
    }

    public boolean estaActivo() {
        return activo;
    }

    @Override
    public void dibujar(Graphics g) {
        if (!activo)
            return; // Inactivo: no se dibuja.
        g.setColor(new Color(70, 255, 180));
        g.fillOval(x, y, ancho, alto);
        g.setColor(new Color(20, 255, 100));
        g.drawOval(x, y, ancho, alto);
        g.setColor(Color.WHITE);
        g.fillRect(x + 9, y + 4, 6, 14);
        g.fillRect(x + 6, y + 10, 12, 4);
    }

    @Override
    public int getPuntos() {
        return 0; // Solo modifica velocidad.
    }

    public void aplicarEfecto(Jugador jugador) {
        // El aumento es breve y moderado para no romper el balance.
        jugador.activarTurbo(1, 4 * 60);
    }

}
