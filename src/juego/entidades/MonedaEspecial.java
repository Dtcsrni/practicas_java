package juego.entidades;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;

// Moneda de mayor valor que aparece por tiempo limitado.
public class MonedaEspecial extends Moneda {
    private boolean activa;
    private int framesRestantes; // Tiempo visible restante.

    public MonedaEspecial(int tamano) {
        super(0, 0, tamano);
        // Permanece oculta hasta que el motor la active.
        this.activa = false;
        this.framesRestantes = 0;
        desactivar();
    }

    public void activar(int x, int y, int duracionFrames) {
        // La coloca en escena y reinicia su tiempo de vida.
        setX(x);
        setY(y);
        this.framesRestantes = duracionFrames;
        this.activa = true;
    }

    public void actualizar() {
        // Cuenta regresiva hasta desaparecer.
        if (activa && framesRestantes > 0) {
            framesRestantes--;
            if (framesRestantes <= 0) {
                desactivar();
            }
        }
    }

    public void desactivar() {
        // Sale del campo y deja de dibujarse.
        activa = false;
        framesRestantes = 0;
        setX(-100);
        setY(-100);
    }

    public boolean estaActiva() {
        return activa;
    }

    @Override
    public int getPuntos() {
        return 3; // Vale mas que la moneda comun.
    }

    @Override
    public void dibujar(Graphics g) {
        if (!activa) {
            return; // Inactiva: no se renderiza.
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int pulso = (int) ((System.nanoTime() / 10_000_000L) % 8);
        g2.setColor(new Color(255, 74, 184, 60));
        g2.fillOval(x - 5 - pulso / 2, y - 5 - pulso / 2, ancho + 10 + pulso, alto + 10 + pulso);
        g2.setPaint(new GradientPaint(x, y, new Color(255, 148, 238), x, y + alto, new Color(255, 52, 158)));
        g2.fillOval(x, y, ancho, alto);
        g2.setColor(new Color(170, 20, 102));
        g2.drawOval(x, y, ancho, alto);
        g2.setColor(new Color(255, 255, 255, 210));
        g2.fillOval(x + ancho / 5, y + alto / 5, ancho / 3, alto / 3);
        g2.setColor(new Color(255, 245, 255));
        g2.drawString("3", x + ancho / 2 - 3, y + alto / 2 + 5);
        g2.dispose();
    }
}
