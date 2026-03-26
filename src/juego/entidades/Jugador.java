package juego.entidades;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;

// Jugador vectorial con orientacion, animacion y estado de turbo.
public class Jugador extends EntidadJuego {
    private static final double STAMINA_MAX_DEFAULT = 100.0;
    private String nombre;
    private int velocidadBase;
    private int bonusVelocidadTemporal;
    private int framesTurboRestantes;
    private double staminaMax;
    private double stamina;
    private boolean agotado;
    private int inteligencia;
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
        this.staminaMax = STAMINA_MAX_DEFAULT;
        this.stamina = staminaMax;
        this.agotado = false;
        this.inteligencia = 50;
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
        // Silueta organica: cabeza, torso curvo y extremidades con articulacion.
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int oscilacion = (int) Math.round(Math.sin(faseAnimacion) * 2.0);
        int braceo = (int) Math.round(Math.sin(faseAnimacion + Math.PI / 5.0) * 4.0);
        int zancada = (int) Math.round(Math.sin(faseAnimacion + Math.PI / 2.0) * 4.0);
        int inclinacionX = direccionX * Math.max(2, ancho / 8);
        int inclinacionY = direccionY * Math.max(1, alto / 12);
        int cabezaTam = Math.max(8, ancho / 3);
        int torsoW = Math.max(11, (int) Math.round(ancho * 0.52));
        int torsoH = Math.max(14, (int) Math.round(alto * 0.42));
        int torsoX = x + (ancho - torsoW) / 2;
        int torsoY = y + cabezaTam - 1 + oscilacion;
        int caderaY = torsoY + torsoH - 2;
        int cuelloX = torsoX + torsoW / 2;
        int hombroY = torsoY + 6;
        int caderaX = torsoX + torsoW / 2;
        int cabezaX = x + (ancho - cabezaTam) / 2 + inclinacionX;
        int cabezaY = y + inclinacionY;
        int dorsal = Math.abs(nombre.hashCode()) % 89 + 10;

        // Sombra de contacto con el piso.
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillOval(x + ancho / 6, y + alto - 4, (int) Math.round(ancho * 0.66), 7);

        if (tieneTurboActivo()) {
            int aura = 6 + (framesTurboRestantes % 4);
            g2.setColor(new Color(90, 255, 205, 60));
            g2.fillOval(x - aura, y + alto - 12 - aura / 2, ancho + aura * 2, 18 + aura);
        }
        if (agotado) {
            g2.setColor(new Color(255, 102, 88, 58));
            g2.fillOval(x - 3, y + alto - 10, ancho + 6, 14);
        }

        // Cabeza.
        g2.setPaint(new GradientPaint(cabezaX, cabezaY, new Color(255, 228, 186), cabezaX, cabezaY + cabezaTam, new Color(236, 188, 148)));
        g2.fillOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(colorBorde);
        g2.drawOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(new Color(255, 255, 255, 110));
        g2.fillOval(cabezaX + 2, cabezaY + 2, Math.max(2, cabezaTam / 3), Math.max(2, cabezaTam / 4));
        g2.setColor(new Color(48, 32, 24, 210));
        g2.drawArc(cabezaX + 2, cabezaY + 1, cabezaTam - 4, Math.max(3, cabezaTam / 2), 10, 160);

        // Cuello.
        g2.setColor(new Color(234, 182, 142));
        g2.fillRoundRect(cuelloX - 2, cabezaY + cabezaTam - 2, 4, 5, 2, 2);

        // Torso y short (sin bloques rectos duros).
        g2.setPaint(new GradientPaint(
            torsoX,
            torsoY,
            colorCuerpo.brighter(),
            torsoX,
            torsoY + torsoH,
            colorCuerpo.darker()
        ));
        g2.fillRoundRect(torsoX, torsoY, torsoW, torsoH, 12, 12);
        g2.setColor(colorBorde);
        g2.drawRoundRect(torsoX, torsoY, torsoW, torsoH, 12, 12);
        g2.setColor(colorDetalle);
        g2.fillRoundRect(torsoX + torsoW / 2 - 3, torsoY + 2, 6, torsoH - 4, 4, 4);
        g2.fillOval(torsoX - 2, hombroY - 2, 7, 7);
        g2.fillOval(torsoX + torsoW - 5, hombroY - 2, 7, 7);
        g2.setColor(new Color(255, 255, 255, 190));
        g2.drawLine(torsoX + 2, torsoY + 4, torsoX + torsoW - 3, torsoY + 4);
        g2.setColor(new Color(28, 28, 28, 190));
        g2.fillRoundRect(torsoX + 1, caderaY - 2, torsoW - 2, Math.max(6, alto / 8), 8, 8);
        g2.setColor(new Color(250, 250, 250, 210));
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, torsoW / 3 + 4)));
        g2.drawString(String.valueOf(dorsal), torsoX + torsoW / 2 - 5, torsoY + torsoH / 2 + 4);

        // Extremidades como trazos redondeados.
        g2.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(245, 206, 168));
        g2.drawLine(torsoX + 3, hombroY, torsoX - 4 - direccionX, hombroY + 10 + braceo);
        g2.drawLine(torsoX + torsoW - 3, hombroY, torsoX + torsoW + 4 - direccionX, hombroY + 10 - braceo);

        int piernaBaseY = caderaY + 4;
        int piernaPaso = Math.max(4, ancho / 5);
        g2.setColor(colorDetalle.darker());
        g2.drawLine(caderaX - 3, piernaBaseY, caderaX - piernaPaso, y + alto - 5 - zancada);
        g2.drawLine(caderaX + 3, piernaBaseY, caderaX + piernaPaso, y + alto - 5 + zancada);

        g2.setColor(colorDetalle);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(caderaX - piernaPaso, y + alto - 11 - zancada, caderaX - piernaPaso, y + alto - 7 - zancada);
        g2.drawLine(caderaX + piernaPaso, y + alto - 11 + zancada, caderaX + piernaPaso, y + alto - 7 + zancada);

        g2.setColor(new Color(20, 20, 20));
        g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(caderaX - piernaPaso - 3, y + alto - 3 - zancada, caderaX - piernaPaso + 5, y + alto - 3 - zancada);
        g2.drawLine(caderaX + piernaPaso - 5, y + alto - 3 + zancada, caderaX + piernaPaso + 3, y + alto - 3 + zancada);

        double energia = staminaMax > 0.0 ? Math.max(0.0, Math.min(1.0, stamina / staminaMax)) : 0.0;
        Color barraEnergia = energia > 0.55 ? new Color(88, 220, 120) : (energia > 0.25 ? new Color(255, 198, 80) : new Color(255, 104, 84));
        int barraY = y + alto + 2;
        int barraAncho = Math.max(10, ancho - 4);
        int relleno = (int) Math.round(barraAncho * energia);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(x + 2, barraY, barraAncho, 4, 4, 4);
        g2.setColor(barraEnergia);
        g2.fillRoundRect(x + 2, barraY, Math.max(2, relleno), 4, 4, 4);

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
            stamina = Math.max(0.0, stamina - 0.62);
            if (stamina <= 0.0) {
                agotado = true;
            }
        } else {
            double recuperacion = moviendose ? 0.24 : 0.64;
            stamina = Math.min(staminaMax, stamina + recuperacion);
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

    public boolean estaAgotado() {
        return agotado;
    }

    public double getStamina() {
        return stamina;
    }

    public double getStaminaMax() {
        return staminaMax;
    }

    public void setStaminaMax(double staminaMax) {
        this.staminaMax = Math.max(70.0, Math.min(130.0, staminaMax));
        this.stamina = Math.min(this.stamina, this.staminaMax);
        if (this.stamina <= 0.0) {
            agotado = true;
        }
    }

    public void recargarStaminaCompleta() {
        stamina = staminaMax;
        agotado = false;
    }

    public void recuperarStamina(double cantidad) {
        if (cantidad <= 0.0) {
            return;
        }
        stamina = Math.min(staminaMax, stamina + cantidad);
        if (agotado && stamina >= 34.0) {
            agotado = false;
        }
    }

    public void gastarStamina(double cantidad) {
        if (cantidad <= 0.0) {
            return;
        }
        stamina = Math.max(0.0, stamina - cantidad);
        if (stamina <= 0.0) {
            agotado = true;
        }
    }

    public int getInteligencia() {
        return inteligencia;
    }

    public void setInteligencia(int inteligencia) {
        this.inteligencia = Math.max(35, Math.min(85, inteligencia));
    }

    public void setColores(Color colorCuerpo, Color colorBorde, Color colorDetalle) {
        this.colorCuerpo = colorCuerpo;
        this.colorBorde = colorBorde;
        this.colorDetalle = colorDetalle;
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
