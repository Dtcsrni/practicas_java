import java.awt.Color;
import java.awt.Graphics;;

public class Turbo extends EntidadJuego implements Recolectable {
    private boolean activo;

    public Turbo() {
        super(-100, -100, 24, 24);
        this.activo = false;
    }

    public void activar(int x, int y) {
        setX(x);
        setY(y);
        this.activo = true;
    }

    public void desactivar() {
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

    @Override
    public void aplicarEfecto(Jugador jugador) {
        jugador.activarTurbo(3, 5*60); //5 segundos a 60fps;
    }

}