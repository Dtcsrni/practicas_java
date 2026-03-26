package juego.entidades;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

// Jugador renderizado por vectores (sin sprites) con estado de turbo.
public class Jugador extends EntidadJuego {
    private String nombre;
    private int velocidad;
    private int velocidadBase;
    private int framesTurboRestantes;
    private Color colorCuerpo;
    private Color colorBorde;
    private Color colorDetalle;
    private double faseAnimacion;
    private int direccionX;
    private int direccionY;

    // Constructor para inicializar el jugador
    public Jugador(int x, int y, int ancho, int alto, int velocidad) {
        // Constructor de conveniencia con paleta por defecto.
        this("Jugador", x, y, ancho, alto, velocidad, Color.CYAN, Color.WHITE, Color.RED);
    }

    // Constructor con colores personalizados para distinguir personajes
    public Jugador(int x, int y, int ancho, int alto, int velocidad, Color colorCuerpo, Color colorBorde, Color colorDetalle) {
        this("Jugador", x, y, ancho, alto, velocidad, colorCuerpo, colorBorde, colorDetalle);
    }

    public Jugador(String nombre, int x, int y, int ancho, int alto, int velocidad, Color colorCuerpo, Color colorBorde, Color colorDetalle) {
        super(x, y, ancho, alto);
        this.nombre = nombre;
        this.velocidad = velocidad;
        this.velocidadBase = velocidad;
        this.framesTurboRestantes = 0;
        this.colorCuerpo = colorCuerpo;
        this.colorBorde = colorBorde;
        this.colorDetalle = colorDetalle;
        this.faseAnimacion = 0.0;
        this.direccionX = 1;
        this.direccionY = 0;
    }

    public void mover(int dx, int dy) {
        // Movimiento directo por delta.
        x += dx;
        y += dy;
    }

    // Método para dibujar al jugador
    @Override
    public void dibujar(Graphics g) {
        // Dibujo vectorial estilizado.
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int oscilacion = (int) Math.round(Math.sin(faseAnimacion) * 2.0);
        int brazoOscilacion = (int) Math.round(Math.sin(faseAnimacion) * 3.0);
        int inclinacionX = direccionX * Math.max(2, ancho / 8);
        int inclinacionY = direccionY * Math.max(1, alto / 10);

        int cabezaTam = Math.max(8, ancho / 3);
        int cuerpoAncho = Math.max(10, ancho / 2);
        int cuerpoAlto = Math.max(14, alto / 2);
        int cuerpoX = x + (ancho - cuerpoAncho) / 2;
        int cuerpoY = y + cabezaTam - 2 + oscilacion;
        int cabezaX = x + (ancho - cabezaTam) / 2 + inclinacionX;
        int cabezaY = y + inclinacionY;
        int frenteCabezaX = cabezaX + cabezaTam / 2 + direccionX * Math.max(2, cabezaTam / 5);
        int frenteCabezaY = cabezaY + cabezaTam / 2 + direccionY * Math.max(1, cabezaTam / 5);
        int centroTorsoX = cuerpoX + cuerpoAncho / 2;
        int centroTorsoY = cuerpoY + cuerpoAlto / 2;

        // Sombra suave para dar profundidad.
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillOval(x + ancho / 4, y + alto - 4, ancho / 2, 6);

        // Cabeza.
        g2.setColor(new Color(255, 220, 180));
        g2.fillOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(colorBorde);
        g2.drawOval(cabezaX, cabezaY, cabezaTam, cabezaTam);

        // Rostro orientado hacia la direccion actual.
        g2.setColor(new Color(38, 24, 18));
        g2.fillOval(frenteCabezaX - 2, frenteCabezaY - 2, 4, 4);
        if (Math.abs(direccionX) > 0) {
            g2.drawLine(frenteCabezaX, frenteCabezaY + 3, frenteCabezaX + direccionX * 3, frenteCabezaY + 4);
        } else {
            g2.drawLine(frenteCabezaX - 2, frenteCabezaY + 3, frenteCabezaX + 2, frenteCabezaY + 3);
        }

        // Torso (camiseta).
        g2.setColor(colorCuerpo);
        g2.fillRoundRect(cuerpoX, cuerpoY, cuerpoAncho, cuerpoAlto, 8, 8);
        g2.setColor(colorBorde);
        g2.drawRoundRect(cuerpoX, cuerpoY, cuerpoAncho, cuerpoAlto, 8, 8);

        // Franja del uniforme marcada segun la direccion de frente.
        g2.setColor(colorDetalle);
        if (Math.abs(direccionX) >= Math.abs(direccionY)) {
            int franjaX = direccionX >= 0 ? cuerpoX + cuerpoAncho / 2 : cuerpoX + cuerpoAncho / 2 - 4;
            g2.fillRect(franjaX, cuerpoY + 2, 4, cuerpoAlto - 4);
        } else {
            int franjaY = direccionY >= 0 ? cuerpoY + cuerpoAlto / 2 : cuerpoY + cuerpoAlto / 2 - 4;
            g2.fillRect(cuerpoX + 2, franjaY, cuerpoAncho - 4, 4);
        }

        // Brazos.
        g2.setColor(colorDetalle);
        g2.drawLine(
            cuerpoX + 1,
            cuerpoY + 8,
            cuerpoX - 5 - direccionX,
            cuerpoY + 12 + brazoOscilacion - direccionY
        );
        g2.drawLine(
            cuerpoX + cuerpoAncho - 1,
            cuerpoY + 8,
            cuerpoX + cuerpoAncho + 5 - direccionX,
            cuerpoY + 14 - brazoOscilacion - direccionY
        );

        // Indica el frente del jugador como en una vista isometrica simple.
        g2.setColor(colorBorde);
        g2.drawLine(
            centroTorsoX,
            centroTorsoY,
            centroTorsoX + direccionX * Math.max(4, ancho / 5),
            centroTorsoY + direccionY * Math.max(4, alto / 6)
        );

        // Piernas.
        int piernaY = cuerpoY + cuerpoAlto;
        int separacionPierna = (int) Math.round(Math.sin(faseAnimacion) * 2.0);
        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(cuerpoX + 1, piernaY, Math.max(3, cuerpoAncho / 3), Math.max(7, alto / 4));
        g2.fillRect(
            cuerpoX + cuerpoAncho - Math.max(3, cuerpoAncho / 3) - 1 + separacionPierna,
            piernaY,
            Math.max(3, cuerpoAncho / 3),
            Math.max(7, alto / 4)
        );

        g2.dispose();
    }

    public void activarTurbo(int velocidadExtra, int duracionFrames)
    {
        // Aplica boost temporal de velocidad.
        velocidad = velocidadBase + velocidadExtra;
        framesTurboRestantes = duracionFrames;
    }
    public void actualizarEstado(){
        // Cuenta regresiva del turbo.
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
        // Mantiene firma existente del proyecto.
        this.velocidadBase = velocidadBase;
        return velocidadBase;
    }

    // Getters y setters para acceder y modificar los atributos de forma controlada

    public int getVelocidad() {
        return velocidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setVelocidad(int velocidad) {
        this.velocidad = velocidad;
    }

    public void actualizarAnimacion(int dx, int dy) {
        // Si se mueve, anima mas rapido; si no, queda respiracion suave.
        int actividad = Math.abs(dx) + Math.abs(dy);
        if (actividad > 0) {
            faseAnimacion += 0.35;
            if (Math.abs(dx) >= Math.abs(dy)) {
                direccionX = dx > 0 ? 1 : -1;
                direccionY = dy == 0 ? 0 : (dy > 0 ? 1 : -1);
            } else {
                direccionY = dy > 0 ? 1 : -1;
                direccionX = dx == 0 ? 0 : (dx > 0 ? 1 : -1);
            }
        } else {
            faseAnimacion += 0.08;
        }
    }
}
