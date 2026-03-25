import java.awt.Color;
import java.awt.Graphics;
import java.awt.Grpahics;

public class MonedaEspecial extends Moneda {
    private boolean activa;
    private int framesRestantes; // Duración del efecto en fram

    public MonedaEspecial(int tamano)
        super(-100, -100, tamano);
        this.activa = false;
        this.framesRestantes = 0;
    }

    public void activar(int x, int y, int duracionFrames) {
        setX(x);
        setY(y);
        this.framesRestantes = duracionFrames;
        this.activa = true;
    }

    public void actualizar() {
        if (activa && framesRestantes > 0) {
            framesRestantes--;
            if (framesRestantes <= 0) {
                desactivar();
            }
        }
    }

    public void desactivar() {
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
        return 3; // La moneda especial otorga más puntos
    }

    @Override
    public void dibujar(Graphics g) {
        if (!activa)
            return; // No dibujar si no está activa
        g.setColor(new Color(255, 70, 180));
        g.fillOval(x, y, ancho, alto);
        g.setColor(new Color(255, 20, 100));
        g.drawOval(x, y, ancho, alto);
        g.setColor(Color.WHITE);
        g.fillOval(x + ancho / 4, y + alto / 4, ancho / 2, alto / 2);
    }

}
