package juego.entidades;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;

// Jugador vectorial con orientacion, animacion y estado de turbo.
public class Jugador extends EntidadJuego {
    public enum AnimacionSprite {
        IDLE,
        RUN,
        SPRINT,
        KICK,
        SLIDE,
        DIVE,
        DOWN,
        CELEBRATE,
        DEFEAT
    }

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
    private int framesCelebracionEvento;
    private int duracionCelebracionEvento;
    private int framesPatadaVisual;
    private int duracionPatadaVisual;
    private boolean sprintVisualActivo;
    private int intensidadMovimientoVisual;
    private boolean celebracionFinalActiva;
    private boolean derrotaFinalActiva;
    private int framesReaccionFinal;
    private boolean portero;
    private boolean arbitro;
    private int framesHidratacion;

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
        this.framesCelebracionEvento = 0;
        this.duracionCelebracionEvento = 0;
        this.framesPatadaVisual = 0;
        this.duracionPatadaVisual = 0;
        this.sprintVisualActivo = false;
        this.intensidadMovimientoVisual = 0;
        this.celebracionFinalActiva = false;
        this.derrotaFinalActiva = false;
        this.framesReaccionFinal = 0;
        this.portero = false;
        this.arbitro = false;
        this.framesHidratacion = 0;
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
        if (framesCelebracionEvento > 0) {
            dibujarCelebracionEvento(g2);
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
        double pulso = Math.sin(faseAnimacion);
        double pulsoComplemento = Math.sin(faseAnimacion + Math.PI / 2.0);
        boolean moviendose = intensidadMovimientoVisual > 0;
        boolean corriendo = sprintVisualActivo && moviendose;
        int oscilacion = moviendose ? (int) Math.round(pulso * (corriendo ? 3.0 : 2.0)) : (int) Math.round(Math.sin(faseAnimacion * 0.55) * 1.5);
        int braceo = (int) Math.round(pulso * (corriendo ? 7.0 : 4.0));
        int zancada = (int) Math.round(pulsoComplemento * (corriendo ? 7.0 : 4.0));
        int inclinacionX = direccionX * Math.max(2, ancho / 8) + (corriendo ? direccionX * 2 : 0);
        int inclinacionY = direccionY * Math.max(1, alto / 12);
        int cabezaTam = calcCabeza(ancho);
        int torsoW = calcTorsoW(ancho);
        int torsoH = calcTorsoH(alto);
        int torsoX = x + (ancho - torsoW) / 2;
        int torsoY = y + cabezaTam - 1 + oscilacion + (agotado ? 2 : 0);
        int caderaY = torsoY + torsoH - 2;
        int cuelloX = torsoX + torsoW / 2;
        int hombroY = torsoY + 6 + (agotado ? 1 : 0);
        int caderaX = torsoX + torsoW / 2;
        int cabezaX = x + (ancho - cabezaTam) / 2 + inclinacionX;
        int cabezaY = y + inclinacionY + (agotado ? 2 : 0);
        int dorsal = getNumeroCamiseta();
        int hombroIzqX = torsoX + 3;
        int hombroDerX = torsoX + torsoW - 3;
        int manoIzqX = torsoX - 4 - direccionX;
        int manoIzqY = hombroY + 10 + braceo + (corriendo ? 2 : 0);
        int manoDerX = torsoX + torsoW + 4 - direccionX;
        int manoDerY = hombroY + 10 - braceo;
        int piernaBaseY = caderaY + 4;
        int piernaPaso = Math.max(4, ancho / 5) + (corriendo ? 1 : 0);
        int rodillaIzqX = caderaX - Math.max(2, piernaPaso / 3);
        int rodillaIzqY = piernaBaseY + 6 + Math.max(0, zancada / 2);
        int rodillaDerX = caderaX + Math.max(2, piernaPaso / 3);
        int rodillaDerY = piernaBaseY + 6 - Math.min(0, zancada / 2);
        int pieIzqX = caderaX - piernaPaso;
        int pieIzqY = y + alto - 5 - zancada;
        int pieDerX = caderaX + piernaPaso;
        int pieDerY = y + alto - 5 + zancada;

        // Sombra de contacto con el piso.
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillOval(x + ancho / 6 - (corriendo ? 2 : 0), y + alto - 4, (int) Math.round(ancho * 0.66) + (corriendo ? 4 : 0), 7);

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
        Color pielClara = getColorPielBase();
        Color pielOscura = pielClara.darker();
        g2.setPaint(new GradientPaint(cabezaX, cabezaY, pielClara, cabezaX, cabezaY + cabezaTam, pielOscura));
        g2.fillOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(colorBorde);
        g2.drawOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(new Color(255, 255, 255, 110));
        g2.fillOval(cabezaX + 2, cabezaY + 2, Math.max(2, cabezaTam / 3), Math.max(2, cabezaTam / 4));
        g2.setColor(new Color(48, 32, 24, 210));
        g2.drawArc(cabezaX + 2, cabezaY + 1, cabezaTam - 4, Math.max(3, cabezaTam / 2), 10, 160);
        dibujarExpresionFacial(g2, cabezaX, cabezaY, cabezaTam, corriendo || moviendose, false);

        // Cuello.
        g2.setColor(new Color(Math.max(0, pielClara.getRed() - 18), Math.max(0, pielClara.getGreen() - 24), Math.max(0, pielClara.getBlue() - 26)));
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
        g2.fillRoundRect(torsoX + 2, torsoY + 3, 5, torsoH - 6, 4, 4);
        g2.fillRoundRect(torsoX + torsoW - 7, torsoY + 3, 5, torsoH - 6, 4, 4);
        g2.fillOval(torsoX - 2, hombroY - 2, 7, 7);
        g2.fillOval(torsoX + torsoW - 5, hombroY - 2, 7, 7);
        g2.setColor(new Color(255, 255, 255, 190));
        g2.drawLine(torsoX + 2, torsoY + 4, torsoX + torsoW - 3, torsoY + 4);
        Color shortColor = mezclarColores(colorDetalle.darker(), colorBorde, 0.35);
        g2.setColor(shortColor);
        g2.fillRoundRect(torsoX + 1, caderaY - 2, torsoW - 2, Math.max(6, alto / 8), 8, 8);
        g2.setColor(colorBorde);
        g2.drawLine(torsoX + torsoW / 2, caderaY - 1, torsoX + torsoW / 2, caderaY + Math.max(4, alto / 8) - 1);
        g2.setColor(new Color(250, 250, 250, 210));
        Font dorsalFont = new Font("SansSerif", Font.BOLD, Math.max(8, torsoW / 3 + 4));
        g2.setFont(dorsalFont);
        String dorsalTexto = String.valueOf(dorsal);
        int dorsalX = torsoX + torsoW / 2 - g2.getFontMetrics().stringWidth(dorsalTexto) / 2;
        g2.drawString(dorsalTexto, dorsalX, torsoY + torsoH / 2 + 4);

        dibujarBrazo(g2, hombroIzqX, hombroY, torsoX - 1, hombroY + 6 + braceo / 2, manoIzqX, manoIzqY, pielClara, 3.4f);
        dibujarBrazo(g2, hombroDerX, hombroY, torsoX + torsoW + 1, hombroY + 6 - braceo / 2, manoDerX, manoDerY, pielClara, 3.4f);
        dibujarPierna(g2, caderaX - 3, piernaBaseY, rodillaIzqX, rodillaIzqY, pieIzqX, pieIzqY, colorDetalle.darker(), colorDetalle);
        dibujarPierna(g2, caderaX + 3, piernaBaseY, rodillaDerX, rodillaDerY, pieDerX, pieDerY, colorDetalle.darker(), colorDetalle);

        if (corriendo) {
            g2.setColor(new Color(255, 255, 255, 48));
            g2.drawArc(x - 2, y + 4, ancho + 4, alto - 8, 210, 96);
        }

        if (agotado) {
            g2.setColor(new Color(120, 190, 255, 104));
            g2.fillOval(cabezaX + cabezaTam - 4, cabezaY + cabezaTam / 2, 3, 7);
        }

        dibujarBarraEnergia(g2, x + 2, y + alto + 4, Math.max(10, ancho - 4));

        g2.dispose();
    }

    public void dibujarFiguraEvento(Graphics2D g2, int x, int y, int w, int h, boolean celebrar) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cabezaTam = Math.max(12, calcCabeza(w));
        int torsoW = Math.max(16, calcTorsoW(w));
        int torsoH = Math.max(14, calcTorsoH(h));
        int torsoX = x + (w - torsoW) / 2;
        int torsoY = y + cabezaTam + 2;
        int cabezaX = x + (w - cabezaTam) / 2;
        int caderaX = torsoX + torsoW / 2;
        int caderaY = torsoY + torsoH - 2;
        int dir = celebrar ? 1 : (direccionX == 0 ? 1 : direccionX);
        Color piel = getColorPielBase();

        g.setColor(new Color(0, 0, 0, 52));
        g.fillOval(x + 6, y + h - 8, w - 12, 12);
        g.setColor(new Color(255, 240, 156, 72));
        g.fillOval(x - 2, y + h - 20, w + 4, 20);

        g.setPaint(new GradientPaint(cabezaX, y, piel, cabezaX, y + cabezaTam, piel.darker()));
        g.fillOval(cabezaX, y, cabezaTam, cabezaTam);
        g.setColor(colorBorde);
        g.drawOval(cabezaX, y, cabezaTam, cabezaTam);
        dibujarExpresionFacial(g, cabezaX, y, cabezaTam, celebrar, true);

        g.setColor(new Color(Math.max(0, piel.getRed() - 20), Math.max(0, piel.getGreen() - 24), Math.max(0, piel.getBlue() - 28)));
        g.fillRoundRect(torsoX + torsoW / 2 - 2, y + cabezaTam - 2, 4, 6, 2, 2);

        g.setPaint(new GradientPaint(torsoX, torsoY, colorCuerpo.brighter(), torsoX, torsoY + torsoH, colorCuerpo.darker()));
        g.fillRoundRect(torsoX, torsoY, torsoW, torsoH, 14, 14);
        g.setColor(colorBorde);
        g.drawRoundRect(torsoX, torsoY, torsoW, torsoH, 14, 14);
        g.setColor(colorDetalle);
        g.fillRoundRect(torsoX + torsoW / 2 - 3, torsoY + 2, 6, torsoH - 4, 4, 4);
        g.fillRoundRect(torsoX + 2, torsoY + 4, 5, torsoH - 8, 4, 4);
        g.fillRoundRect(torsoX + torsoW - 7, torsoY + 4, 5, torsoH - 8, 4, 4);
        g.setColor(new Color(255, 255, 255, 196));
        g.drawLine(torsoX + 3, torsoY + 5, torsoX + torsoW - 4, torsoY + 5);

        String dorsal = String.valueOf(getNumeroCamiseta());
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, torsoW / 3 + 3)));
        int dorsalX = torsoX + torsoW / 2 - g.getFontMetrics().stringWidth(dorsal) / 2;
        g.drawString(dorsal, dorsalX, torsoY + torsoH / 2 + 5);

        Color shortColor = mezclarColores(colorDetalle.darker(), colorBorde, 0.35);
        g.setColor(shortColor);
        g.fillRoundRect(torsoX + 1, caderaY - 1, torsoW - 2, Math.max(8, h / 8), 8, 8);

        int hombroY = torsoY + 8;
        Stroke anterior = g.getStroke();
        g.setStroke(new BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(piel);
        if (celebrar) {
            g.drawLine(torsoX + 4, hombroY, torsoX - 2, torsoY - 10);
            g.drawLine(torsoX + torsoW - 4, hombroY, torsoX + torsoW + 4, torsoY - 10);
        } else {
            g.drawLine(torsoX + 4, hombroY, torsoX - 3, torsoY + 6);
            g.drawLine(torsoX + torsoW - 4, hombroY, torsoX + torsoW + 3, torsoY + 6);
        }
        g.setStroke(new BasicStroke(3.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(colorDetalle.darker());
        g.drawLine(caderaX - 4, caderaY + 5, caderaX - 8, y + h - 14);
        g.drawLine(caderaX + 4, caderaY + 5, caderaX + 8, y + h - 14);
        g.setColor(colorDetalle);
        g.drawLine(caderaX - 8, y + h - 18, caderaX - 8, y + h - 12);
        g.drawLine(caderaX + 8, y + h - 18, caderaX + 8, y + h - 12);
        g.setColor(new Color(20, 20, 22));
        g.fillRoundRect(caderaX - 13, y + h - 13, 10, 5, 4, 4);
        g.fillRoundRect(caderaX + 3, y + h - 13, 10, 5, 4, 4);
        g.setStroke(anterior);
        g.dispose();
    }

    private void dibujarBrazo(Graphics2D g2, int hombroX, int hombroY, int codoX, int codoY, int manoX, int manoY, Color colorPiel, float grosor) {
        g2.setStroke(new BasicStroke(grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(colorPiel);
        g2.drawLine(hombroX, hombroY, codoX, codoY);
        g2.drawLine(codoX, codoY, manoX, manoY);
        g2.fillOval(codoX - 2, codoY - 2, 4, 4);
        g2.fillOval(manoX - 3, manoY - 3, 6, 6);
    }

    private void dibujarPierna(Graphics2D g2, int caderaX, int caderaY, int rodillaX, int rodillaY, int pieX, int pieY, Color colorPierna, Color colorMedia) {
        g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(colorPierna);
        g2.drawLine(caderaX, caderaY, rodillaX, rodillaY);
        g2.drawLine(rodillaX, rodillaY, pieX, pieY - 4);
        g2.fillOval(rodillaX - 2, rodillaY - 2, 4, 4);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(colorMedia);
        g2.drawLine(pieX, pieY - 9, pieX, pieY - 5);
        g2.setColor(new Color(20, 20, 20));
        g2.fillRoundRect(pieX - 5, pieY - 3, 10, 4, 4, 4);
    }

    public int getNumeroCamiseta() {
        return Math.abs(nombre.hashCode()) % 89 + 10;
    }

    private Color getColorPielBase() {
        int tono = Math.abs(nombre.hashCode());
        int variante = tono % 4;
        if (variante == 0) {
            return new Color(255, 224, 188);
        }
        if (variante == 1) {
            return new Color(236, 194, 150);
        }
        if (variante == 2) {
            return new Color(214, 164, 120);
        }
        return new Color(176, 122, 88);
    }

    private Color mezclarColores(Color a, Color b, double proporcionB) {
        double pb = Math.max(0.0, Math.min(1.0, proporcionB));
        double pa = 1.0 - pb;
        return new Color(
            (int) Math.round(a.getRed() * pa + b.getRed() * pb),
            (int) Math.round(a.getGreen() * pa + b.getGreen() * pb),
            (int) Math.round(a.getBlue() * pa + b.getBlue() * pb)
        );
    }

    private int calcCabeza(int w) {
        return Math.max(8, w / 4);
    }

    private int calcTorsoW(int w) {
        return Math.max(11, (int) Math.round(w * 0.58));
    }

    private int calcTorsoH(int h) {
        return Math.max(14, (int) Math.round(h * 0.46));
    }

    private void dibujarBarraEnergia(Graphics2D g2, int barraX, int barraY, int barraAncho) {
        double energia = staminaMax > 0.0 ? Math.max(0.0, Math.min(1.0, stamina / staminaMax)) : 0.0;
        Color barraColor = energia > 0.66 ? new Color(72, 200, 120) : (energia > 0.33 ? new Color(255, 198, 80) : new Color(255, 96, 88));
        int barraAltura = 8;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(barraX - 1, barraY - 1, barraAncho + 2, barraAltura + 2, barraAltura, barraAltura);
        g2.setColor(new Color(40, 40, 44, 200));
        g2.fillRoundRect(barraX, barraY, barraAncho, barraAltura, barraAltura - 2, barraAltura - 2);
        int relleno = (int) Math.round(barraAncho * energia);
        g2.setColor(barraColor);
        g2.fillRoundRect(barraX, barraY, Math.max(2, relleno), barraAltura, barraAltura - 2, barraAltura - 2);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawRoundRect(barraX, barraY, barraAncho, barraAltura, barraAltura - 2, barraAltura - 2);
    }

    private void dibujarExpresionFacial(Graphics2D g2, int cabezaX, int cabezaY, int cabezaTam, boolean emocionAlta, boolean retrato) {
        int ojoY = cabezaY + cabezaTam / 2 - 1;
        int ojoIzqX = cabezaX + cabezaTam / 3 - 2;
        int ojoDerX = cabezaX + cabezaTam * 2 / 3 - 2;
        int energia = staminaMax > 0.0 ? (int) Math.round((stamina / staminaMax) * 100.0) : 0;
        boolean exhausto = agotado || energia < 28;
        boolean lesionado = framesLesion > 0 || framesDerribado > 0;
        boolean celebrando = emocionAlta && (framesCelebracionEvento > 0 || celebracionFinalActiva || retrato);

        Stroke previo = g2.getStroke();
        g2.setStroke(new BasicStroke(retrato ? 1.8f : 1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(22, 18, 18, 220));

        if (exhausto) {
            g2.drawLine(ojoIzqX - 1, ojoY + 1, ojoIzqX + 3, ojoY + 2);
            g2.drawLine(ojoDerX - 1, ojoY + 2, ojoDerX + 3, ojoY + 1);
        } else {
            g2.fillOval(ojoIzqX, ojoY, retrato ? 4 : 3, retrato ? 4 : 3);
            g2.fillOval(ojoDerX, ojoY, retrato ? 4 : 3, retrato ? 4 : 3);
        }

        if (lesionado) {
            g2.drawArc(cabezaX + cabezaTam / 2 - 4, cabezaY + cabezaTam * 2 / 3 + 1, 8, 5, 20, 140);
            g2.drawLine(cabezaX + 2, cabezaY + 2, cabezaX + 5, cabezaY + 5);
        } else if (celebrando) {
            g2.drawArc(cabezaX + cabezaTam / 2 - 5, cabezaY + cabezaTam * 2 / 3 - 1, 10, 6, 180, 180);
        } else if (exhausto) {
            g2.drawArc(cabezaX + cabezaTam / 2 - 4, cabezaY + cabezaTam * 2 / 3 + 2, 8, 4, 20, 140);
        } else {
            g2.drawArc(cabezaX + cabezaTam / 2 - 4, cabezaY + cabezaTam * 2 / 3 + 1, 8, 4, 180, 180);
        }

        g2.setStroke(previo);
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

        int cuerpoW = calcTorsoW(ancho);
        int cuerpoH = calcTorsoH(alto);
        int cuerpoX = x + (ancho - cuerpoW) / 2 + (int) Math.round(dirX * 4.0);
        int cuerpoY = y + alto - cuerpoH - 12 + (int) Math.round(dirY * 2.0);
        int cabezaTam = calcCabeza(ancho);
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

        Color piel = getColorPielBase();
        g2.setPaint(new GradientPaint(cabezaX, cabezaY, piel, cabezaX, cabezaY + cabezaTam, piel.darker()));
        g2.fillOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(colorBorde);
        g2.drawOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(new Color(48, 32, 24, 210));
        g2.drawArc(cabezaX + 2, cabezaY + 1, cabezaTam - 4, Math.max(3, cabezaTam / 2), dirX >= 0 ? 20 : 0, 160);
        dibujarExpresionFacial(g2, cabezaX, cabezaY, cabezaTam, true, false);

        int hombroX = cuerpoX + (dirX >= 0 ? cuerpoW - 6 : 6);
        int hombroY = cuerpoY + 4;
        int caderaX = cuerpoX + cuerpoW / 2;
        int caderaY = cuerpoY + cuerpoH - 1;
        dibujarBrazo(g2, hombroX, hombroY, hombroX + (int) Math.round(dirX * 7.0), hombroY + 6, hombroX + (int) Math.round(dirX * 12.0), hombroY + 10 + (int) Math.round(dirY * 4.0), piel, 3.4f);
        dibujarBrazo(g2, cuerpoX + cuerpoW / 3, cuerpoY + 6, cuerpoX + cuerpoW / 3 - (int) Math.round(dirX * 7.0), cuerpoY + cuerpoH / 2, cuerpoX + cuerpoW / 3 - (int) Math.round(dirX * 14.0), cuerpoY + cuerpoH + 4, piel, 3.0f);
        dibujarPierna(g2, caderaX, caderaY, caderaX + (int) Math.round(dirX * 10.0), y + alto - 14, caderaX + (int) Math.round(dirX * 18.0), y + alto - 7, colorDetalle.darker(), colorDetalle);
        dibujarPierna(g2, caderaX - (int) Math.round(dirX * 4.0), caderaY, caderaX - (int) Math.round(dirX * 9.0), y + alto - 10, caderaX - (int) Math.round(dirX * 16.0), y + alto - 3, colorDetalle.darker(), colorDetalle);

        int barraY = y + alto + 4;
        int barraAncho = Math.max(10, ancho - 4);
        dibujarBarraEnergia(g2, x + 2, barraY, barraAncho);
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

        int cuerpoW = calcTorsoW(ancho);
        int cuerpoH = calcTorsoH(alto);
        int cuerpoX = x + (ancho - cuerpoW) / 2 + (int) Math.round(dirX * 8.0);
        int cuerpoY = y + alto - cuerpoH - 17 + (int) Math.round(dirY * 4.0);
        int cabezaTam = calcCabeza(ancho);
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
        Color piel = getColorPielBase();
        g2.setPaint(new GradientPaint(cabezaX, cabezaY, piel, cabezaX, cabezaY + cabezaTam, piel.darker()));
        g2.fillOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g2.setColor(colorBorde);
        g2.drawOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        dibujarExpresionFacial(g2, cabezaX, cabezaY, cabezaTam, true, false);

        int hombroX = cuerpoX + (dirX >= 0 ? cuerpoW - 4 : 4);
        int hombroY = cuerpoY + 4;
        int caderaX = cuerpoX + cuerpoW / 2;
        int caderaY = cuerpoY + cuerpoH - 1;
        dibujarBrazo(g2, hombroX, hombroY, hombroX + (int) Math.round(dirX * (10.0 + estiron * 0.20)), hombroY + (int) Math.round(dirY * 4.0), hombroX + (int) Math.round(dirX * (18.0 + estiron * 0.45)), hombroY + (int) Math.round(dirY * 8.0), piel, 3.8f);
        dibujarBrazo(g2, cuerpoX + cuerpoW / 3, cuerpoY + 7, cuerpoX + cuerpoW / 3 - (int) Math.round(dirX * 5.0), cuerpoY + cuerpoH / 2 + 4, cuerpoX + cuerpoW / 3 - (int) Math.round(dirX * 10.0), cuerpoY + cuerpoH + 8, piel, 3.2f);
        dibujarPierna(g2, caderaX, caderaY, caderaX + (int) Math.round(dirX * 8.0), y + alto - 15, caderaX + (int) Math.round(dirX * 16.0), y + alto - 7, colorDetalle.darker(), colorDetalle);
        dibujarPierna(g2, caderaX - (int) Math.round(dirX * 3.0), caderaY, caderaX - (int) Math.round(dirX * 6.0), y + alto - 11, caderaX - (int) Math.round(dirX * 12.0), y + alto - 4, colorDetalle.darker(), colorDetalle);
        g2.rotate(-angulo, cuerpoX + cuerpoW / 2.0, cuerpoY + cuerpoH / 2.0);

        g2.setColor(new Color(120, 210, 255, 90));
        g2.fillRoundRect(sombraX - 4, sombraY - 4, Math.max(10, sombraW / 2), 4, 4, 4);

        int barraY = y + alto + 4;
        int barraAncho = Math.max(10, ancho - 4);
        dibujarBarraEnergia(g2, x + 2, barraY, barraAncho);
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

        int cuerpoW = calcTorsoW(ancho);
        int cuerpoH = calcTorsoH(alto);
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
        dibujarBrazo(g2, cuerpoX + 4, cuerpoY + 5, cuerpoX - 1, cuerpoY + cuerpoH / 2, cuerpoX - 8, cuerpoY + cuerpoH - 2, new Color(245, 206, 168), 3.2f);
        dibujarBrazo(g2, cuerpoX + cuerpoW - 4, cuerpoY + 5, cuerpoX + cuerpoW + 4, cuerpoY + cuerpoH / 2, cuerpoX + cuerpoW + 10, cuerpoY + cuerpoH - 1, new Color(245, 206, 168), 3.2f);
        dibujarPierna(g2, cuerpoX + cuerpoW / 3, cuerpoY + cuerpoH - 1, cuerpoX + cuerpoW / 3 - 4, y + alto - 9, cuerpoX + cuerpoW / 3 - 10, y + alto - 3, colorDetalle.darker(), colorDetalle);
        dibujarPierna(g2, cuerpoX + cuerpoW * 2 / 3, cuerpoY + cuerpoH - 1, cuerpoX + cuerpoW * 2 / 3 + 5, y + alto - 10, cuerpoX + cuerpoW * 2 / 3 + 12, y + alto - 5, colorDetalle.darker(), colorDetalle);
        g2.rotate(-angulo, cuerpoX + cuerpoW / 2.0, cuerpoY + cuerpoH / 2.0);

        int cabezaTam = calcCabeza(ancho);
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

        int barraY = y + alto + 4;
        int barraAncho = Math.max(10, ancho - 4);
        dibujarBarraEnergia(g2, x + 2, barraY, barraAncho);
    }

    private void dibujarCelebracionFinal(Graphics2D g2) {
        dibujarCelebracion(g2, framesReaccionFinal, new Color(255, 236, 120, 58));
    }

    private void dibujarCelebracionEvento(Graphics2D g2) {
        int frameCelebracion = duracionCelebracionEvento <= 0 ? 0 : duracionCelebracionEvento - framesCelebracionEvento;
        dibujarCelebracion(g2, frameCelebracion, new Color(255, 240, 160, 72));
    }

    private void dibujarCelebracion(Graphics2D g2, int frameCelebracion, Color haloColor) {
        int salto = (int) Math.round(Math.abs(Math.sin(frameCelebracion * 0.22)) * 10.0);
        int rebote = (int) Math.round(Math.sin(frameCelebracion * 0.22) * 2.0);
        int baseY = y - salto;
        int cabezaTam = calcCabeza(ancho);
        int torsoW = calcTorsoW(ancho);
        int torsoH = calcTorsoH(alto);
        int torsoX = x + (ancho - torsoW) / 2;
        int torsoY = baseY + cabezaTam + rebote;
        int cabezaX = x + (ancho - cabezaTam) / 2;
        int cabezaY = baseY;
        int cuelloX = torsoX + torsoW / 2;
        int caderaX = torsoX + torsoW / 2;
        int caderaY = torsoY + torsoH - 2;

        g2.setColor(new Color(0, 0, 0, 58));
        g2.fillOval(x + ancho / 6, y + alto - 4, (int) Math.round(ancho * 0.66), 7);
        g2.setColor(haloColor);
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

        int apertura = 4 + (int) Math.round(Math.abs(Math.sin(frameCelebracion * 0.18)) * 3.0);
        dibujarBrazo(g2, torsoX + 3, torsoY + 6, torsoX - 1, torsoY - 2, torsoX - 3, torsoY - 10, new Color(245, 206, 168), 3.5f);
        dibujarBrazo(g2, torsoX + torsoW - 3, torsoY + 6, torsoX + torsoW + 1, torsoY - 2, torsoX + torsoW + 3, torsoY - 10, new Color(245, 206, 168), 3.5f);
        dibujarPierna(g2, caderaX - 3, caderaY, caderaX - apertura + 1, baseY + alto - 12, caderaX - apertura, baseY + alto - 5, colorDetalle.darker(), colorDetalle);
        dibujarPierna(g2, caderaX + 3, caderaY, caderaX + apertura - 1, baseY + alto - 12, caderaX + apertura, baseY + alto - 5, colorDetalle.darker(), colorDetalle);

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

        int cuerpoW = calcTorsoW(ancho);
        int cuerpoH = calcTorsoH(alto);
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

        int cabezaTam = calcCabeza(ancho);
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
        sprintVisualActivo = sprintando;
        intensidadMovimientoVisual = Math.max(0, intensidadMovimiento);
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
        if (framesCelebracionEvento > 0) {
            framesCelebracionEvento--;
        }
        if (framesPatadaVisual > 0) {
            framesPatadaVisual--;
        }
        if (framesHidratacion > 0) {
            framesHidratacion = Math.max(0, framesHidratacion - 1);
            sprintVisualActivo = false;
            intensidadMovimientoVisual = 0;
            if (agotado && stamina >= 34.0) {
                agotado = false;
            }
            return;
        }
        if (celebracionFinalActiva || derrotaFinalActiva) {
            framesReaccionFinal++;
        } else {
            framesReaccionFinal = 0;
        }

        boolean moviendose = intensidadMovimiento > 0;
        if (sprintando && moviendose && !agotado) {
            // Menor consumo por sprint — jugadores se cansan más despacio
            stamina = Math.max(0.0, stamina - 0.24);
            if (stamina <= 0.0) {
                agotado = true;
            }
        } else {
            // Recuperacion aumentada cuando no sprintan o en reposo
            double recuperacion = moviendose ? 0.48 : 1.20;
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
        if (framesCelebracionEvento > 0) {
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
        return !agotado
            && framesDerribado <= 0
            && framesLesion <= 0
            && framesCelebracionEvento <= 0
            && !celebracionFinalActiva
            && !derrotaFinalActiva
            && stamina >= 8.0;
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

    public void iniciarHidratacion(int duracionFrames) {
        framesHidratacion = Math.max(framesHidratacion, Math.max(0, duracionFrames));
        if (agotado && stamina >= 34.0) {
            agotado = false;
        }
    }

    public boolean estaHidratando() {
        return framesHidratacion > 0;
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

    public void activarCelebracionEvento(int duracionFrames) {
        if (celebracionFinalActiva || derrotaFinalActiva) {
            return;
        }
        duracionCelebracionEvento = Math.max(1, duracionFrames);
        framesCelebracionEvento = Math.max(framesCelebracionEvento, duracionCelebracionEvento);
        framesDerribado = 0;
        framesBarridaVisual = 0;
        framesLanzadaVisual = 0;
        framesPatadaVisual = 0;
    }

    public void activarCelebracionFinal() {
        celebracionFinalActiva = true;
        derrotaFinalActiva = false;
        framesReaccionFinal = 0;
        framesDerribado = 0;
        framesBarridaVisual = 0;
        framesLanzadaVisual = 0;
        framesPatadaVisual = 0;
    }

    public void activarDerrotaFinal() {
        derrotaFinalActiva = true;
        celebracionFinalActiva = false;
        framesReaccionFinal = 0;
        framesDerribado = 0;
        framesBarridaVisual = 0;
        framesLanzadaVisual = 0;
        framesPatadaVisual = 0;
    }

    public void limpiarReaccionFinal() {
        celebracionFinalActiva = false;
        derrotaFinalActiva = false;
        framesReaccionFinal = 0;
    }

    public Color getColorCuerpo() {
        return colorCuerpo;
    }

    public Color getColorBorde() {
        return colorBorde;
    }

    public Color getColorDetalle() {
        return colorDetalle;
    }

    public double getFaseAnimacion() {
        return faseAnimacion;
    }

    public boolean isSprintVisualActivo() {
        return sprintVisualActivo;
    }

    public int getIntensidadMovimientoVisual() {
        return intensidadMovimientoVisual;
    }

    public int getFramesBarridaVisual() {
        return framesBarridaVisual;
    }

    public int getFramesLanzadaVisual() {
        return framesLanzadaVisual;
    }

    public int getFramesDerribado() {
        return framesDerribado;
    }

    public int getFramesLesion() {
        return framesLesion;
    }

    public int getFramesCelebracionEvento() {
        return framesCelebracionEvento;
    }

    public int getFramesPatadaVisual() {
        return framesPatadaVisual;
    }

    public boolean isCelebracionFinalActiva() {
        return celebracionFinalActiva;
    }

    public boolean isDerrotaFinalActiva() {
        return derrotaFinalActiva;
    }

    public int getFramesReaccionFinal() {
        return framesReaccionFinal;
    }

    public boolean isPortero() {
        return portero;
    }

    public void setPortero(boolean portero) {
        this.portero = portero;
    }

    public boolean isArbitro() {
        return arbitro;
    }

    public void setArbitro(boolean arbitro) {
        this.arbitro = arbitro;
    }

    public AnimacionSprite getAnimacionSprite() {
        if (framesLanzadaVisual > 0) {
            return AnimacionSprite.DIVE;
        }
        if (framesCelebracionEvento > 0 || celebracionFinalActiva) {
            return AnimacionSprite.CELEBRATE;
        }
        if (derrotaFinalActiva) {
            return AnimacionSprite.DEFEAT;
        }
        if (framesDerribado > 0) {
            return AnimacionSprite.DOWN;
        }
        if (framesBarridaVisual > 0) {
            return AnimacionSprite.SLIDE;
        }
        if (framesPatadaVisual > 0) {
            return AnimacionSprite.KICK;
        }
        if (sprintVisualActivo && intensidadMovimientoVisual > 0) {
            return AnimacionSprite.SPRINT;
        }
        if (intensidadMovimientoVisual > 0) {
            return AnimacionSprite.RUN;
        }
        return AnimacionSprite.IDLE;
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

    public void activarAnimacionPatada(int duracionFrames, double dirX, double dirY) {
        if (framesLanzadaVisual > 0 || framesBarridaVisual > 0 || framesDerribado > 0 || celebracionFinalActiva || derrotaFinalActiva) {
            return;
        }
        duracionPatadaVisual = Math.max(1, duracionFrames);
        framesPatadaVisual = Math.max(framesPatadaVisual, duracionPatadaVisual);
        if (Math.abs(dirX) >= 0.001) {
            direccionX = dirX > 0.0 ? 1 : -1;
        }
        if (Math.abs(dirY) >= 0.001) {
            direccionY = dirY > 0.0 ? 1 : -1;
        }
    }
}
