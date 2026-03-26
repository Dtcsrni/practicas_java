package juego.entidades;

import java.awt.Color;
import java.awt.Graphics;

// Bonus temporal que aumenta la velocidad del jugador principal.
public class Turbo extends EntidadJuego {
    private boolean activo;

    public Turbo() {
        // Inicia inactivo y fuera de pantalla.
        super(-100, -100, 24, 24);
        this.activo = false;
    }

    public void activar(int x, int y) {
        // Se coloca en escena y queda disponible.
        setX(x);
        setY(y);
        this.activo = true;
    }

    public void desactivar() {
        // Oculta el bonus.
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
            return; // No dibujar si no está activo
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
        return 0; // El turbo no otorga puntos, solo velocidad
    }

    public void aplicarEfecto(Jugador jugador) {
        // Incremento moderado para no romper el ritmo de partido.
        jugador.activarTurbo(1, 4 * 60); // Aumento suave para mantener ritmo controlado.
    }

}
