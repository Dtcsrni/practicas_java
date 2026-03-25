import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Jugador {
    //Atributos privados encapsulados
    private int x;
    private int y;
    private int ancho;
    private int alto;
    private int velocidad;
    //Constructor para inicializar el jugador
    public Jugador(int x, int y, int ancho, int alto, int velocidad) {
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;
        this.velocidad = velocidad;
    }
    //Método para mover al jugador
    public void mover(int dx, int dy){
        x += dx;
        y += dy;
    }
    //Método para dibujar al jugador
    public void dibujar(Graphics g){
        //Cuerpo principal
        g.setColor(Color.CYAN);
        g.fillRect(x, y, ancho, alto);
        //Borde blanco
        g.setColor(Color.WHITE);
        g.drawRect(x, y, ancho, alto);
        //Marca frontal para distinguir orientacion
        g.setColor(Color.RED);
        g.fillRect(x+10, y + 4, 8, 4);
    }
    public Rectangle getBounds(){
        return new Rectangle(x, y, ancho, alto);
    }

    //Getters y setters para acceder y modificar los atributos de forma controlada
    public int getX() {
        return x;
    }
    public void setX(int x){
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y){
        this.y = y;
    }
    public int getAncho() {
        return ancho;
    }
    public void setAncho(int ancho){
        this.ancho = ancho;
    }
    public int getAlto() {
        return alto;
    }
    public void setAlto(int alto){
        this.alto = alto;
    }
    public int getVelocidad() {
        return velocidad;
    }
    public void setVelocidad(int velocidad){
        this.velocidad = velocidad;
    }

}