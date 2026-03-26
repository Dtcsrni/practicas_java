package juego.entidades;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

// Jugador vectorial con orientacion, animacion y estado de turbo.
public class Jugador extends EntidadJuego {
    private static final double STAMINA_MAX = 100.0;
    private String nombre;
    private int velocidadBase;
    private int bonusVelocidadTemporal;
    private int framesTurboRestantes;
    private double stamina;
    private boolean agotado;
    private Color colorCuerpo;
    private Color colorBorde;
    private Color colorDetalle;
    private double faseAnimacion;
    private int direccionX;
    private int direccionY;

    public Jugador(int x, int y, int ancho, int alto, int velocidad) {
        // Atajo con nombre generico y paleta base.
        this("Jugador", x, y, ancho, alto, velocidad, Color.CYAN, Color.WHITE, Color.RED);
    }

    // Variante completa para diferenciar jugadores y equipos.
    public Jugador(int x, int y, int ancho, int alto, int velocidad, Color colorCuerpo, Color colorBorde, Color colorDetalle) {
        this("Jugador", x, y, ancho, alto, velocidad, colorCuerpo, colorBorde, colorDetalle);
    }

    public Jugador(String nombre, int x, int y, int ancho, int alto, int velocidad, Color colorCuerpo, Color colorBorde, Color colorDetalle) {
        super(x, y, ancho, alto);
        this.nombre = nombre;
        this.velocidadBase = velocidad;
        this.bonusVelocidadTemporal = 0;
        this.framesTurboRestantes = 0;
        this.stamina = STAMINA_MAX;
        this.agotado = false;
        this.colorCuerpo = colorCuerpo;
        this.colorBorde = colorBorde;
        this.colorDetalle = colorDetalle;
        this.faseAnimacion = 0.0;
        this.direccionX = 1;
        this.direccionY = 0;
    }

    public void mover(int dx, int dy) {
        // El motor resuelve colisiones y limites fuera de esta clase.
        x += dx;
        y += dy;
    }

    @Override
    public Rectangle getBounds() {
        // Hitbox centrada en torso/piernas: evita colisiones por cabeza y brazos.
        int margenX = Math.max(3, (int) Math.round(ancho * 0.18));
        int margenY = Math.max(6, (int) Math.round(alto * 0.24));
        int anchoCaja = Math.max(6, ancho - margenX * 2);
        int altoCaja = Math.max(10, (int) Math.round(alto * 0.64));
        int yCaja = y + alto - altoCaja - 2;
        return new Rectangle(x + margenX, yCaja, anchoCaja, altoCaja);
    }

    @Override
    public void dibujar(Graphics g) {
        // La pose intenta sugerir direccion y ritmo sin usar sprites.
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

        // Sombra de contacto con el piso.
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillOval(x + ancho / 4, y + alto - 4, ancho / 2, 6);

        // Cabeza y contorno.
        g2.setColor(new Color(255, 220, 180));
        g2.fillOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(colorBorde);
        g2.drawOval(cabezaX, cabezaY, cabezaTam, cabezaTam);

        // El rostro se desplaza levemente hacia el frente del jugador.
        g2.setColor(new Color(38, 24, 18));
        g2.fillOval(frenteCabezaX - 2, frenteCabezaY - 2, 4, 4);
        if (Math.abs(direccionX) > 0) {
            g2.drawLine(frenteCabezaX, frenteCabezaY + 3, frenteCabezaX + direccionX * 3, frenteCabezaY + 4);
        } else {
            g2.drawLine(frenteCabezaX - 2, frenteCabezaY + 3, frenteCabezaX + 2, frenteCabezaY + 3);
        }

        // Torso.
        g2.setColor(colorCuerpo);
        g2.fillRoundRect(cuerpoX, cuerpoY, cuerpoAncho, cuerpoAlto, 8, 8);
        g2.setColor(colorBorde);
        g2.drawRoundRect(cuerpoX, cuerpoY, cuerpoAncho, cuerpoAlto, 8, 8);

        // La franja cambia de orientacion para reforzar el frente.
        g2.setColor(colorDetalle);
        if (Math.abs(direccionX) >= Math.abs(direccionY)) {
            int franjaX = direccionX >= 0 ? cuerpoX + cuerpoAncho / 2 : cuerpoX + cuerpoAncho / 2 - 4;
            g2.fillRect(franjaX, cuerpoY + 2, 4, cuerpoAlto - 4);
        } else {
            int franjaY = direccionY >= 0 ? cuerpoY + cuerpoAlto / 2 : cuerpoY + cuerpoAlto / 2 - 4;
            g2.fillRect(cuerpoX + 2, franjaY, cuerpoAncho - 4, 4);
        }

        // Brazos con oscilacion ligera al correr.
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

        // Trazo de direccion para reforzar la lectura visual del frente.
        g2.setColor(colorBorde);
        g2.drawLine(
            centroTorsoX,
            centroTorsoY,
            centroTorsoX + direccionX * Math.max(4, ancho / 5),
            centroTorsoY + direccionY * Math.max(4, alto / 6)
        );

        // Piernas con separacion variable segun la animacion.
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
        // El turbo suma velocidad durante un numero finito de frames.
        bonusVelocidadTemporal = velocidadExtra;
        framesTurboRestantes = duracionFrames;
    }

    public void actualizarEstado(boolean sprintando, int intensidadMovimiento){
        // Consume la duracion pendiente del turbo.
        if (framesTurboRestantes > 0) {
            framesTurboRestantes--;
            if (framesTurboRestantes == 0) {
                bonusVelocidadTemporal = 0;
            }
        }

        boolean moviendose = intensidadMovimiento > 0;
        if (sprintando && moviendose && !agotado) {
            stamina = Math.max(0.0, stamina - 1.05);
            if (stamina <= 0.0) {
                agotado = true;
            }
        } else {
            double recuperacion = moviendose ? 0.28 : 0.70;
            stamina = Math.min(STAMINA_MAX, stamina + recuperacion);
            if (agotado && stamina >= 34.0) {
                agotado = false;
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
        // Mantiene la API ya usada por el proyecto.
        this.velocidadBase = velocidadBase;
        return velocidadBase;
    }

    public int getVelocidad() {
        return velocidadBase + bonusVelocidadTemporal;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setVelocidad(int velocidad) {
        this.velocidadBase = velocidad;
    }

    public int getVelocidadMovimiento(boolean sprintando) {
        int velocidadNormal = Math.max(2, getVelocidad() - 1);
        if (!sprintando || agotado || stamina < 8.0) {
            return velocidadNormal;
        }
        return getVelocidad() + 1;
    }

    public boolean puedeSprintar() {
        return !agotado && stamina >= 8.0;
    }

    public double getStamina() {
        return stamina;
    }

    public double getStaminaMax() {
        return STAMINA_MAX;
    }

    public int getDireccionX() {
        return direccionX;
    }

    public int getDireccionY() {
        return direccionY;
    }

    public void actualizarAnimacion(int dx, int dy) {
        // En movimiento el ciclo avanza rapido; quieto, solo respira.
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
