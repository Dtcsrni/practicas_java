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
    private int entrada;
    private int disciplina;
    private int faltasCometidas;
    private int tarjetasAmarillas;
    private boolean tarjetaRoja;
    private boolean expulsado;
    private Color colorCuerpo;
    private Color colorBorde;
    private Color colorDetalle;
    private double faseAnimacion;
    private int direccionX;
    private int direccionY;
    private int framesBarridaVisual;
    private int duracionBarridaVisual;
    private double barridaVisualDirX;
    private double barridaVisualDirY;
    private int framesLanzadaVisual;
    private int duracionLanzadaVisual;
    private double lanzadaVisualDirX;
    private double lanzadaVisualDirY;
    private int framesDerribado;
    private int framesLesion;
    private boolean celebracionFinalActiva;
    private boolean derrotaFinalActiva;
    private int framesReaccionFinal;

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
        this.entrada = 50;
        this.disciplina = 50;
        this.faltasCometidas = 0;
        this.tarjetasAmarillas = 0;
        this.tarjetaRoja = false;
        this.expulsado = false;
        this.colorCuerpo = colorCuerpo;
        this.colorBorde = colorBorde;
        this.colorDetalle = colorDetalle;
        this.faseAnimacion = 0.0;
        this.direccionX = 1;
        this.direccionY = 0;
        this.framesBarridaVisual = 0;
        this.duracionBarridaVisual = 0;
        this.barridaVisualDirX = 0.0;
        this.barridaVisualDirY = 0.0;
        this.framesLanzadaVisual = 0;
        this.duracionLanzadaVisual = 0;
        this.lanzadaVisualDirX = 0.0;
        this.lanzadaVisualDirY = 0.0;
        this.framesDerribado = 0;
        this.framesLesion = 0;
        this.celebracionFinalActiva = false;
        this.derrotaFinalActiva = false;
        this.framesReaccionFinal = 0;
    }

    public void mover(int dx, int dy) {
        // El motor resuelve colisiones y limites fuera de esta clase.
        if (expulsado) {
            return;
        }
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
        if (framesLanzadaVisual > 0) {
            dibujarLanzada(g2);
            g2.dispose();
            return;
        }
        if (derrotaFinalActiva) {
            dibujarDerrotaFinal(g2);
            g2.dispose();
            return;
        }
        if (celebracionFinalActiva) {
            dibujarCelebracionFinal(g2);
            g2.dispose();
            return;
        }
        if (framesDerribado > 0) {
            dibujarDerribado(g2);
            g2.dispose();
            return;
        }
        if (framesBarridaVisual > 0) {
            dibujarBarrida(g2);
            g2.dispose();
            return;
        }
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

    private void dibujarBarrida(Graphics2D g2) {
        double progreso = duracionBarridaVisual <= 0 ? 1.0 : 1.0 - (framesBarridaVisual / (double) duracionBarridaVisual);
        double dirX = Math.abs(barridaVisualDirX) < 0.001 ? (direccionX == 0 ? 1.0 : direccionX) : barridaVisualDirX;
        double dirY = Math.abs(barridaVisualDirY) < 0.001 ? direccionY : barridaVisualDirY;
        double norma = Math.hypot(dirX, dirY);
        if (norma < 0.001) {
            dirX = 1.0;
            dirY = 0.0;
            norma = 1.0;
        }
        dirX /= norma;
        dirY /= norma;

        int arrastre = (int) Math.round((1.0 - progreso) * 8.0);
        int sombraY = y + alto - 5 + Math.max(0, (int) Math.round(dirY * 2.0));
        g2.setColor(new Color(0, 0, 0, 58));
        g2.fillOval(x + ancho / 8 - 3, sombraY, (int) Math.round(ancho * 0.95) + arrastre, 8);

        int estelaX = x + ancho / 2 - (int) Math.round(dirX * 12.0) - 6;
        int estelaY = y + alto - 10 - (int) Math.round(dirY * 4.0);
        g2.setColor(new Color(214, 214, 214, 70));
        g2.fillRoundRect(estelaX, estelaY, 14 + arrastre, 5, 5, 5);
        g2.setColor(new Color(176, 176, 176, 88));
        g2.fillRoundRect(estelaX - 6, estelaY + 4, 10 + arrastre / 2, 4, 4, 4);

        int cuerpoW = Math.max(18, (int) Math.round(ancho * 0.88));
        int cuerpoH = Math.max(13, (int) Math.round(alto * 0.28));
        int cuerpoX = x + (ancho - cuerpoW) / 2 + (int) Math.round(dirX * 4.0);
        int cuerpoY = y + alto - cuerpoH - 12 + (int) Math.round(dirY * 2.0);
        int cabezaTam = Math.max(8, ancho / 3);
        int cabezaX = cuerpoX + (dirX >= 0 ? cuerpoW - cabezaTam / 2 : -cabezaTam / 2);
        int cabezaY = cuerpoY - cabezaTam / 2 - 1;

        g2.rotate(Math.toRadians(18.0 * dirX), cuerpoX + cuerpoW / 2.0, cuerpoY + cuerpoH / 2.0);
        g2.setPaint(new GradientPaint(cuerpoX, cuerpoY, colorCuerpo.brighter(), cuerpoX, cuerpoY + cuerpoH, colorCuerpo.darker()));
        g2.fillRoundRect(cuerpoX, cuerpoY, cuerpoW, cuerpoH, 14, 14);
        g2.setColor(colorBorde);
        g2.drawRoundRect(cuerpoX, cuerpoY, cuerpoW, cuerpoH, 14, 14);
        g2.setColor(colorDetalle);
        g2.fillRoundRect(cuerpoX + cuerpoW / 2 - 3, cuerpoY + 2, 6, cuerpoH - 4, 4, 4);
        g2.rotate(Math.toRadians(-18.0 * dirX), cuerpoX + cuerpoW / 2.0, cuerpoY + cuerpoH / 2.0);

        g2.setPaint(new GradientPaint(cabezaX, cabezaY, new Color(255, 228, 186), cabezaX, cabezaY + cabezaTam, new Color(236, 188, 148)));
        g2.fillOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(colorBorde);
        g2.drawOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(new Color(48, 32, 24, 210));
        g2.drawArc(cabezaX + 2, cabezaY + 1, cabezaTam - 4, Math.max(3, cabezaTam / 2), dirX >= 0 ? 20 : 0, 160);

        g2.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(245, 206, 168));
        int hombroX = cuerpoX + (dirX >= 0 ? cuerpoW - 6 : 6);
        int hombroY = cuerpoY + 4;
        g2.drawLine(hombroX, hombroY, hombroX + (int) Math.round(dirX * 12.0), hombroY + 10 + (int) Math.round(dirY * 4.0));
        g2.drawLine(cuerpoX + cuerpoW / 3, cuerpoY + 6, cuerpoX + cuerpoW / 3 - (int) Math.round(dirX * 14.0), cuerpoY + cuerpoH + 4);

        g2.setColor(colorDetalle.darker());
        int caderaX = cuerpoX + cuerpoW / 2;
        int caderaY = cuerpoY + cuerpoH - 1;
        g2.drawLine(caderaX, caderaY, caderaX + (int) Math.round(dirX * 18.0), y + alto - 7);
        g2.drawLine(caderaX - (int) Math.round(dirX * 4.0), caderaY, caderaX - (int) Math.round(dirX * 16.0), y + alto - 3);

        g2.setColor(new Color(20, 20, 20));
        g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(caderaX + (int) Math.round(dirX * 15.0) - 4, y + alto - 5, caderaX + (int) Math.round(dirX * 15.0) + 5, y + alto - 5);
        g2.drawLine(caderaX - (int) Math.round(dirX * 16.0) - 5, y + alto - 2, caderaX - (int) Math.round(dirX * 16.0) + 4, y + alto - 2);

        double energia = staminaMax > 0.0 ? Math.max(0.0, Math.min(1.0, stamina / staminaMax)) : 0.0;
        Color barraEnergia = energia > 0.55 ? new Color(88, 220, 120) : (energia > 0.25 ? new Color(255, 198, 80) : new Color(255, 104, 84));
        int barraY = y + alto + 2;
        int barraAncho = Math.max(10, ancho - 4);
        int relleno = (int) Math.round(barraAncho * energia);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(x + 2, barraY, barraAncho, 4, 4, 4);
        g2.setColor(barraEnergia);
        g2.fillRoundRect(x + 2, barraY, Math.max(2, relleno), 4, 4, 4);
    }

    private void dibujarLanzada(Graphics2D g2) {
        double progreso = duracionLanzadaVisual <= 0 ? 1.0 : 1.0 - (framesLanzadaVisual / (double) duracionLanzadaVisual);
        double dirX = Math.abs(lanzadaVisualDirX) < 0.001 ? (direccionX == 0 ? 1.0 : direccionX) : lanzadaVisualDirX;
        double dirY = Math.abs(lanzadaVisualDirY) < 0.001 ? direccionY : lanzadaVisualDirY;
        double norma = Math.hypot(dirX, dirY);
        if (norma < 0.001) {
            dirX = 1.0;
            dirY = 0.0;
            norma = 1.0;
        }
        dirX /= norma;
        dirY /= norma;

        int estiron = 8 + (int) Math.round(progreso * 12.0);
        int sombraW = (int) Math.round(ancho * 1.05) + estiron;
        int sombraX = x + ancho / 2 - sombraW / 2;
        int sombraY = y + alto - 5 + (int) Math.round(dirY * 3.0);
        g2.setColor(new Color(0, 0, 0, 64));
        g2.fillOval(sombraX, sombraY, sombraW, 8);

        int cuerpoW = Math.max(22, (int) Math.round(ancho * 1.02));
        int cuerpoH = Math.max(14, (int) Math.round(alto * 0.30));
        int cuerpoX = x + (ancho - cuerpoW) / 2 + (int) Math.round(dirX * 8.0);
        int cuerpoY = y + alto - cuerpoH - 17 + (int) Math.round(dirY * 4.0);
        int cabezaTam = Math.max(9, ancho / 3);
        double angulo = Math.toRadians(32.0 * dirX + 10.0 * dirY);

        g2.rotate(angulo, cuerpoX + cuerpoW / 2.0, cuerpoY + cuerpoH / 2.0);
        g2.setPaint(new GradientPaint(cuerpoX, cuerpoY, colorCuerpo.brighter(), cuerpoX, cuerpoY + cuerpoH, colorCuerpo.darker()));
        g2.fillRoundRect(cuerpoX, cuerpoY, cuerpoW, cuerpoH, 14, 14);
        g2.setColor(colorBorde);
        g2.drawRoundRect(cuerpoX, cuerpoY, cuerpoW, cuerpoH, 14, 14);
        g2.setColor(colorDetalle);
        g2.fillRoundRect(cuerpoX + cuerpoW / 2 - 3, cuerpoY + 2, 6, cuerpoH - 4, 4, 4);

        int cabezaX = cuerpoX + (dirX >= 0 ? cuerpoW - cabezaTam / 3 : -cabezaTam * 2 / 3);
        int cabezaY = cuerpoY - cabezaTam / 2;
        g2.setPaint(new GradientPaint(cabezaX, cabezaY, new Color(255, 228, 186), cabezaX, cabezaY + cabezaTam, new Color(236, 188, 148)));
        g2.fillOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(colorBorde);
        g2.drawOval(cabezaX, cabezaY, cabezaTam, cabezaTam);

        g2.setStroke(new BasicStroke(3.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(245, 206, 168));
        int hombroX = cuerpoX + (dirX >= 0 ? cuerpoW - 4 : 4);
        int hombroY = cuerpoY + 4;
        g2.drawLine(hombroX, hombroY, hombroX + (int) Math.round(dirX * (18.0 + estiron * 0.45)), hombroY + (int) Math.round(dirY * 8.0));
        g2.drawLine(cuerpoX + cuerpoW / 3, cuerpoY + 7, cuerpoX + cuerpoW / 3 - (int) Math.round(dirX * 10.0), cuerpoY + cuerpoH + 8);

        g2.setColor(colorDetalle.darker());
        int caderaX = cuerpoX + cuerpoW / 2;
        int caderaY = cuerpoY + cuerpoH - 1;
        g2.drawLine(caderaX, caderaY, caderaX + (int) Math.round(dirX * 16.0), y + alto - 7);
        g2.drawLine(caderaX - (int) Math.round(dirX * 3.0), caderaY, caderaX - (int) Math.round(dirX * 12.0), y + alto - 4);
        g2.rotate(-angulo, cuerpoX + cuerpoW / 2.0, cuerpoY + cuerpoH / 2.0);

        g2.setColor(new Color(120, 210, 255, 90));
        g2.fillRoundRect(sombraX - 4, sombraY - 4, Math.max(10, sombraW / 2), 4, 4, 4);

        double energia = staminaMax > 0.0 ? Math.max(0.0, Math.min(1.0, stamina / staminaMax)) : 0.0;
        Color barraEnergia = energia > 0.55 ? new Color(88, 220, 120) : (energia > 0.25 ? new Color(255, 198, 80) : new Color(255, 104, 84));
        int barraY = y + alto + 2;
        int barraAncho = Math.max(10, ancho - 4);
        int relleno = (int) Math.round(barraAncho * energia);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(x + 2, barraY, barraAncho, 4, 4, 4);
        g2.setColor(barraEnergia);
        g2.fillRoundRect(x + 2, barraY, Math.max(2, relleno), 4, 4, 4);
    }

    private void dibujarDerribado(Graphics2D g2) {
        double dirX = direccionX == 0 ? 1.0 : direccionX;
        double dirY = direccionY;
        double norma = Math.hypot(dirX, dirY);
        if (norma < 0.001) {
            dirX = 1.0;
            dirY = 0.0;
            norma = 1.0;
        }
        dirX /= norma;
        dirY /= norma;
        int sombraW = (int) Math.round(ancho * 0.96) + 10;
        int sombraX = x + ancho / 2 - sombraW / 2;
        int sombraY = y + alto - 5;
        g2.setColor(new Color(0, 0, 0, 62));
        g2.fillOval(sombraX, sombraY, sombraW, 8);

        int cuerpoW = Math.max(20, (int) Math.round(ancho * 0.92));
        int cuerpoH = Math.max(14, (int) Math.round(alto * 0.28));
        int cuerpoX = x + (ancho - cuerpoW) / 2;
        int cuerpoY = y + alto - cuerpoH - 12;
        double angulo = Math.toRadians(12.0 * dirX);
        g2.rotate(angulo, cuerpoX + cuerpoW / 2.0, cuerpoY + cuerpoH / 2.0);
        g2.setPaint(new GradientPaint(cuerpoX, cuerpoY, colorCuerpo.brighter(), cuerpoX, cuerpoY + cuerpoH, colorCuerpo.darker()));
        g2.fillRoundRect(cuerpoX, cuerpoY, cuerpoW, cuerpoH, 14, 14);
        g2.setColor(colorBorde);
        g2.drawRoundRect(cuerpoX, cuerpoY, cuerpoW, cuerpoH, 14, 14);
        g2.setColor(colorDetalle);
        g2.fillRoundRect(cuerpoX + cuerpoW / 2 - 3, cuerpoY + 2, 6, cuerpoH - 4, 4, 4);
        g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(245, 206, 168));
        g2.drawLine(cuerpoX + 4, cuerpoY + 5, cuerpoX - 8, cuerpoY + cuerpoH - 2);
        g2.drawLine(cuerpoX + cuerpoW - 4, cuerpoY + 5, cuerpoX + cuerpoW + 10, cuerpoY + cuerpoH - 1);
        g2.setColor(colorDetalle.darker());
        g2.drawLine(cuerpoX + cuerpoW / 3, cuerpoY + cuerpoH - 1, cuerpoX + cuerpoW / 3 - 10, y + alto - 3);
        g2.drawLine(cuerpoX + cuerpoW * 2 / 3, cuerpoY + cuerpoH - 1, cuerpoX + cuerpoW * 2 / 3 + 12, y + alto - 5);
        g2.rotate(-angulo, cuerpoX + cuerpoW / 2.0, cuerpoY + cuerpoH / 2.0);

        int cabezaTam = Math.max(9, ancho / 3);
        int cabezaX = cuerpoX + cuerpoW - cabezaTam / 2;
        int cabezaY = cuerpoY - cabezaTam / 2;
        g2.setPaint(new GradientPaint(cabezaX, cabezaY, new Color(255, 228, 186), cabezaX, cabezaY + cabezaTam, new Color(236, 188, 148)));
        g2.fillOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(colorBorde);
        g2.drawOval(cabezaX, cabezaY, cabezaTam, cabezaTam);

        if (framesLesion > 0) {
            g2.setColor(new Color(255, 110, 92, 110));
            g2.fillOval(x - 4, y + alto - 16, ancho + 8, 18);
        }

        double energia = staminaMax > 0.0 ? Math.max(0.0, Math.min(1.0, stamina / staminaMax)) : 0.0;
        Color barraEnergia = energia > 0.55 ? new Color(88, 220, 120) : (energia > 0.25 ? new Color(255, 198, 80) : new Color(255, 104, 84));
        int barraY = y + alto + 2;
        int barraAncho = Math.max(10, ancho - 4);
        int relleno = (int) Math.round(barraAncho * energia);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(x + 2, barraY, barraAncho, 4, 4, 4);
        g2.setColor(barraEnergia);
        g2.fillRoundRect(x + 2, barraY, Math.max(2, relleno), 4, 4, 4);
    }

    private void dibujarCelebracionFinal(Graphics2D g2) {
        int salto = (int) Math.round(Math.abs(Math.sin(framesReaccionFinal * 0.22)) * 10.0);
        int rebote = (int) Math.round(Math.sin(framesReaccionFinal * 0.22) * 2.0);
        int baseY = y - salto;
        int cabezaTam = Math.max(8, ancho / 3);
        int torsoW = Math.max(11, (int) Math.round(ancho * 0.52));
        int torsoH = Math.max(14, (int) Math.round(alto * 0.42));
        int torsoX = x + (ancho - torsoW) / 2;
        int torsoY = baseY + cabezaTam + rebote;
        int cabezaX = x + (ancho - cabezaTam) / 2;
        int cabezaY = baseY;
        int cuelloX = torsoX + torsoW / 2;
        int caderaX = torsoX + torsoW / 2;
        int caderaY = torsoY + torsoH - 2;

        g2.setColor(new Color(0, 0, 0, 58));
        g2.fillOval(x + ancho / 6, y + alto - 4, (int) Math.round(ancho * 0.66), 7);
        g2.setColor(new Color(255, 236, 120, 58));
        g2.fillOval(x - 4, y + alto - 18, ancho + 8, 18);

        g2.setPaint(new GradientPaint(cabezaX, cabezaY, new Color(255, 228, 186), cabezaX, cabezaY + cabezaTam, new Color(236, 188, 148)));
        g2.fillOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(colorBorde);
        g2.drawOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(new Color(76, 34, 26, 220));
        g2.drawArc(cabezaX + 2, cabezaY + cabezaTam / 3, cabezaTam - 4, Math.max(3, cabezaTam / 3), 190, 160);

        g2.setColor(new Color(234, 182, 142));
        g2.fillRoundRect(cuelloX - 2, cabezaY + cabezaTam - 2, 4, 5, 2, 2);

        g2.setPaint(new GradientPaint(torsoX, torsoY, colorCuerpo.brighter(), torsoX, torsoY + torsoH, colorCuerpo.darker()));
        g2.fillRoundRect(torsoX, torsoY, torsoW, torsoH, 12, 12);
        g2.setColor(colorBorde);
        g2.drawRoundRect(torsoX, torsoY, torsoW, torsoH, 12, 12);
        g2.setColor(colorDetalle);
        g2.fillRoundRect(torsoX + torsoW / 2 - 3, torsoY + 2, 6, torsoH - 4, 4, 4);

        g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(245, 206, 168));
        g2.drawLine(torsoX + 3, torsoY + 6, torsoX - 3, torsoY - 10);
        g2.drawLine(torsoX + torsoW - 3, torsoY + 6, torsoX + torsoW + 3, torsoY - 10);

        g2.setColor(colorDetalle.darker());
        int apertura = 4 + (int) Math.round(Math.abs(Math.sin(framesReaccionFinal * 0.18)) * 3.0);
        g2.drawLine(caderaX - 3, caderaY, caderaX - apertura, baseY + alto - 5);
        g2.drawLine(caderaX + 3, caderaY, caderaX + apertura, baseY + alto - 5);

        g2.setColor(new Color(20, 20, 20));
        g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(caderaX - apertura - 3, baseY + alto - 3, caderaX - apertura + 4, baseY + alto - 3);
        g2.drawLine(caderaX + apertura - 4, baseY + alto - 3, caderaX + apertura + 3, baseY + alto - 3);

        g2.setColor(new Color(255, 250, 180, 88));
        g2.drawOval(x - 2, baseY - 2, ancho + 4, alto + 4);
    }

    private void dibujarDerrotaFinal(Graphics2D g2) {
        int sollozo = (int) Math.round(Math.sin(framesReaccionFinal * 0.18) * 2.0);
        int sombraW = (int) Math.round(ancho * 0.98) + 10;
        int sombraX = x + ancho / 2 - sombraW / 2;
        int sombraY = y + alto - 5;
        g2.setColor(new Color(0, 0, 0, 62));
        g2.fillOval(sombraX, sombraY, sombraW, 8);

        int cuerpoW = Math.max(20, (int) Math.round(ancho * 0.92));
        int cuerpoH = Math.max(14, (int) Math.round(alto * 0.28));
        int cuerpoX = x + (ancho - cuerpoW) / 2;
        int cuerpoY = y + alto - cuerpoH - 12;
        g2.rotate(Math.toRadians(10.0), cuerpoX + cuerpoW / 2.0, cuerpoY + cuerpoH / 2.0);
        g2.setPaint(new GradientPaint(cuerpoX, cuerpoY, colorCuerpo.brighter(), cuerpoX, cuerpoY + cuerpoH, colorCuerpo.darker()));
        g2.fillRoundRect(cuerpoX, cuerpoY, cuerpoW, cuerpoH, 14, 14);
        g2.setColor(colorBorde);
        g2.drawRoundRect(cuerpoX, cuerpoY, cuerpoW, cuerpoH, 14, 14);
        g2.setColor(colorDetalle);
        g2.fillRoundRect(cuerpoX + cuerpoW / 2 - 3, cuerpoY + 2, 6, cuerpoH - 4, 4, 4);
        g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(245, 206, 168));
        g2.drawLine(cuerpoX + 5, cuerpoY + 6, cuerpoX - 8, cuerpoY + cuerpoH + 1);
        g2.drawLine(cuerpoX + cuerpoW - 5, cuerpoY + 7, cuerpoX + cuerpoW + 9, cuerpoY + cuerpoH + 3);
        g2.setColor(colorDetalle.darker());
        g2.drawLine(cuerpoX + cuerpoW / 3, cuerpoY + cuerpoH - 1, cuerpoX + cuerpoW / 3 - 10, y + alto - 3);
        g2.drawLine(cuerpoX + cuerpoW * 2 / 3, cuerpoY + cuerpoH - 1, cuerpoX + cuerpoW * 2 / 3 + 12, y + alto - 5);
        g2.rotate(Math.toRadians(-10.0), cuerpoX + cuerpoW / 2.0, cuerpoY + cuerpoH / 2.0);

        int cabezaTam = Math.max(9, ancho / 3);
        int cabezaX = cuerpoX + cuerpoW - cabezaTam / 2;
        int cabezaY = cuerpoY - cabezaTam / 2 + sollozo;
        g2.setPaint(new GradientPaint(cabezaX, cabezaY, new Color(255, 228, 186), cabezaX, cabezaY + cabezaTam, new Color(236, 188, 148)));
        g2.fillOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(colorBorde);
        g2.drawOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(new Color(72, 46, 38, 210));
        g2.drawArc(cabezaX + 2, cabezaY + cabezaTam / 2, cabezaTam - 4, Math.max(3, cabezaTam / 3), 15, 150);
        g2.setColor(new Color(120, 190, 255, 120));
        g2.fillOval(cabezaX + cabezaTam - 4, cabezaY + cabezaTam / 2, 3, 7);
        g2.fillOval(cabezaX + cabezaTam - 7, cabezaY + cabezaTam / 2 + 2, 2, 5);
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
        if (framesBarridaVisual > 0) {
            framesBarridaVisual--;
        }
        if (framesLanzadaVisual > 0) {
            framesLanzadaVisual--;
        }
        if (framesDerribado > 0) {
            framesDerribado--;
        }
        if (framesLesion > 0) {
            framesLesion--;
        }
        if (celebracionFinalActiva || derrotaFinalActiva) {
            framesReaccionFinal++;
        } else {
            framesReaccionFinal = 0;
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
        if (framesDerribado > 0) {
            return 0;
        }
        if (celebracionFinalActiva || derrotaFinalActiva) {
            return 0;
        }
        int velocidadNormal = Math.max(1, getVelocidad() - 2);
        if (framesLesion > 0) {
            velocidadNormal = Math.max(1, velocidadNormal - 1);
        }
        if (!sprintando || agotado || stamina < 8.0) {
            return velocidadNormal;
        }
        int velocidadSprint = Math.max(2, getVelocidad() - 1);
        if (framesLesion > 0) {
            velocidadSprint = Math.max(1, velocidadSprint - 1);
        }
        return velocidadSprint;
    }

    public boolean puedeSprintar() {
        return !agotado && framesDerribado <= 0 && framesLesion <= 0 && !celebracionFinalActiva && !derrotaFinalActiva && stamina >= 8.0;
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

    public int getEntrada() {
        return entrada;
    }

    public void setEntrada(int entrada) {
        this.entrada = Math.max(35, Math.min(85, entrada));
    }

    public int getDisciplina() {
        return disciplina;
    }

    public void setDisciplina(int disciplina) {
        this.disciplina = Math.max(35, Math.min(85, disciplina));
    }

    public int getFaltasCometidas() {
        return faltasCometidas;
    }

    public int getTarjetasAmarillas() {
        return tarjetasAmarillas;
    }

    public boolean tieneTarjetaRoja() {
        return tarjetaRoja;
    }

    public boolean estaExpulsado() {
        return expulsado;
    }

    public boolean estaDerribado() {
        return framesDerribado > 0;
    }

    public boolean estaLesionado() {
        return framesLesion > 0;
    }

    public void limpiarDisciplina() {
        faltasCometidas = 0;
        tarjetasAmarillas = 0;
        tarjetaRoja = false;
        expulsado = false;
    }

    public void registrarFaltaCometida() {
        faltasCometidas++;
    }

    public void derribar(int duracionFrames, double dirX, double dirY) {
        framesDerribado = Math.max(framesDerribado, Math.max(1, duracionFrames));
        activarAnimacionBarrida(Math.max(10, duracionFrames), dirX, dirY);
        if (Math.abs(dirX) >= 0.001) {
            direccionX = dirX > 0.0 ? 1 : -1;
        }
        if (Math.abs(dirY) >= 0.001) {
            direccionY = dirY > 0.0 ? 1 : -1;
        }
    }

    public void aplicarLesionTemporal(int duracionFrames) {
        framesLesion = Math.max(framesLesion, Math.max(1, duracionFrames));
    }

    public void activarCelebracionFinal() {
        celebracionFinalActiva = true;
        derrotaFinalActiva = false;
        framesReaccionFinal = 0;
        framesDerribado = 0;
        framesBarridaVisual = 0;
        framesLanzadaVisual = 0;
    }

    public void activarDerrotaFinal() {
        derrotaFinalActiva = true;
        celebracionFinalActiva = false;
        framesReaccionFinal = 0;
        framesDerribado = 0;
        framesBarridaVisual = 0;
        framesLanzadaVisual = 0;
    }

    public void limpiarReaccionFinal() {
        celebracionFinalActiva = false;
        derrotaFinalActiva = false;
        framesReaccionFinal = 0;
    }

    public boolean recibirAmarilla() {
        tarjetasAmarillas++;
        if (tarjetasAmarillas >= 2) {
            tarjetaRoja = true;
            expulsado = true;
            return true;
        }
        return false;
    }

    public void recibirRojaDirecta() {
        tarjetaRoja = true;
        expulsado = true;
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
        if (framesLanzadaVisual > 0) {
            faseAnimacion += 0.20;
            if (Math.abs(lanzadaVisualDirX) >= 0.001) {
                direccionX = lanzadaVisualDirX > 0.0 ? 1 : -1;
            }
            if (Math.abs(lanzadaVisualDirY) >= 0.001) {
                direccionY = lanzadaVisualDirY > 0.0 ? 1 : -1;
            }
            return;
        }
        if (framesBarridaVisual > 0) {
            faseAnimacion += 0.18;
            if (Math.abs(dx) >= Math.abs(dy)) {
                direccionX = dx == 0 ? (direccionX == 0 ? 1 : direccionX) : (dx > 0 ? 1 : -1);
                if (dy != 0) {
                    direccionY = dy > 0 ? 1 : -1;
                }
            } else if (dy != 0) {
                direccionY = dy > 0 ? 1 : -1;
                if (dx != 0) {
                    direccionX = dx > 0 ? 1 : -1;
                }
            }
            return;
        }
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

    public void activarAnimacionBarrida(int duracionFrames, double dirX, double dirY) {
        duracionBarridaVisual = Math.max(1, duracionFrames);
        framesBarridaVisual = duracionBarridaVisual;
        barridaVisualDirX = dirX;
        barridaVisualDirY = dirY;
        if (Math.abs(dirX) >= 0.001) {
            direccionX = dirX > 0.0 ? 1 : -1;
        }
        if (Math.abs(dirY) >= 0.001) {
            direccionY = dirY > 0.0 ? 1 : -1;
        }
    }

    public void activarAnimacionLanzada(int duracionFrames, double dirX, double dirY) {
        duracionLanzadaVisual = Math.max(1, duracionFrames);
        framesLanzadaVisual = duracionLanzadaVisual;
        lanzadaVisualDirX = dirX;
        lanzadaVisualDirY = dirY;
        if (Math.abs(dirX) >= 0.001) {
            direccionX = dirX > 0.0 ? 1 : -1;
        }
        if (Math.abs(dirY) >= 0.001) {
            direccionY = dirY > 0.0 ? 1 : -1;
        }
    }
}
