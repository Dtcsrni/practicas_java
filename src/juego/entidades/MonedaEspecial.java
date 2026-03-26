package juego.entidades;

import java.awt.Color;
import java.awt.Graphics;

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
        g.setColor(new Color(255, 70, 180));
        g.fillOval(x, y, ancho, alto);
        g.setColor(new Color(255, 20, 100));
        g.drawOval(x, y, ancho, alto);
        g.setColor(Color.WHITE);
        g.fillOval(x + ancho / 4, y + alto / 4, ancho / 2, alto / 2);
    }
}
