package juego.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import juego.core.ConfiguracionJuego;
import juego.core.GeometriaCancha;
import juego.entidades.Balon;
import juego.entidades.HidratacionBanca;
import juego.entidades.Jugador;
import juego.entidades.Turbo;

// Generador local de sprites pixel-art y cache de variantes.
public final class GestorSprites {
    private static final Color COLOR_OUTLINE = new Color(22, 24, 28);
    private static final Color COLOR_PIEL_A = new Color(245, 206, 168);
    private static final Color COLOR_PIEL_B = new Color(212, 164, 120);
    private static final Color COLOR_JERSEY = new Color(255, 0, 255);
    private static final Color COLOR_TRIM = new Color(0, 255, 255);
    private static final Color COLOR_SHORT = new Color(255, 238, 84);
    private static final Color COLOR_SOCK = new Color(238, 238, 238);
    private static final Color COLOR_SHOE = new Color(20, 20, 24);

    private static final GestorSprites INSTANCIA = new GestorSprites();

    private final Map<String, BufferedImage[]> jugadoresCache;
    private final BufferedImage[] balonFrames;
    private final BufferedImage turboSprite;
    private final BufferedImage hidratacionSprite;
    private final BufferedImage bancaSprite;
    private final BufferedImage tileCesped;
    private final BufferedImage tileMuro;
    private final BufferedImage tileGrada;
    private final BufferedImage panelHud;
    private final BufferedImage panelEvento;
    private final BufferedImage panelMenu;
    private final BufferedImage backdrop;

    private GestorSprites() {
        jugadoresCache = new HashMap<>();
        balonFrames = crearBalonFrames();
        turboSprite = crearTurboSprite();
        hidratacionSprite = crearHidratacionSprite();
        bancaSprite = crearBancaSprite();
        tileCesped = crearTileCesped();
        tileMuro = crearTileMuro();
        tileGrada = crearTileGrada();
        panelHud = crearPanel(200, 84, new Color(20, 28, 42, 236), new Color(54, 72, 98, 236), new Color(255, 220, 120, 64));
        panelEvento = crearPanel(320, 128, new Color(64, 34, 20, 236), new Color(220, 148, 72, 236), new Color(255, 244, 190, 92));
        panelMenu = crearPanel(440, 300, new Color(18, 30, 40, 238), new Color(32, 88, 70, 238), new Color(110, 220, 255, 72));
        backdrop = crearBackdrop();
    }

    public static GestorSprites getInstancia() {
        return INSTANCIA;
    }

    public void configurarPixelArt(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
    }

    public void dibujarBackdrop(Graphics2D g) {
        g.drawImage(backdrop, 0, 0, null);
    }

    public void rellenarTile(Graphics2D g, BufferedImage tile, int x, int y, int w, int h) {
        for (int py = y; py < y + h; py += tile.getHeight()) {
            for (int px = x; px < x + w; px += tile.getWidth()) {
                g.drawImage(tile, px, py, null);
            }
        }
    }

    public void dibujarCancha(Graphics2D g, GeometriaCancha cancha, int relojUI) {
        int x = cancha.getCampoXMin();
        int y = cancha.getCampoYMin();
        int w = cancha.getCampoXMax() - cancha.getCampoXMin();
        int h = cancha.getCampoYMax() - cancha.getCampoYMin();
        rellenarTile(g, tileCesped, x, y, w, h);
        g.setColor(new Color(12, 18, 16, 80));
        g.fillRoundRect(x - 8, y - 8, w + 16, h + 16, 18, 18);
        g.setColor(new Color(240, 240, 232, 220));
        g.drawRect(x, y, w, h);
        g.drawLine(cancha.getCentroX(), y, cancha.getCentroX(), y + h);
        g.drawOval(cancha.getCentroX() - cancha.getRadioCirculoCentral(), cancha.getCentroY() - cancha.getRadioCirculoCentral(),
            cancha.getRadioCirculoCentral() * 2, cancha.getRadioCirculoCentral() * 2);
        dibujarArea(g, cancha.getAreaGrande(true));
        dibujarArea(g, cancha.getAreaGrande(false));
        dibujarArea(g, cancha.getAreaChica(true));
        dibujarArea(g, cancha.getAreaChica(false));
        g.fillOval(cancha.getCentroX() - 4, cancha.getCentroY() - 4, 8, 8);
        g.fillOval(cancha.getPuntoPenalX(true) - 3, cancha.getPuntoPenalY() - 3, 6, 6);
        g.fillOval(cancha.getPuntoPenalX(false) - 3, cancha.getPuntoPenalY() - 3, 6, 6);
        int pulso = (int) Math.round(Math.abs(Math.sin(relojUI * 0.05)) * 22.0);
        g.setColor(new Color(255, 216, 104, 60 + pulso));
        g.drawRoundRect(x + 2, y + 2, w - 4, h - 4, 16, 16);
        dibujarPorteria(g, cancha, true);
        dibujarPorteria(g, cancha, false);
    }

    private void dibujarArea(Graphics2D g, java.awt.Rectangle rect) {
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
    }

    private void dibujarPorteria(Graphics2D g, GeometriaCancha cancha, boolean local) {
        java.awt.Rectangle porteria = cancha.getPorteria(local);
        int x = porteria.x;
        int y = porteria.y;
        int w = porteria.width;
        int h = porteria.height;
        g.setColor(new Color(236, 236, 236));
        g.fillRect(x, y, w, h);
        int fondo = Math.max(12, porteria.width);
        int inicio = local ? x - fondo : x + w;
        g.setColor(new Color(176, 188, 204, 180));
        for (int dx = 0; dx <= fondo; dx += 6) {
            int px = local ? x - dx : x + w + dx;
            g.drawLine(px, y, px, y + h);
        }
        for (int dy = 0; dy < h; dy += 10) {
            g.drawLine(local ? x : x + w, y + dy, local ? inicio + fondo : inicio, y + Math.min(h, dy + 5));
        }
    }

    public void dibujarBanca(Graphics2D g, int x, int y) {
        g.setComposite(AlphaComposite.SrcOver.derive(0.18f));
        g.setColor(new Color(0, 0, 0));
        g.fillRoundRect(x + 16, y + bancaSprite.getHeight() * 4 - 8, bancaSprite.getWidth() * 4 - 32, 16, 18, 18);
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(bancaSprite, x, y, bancaSprite.getWidth() * 4, bancaSprite.getHeight() * 4, null);
    }

    public void dibujarTurbo(Graphics2D g, Turbo turbo, int relojUI) {
        if (!turbo.estaActivo()) {
            return;
        }
        int aura = 10 + (int) Math.round(Math.sin(relojUI * 0.18) * 2.0);
        g.setComposite(AlphaComposite.SrcOver.derive(0.45f));
        g.setColor(new Color(92, 255, 210));
        g.fillOval(turbo.getX() - aura / 2, turbo.getY() - aura / 2, turbo.getAncho() + aura, turbo.getAlto() + aura);
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(turboSprite, turbo.getX(), turbo.getY(), turbo.getAncho(), turbo.getAlto(), null);
    }

    public void dibujarHidratacion(Graphics2D g, HidratacionBanca hidratacion, int relojUI) {
        int brillo = hidratacion.estaDisponible() ? 32 + (int) Math.round(Math.abs(Math.sin(relojUI * 0.12)) * 26.0) : 12;
        int x = hidratacion.getX();
        int y = hidratacion.getY();
        int w = hidratacion.getAncho();
        int h = hidratacion.getAlto();
        g.setComposite(AlphaComposite.SrcOver.derive(0.30f));
        g.setColor(new Color(104, 214, 255, brillo));
        g.fillRoundRect(x - 8, y - 8, w + 16, h + 16, 12, 12);
        g.setComposite(AlphaComposite.SrcOver.derive(0.18f));
        g.setColor(new Color(0, 0, 0));
        g.fillOval(x - 4, y + h - 2, w + 8, 10);
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(hidratacionSprite, x, y, w, h, null);
        g.setColor(hidratacion.estaDisponible() ? new Color(168, 255, 202) : new Color(255, 180, 120));
        g.fillRoundRect(x + w - 18, y + 3, 14, 5, 4, 4);
        g.setColor(new Color(16, 24, 30));
        g.fillRoundRect(x + 4, y + h - 7, Math.max(8, Math.min(w - 24, hidratacion.getUsosRestantes() * 4)), 4, 4, 4);
    }

    public void dibujarJugador(Graphics2D g, Jugador jugador) {
        BufferedImage frame = frameJugador(jugador);
        int drawX = jugador.getX() - 6;
        int drawY = jugador.getY() - 6;
        int drawW = jugador.getAncho() + 12;
        int drawH = jugador.getAlto() + 14;

        if (jugador.tieneTurboActivo()) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.32f));
            g.setColor(new Color(90, 255, 206));
            g.fillOval(jugador.getX() - 8, jugador.getY() + jugador.getAlto() - 14, jugador.getAncho() + 16, 18);
            g.setComposite(AlphaComposite.SrcOver);
        }
        if (jugador.estaAgotado()) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.22f));
            g.setColor(new Color(255, 120, 90));
            g.fillOval(jugador.getX() - 4, jugador.getY() + jugador.getAlto() - 12, jugador.getAncho() + 8, 14);
            g.setComposite(AlphaComposite.SrcOver);
        }
        if (jugador.isSprintVisualActivo() && jugador.getIntensidadMovimientoVisual() > 0) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.14f));
            g.drawImage(frame, drawX - jugador.getDireccionX() * 3, drawY + 1, drawW, drawH, null);
            g.setComposite(AlphaComposite.SrcOver);
        }
        if (jugador.getDireccionX() < 0) {
            g.drawImage(frame, drawX + drawW, drawY, -drawW, drawH, null);
        } else {
            g.drawImage(frame, drawX, drawY, drawW, drawH, null);
        }
        dibujarDorsalJugador(g, jugador, drawX, drawY, drawW, drawH);
    }

    public void dibujarJugadorEvento(Graphics2D g, Jugador jugador, int x, int y, int w, int h, boolean celebrar) {
        BufferedImage frame = frameJugadorEvento(jugador, celebrar);
        double escala = Math.min(w / (double) frame.getWidth(), h / (double) frame.getHeight());
        escala *= celebrar ? 0.92 : 0.88;
        int drawW = Math.max(1, (int) Math.round(frame.getWidth() * escala));
        int drawH = Math.max(1, (int) Math.round(frame.getHeight() * escala));
        int drawX = x + (w - drawW) / 2;
        int drawY = y + (h - drawH) / 2 + (celebrar ? 2 : 6);
        if (jugador.getDireccionX() < 0 && !celebrar) {
            g.drawImage(frame, drawX + drawW, drawY, -drawW, drawH, null);
        } else {
            g.drawImage(frame, drawX, drawY, drawW, drawH, null);
        }
        dibujarDorsalJugador(g, jugador, drawX, drawY, drawW, drawH);
    }

    public void dibujarBalon(Graphics2D g, Balon balon) {
        int frame = Math.max(0, Math.min(balonFrames.length - 1, balon.getIndiceSpriteFrame()));
        double altura = balon.getAltura();
        double velocidadZ = Math.abs(balon.getVelocidadZ());
        boolean reboteFuerte = altura < 2.2 && velocidadZ > 1.8;
        boolean enAire = balon.isSpriteEnAire();
        int drawW = balon.getAncho();
        int drawH = balon.getAlto();
        int drawX = balon.getX();
        int drawY = balon.getYRender();
        if (reboteFuerte || frame == 7) {
            drawW += 4;
            drawH = Math.max(10, drawH - 3);
            drawX -= 2;
            drawY += 2;
        } else if (enAire) {
            drawW += 2;
            drawH += 2;
            drawX -= 1;
            drawY -= 1;
        }
        int sombraAncho = Math.max(8, drawW + 2 - (int) Math.round(altura * 0.12));
        int sombraAlto = Math.max(4, drawH / 3 - (int) Math.round(altura * 0.04));
        g.setComposite(AlphaComposite.SrcOver.derive(0.35f));
        g.setColor(new Color(0, 0, 0));
        g.fillOval(balon.getX() + (balon.getAncho() - sombraAncho) / 2, balon.getY() + balon.getAlto() - sombraAlto / 2, sombraAncho, sombraAlto);
        if (altura > 2.5) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.18f));
            g.fillOval(balon.getX() + (balon.getAncho() - Math.max(6, sombraAncho - 4)) / 2, balon.getY() + balon.getAlto() - sombraAlto / 3,
                Math.max(6, sombraAncho - 4), Math.max(3, sombraAlto - 1));
        }
        g.setComposite(AlphaComposite.SrcOver);
        if (enAire && balon.getRapidez() > 2.8) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.16f));
            g.drawImage(balonFrames[Math.min(5, frame)], drawX - (int) Math.round(balon.getVelocidadX() * 0.8), drawY - (int) Math.round(balon.getVelocidadY() * 0.8),
                drawW, drawH, null);
            g.setComposite(AlphaComposite.SrcOver);
        }
        g.drawImage(balonFrames[frame], drawX, drawY, drawW, drawH, null);
    }

    public BufferedImage getPanelHud() {
        return panelHud;
    }

    public BufferedImage getPanelEvento() {
        return panelEvento;
    }

    public BufferedImage getPanelMenu() {
        return panelMenu;
    }

    public BufferedImage getTileMuro() {
        return tileMuro;
    }

    public BufferedImage getTileGrada() {
        return tileGrada;
    }

    private BufferedImage frameJugadorEvento(Jugador jugador, boolean celebrar) {
        Jugador.AnimacionSprite animacion = celebrar ? Jugador.AnimacionSprite.CELEBRATE : jugador.getAnimacionSprite();
        return spritesJugador(jugador)[indiceAnimacion(animacion, jugador)];
    }

    private BufferedImage frameJugador(Jugador jugador) {
        return spritesJugador(jugador)[indiceAnimacion(jugador.getAnimacionSprite(), jugador)];
    }

    private int indiceAnimacion(Jugador.AnimacionSprite animacion, Jugador jugador) {
        return switch (animacion) {
            case IDLE -> 0;
            case RUN -> ((int) Math.floor(jugador.getFaseAnimacion() * 1.7) & 1) == 0 ? 1 : 2;
            case SPRINT -> ((int) Math.floor(jugador.getFaseAnimacion() * 1.9) & 1) == 0 ? 3 : 4;
            case KICK -> ((int) Math.floor(jugador.getFramesPatadaVisual() * 0.5) & 1) == 0 ? 5 : 6;
            case SLIDE -> 7;
            case DIVE -> 8;
            case DOWN -> 9;
            case CELEBRATE -> 10;
            case DEFEAT -> 11;
        };
    }

    private BufferedImage[] spritesJugador(Jugador jugador) {
        String clave = colorKey(jugador.getColorCuerpo(), jugador.getColorBorde(), jugador.getColorDetalle(), jugador.isPortero(), jugador.isArbitro());
        return jugadoresCache.computeIfAbsent(clave, k -> crearSpritesJugador(jugador.getColorCuerpo(), jugador.getColorBorde(), jugador.getColorDetalle(), jugador.isPortero(), jugador.isArbitro()));
    }

    private String colorKey(Color cuerpo, Color borde, Color detalle, boolean portero, boolean arbitro) {
        return cuerpo.getRGB() + "|" + borde.getRGB() + "|" + detalle.getRGB() + "|" + portero + "|" + arbitro;
    }

    private BufferedImage[] crearSpritesJugador(Color cuerpo, Color borde, Color detalle, boolean portero, boolean arbitro) {
        BufferedImage[] frames = new BufferedImage[12];
        for (int i = 0; i < frames.length; i++) {
            BufferedImage img = nuevaImagen(32, 32);
            int headX = 10;
            int headY = 2;
            int bodyX = 10;
            int bodyY = 10;
            int leftFootX = 8;
            int rightFootX = 18;
            int leftArmX = 6;
            int rightArmX = 22;
            switch (i) {
                case 1 -> { headX = 12; headY = 3; leftFootX = 4; rightFootX = 21; leftArmX = 4; rightArmX = 24; }
                case 2 -> { headX = 8; headY = 2; leftFootX = 11; rightFootX = 16; leftArmX = 5; rightArmX = 22; }
                case 3 -> { headX = 13; bodyX = 11; headY = 3; leftFootX = 3; rightFootX = 21; leftArmX = 3; rightArmX = 25; }
                case 4 -> { headX = 7; bodyX = 8; headY = 2; leftFootX = 11; rightFootX = 16; leftArmX = 4; rightArmX = 23; }
                case 5 -> { headX = 8; bodyX = 8; bodyY = 10; leftFootX = 9; rightFootX = 23; leftArmX = 4; rightArmX = 19; }
                case 6 -> { headX = 12; bodyX = 10; bodyY = 9; leftFootX = 5; rightFootX = 23; leftArmX = 7; rightArmX = 23; }
                case 7 -> { headX = 16; headY = 10; bodyX = 9; bodyY = 13; leftFootX = 4; rightFootX = 22; leftArmX = 5; rightArmX = 20; }
                case 8 -> { headX = 18; headY = 8; bodyX = 11; bodyY = 12; leftFootX = 8; rightFootX = 22; leftArmX = 4; rightArmX = 24; }
                case 9 -> { headX = 18; headY = 12; bodyX = 11; bodyY = 16; leftFootX = 7; rightFootX = 21; leftArmX = 5; rightArmX = 22; }
                case 10 -> { headX = 10; headY = 4; bodyX = 10; bodyY = 11; leftArmX = 3; rightArmX = 25; leftFootX = 7; rightFootX = 19; }
                case 11 -> { headX = 18; headY = 12; bodyX = 11; bodyY = 16; leftFootX = 6; rightFootX = 22; leftArmX = 7; rightArmX = 22; }
                default -> { }
            }

            sombraJugador(img, i);
            pintarCabezaJugador(img, headX, headY, i, borde, arbitro);

            fillRect(img, bodyX, bodyY, 10, 9, cuerpo);
            fillRect(img, bodyX, bodyY, 10, 1, arbitro ? new Color(255, 214, 64) : detalle);
            fillRect(img, bodyX + 4, bodyY, 2, 9, detalle);
            if (!arbitro) {
                fillRect(img, bodyX + 1, bodyY + 1, 1, 7, borde);
                fillRect(img, bodyX + 8, bodyY + 1, 1, 7, borde);
            } else {
                fillRect(img, bodyX + 1, bodyY + 2, 8, 1, detalle);
                fillRect(img, bodyX + 1, bodyY + 5, 8, 1, detalle);
            }
            fillRect(img, bodyX, bodyY + 9, 10, 4, arbitro ? new Color(30, 30, 34) : oscurecer(detalle, 0.26));
            fillRect(img, bodyX + 2, bodyY + 2, 6, 1, new Color(255, 255, 255, 120));
            setPixel(img, bodyX + 2, bodyY + 4, new Color(255, 255, 255, 110));
            setPixel(img, bodyX + 7, bodyY + 4, new Color(255, 255, 255, 110));

            Color mano = portero && (i == 6 || i == 8) ? new Color(146, 255, 224) : COLOR_PIEL_B;
            if (i == 10) {
                fillRect(img, bodyX - 3, bodyY - 2, 3, 10, mano);
                fillRect(img, bodyX + 10, bodyY - 2, 3, 10, mano);
                fillRect(img, bodyX - 5, bodyY - 5, 3, 6, mano);
                fillRect(img, bodyX + 12, bodyY - 5, 3, 6, mano);
            } else {
                fillRect(img, leftArmX, bodyY + 2, 3, 7, mano);
                fillRect(img, rightArmX, bodyY + 2, 3, 7, mano);
            }
            fillRect(img, leftFootX, bodyY + 12, 3, 8, detalle);
            fillRect(img, rightFootX, bodyY + 12, 3, 8, detalle);
            fillRect(img, leftFootX, bodyY + 18, 3, 4, COLOR_SOCK);
            fillRect(img, rightFootX, bodyY + 18, 3, 4, COLOR_SOCK);
            fillRect(img, leftFootX - 1, bodyY + 22, 5, 2, COLOR_SHOE);
            fillRect(img, rightFootX - 1, bodyY + 22, 5, 2, COLOR_SHOE);

            if (i == 5 || i == 6) {
                fillRect(img, bodyX + 8, bodyY + 10, 6, 2, COLOR_SHOE);
                fillRect(img, bodyX + 12, bodyY + 8, 2, 3, COLOR_SOCK);
                fillRect(img, bodyX - 2, bodyY + 4, 3, 7, COLOR_PIEL_B);
                if (i == 6) {
                    fillRect(img, bodyX + 12, bodyY + 7, 5, 2, new Color(255, 236, 180));
                }
            }
            if (portero) {
                fillRect(img, bodyX + 1, bodyY + 3, 8, 1, new Color(244, 244, 244));
                fillRect(img, leftArmX - 1, bodyY + 7, 3, 2, new Color(150, 255, 228));
                fillRect(img, rightArmX + 1, bodyY + 7, 3, 2, new Color(150, 255, 228));
            }
            if (i == 10) {
                halo(img, new Color(255, 236, 128, 92));
                fillRect(img, bodyX - 4, bodyY - 6, 4, 2, COLOR_PIEL_B);
                fillRect(img, bodyX + 10, bodyY - 6, 4, 2, COLOR_PIEL_B);
            }
            if (i == 11) {
                setPixel(img, headX + 8, headY + 8, new Color(120, 190, 255));
            }
            outlineRect(img, bodyX, bodyY, 10, 13, COLOR_OUTLINE);
            outlineRect(img, headX, headY, 10, 10, COLOR_OUTLINE);
            frames[i] = img;
        }
        return frames;
    }

    private void sombraJugador(BufferedImage img, int estado) {
        int x = estado >= 5 ? 7 : 8;
        int w = estado >= 5 ? 18 : 14;
        fillOval(img, x, 25, w, 4, new Color(0, 0, 0, 82));
    }

    private void dibujarDorsalJugador(Graphics2D g, Jugador jugador, int x, int y, int w, int h) {
        String dorsal = String.valueOf(jugador.getNumeroCamiseta());
        int anchoDigito = Math.max(3, w / 10);
        int altoDigito = Math.max(5, h / 8);
        int espacio = Math.max(1, anchoDigito / 2);
        int anchoTotal = dorsal.length() * anchoDigito + Math.max(0, dorsal.length() - 1) * espacio;
        int origenX = x + w / 2 - anchoTotal / 2;
        int origenY = y + Math.max(11, h / 3);
        Color relleno = new Color(248, 248, 244);
        Color borde = new Color(18, 22, 28);
        for (int i = 0; i < dorsal.length(); i++) {
            dibujarDigitoPixel(g, dorsal.charAt(i), origenX + i * (anchoDigito + espacio), origenY, anchoDigito, altoDigito, borde);
            dibujarDigitoPixel(g, dorsal.charAt(i), origenX + 1 + i * (anchoDigito + espacio), origenY + 1, anchoDigito, altoDigito, relleno);
        }
    }

    private void dibujarDigitoPixel(Graphics2D g, char digito, int x, int y, int ancho, int alto, Color color) {
        boolean[] segmentos = segmentosDigito(digito);
        int grosor = Math.max(1, Math.min(3, ancho / 2));
        g.setColor(color);
        if (segmentos[0]) {
            g.fillRect(x, y, ancho, grosor);
        }
        if (segmentos[1]) {
            g.fillRect(x + ancho - grosor, y, grosor, alto / 2);
        }
        if (segmentos[2]) {
            g.fillRect(x + ancho - grosor, y + alto / 2, grosor, alto / 2);
        }
        if (segmentos[3]) {
            g.fillRect(x, y + alto - grosor, ancho, grosor);
        }
        if (segmentos[4]) {
            g.fillRect(x, y + alto / 2, grosor, alto / 2);
        }
        if (segmentos[5]) {
            g.fillRect(x, y, grosor, alto / 2);
        }
        if (segmentos[6]) {
            g.fillRect(x, y + alto / 2 - grosor / 2, ancho, grosor);
        }
    }

    private boolean[] segmentosDigito(char digito) {
        return switch (digito) {
            case '0' -> new boolean[] { true, true, true, true, true, true, false };
            case '1' -> new boolean[] { false, true, true, false, false, false, false };
            case '2' -> new boolean[] { true, true, false, true, true, false, true };
            case '3' -> new boolean[] { true, true, true, true, false, false, true };
            case '4' -> new boolean[] { false, true, true, false, false, true, true };
            case '5' -> new boolean[] { true, false, true, true, false, true, true };
            case '6' -> new boolean[] { true, false, true, true, true, true, true };
            case '7' -> new boolean[] { true, true, true, false, false, false, false };
            case '8' -> new boolean[] { true, true, true, true, true, true, true };
            case '9' -> new boolean[] { true, true, true, true, false, true, true };
            default -> new boolean[] { false, false, false, false, false, false, false };
        };
    }

    private void pintarCabezaJugador(BufferedImage img, int headX, int headY, int estado, Color borde, boolean arbitro) {
        fillOval(img, headX, headY, 10, 10, COLOR_PIEL_A);
        fillOval(img, headX + 1, headY + 5, 8, 4, new Color(224, 178, 142));
        Color cabello = cabello(arbitro, borde);
        fillRect(img, headX + 1, headY + 1, 8, 3, cabello);
        fillRect(img, headX + 2, headY + 4, 6, 1, cabello);
        if (!arbitro) {
            setPixel(img, headX + 8, headY + 2, oscurecer(cabello, 0.10));
            setPixel(img, headX + 7, headY + 1, oscurecer(cabello, 0.06));
        }
        setPixel(img, headX + 2, headY + 5, COLOR_OUTLINE);
        setPixel(img, headX + 7, headY + 5, COLOR_OUTLINE);
        setPixel(img, headX + 2, headY + 7, new Color(232, 154, 136));
        setPixel(img, headX + 7, headY + 7, new Color(232, 154, 136));
        setPixel(img, headX + 4, headY + 6, new Color(52, 34, 24));
        setPixel(img, headX + 5, headY + 6, new Color(52, 34, 24));
        if (estado == 10) {
            fillRect(img, headX + 2, headY + 7, 6, 1, new Color(126, 52, 32));
            setPixel(img, headX + 4, headY + 8, new Color(255, 218, 218));
            setPixel(img, headX + 5, headY + 8, new Color(255, 218, 218));
        } else if (estado == 11) {
            fillRect(img, headX + 3, headY + 8, 4, 1, new Color(70, 48, 46));
            setPixel(img, headX + 7, headY + 8, new Color(120, 190, 255));
        } else if (estado == 7 || estado == 8) {
            fillRect(img, headX + 3, headY + 8, 3, 1, new Color(62, 32, 24));
            setPixel(img, headX + 6, headY + 7, COLOR_OUTLINE);
        } else if (estado == 5 || estado == 6) {
            fillRect(img, headX + 3, headY + 8, 4, 1, new Color(72, 28, 24));
            setPixel(img, headX + 6, headY + 7, new Color(120, 72, 64));
        } else {
            fillRect(img, headX + 3, headY + 8, 3, 1, new Color(78, 40, 30));
        }
        outlineRect(img, headX, headY, 10, 10, COLOR_OUTLINE);
    }

    private void halo(BufferedImage img, Color color) {
        fillOval(img, 3, 3, 26, 24, color);
    }

    private Color cabello(boolean arbitro, Color borde) {
        return arbitro ? new Color(34, 34, 34) : oscurecer(borde, 0.40);
    }

    private BufferedImage[] crearBalonFrames() {
        BufferedImage[] frames = new BufferedImage[8];
        for (int i = 0; i < frames.length; i++) {
            BufferedImage img = nuevaImagen(20, 20);
            fillOval(img, 1, 1, 18, 18, new Color(248, 247, 240));
            fillOval(img, 2, 2, 16, 7, new Color(255, 255, 255));
            fillOval(img, 4, 12, 12, 4, new Color(196, 200, 206));
            dibujarPanelCentralBalon(img, 10, 10, i);
            dibujarPanelesLateralesBalon(img, i);
            outlineRect(img, 1, 1, 18, 18, COLOR_OUTLINE);
            setPixel(img, 5, 4, new Color(255, 255, 255));
            setPixel(img, 6, 4, new Color(255, 255, 255));
            if (i == 6) {
                fillRect(img, 8, 1, 4, 18, new Color(36, 38, 42));
                fillRect(img, 4, 6, 12, 2, new Color(36, 38, 42));
                fillRect(img, 5, 12, 10, 2, new Color(36, 38, 42));
                fillRect(img, 2, 8, 3, 3, new Color(36, 38, 42));
                fillRect(img, 15, 8, 3, 3, new Color(36, 38, 42));
            } else if (i == 7) {
                fillOval(img, 2, 4, 16, 12, new Color(248, 247, 240));
                fillRect(img, 4, 8, 12, 2, new Color(34, 36, 40));
                fillRect(img, 8, 5, 4, 8, new Color(34, 36, 40));
                fillRect(img, 6, 11, 8, 2, new Color(34, 36, 40));
                outlineRect(img, 2, 4, 16, 12, COLOR_OUTLINE);
                setPixel(img, 5, 6, new Color(255, 255, 255));
                setPixel(img, 6, 6, new Color(255, 255, 255));
            }
            frames[i] = img;
        }
        return frames;
    }

    private void dibujarPanelCentralBalon(BufferedImage img, int cx, int cy, int frame) {
        int[][] offsets = {
            { 0, 0 }, { -2, -1 }, { -3, 0 }, { -2, 1 }, { 0, 2 }, { 2, 1 }, { 0, 0 }, { 0, 0 }
        };
        int ox = offsets[frame][0];
        int oy = offsets[frame][1];
        fillRect(img, cx - 2 + ox, cy - 2 + oy, 5, 5, new Color(34, 36, 40));
        setPixel(img, cx + ox, cy - 3 + oy, new Color(34, 36, 40));
        setPixel(img, cx - 3 + ox, cy - 1 + oy, new Color(34, 36, 40));
        setPixel(img, cx + 3 + ox, cy - 1 + oy, new Color(34, 36, 40));
        setPixel(img, cx - 2 + ox, cy + 3 + oy, new Color(34, 36, 40));
        setPixel(img, cx + 2 + ox, cy + 3 + oy, new Color(34, 36, 40));
    }

    private void dibujarPanelesLateralesBalon(BufferedImage img, int frame) {
        switch (frame) {
            case 0 -> {
                fillRect(img, 3, 8, 4, 2, new Color(38, 40, 44));
                fillRect(img, 13, 8, 4, 2, new Color(38, 40, 44));
                fillRect(img, 8, 3, 2, 3, new Color(38, 40, 44));
                fillRect(img, 10, 14, 2, 3, new Color(38, 40, 44));
            }
            case 1 -> {
                fillRect(img, 4, 5, 3, 4, new Color(38, 40, 44));
                fillRect(img, 13, 10, 3, 4, new Color(38, 40, 44));
                fillRect(img, 8, 14, 4, 2, new Color(38, 40, 44));
            }
            case 2 -> {
                fillRect(img, 3, 10, 4, 3, new Color(38, 40, 44));
                fillRect(img, 13, 6, 4, 3, new Color(38, 40, 44));
                fillRect(img, 8, 3, 4, 2, new Color(38, 40, 44));
            }
            case 3 -> {
                fillRect(img, 4, 12, 3, 4, new Color(38, 40, 44));
                fillRect(img, 13, 5, 3, 4, new Color(38, 40, 44));
                fillRect(img, 8, 2, 4, 2, new Color(38, 40, 44));
            }
            case 4 -> {
                fillRect(img, 3, 8, 4, 2, new Color(38, 40, 44));
                fillRect(img, 13, 8, 4, 2, new Color(38, 40, 44));
                fillRect(img, 7, 14, 2, 3, new Color(38, 40, 44));
                fillRect(img, 11, 3, 2, 3, new Color(38, 40, 44));
            }
            case 5 -> {
                fillRect(img, 4, 10, 3, 4, new Color(38, 40, 44));
                fillRect(img, 13, 5, 3, 4, new Color(38, 40, 44));
                fillRect(img, 8, 4, 4, 2, new Color(38, 40, 44));
            }
            default -> {
                fillRect(img, 4, 7, 3, 3, new Color(38, 40, 44));
                fillRect(img, 13, 7, 3, 3, new Color(38, 40, 44));
                fillRect(img, 8, 14, 4, 2, new Color(38, 40, 44));
            }
        }
    }

    private BufferedImage crearTurboSprite() {
        BufferedImage img = nuevaImagen(18, 18);
        fillOval(img, 1, 1, 16, 16, new Color(82, 255, 196));
        fillRect(img, 7, 3, 4, 12, new Color(244, 255, 248));
        fillRect(img, 4, 7, 10, 4, new Color(244, 255, 248));
        outlineRect(img, 1, 1, 16, 16, COLOR_OUTLINE);
        return img;
    }

    private BufferedImage crearHidratacionSprite() {
        BufferedImage img = nuevaImagen(52, 28);
        fillRect(img, 2, 20, 48, 5, new Color(64, 76, 88));
        fillRect(img, 4, 18, 44, 3, new Color(116, 128, 142));

        fillRect(img, 3, 8, 18, 12, new Color(48, 138, 196));
        fillRect(img, 4, 9, 16, 3, new Color(112, 214, 255));
        fillRect(img, 6, 13, 12, 6, new Color(38, 92, 148));
        fillRect(img, 7, 5, 10, 3, new Color(238, 244, 250));
        fillRect(img, 20, 10, 2, 10, new Color(238, 244, 250));
        outlineRect(img, 3, 8, 18, 12, COLOR_OUTLINE);

        for (int i = 0; i < 4; i++) {
            int x = 24 + i * 6;
            int alturaBotella = i % 2 == 0 ? 14 : 13;
            fillRect(img, x, 5 + (14 - alturaBotella), 4, alturaBotella, new Color(226, 246, 255));
            fillRect(img, x, 12, 4, 6, new Color(92, 194, 255));
            fillRect(img, x + 1, 3 + (14 - alturaBotella), 2, 2, new Color(248, 250, 255));
            fillRect(img, x + 1, 6 + (14 - alturaBotella), 1, 7, new Color(255, 255, 255));
            outlineRect(img, x, 5 + (14 - alturaBotella), 4, alturaBotella, COLOR_OUTLINE);
        }

        fillRect(img, 22, 19, 24, 4, new Color(130, 92, 60));
        fillRect(img, 23, 18, 22, 1, new Color(184, 132, 84));
        fillRect(img, 24, 23, 2, 3, new Color(78, 54, 36));
        fillRect(img, 42, 23, 2, 3, new Color(78, 54, 36));
        setPixel(img, 9, 11, new Color(255, 255, 255));
        setPixel(img, 10, 11, new Color(255, 255, 255));
        return img;
    }

    private BufferedImage crearBancaSprite() {
        BufferedImage img = nuevaImagen(68, 26);
        fillRect(img, 0, 22, 68, 4, new Color(66, 74, 80));
        fillRect(img, 2, 20, 64, 2, new Color(110, 118, 126));

        fillRect(img, 4, 2, 60, 3, new Color(188, 210, 224));
        fillRect(img, 6, 5, 56, 2, new Color(118, 140, 158));
        fillRect(img, 6, 7, 3, 13, new Color(86, 94, 102));
        fillRect(img, 59, 7, 3, 13, new Color(86, 94, 102));
        fillRect(img, 16, 7, 2, 11, new Color(132, 140, 148));
        fillRect(img, 50, 7, 2, 11, new Color(132, 140, 148));

        fillRect(img, 9, 12, 50, 5, new Color(146, 92, 58));
        fillRect(img, 10, 10, 48, 2, new Color(196, 134, 88));
        fillRect(img, 11, 17, 46, 2, new Color(98, 62, 38));
        fillRect(img, 14, 19, 2, 5, new Color(84, 56, 38));
        fillRect(img, 52, 19, 2, 5, new Color(84, 56, 38));

        for (int i = 0; i < 3; i++) {
            int px = 17 + i * 14;
            fillOval(img, px, 6, 6, 6, COLOR_PIEL_A);
            fillRect(img, px + 1, 7, 4, 2, new Color(64, 44, 34));
            fillRect(img, px, 12, 6, 4, i == 1 ? new Color(250, 198, 74) : new Color(212, 64, 60));
            fillRect(img, px + 1, 16, 4, 2, new Color(34, 36, 42));
            setPixel(img, px + 2, 8, COLOR_OUTLINE);
            setPixel(img, px + 4, 8, COLOR_OUTLINE);
        }

        fillRect(img, 6, 8, 2, 11, new Color(58, 66, 74));
        fillRect(img, 60, 8, 2, 11, new Color(58, 66, 74));
        outlineRect(img, 4, 2, 60, 3, COLOR_OUTLINE);
        outlineRect(img, 9, 12, 50, 5, COLOR_OUTLINE);
        return img;
    }

    private BufferedImage crearTileCesped() {
        BufferedImage img = nuevaImagen(32, 32);
        fillRect(img, 0, 0, 32, 32, new Color(56, 128, 68));
        for (int i = 0; i < 56; i++) {
            int x = (i * 7) % 31;
            int y = (i * 11) % 31;
            fillRect(img, x, y, 1, 2, i % 2 == 0 ? new Color(78, 160, 84) : new Color(40, 104, 54));
        }
        for (int i = 0; i < 14; i++) {
            fillRect(img, 1 + (i * 5) % 30, 2 + (i * 9) % 28, 2, 1, new Color(96, 182, 88, 120));
        }
        return img;
    }

    private BufferedImage crearTileMuro() {
        BufferedImage img = nuevaImagen(32, 32);
        fillRect(img, 0, 0, 32, 32, new Color(86, 86, 94));
        for (int y = 4; y < 32; y += 8) {
            fillRect(img, 0, y, 32, 1, new Color(116, 116, 126));
        }
        for (int x = 5; x < 32; x += 8) {
            fillRect(img, x, 0, 1, 32, new Color(70, 70, 80));
        }
        fillRect(img, 6, 18, 20, 3, new Color(210, 80, 96, 150));
        return img;
    }

    private BufferedImage crearTileGrada() {
        BufferedImage img = nuevaImagen(32, 24);
        fillRect(img, 0, 0, 32, 24, new Color(42, 52, 68));
        for (int y = 2; y < 24; y += 6) {
            fillRect(img, 0, y, 32, 2, new Color(62, 76, 98));
        }
        Color[] publico = { new Color(246, 198, 78), new Color(106, 220, 255), new Color(252, 96, 86), new Color(242, 242, 242) };
        for (int i = 0; i < 18; i++) {
            fillRect(img, 1 + (i * 7) % 30, 2 + (i * 5) % 20, 2, 2, publico[i % publico.length]);
        }
        return img;
    }

    private BufferedImage crearPanel(int w, int h, Color fondoA, Color fondoB, Color brillo) {
        BufferedImage img = nuevaImagen(w, h);
        for (int y = 0; y < h; y++) {
            double t = y / (double) Math.max(1, h - 1);
            Color linea = mezclar(fondoA, fondoB, t);
            fillRect(img, 0, y, w, 1, linea);
        }
        outlineRect(img, 0, 0, w, h, new Color(248, 248, 248, 150));
        fillRect(img, 8, 8, Math.max(12, w / 3), 6, brillo);
        for (int i = 0; i < 30; i++) {
            fillRect(img, 10 + (i * 19) % Math.max(12, w - 12), 12 + (i * 11) % Math.max(12, h - 12), 2, 2, new Color(255, 255, 255, 36));
        }
        return img;
    }

    private BufferedImage crearBackdrop() {
        BufferedImage img = nuevaImagen(ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(new Color(10, 18, 26));
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            for (int y = 0; y < 180; y++) {
                double t = y / 180.0;
                g.setColor(mezclar(new Color(16, 28, 42), new Color(255, 146, 80), t * 0.55));
                g.drawLine(0, y, img.getWidth(), y);
            }
            g.setComposite(AlphaComposite.SrcOver.derive(0.32f));
            g.setColor(new Color(255, 194, 112));
            g.fillOval(img.getWidth() - 250, -70, 220, 160);
            g.setComposite(AlphaComposite.SrcOver);
            rellenarTile(g, tileMuro, 24, 118, 104, img.getHeight() - 250);
            rellenarTile(g, tileMuro, img.getWidth() - 128, 118, 104, img.getHeight() - 250);
            for (int i = 0; i < 8; i++) {
                int x = 90 + i * 145;
                g.setColor(new Color(18, 20, 26, 160));
                g.fillRect(x, 66 - (i % 3) * 10, 32 + (i % 2) * 12, 46 + (i % 3) * 12);
                g.setColor(new Color(82, 88, 104, 120));
                g.fillRect(x + 6, 76 - (i % 3) * 10, 6, 6);
                g.fillRect(x + 18, 88 - (i % 3) * 10, 6, 6);
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    private BufferedImage nuevaImagen(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private void fillRect(BufferedImage img, int x, int y, int w, int h, Color color) {
        int rgb = color.getRGB();
        for (int py = Math.max(0, y); py < Math.min(img.getHeight(), y + h); py++) {
            for (int px = Math.max(0, x); px < Math.min(img.getWidth(), x + w); px++) {
                img.setRGB(px, py, rgb);
            }
        }
    }

    private void fillOval(BufferedImage img, int x, int y, int w, int h, Color color) {
        double rx = w / 2.0;
        double ry = h / 2.0;
        double cx = x + rx;
        double cy = y + ry;
        int rgb = color.getRGB();
        for (int py = Math.max(0, y); py < Math.min(img.getHeight(), y + h); py++) {
            for (int px = Math.max(0, x); px < Math.min(img.getWidth(), x + w); px++) {
                double dx = ((px + 0.5) - cx) / Math.max(0.5, rx);
                double dy = ((py + 0.5) - cy) / Math.max(0.5, ry);
                if (dx * dx + dy * dy <= 1.0) {
                    img.setRGB(px, py, rgb);
                }
            }
        }
    }

    private void outlineRect(BufferedImage img, int x, int y, int w, int h, Color color) {
        fillRect(img, x, y, w, 1, color);
        fillRect(img, x, y + h - 1, w, 1, color);
        fillRect(img, x, y, 1, h, color);
        fillRect(img, x + w - 1, y, 1, h, color);
    }

    private void setPixel(BufferedImage img, int x, int y, Color color) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) {
            img.setRGB(x, y, color.getRGB());
        }
    }

    private Color mezclar(Color a, Color b, double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        return new Color(
            (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * clamped),
            (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * clamped),
            (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * clamped),
            (int) Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * clamped)
        );
    }

    private Color oscurecer(Color color, double factor) {
        double clamped = Math.max(0.0, Math.min(1.0, factor));
        return new Color(
            (int) Math.round(color.getRed() * (1.0 - clamped)),
            (int) Math.round(color.getGreen() * (1.0 - clamped)),
            (int) Math.round(color.getBlue() * (1.0 - clamped)),
            color.getAlpha()
        );
    }
}
