import java.awt.Rectangle;

public abstract class EntidadJuego implements Dibujable, Colisionable{
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

    @Override 
    public Rectangle getBounds() {
        return new Rectangle(x, y, ancho, alto);
    }

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
