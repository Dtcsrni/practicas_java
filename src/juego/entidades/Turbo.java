package juego.entidades;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;

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
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int pulso = (int) ((System.nanoTime() / 9_000_000L) % 10);
        g2.setColor(new Color(60, 255, 190, 60));
        g2.fillOval(x - 4 - pulso / 3, y - 4 - pulso / 3, ancho + 8 + pulso, alto + 8 + pulso);
        g2.setPaint(new GradientPaint(x, y, new Color(114, 255, 210), x, y + alto, new Color(26, 212, 134)));
        g2.fillOval(x, y, ancho, alto);
        g2.setColor(new Color(8, 146, 86));
        g2.drawOval(x, y, ancho, alto);
        g2.setColor(Color.WHITE);
        g2.fillRect(x + 9, y + 4, 6, 14);
        g2.fillRect(x + 6, y + 10, 12, 4);
        g2.setColor(new Color(255, 255, 255, 110));
        g2.drawLine(x + 5, y + 6, x + ancho - 5, y + 6);
        g2.dispose();
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
