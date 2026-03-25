import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Jugador extends EntidadJuego implements Movible{
    private int velocidad;
    private int velocidadBase;
    private int framesTurboRestantes;

    // Constructor para inicializar el jugador
    public Jugador(int x, int y, int ancho, int alto, int velocidad) {
        super(x, y, ancho, alto);
        this.velocidad = velocidad;
        this.velocidadBase = velocidad;
        this.framesTurboRestantes = 0;
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

    public void activarTurbo(int velocidadExtra, int duracionFrames)
    {
        velocidad = velocidadBase + velocidadExtra;
        framesTurboRestantes = duracionFrames;
    }
    public void actualizarEstado(){
        if (framesTurboRestantes > 0) {
            framesTurboRestantes--;
            if (framesTurboRestantes == 0) {
                velocidad = velocidadBase; // Volver a la velocidad normal
            }
        }
    }
    public boolean tieneTurboActivo() {
        return framesTurboRestantes > 0;
    }
    public int getFramesTurboRestantes() {
        return framesTurboRestantes;
    }
    public int getVelocidadBase(){
        return velocidadBase;
    }
    public int setVelocidadBase(int velocidadBase){
        this.velocidadBase = velocidadBase;
        return velocidadBase;
    }

    // Getters y setters para acceder y modificar los atributos de forma controlada

    public int getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(int velocidad) {
        this.velocidad = velocidad;
    }

}