package juego.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.awt.font.GlyphVector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import juego.core.ConfiguracionJuego;
import juego.core.EntradaJuego;
import juego.core.EstadoJuego;
import juego.core.GeometriaCancha;
import juego.core.MaquinaEstadosJuego;
import juego.core.MotorJuego;
import juego.sonido.MusicaInicio;
import juego.entidades.Jugador;

// Renderizador 2D del partido: fondo, cancha, entidades, HUD y menus animados.
public class RenderJuego {
    private EstadoJuego ultimoEstado = EstadoJuego.INICIO;
    private int framesEnEstado = 0;
    private int relojUI = 0;
    private final GestorSprites sprites;
    private final ExecutorService compositorCapas;
    private final Object cacheEntornoLock = new Object();
    private BufferedImage entornoEstaticoCache;
    private long entornoEstaticoSeed = Long.MIN_VALUE;
    // Estado visual del slider de volumen (hover / active)
    private volatile boolean sliderHover = false;
    private volatile boolean sliderActive = false;
    // Constantes para control de musica en la esquina
    private static final int MUSIC_ICON_X = ConfiguracionJuego.ANCHO_PANEL - 160;
    private static final int MUSIC_ICON_Y = 22;
    private static final int MUSIC_ICON_W = 128;
    private static final int MUSIC_ICON_H = 26;
    private static final int MUSIC_ICON_PAD_X = -8;
    private static final int MUSIC_ICON_PAD_Y = -6;

    public RenderJuego() {
        sprites = GestorSprites.getInstancia();
        compositorCapas = Executors.newFixedThreadPool(3, runnable -> {
            Thread hilo = new Thread(runnable, "render-cache-juego");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    public void dibujarEscena(Graphics g, MotorJuego motor, MaquinaEstadosJuego maquinaEstados, EntradaJuego entrada, int frameAnimacion) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        sprites.configurarPixelArt(g2);

        relojUI++;
        actualizarEstadoVisual(maquinaEstados.getEstadoActual());

        dibujarCielo(g2);
        sprites.dibujarBackdrop(g2);
        dibujarEntornoUrbano(g2, motor);
        dibujarCancha(g2, motor.getCancha(), frameAnimacion);
        if (motor.isMedioTiempoActivo()) {
            dibujarShowMedioTiempo(g2, motor);
        }
        dibujarBanca(g2);

        sprites.dibujarHidratacion(g2, motor.getHidratacionBanca(), relojUI);
        sprites.dibujarTurbo(g2, motor.getTurbo(), relojUI);

        dibujarJugadoresPorProfundidad(g2, motor);
        sprites.dibujarBalon(g2, motor.getBalon());
        dibujarParticulasJuego(g2, motor);
        dibujarEtiquetasJugadores(g2, motor);
        dibujarIndicadorArbitral(g2, motor);
        dibujarNarracionEnPantalla(g2, motor);

        dibujarHUD(g2, motor, entrada);
        dibujarAnimacionSorteoMoneda(g2, motor);
        dibujarOverlayEstado(g2, maquinaEstados.getEstadoActual(), maquinaEstados.getMensajeTemporal(), motor, maquinaEstados.isInicioModoEspectador());
        g2.dispose();
    }

    private void actualizarEstadoVisual(EstadoJuego estadoActual) {
        if (estadoActual != ultimoEstado) {
            ultimoEstado = estadoActual;
            framesEnEstado = 0;
        } else {
            framesEnEstado++;
        }
    }

    private double progresoEntrada(int duracionFrames) {
        if (duracionFrames <= 0) {
            return 1.0;
        }
        double t = Math.max(0.0, Math.min(1.0, framesEnEstado / (double) duracionFrames));
        return 1.0 - Math.pow(1.0 - t, 3.0);
    }

    private int alpha(double valor) {
        return (int) Math.max(0, Math.min(255, Math.round(valor)));
    }

    private void dibujarCielo(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(10, 26, 40), 0, ConfiguracionJuego.ALTO_PANEL, new Color(6, 12, 18)));
        g.fillRect(0, 0, ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL);

        int pulso = (int) (Math.sin(relojUI * 0.018) * 22.0);
        g.setColor(new Color(255, 188, 88, 46));
        g.fillOval(ConfiguracionJuego.ANCHO_PANEL - 322 + pulso, -122, 360, 260);
        g.setColor(new Color(255, 223, 172, 24));
        g.fillOval(ConfiguracionJuego.ANCHO_PANEL - 246 + pulso / 2, -88, 246, 186);

        dibujarNubes(g);
        dibujarDronesLejanos(g);
    }

    private void dibujarNubes(Graphics2D g) {
        for (int i = 0; i < 5; i++) {
            int ciclo = 420 + i * 60;
            int desplazamiento = (relojUI * (i + 1) / 3) % ciclo;
            int x = -260 + desplazamiento + i * 250;
            int y = 62 + i * 18;
            g.setColor(new Color(255, 255, 255, 14 - i));
            g.fillOval(x, y, 240, 70);
            g.fillOval(x + 42, y - 18, 190, 64);
        }
    }

    private void dibujarEntornoUrbano(Graphics2D g, MotorJuego motor) {
        asegurarCacheEntornoEstatico(motor.getDecorMurosSeed());
        dibujarOperativoLejano(g);
        if (entornoEstaticoCache != null) {
            g.drawImage(entornoEstaticoCache, 0, 0, null);
        } else {
            dibujarEntornoUrbanoEstatico(g, motor.getDecorMurosSeed());
        }
    }

    private void asegurarCacheEntornoEstatico(long seed) {
        synchronized (cacheEntornoLock) {
            if (entornoEstaticoCache != null && entornoEstaticoSeed == seed) {
                return;
            }
            try {
                Future<BufferedImage> capaLaterales = compositorCapas.submit(() -> {
                    BufferedImage imagen = nuevaCapaTransparente();
                    Graphics2D g = imagen.createGraphics();
                    try {
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        dibujarLateralesEstaticos(g, seed);
                    } finally {
                        g.dispose();
                    }
                    return imagen;
                });
                Future<BufferedImage> capaMuros = compositorCapas.submit(() -> {
                    BufferedImage imagen = nuevaCapaTransparente();
                    Graphics2D g = imagen.createGraphics();
                    try {
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        dibujarMurosLaterales(g, seed);
                        dibujarBardasYGraffitis(g, seed);
                    } finally {
                        g.dispose();
                    }
                    return imagen;
                });
                Future<BufferedImage> capaGradas = compositorCapas.submit(() -> {
                    BufferedImage imagen = nuevaCapaTransparente();
                    Graphics2D g = imagen.createGraphics();
                    try {
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        dibujarSombrasBordesCancha(g);
                    } finally {
                        g.dispose();
                    }
                    return imagen;
                });

                BufferedImage compuesta = nuevaCapaTransparente();
                Graphics2D gCompuesta = compuesta.createGraphics();
                try {
                    gCompuesta.drawImage(capaLaterales.get(), 0, 0, null);
                    gCompuesta.drawImage(capaMuros.get(), 0, 0, null);
                    gCompuesta.drawImage(capaGradas.get(), 0, 0, null);
                } finally {
                    gCompuesta.dispose();
                }
                entornoEstaticoCache = compuesta;
                entornoEstaticoSeed = seed;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                entornoEstaticoCache = null;
            } catch (ExecutionException e) {
                entornoEstaticoCache = null;
            }
        }
    }

    private BufferedImage nuevaCapaTransparente() {
        return new BufferedImage(ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL, BufferedImage.TYPE_INT_ARGB);
    }

    public static java.awt.Rectangle getMusicControlRect() {
        return new java.awt.Rectangle(MUSIC_ICON_X + MUSIC_ICON_PAD_X, MUSIC_ICON_Y + MUSIC_ICON_PAD_Y, MUSIC_ICON_W, MUSIC_ICON_H);
    }

    public static java.awt.Rectangle getMusicSliderRect() {
        int bx = MUSIC_ICON_X + MUSIC_ICON_PAD_X;
        int by = MUSIC_ICON_Y + MUSIC_ICON_PAD_Y;
        int sx = bx + 56;
        int sy = by + MUSIC_ICON_H - 12;
        int sw = Math.max(48, MUSIC_ICON_W - 64);
        int sh = 10;
        return new java.awt.Rectangle(sx, sy, sw, sh);
    }

    // Permite a PanelJuego notificar hover/active para afinar la visual del knob
    public void setSliderHover(boolean hover) {
        this.sliderHover = hover;
    }

    public void setSliderActive(boolean active) {
        this.sliderActive = active;
    }

    private void dibujarEntornoUrbanoEstatico(Graphics2D g, long seed) {
        dibujarLateralesEstaticos(g, seed);
        dibujarMurosLaterales(g, seed);
        dibujarBardasYGraffitis(g, seed);
        dibujarSombrasBordesCancha(g);
    }

    private void dibujarLateralesEstaticos(Graphics2D g, long seed) {
        g.setPaint(new GradientPaint(0, 0, new Color(62, 66, 72), 0, ConfiguracionJuego.ALTO_PANEL, new Color(44, 46, 52)));
        g.fillRect(0, 0, 52, ConfiguracionJuego.ALTO_PANEL);
        g.fillRect(ConfiguracionJuego.ANCHO_PANEL - 52, 0, 52, ConfiguracionJuego.ALTO_PANEL);
        g.setColor(new Color(82, 86, 94));
        g.fillRect(52, 0, 14, ConfiguracionJuego.ALTO_PANEL);
        g.fillRect(ConfiguracionJuego.ANCHO_PANEL - 66, 0, 14, ConfiguracionJuego.ALTO_PANEL);

        for (int y = 0; y < ConfiguracionJuego.ALTO_PANEL; y += 28) {
            g.setColor(new Color(120, 124, 132, 54));
            g.drawLine(0, y, 66, y + 6);
            g.drawLine(ConfiguracionJuego.ANCHO_PANEL - 66, y + 6, ConfiguracionJuego.ANCHO_PANEL, y);
        }
    }

    private void dibujarSombrasBordesCancha(Graphics2D g) {
        GeometriaCancha cancha = ConfiguracionJuego.MAPA_CANCHA;
        g.setColor(new Color(0, 0, 0, 68));
        g.fillRect(0, cancha.getCampoYMin() - 14, ConfiguracionJuego.ANCHO_PANEL, 10);
        g.fillRect(0, cancha.getCampoYMax() + 4, ConfiguracionJuego.ANCHO_PANEL, 10);
    }

    private void dibujarEscenaLejana(Graphics2D g) {
        int horizonteY = 54;
        int carreteraY = 64;
        int carreteraH = 26;
        g.setPaint(new GradientPaint(0, horizonteY, new Color(16, 20, 28, 220), 0, carreteraY + carreteraH, new Color(10, 12, 18, 204)));
        g.fillRect(72, horizonteY, ConfiguracionJuego.ANCHO_PANEL - 144, carreteraY + carreteraH - horizonteY);

        for (int i = 0; i < 8; i++) {
            int x = 96 + i * 168;
            int alto = 26 + (i % 3) * 10;
            g.setColor(new Color(18, 20, 24, 168));
            g.fillRect(x, carreteraY - alto, 34 + (i % 2) * 18, alto);
            g.setColor(new Color(58, 62, 74, 72));
            g.fillRect(x + 8, carreteraY - alto + 6, 8, 8);
            g.fillRect(x + 20, carreteraY - alto + 18, 8, 8);
        }

        g.setColor(new Color(82, 86, 98, 110));
        g.drawLine(72, carreteraY, ConfiguracionJuego.ANCHO_PANEL - 72, carreteraY);
        g.setColor(new Color(34, 36, 44, 180));
        g.fillRect(72, carreteraY, ConfiguracionJuego.ANCHO_PANEL - 144, carreteraH);
        g.setColor(new Color(140, 144, 154, 90));
        for (int x = 104; x < ConfiguracionJuego.ANCHO_PANEL - 104; x += 74) {
            g.fillRect(x, carreteraY + 11, 26, 3);
        }
    }

    private void dibujarMurosLaterales(Graphics2D g, long seed) {
        Random random = new Random(seed ^ 0x4C41544552414C4CL);
        int panelW = 104;
        int izquierdaX = 10;
        int derechaX = ConfiguracionJuego.ANCHO_PANEL - panelW - 10;
        int muroY = 92;
        int muroH = ConfiguracionJuego.ALTO_PANEL - 184;

        dibujarSuperficieMuro(g, izquierdaX, muroY, panelW, muroH, new Color(58, 60, 68), new Color(32, 34, 40));
        dibujarSuperficieMuro(g, derechaX, muroY, panelW, muroH, new Color(58, 60, 68), new Color(32, 34, 40));

        for (int i = 0; i < 2; i++) {
            int panelX = i == 0 ? izquierdaX + 6 : derechaX + 6;
            boolean mirarDerecha = i == 0;
            int muralSuperiorY = muroY + 16;
            int muralInferiorY = muroY + muroH - 196;
            dibujarPanelLateralReservado(g, panelX, muralSuperiorY, panelW - 12, 148);
            dibujarPanelLateralReservado(g, panelX, muralInferiorY, panelW - 12, 148);
            dibujarMuralStencil(g, panelX + 4, muralSuperiorY + 8, panelW - 20, 132, mirarDerecha);
            dibujarMuralStencil(g, panelX + 4, muralInferiorY + 8, panelW - 20, 132, !mirarDerecha);

            String[][] consignas = {
                { "MUNDIAL", "DEL", "DESPOJO" },
                { "MUNDIAL", "DEL", "DESPOJO" },
                { "SU MUNDIAL", "NOS VALE", "VRG" },
                { "VIVIENDAS", "Y", "DERECHOS" },
                { "NO", "AL", "DESALOJO" },
                { "BARRIO", "NO", "SE VENDE" }
            };
            String[] consigna = consignas[random.nextInt(consignas.length)];
            int panelConsignaY = muroY + muroH / 2 - 64;
            dibujarPanelLateralReservado(g, panelX, panelConsignaY, panelW - 12, 108);
            dibujarConsignaVerticalMuro(
                g,
                panelX + 8,
                panelConsignaY + 18,
                panelW - 24,
                consigna,
                new Color(120 + random.nextInt(120), 120 + random.nextInt(120), 120 + random.nextInt(120), 240),
                new Color(18, 18, 24, 210)
            );

            int cantidadPersonas = 2;
            for (int p = 0; p < cantidadPersonas; p++) {
                int personaY = panelConsignaY + 120 + p * 78;
                dibujarEspectadorBarda(
                    g,
                    panelX + 18 + random.nextInt(20),
                    personaY,
                    (p + i) % 5,
                    new Color(54 + random.nextInt(90), 70 + random.nextInt(70), 84 + random.nextInt(90)),
                    new Color(26 + random.nextInt(30), 24 + random.nextInt(22), 24 + random.nextInt(18)),
                    new Color(206 + random.nextInt(28), 156 + random.nextInt(24), 118 + random.nextInt(26))
                );
            }
        }
    }

    private void dibujarPanelLateralReservado(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(0, 0, 0, 78));
        g.fillRoundRect(x + 2, y + 3, w, h, 8, 8);
        g.setPaint(new GradientPaint(x, y, new Color(52, 54, 60, 210), x, y + h, new Color(26, 28, 34, 204)));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(210, 198, 178, 18));
        for (int i = 0; i < h; i += 13) {
            g.drawLine(x + 5, y + i, x + w - 6, y + i + 2);
        }
        for (int i = 0; i < 18; i++) {
            int manchaX = x + 6 + (i * 17) % Math.max(12, w - 14);
            int manchaY = y + 8 + (i * 23) % Math.max(12, h - 16);
            g.setColor(new Color(255, 255, 255, 10 + (i % 4) * 4));
            g.fillOval(manchaX, manchaY, 5 + i % 4, 3 + i % 3);
        }
        g.setColor(new Color(224, 224, 224, 34));
        g.drawRoundRect(x, y, w, h, 8, 8);
    }

    private void dibujarSuperficieMuro(Graphics2D g, int x, int y, int w, int h, Color tonoA, Color tonoB) {
        g.setColor(new Color(0, 0, 0, 84));
        g.fillRect(x + 4, y + 6, w, h);
        g.setPaint(new GradientPaint(x, y, tonoA, x, y + h, tonoB));
        g.fillRect(x, y, w, h);
        g.setColor(new Color(112, 118, 128, 120));
        for (int py = y + 10; py < y + h; py += 24) {
            g.drawLine(x + 2, py, x + w - 2, py);
        }
        for (int px = x + 8; px < x + w; px += 14) {
            g.setColor(new Color(92, 96, 108, 88));
            g.drawLine(px, y + 2, px - 3, y + h - 3);
        }
        g.setColor(new Color(228, 228, 228, 54));
        g.drawRect(x, y, w, h);
    }

    private void dibujarGraffitiMuro(Graphics2D g, String texto, int x, int y, Color colorPrincipal, Color colorSombra, int tamano, int rotacionGrados) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        g2.rotate(Math.toRadians(rotacionGrados));
        Font fuente = new Font("Dialog", Font.BOLD, tamano);
        GlyphVector glyphs = fuente.createGlyphVector(g2.getFontRenderContext(), texto);
        Shape forma = glyphs.getOutline();
        int anchoAproximado = Math.max(tamano * 2, tamano * texto.length() / 2);

        g2.setColor(new Color(0, 0, 0, 44));
        for (int i = 0; i < 24; i++) {
            int px = -10 + (i * 13) % Math.max(18, anchoAproximado);
            int py = -tamano + (i * 7) % Math.max(16, tamano + 22);
            g2.fillOval(px, py, 4 + i % 5, 4 + i % 4);
        }

        g2.translate(6, 5);
        g2.setColor(new Color(colorSombra.getRed(), colorSombra.getGreen(), colorSombra.getBlue(), 210));
        g2.fill(forma);

        g2.translate(-8, -6);
        g2.setColor(new Color(16, 12, 18, 210));
        g2.setStroke(new BasicStroke(Math.max(3.2f, tamano * 0.16f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(forma);

        g2.translate(2, 1);
        g2.setColor(new Color(colorPrincipal.getRed(), colorPrincipal.getGreen(), colorPrincipal.getBlue(), 96));
        g2.setStroke(new BasicStroke(Math.max(5.0f, tamano * 0.22f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(forma);
        g2.translate(-2, -1);

        g2.setColor(colorPrincipal);
        g2.fill(forma);

        g2.setColor(new Color(255, 250, 236, 76));
        g2.setStroke(new BasicStroke(Math.max(1.4f, tamano * 0.05f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(3, -tamano / 2, Math.max(tamano * 2, anchoAproximado / 2), -tamano / 3);
        g2.drawLine(tamano / 2, 1, Math.max(tamano * 3 / 2, anchoAproximado / 3), -3);

        g2.setColor(new Color(colorPrincipal.getRed(), colorPrincipal.getGreen(), colorPrincipal.getBlue(), 170));
        int cantidadDrips = Math.max(3, Math.min(6, texto.length() / 4 + 2));
        for (int i = 0; i < cantidadDrips; i++) {
            int dripX = tamano / 3 + i * Math.max(8, anchoAproximado / cantidadDrips);
            int dripH = 6 + i * 3;
            g2.fillRoundRect(dripX, 3, 3, dripH, 2, 2);
            g2.fillOval(dripX - 1, 3 + dripH - 2, 6, 6);
        }
        g2.setColor(new Color(colorPrincipal.getRed(), colorPrincipal.getGreen(), colorPrincipal.getBlue(), 54));
        for (int i = 0; i < 16; i++) {
            int sprayX = -4 + (i * 19) % Math.max(12, anchoAproximado + 8);
            int sprayY = -tamano / 2 + (i * 11) % Math.max(12, tamano + 8);
            g2.fillOval(sprayX, sprayY, 2 + i % 3, 2 + i % 3);
        }
        g2.dispose();
    }

    private void dibujarConsignaVerticalMuro(Graphics2D g, int x, int y, int w, String[] lineas, Color colorPrincipal, Color colorSombra) {
        Graphics2D g2 = (Graphics2D) g.create();
        int cursorY = y + 14;
        for (int i = 0; i < lineas.length; i++) {
            Font fuente = new Font("Dialog", Font.BOLD, i == 1 ? 15 : 17);
            g2.setFont(fuente);
            FontMetrics fm = g2.getFontMetrics();
            int textoX = x + (w - fm.stringWidth(lineas[i])) / 2;
            dibujarGraffitiMuro(
                g2,
                lineas[i],
                textoX,
                cursorY,
                i == 1 ? colorPrincipal.brighter() : colorPrincipal,
                colorSombra,
                fuente.getSize(),
                i == 0 ? -7 : (i == 1 ? 4 : -4)
            );
            cursorY += 24;
        }
        g2.dispose();
    }

    private void dibujarDronesLejanos(Graphics2D g) {
        for (int i = 0; i < 3; i++) {
            int ciclo = 780 + i * 150;
            int x = -80 + ((relojUI * (i + 2)) % ciclo);
            int y = 54 + i * 28 + (int) Math.round(Math.sin((relojUI + i * 20) * 0.04) * 6.0);
            int tam = 12 + i * 2;

            g.setColor(new Color(0, 0, 0, 48));
            g.fillOval(x - tam / 2, y + 8, tam + 8, 4);
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(96, 118, 132, 168));
            g.drawLine(x - tam, y, x + tam, y);
            g.drawLine(x, y - tam / 2, x, y + tam / 2);
            g.setColor(new Color(56, 66, 78, 220));
            g.fillOval(x - 5, y - 3, 10, 6);
            g.setColor(new Color(142, 230, 255, 178));
            g.fillOval(x - 2, y - 2, 4, 4);
            g.setColor(new Color(255, 92, 92, 170));
            g.fillOval(x + tam - 2, y - 1, 3, 3);
            g.fillOval(x - tam - 1, y - 1, 3, 3);
        }
    }

    private void dibujarOperativoLejano(Graphics2D g) {
        int baseY = 68;
        int ciclo = ConfiguracionJuego.ANCHO_PANEL + 460;
        int avance = (relojUI * 4) % ciclo;
        int camionetaX = ConfiguracionJuego.ANCHO_PANEL - avance + 180;
        int camioneta2X = camionetaX + 118;

        if (camioneta2X < -160 || camionetaX > ConfiguracionJuego.ANCHO_PANEL + 120) {
            return;
        }

        dibujarCamionetaLejana(g, camionetaX, baseY + 2, 1.15);
        dibujarCamionetaLejana(g, camioneta2X, baseY - 3, 1.0);

        for (int i = 0; i < 5; i++) {
            int soldadoX = camionetaX - 78 - i * 22;
            int soldadoY = baseY + 8 + (i % 2) * 4;
            dibujarSoldadoLejano(g, soldadoX, soldadoY, i, false);
        }
        for (int i = 0; i < 3; i++) {
            int contrincanteX = camioneta2X + 54 + i * 18;
            int contrincanteY = baseY + 7 + (i % 2) * 3;
            dibujarSoldadoLejano(g, contrincanteX, contrincanteY, i + 5, true);
        }

        dibujarCombateLejano(g, camionetaX - 40, camioneta2X + 28, baseY + 8);
    }

    private void dibujarCamionetaLejana(Graphics2D g, int x, int y, double escala) {
        int w = (int) Math.round(54 * escala);
        int h = (int) Math.round(16 * escala);
        g.setColor(new Color(0, 0, 0, 72));
        g.fillOval(x + 4, y + h - 1, w + 4, 5);
        g.setColor(new Color(12, 14, 18, 230));
        g.fillRoundRect(x, y, w, h, 6, 6);
        g.fillRoundRect(x + 8, y - 7, w - 18, 9, 5, 5);
        g.setColor(new Color(70, 86, 102, 178));
        g.fillRoundRect(x + 11, y - 5, w - 24, 5, 3, 3);
        g.setColor(new Color(160, 36, 28, 200));
        g.fillRect(x + w - 10, y + 4, 3, 3);
        g.setColor(new Color(248, 226, 160, 176));
        g.fillRect(x + 2, y + 4, 3, 3);
        g.setColor(new Color(44, 44, 48, 220));
        g.fillOval(x + 6, y + h - 1, 8, 8);
        g.fillOval(x + w - 14, y + h - 1, 8, 8);
    }

    private void dibujarSoldadoLejano(Graphics2D g, int x, int y, int variante, boolean mirandoIzquierda) {
        int zancada = (int) Math.round(Math.sin((relojUI + variante * 9) * 0.28) * 2.0);
        int dir = mirandoIzquierda ? -1 : 1;
        g.setColor(new Color(0, 0, 0, 38));
        g.fillOval(x - 4, y + 14, 12, 3);
        g.setColor(new Color(72, 84, 70, 212));
        g.fillOval(x, y, 6, 6);
        g.fillRoundRect(x - 1, y + 5, 8, 9, 4, 4);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(46, 54, 42, 220));
        g.drawLine(x + 1, y + 14, x - 2, y + 18 - zancada);
        g.drawLine(x + 5, y + 14, x + 8, y + 18 + zancada);
        g.drawLine(x + 1, y + 8, x + 6 * dir, y + 12);
        g.drawLine(x + 5, y + 8, x + 10 * dir, y + 11);
        g.setColor(new Color(20, 20, 18, 180));
        g.drawLine(x + 3, y + 8, x + 12 * dir, y + 8);
    }

    private void dibujarCombateLejano(Graphics2D g, int inicioX, int finX, int y) {
        for (int i = 0; i < 4; i++) {
            int x = inicioX + i * 58;
            if (((relojUI + i * 7) / 10) % 3 != 0) {
                continue;
            }
            int origenX = x + 2;
            int origenY = y + (i % 2);
            int destinoX = x + 32 + ((relojUI / 5) % 8);
            int destinoY = y - 2 + (i % 3);
            dibujarFogonazoLejano(g, origenX, origenY, new Color(255, 206, 108, 190));
            dibujarTrazadoraLejana(g, origenX + 5, origenY + 1, destinoX, destinoY, new Color(255, 240, 196, 170));
            dibujarImpactoLejano(g, destinoX, destinoY, new Color(255, 198, 112, 120));
        }

        for (int i = 0; i < 3; i++) {
            int x = finX - i * 52;
            if (((relojUI + i * 11) / 9) % 4 != 1) {
                continue;
            }
            int origenX = x;
            int origenY = y + 2 - (i % 2);
            int destinoX = x - 34 - ((relojUI / 6) % 10);
            int destinoY = y + 1 + (i % 2);
            dibujarFogonazoLejano(g, origenX, origenY, new Color(255, 196, 86, 194));
            dibujarTrazadoraLejana(g, origenX - 4, origenY + 1, destinoX, destinoY, new Color(255, 228, 184, 164));
            dibujarImpactoLejano(g, destinoX, destinoY, new Color(255, 184, 88, 118));
        }

        for (int i = 0; i < 5; i++) {
            int humoX = inicioX + 46 + i * 74 + (relojUI / 3 + i * 9) % 18;
            int humoY = y - 10 - i * 2;
            g.setColor(new Color(198, 98, 74, 86));
            g.fillOval(humoX, humoY, 18, 10);
            g.setColor(new Color(124, 74, 68, 72));
            g.fillOval(humoX + 8, humoY - 6, 14, 9);
        }

        for (int i = 0; i < 2; i++) {
            int explosionX = inicioX + 92 + i * 112 + ((relojUI / 4) % 18);
            int explosionY = y - 4 - i * 3;
            if (((relojUI + i * 13) / 14) % 5 == 2) {
                dibujarExplosionLejana(g, explosionX, explosionY);
            }
        }
    }

    private void dibujarFogonazoLejano(Graphics2D g, int x, int y, Color color) {
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 84));
        g.fillOval(x - 5, y - 5, 12, 12);
        g.setColor(color);
        g.fillOval(x - 2, y - 2, 6, 6);
        g.setColor(new Color(255, 248, 224, 196));
        g.fillOval(x, y, 2, 2);
    }

    private void dibujarTrazadoraLejana(Graphics2D g, int x1, int y1, int x2, int y2, Color color) {
        Stroke anterior = g.getStroke();
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 62));
        g.drawLine(x1, y1, x2, y2);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(color);
        g.drawLine(x1, y1, x2, y2);
        g.setColor(new Color(255, 250, 232, 126));
        g.drawLine((int) Math.round(x1 * 0.72 + x2 * 0.28), (int) Math.round(y1 * 0.72 + y2 * 0.28), x2, y2);
        g.setStroke(anterior);
    }

    private void dibujarImpactoLejano(Graphics2D g, int x, int y, Color color) {
        g.setColor(color);
        g.fillOval(x - 2, y - 2, 5, 5);
        g.setColor(new Color(255, 238, 196, 120));
        g.drawLine(x - 4, y, x + 4, y);
        g.drawLine(x, y - 4, x, y + 4);
    }

    private void dibujarExplosionLejana(Graphics2D g, int x, int y) {
        g.setColor(new Color(255, 190, 90, 70));
        g.fillOval(x - 12, y - 10, 26, 22);
        g.setColor(new Color(255, 144, 62, 140));
        g.fillOval(x - 6, y - 5, 14, 12);
        g.setColor(new Color(255, 238, 194, 180));
        g.fillOval(x - 2, y - 1, 6, 6);
        g.setColor(new Color(108, 92, 92, 88));
        g.fillOval(x + 8, y - 10, 16, 10);
        g.fillOval(x + 14, y - 16, 12, 8);
    }

    private void dibujarGradas(Graphics2D g) {
        int alto = 60;
        g.setColor(new Color(18, 18, 18, 118));
        g.fillRect(66, 0, ConfiguracionJuego.ANCHO_PANEL - 132, alto);
        g.fillRect(66, ConfiguracionJuego.ALTO_PANEL - alto, ConfiguracionJuego.ANCHO_PANEL - 132, alto);

        for (int fila = 0; fila < 3; fila++) {
            int ySuperior = 10 + fila * 14;
            int yInferior = ConfiguracionJuego.ALTO_PANEL - alto + 8 + fila * 14;
            for (int x = 82 + (fila % 2) * 8; x < ConfiguracionJuego.ANCHO_PANEL - 90; x += 20) {
                int fase = (relojUI + x + fila * 17) % 24;
                int salto = fase < 8 ? 1 : (fase < 16 ? 3 : 0);
                int accion = (x / 20 + fila) % 6;
                Color ropa = colorPublico(x + fila * 31);
                Color piel = colorPielPublico(x + fila * 19);
                dibujarFanGrada(g, x, ySuperior - salto, accion, ropa, piel, true);
                dibujarFanGrada(g, x, yInferior - salto, (accion + 2) % 6, ropa.darker(), piel, false);
            }
        }

        g.setColor(new Color(255, 214, 96, 36));
        g.fillRect(88, 16, 118, 4);
        g.setColor(new Color(106, 214, 232, 34));
        g.fillRect(ConfiguracionJuego.ANCHO_PANEL - 214, 16, 126, 4);
    }

    private void dibujarBardasYGraffitis(Graphics2D g, long seed) {
        Random random = new Random(seed ^ 0x4752414646495449L);
        int margenLateral = 126;
        int muralX = margenLateral;
        int muralW = ConfiguracionJuego.ANCHO_PANEL - margenLateral * 2;
        int hudIzqX = 18;
        int hudIzqW = 196;
        int hudDerW = 248;
        int hudDerX = ConfiguracionJuego.ANCHO_PANEL - hudDerW - 18;
        int bancaW = 214;
        int bancaX = ConfiguracionJuego.ANCHO_PANEL / 2 - bancaW / 2;
        int narracionX = ConfiguracionJuego.ANCHO_PANEL / 2 - 260;
        int narracionW = 520;

        dibujarMuralHorizontalSegmentado(g, muralX, 2, muralW, 50, random, true, new int[][] {
            { hudIzqX - 8, hudIzqW + 16 },
            { bancaX - 8, bancaW + 16 },
            { hudDerX - 8, hudDerW + 16 }
        });
        dibujarMuralHorizontalSegmentado(g, muralX, ConfiguracionJuego.ALTO_PANEL - 52, muralW, 50, random, false, new int[][] {
            { narracionX - 10, narracionW + 20 }
        });
    }

    private void dibujarMuralHorizontalSegmentado(Graphics2D g, int x, int y, int w, int h, Random random, boolean superior, int[][] exclusiones) {
        int inicio = x;
        int fin = x + w;
        for (int[] exclusion : exclusiones) {
            int exX = Math.max(inicio, exclusion[0]);
            int exFin = Math.min(fin, exclusion[0] + exclusion[1]);
            if (exX - inicio >= 150) {
                dibujarMuralHorizontalPantalla(g, inicio, y, exX - inicio - 6, h, random, superior);
            }
            inicio = Math.max(inicio, exFin + 6);
        }
        if (fin - inicio >= 150) {
            dibujarMuralHorizontalPantalla(g, inicio, y, fin - inicio, h, random, superior);
        }
    }

    private void dibujarMuralHorizontalPantalla(Graphics2D g, int x, int y, int w, int h, Random random, boolean superior) {
        if (w < 150) {
            return;
        }
        g.setColor(new Color(0, 0, 0, 56));
        g.fillRect(x + 4, y + 4, w, h);
        g.setPaint(new GradientPaint(x, y, new Color(72, 68, 64, 224), x, y + h, new Color(28, 28, 30, 220)));
        g.fillRect(x, y, w, h);
        g.setColor(new Color(228, 218, 196, 34));
        g.drawRect(x, y, w, h);

        for (int i = 0; i < 180; i++) {
            int px = x + 6 + random.nextInt(Math.max(1, w - 12));
            int py = y + 6 + random.nextInt(Math.max(1, h - 12));
            int tono = 90 + random.nextInt(46);
            g.setColor(new Color(tono, tono - 8, tono - 12, 30));
            g.fillRect(px, py, 2, 2);
        }
        for (int i = 0; i < 22; i++) {
            int chorroX = x + 12 + random.nextInt(Math.max(12, w - 24));
            int chorroY = y + random.nextInt(Math.max(8, h - 20));
            int chorroH = 10 + random.nextInt(18);
            g.setColor(new Color(16, 12, 12, 42));
            g.drawLine(chorroX, chorroY, chorroX - random.nextInt(3), Math.min(y + h - 4, chorroY + chorroH));
        }

        int stencilY = y + 7;
        boolean usaStencilIzq = w > 150;
        boolean usaStencilDer = w > 260;
        int areaTextoX = x + 16;
        int areaTextoW = w - 32;
        if (usaStencilIzq) {
            dibujarMuralStencil(g, x + 12, stencilY, 82, h - 14, true);
            areaTextoX = x + 104;
            areaTextoW -= 88;
        }
        if (usaStencilDer) {
            dibujarMuralStencil(g, x + w - 94, stencilY, 82, h - 14, false);
            areaTextoW -= 88;
        }

        String[] consignas = {
            "MUNDIAL DEL DESPOJO",
            "MUNDIAL DEL DESPOJO",
            "SU MUNDIAL NOS VALE VRG",
            "EL BARRIO RESISTE",
            "VIVIENDAS Y DERECHOS",
            "NO AL DESALOJO",
            "TECHO TRABAJO Y DIGNIDAD",
            "EL DEPORTE ES DEL PUEBLO"
        };
        String consignaPrincipal = consignas[random.nextInt(consignas.length)];
        String consignaSecundaria = consignas[random.nextInt(consignas.length)];
        while (consignaSecundaria.equals(consignaPrincipal)) {
            consignaSecundaria = consignas[random.nextInt(consignas.length)];
        }

        int textoY = superior ? y + 24 : y + 24;
        dibujarGraffitiMuro(
            g,
            consignaPrincipal,
            areaTextoX,
            textoY,
            new Color(244, 231, 210, 224),
            new Color(28, 12, 10, 196),
            Math.max(16, Math.min(24, areaTextoW / 15)),
            superior ? -4 : 3
        );
        dibujarGraffitiMuro(
            g,
            consignaSecundaria,
            areaTextoX + 18,
            textoY + 18,
            new Color(214, 52, 46, 220),
            new Color(24, 12, 12, 188),
            Math.max(13, Math.min(18, areaTextoW / 20)),
            superior ? 2 : -3
        );

        g.setColor(new Color(210, 204, 188, 120));
        for (int px = x + 118; px < x + w - 118; px += 36) {
            g.drawLine(px, y + 4, px + 8, y + h - 6);
        }
    }

    private void dibujarPanelMuralAmplio(Graphics2D g, int x, int y, int w, int h, String titulo, String bandaTexto, boolean mirarDerecha, Random random) {
        g.setColor(new Color(0, 0, 0, 94));
        g.fillRoundRect(x + 5, y + 6, w, h, 14, 14);
        g.setPaint(new GradientPaint(x, y, new Color(74, 70, 66, 244), x, y + h, new Color(42, 40, 38, 240)));
        g.fillRoundRect(x, y, w, h, 12, 12);
        g.setColor(new Color(236, 228, 208, 72));
        g.drawRoundRect(x, y, w, h, 12, 12);

        for (int i = 0; i < 120; i++) {
            int px = x + 4 + random.nextInt(Math.max(1, w - 8));
            int py = y + 4 + random.nextInt(Math.max(1, h - 8));
            int tono = 88 + random.nextInt(56);
            g.setColor(new Color(tono, tono - 6, tono - 12, 34));
            g.fillRect(px, py, 2, 2);
        }

        int cantidadFiguras = 3 + random.nextInt(2);
        for (int i = 0; i < cantidadFiguras; i++) {
            int figuraX = x + 18 + i * ((w - 48) / Math.max(1, cantidadFiguras - 1)) + random.nextInt(8) - 4;
            int figuraY = y + 24 + random.nextInt(10);
            int figuraW = 42 + random.nextInt(10);
            int figuraH = 46 + random.nextInt(10);
            boolean principal = i == cantidadFiguras / 2;
            dibujarFiguraMural(g, figuraX, figuraY, figuraW, figuraH, principal, mirarDerecha == (i % 2 == 0));
        }

        dibujarTextoMural(g, titulo, x + 14, y + 28, new Color(238, 228, 214, 236), new Color(46, 18, 12, 180), 22);
        dibujarBandaTextoMural(g, x + 8, y + h - 28, w - 16, 20, bandaTexto);
    }

    private void dibujarFiguraMural(Graphics2D g, int x, int y, int w, int h, boolean principal, boolean mirarDerecha) {
        int cabezaW = Math.max(14, w / 2);
        int cabezaH = Math.max(16, h / 3);
        int cabezaX = x + (w - cabezaW) / 2 + (mirarDerecha ? 2 : -2);
        int cabezaY = y;
        int torsoY = cabezaY + cabezaH - 2;
        int torsoH = h - cabezaH + 2;

        g.setColor(new Color(20, 20, 24, 214));
        g.fillOval(cabezaX - 4, cabezaY - 3, cabezaW + 8, cabezaH + 10);
        g.setPaint(new GradientPaint(cabezaX, cabezaY, new Color(44, 44, 50, 234), cabezaX, cabezaY + cabezaH, new Color(14, 14, 18, 232)));
        g.fillOval(cabezaX, cabezaY, cabezaW, cabezaH);

        g.setColor(new Color(234, 234, 232, 220));
        g.fillOval(cabezaX + 4, cabezaY + cabezaH / 2 - 2, Math.max(5, cabezaW / 3), 4);
        g.fillOval(cabezaX + cabezaW - Math.max(5, cabezaW / 3) - 4, cabezaY + cabezaH / 2 - 2, Math.max(5, cabezaW / 3), 4);
        g.setColor(new Color(26, 26, 28, 220));
        g.fillOval(cabezaX + 6, cabezaY + cabezaH / 2 - 1, 2, 2);
        g.fillOval(cabezaX + cabezaW - 8, cabezaY + cabezaH / 2 - 1, 2, 2);

        int[] paliacateX = { x + 4, x + w / 2, x + w - 4, x + w - 8, x + w / 2, x + 8 };
        int[] paliacateY = { torsoY + 6, torsoY + 2, torsoY + 6, torsoY + 16, y + h - 2, torsoY + 16 };
        g.setColor(new Color(128, 26, 26, 220));
        g.fillPolygon(paliacateX, paliacateY, paliacateX.length);
        g.setColor(new Color(218, 188, 172, 120));
        for (int i = 0; i < 4; i++) {
            g.drawLine(x + 8, torsoY + 8 + i * 3, x + w - 8, torsoY + 10 + i * 3);
        }

        g.setPaint(new GradientPaint(x, torsoY + 10, principal ? new Color(76, 78, 88, 230) : new Color(56, 58, 66, 224), x, y + h, new Color(22, 22, 28, 234)));
        g.fillRoundRect(x + 2, torsoY + 10, w - 4, Math.max(14, torsoH - 12), 10, 10);
        g.setColor(new Color(18, 18, 22, 170));
        g.drawLine(x + 8, torsoY + 18, x + (mirarDerecha ? w + 8 : -8), y + h - 10);
        g.drawLine(x + w - 8, torsoY + 18, x + (mirarDerecha ? w + 14 : -2), y + h - 6);

        if (principal) {
            g.setColor(new Color(210, 40, 46, 212));
            g.fillOval(x + w / 2 - 12, y + h / 2 + 2, 24, 24);
            g.setColor(new Color(236, 230, 218, 210));
            g.drawLine(x + w / 2 - 8, y + h / 2 + 14, x + w / 2 + 8, y + h / 2 + 14);
            g.drawLine(x + w / 2, y + h / 2 + 6, x + w / 2, y + h / 2 + 22);
        }
    }

    private void dibujarTextoMural(Graphics2D g, String texto, int x, int y, Color principal, Color sombra, int tamano) {
        Font anterior = g.getFont();
        g.setFont(new Font("SansSerif", Font.BOLD, tamano));
        g.setColor(new Color(255, 255, 255, 34));
        g.drawString(texto, x + 1, y - 1);
        g.setColor(sombra);
        g.drawString(texto, x + 3, y + 2);
        g.setColor(principal);
        g.drawString(texto, x, y);
        g.setFont(anterior);
    }

    private void dibujarBandaTextoMural(Graphics2D g, int x, int y, int w, int h, String texto) {
        g.setColor(new Color(220, 214, 198, 210));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(120, 38, 24, 154));
        g.drawRoundRect(x, y, w, h, 8, 8);
        dibujarTextoMural(g, texto, x + 12, y + h - 5, new Color(34, 28, 24, 230), new Color(160, 124, 108, 110), 12);
    }

    private void dibujarFranjaMuralInferior(Graphics2D g, int x, int y, int w, int h, String texto, Random random) {
        g.setPaint(new GradientPaint(x, y, new Color(44, 40, 42, 242), x, y + h, new Color(20, 18, 22, 236)));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(new Color(226, 220, 206, 54));
        g.drawRoundRect(x, y, w, h, 10, 10);
        for (int i = 0; i < 7; i++) {
            int px = x + 18 + i * ((w - 36) / 6);
            g.setColor(new Color(160, 38, 40, 200));
            g.fillOval(px, y + 10 + random.nextInt(8), 10, 10);
        }
        dibujarTextoMural(g, texto, x + w / 2 - 86, y + 30, new Color(236, 228, 214, 236), new Color(44, 18, 16, 184), 18);
    }

    private void dibujarPanelConsigna(Graphics2D g, int x, int y, int w, int h, String texto, Color colorPrincipal, Color colorSombra, int tamano) {
        g.setColor(new Color(10, 12, 16, 132));
        g.fillRoundRect(x + 3, y + 3, w, h, 12, 12);
        g.setPaint(new GradientPaint(x, y, new Color(255, 255, 255, 46), x, y + h, new Color(255, 255, 255, 18)));
        g.fillRoundRect(x, y, w, h, 12, 12);
        g.setColor(new Color(255, 255, 255, 84));
        g.drawRoundRect(x, y, w, h, 12, 12);
        dibujarGraffiti(g, texto, x + 10, y + h - 10, colorPrincipal, colorSombra, tamano);
    }

    private void dibujarMuralPanelLateral(Graphics2D g, int x, int y, int w, int h, boolean mirarDerecha) {
        g.setColor(new Color(8, 10, 14, 146));
        g.fillRoundRect(x - 4, y - 4, w + 8, h + 8, 14, 14);
        g.setPaint(new GradientPaint(x, y, new Color(46, 48, 58, 238), x, y + h, new Color(20, 22, 28, 234)));
        g.fillRoundRect(x, y, w, h, 12, 12);
        g.setColor(new Color(255, 255, 255, 56));
        g.drawRoundRect(x, y, w, h, 12, 12);
        dibujarMuralStencil(g, x + 6, y + 10, w - 12, h - 20, mirarDerecha);
    }

    private void dibujarGraffiti(Graphics2D g, String texto, int x, int y, Color colorPrincipal, Color colorSombra, int tamano) {
        Font anterior = g.getFont();
        g.setFont(new Font("SansSerif", Font.BOLD, tamano));
        g.setColor(new Color(255, 255, 255, 38));
        g.drawString(texto, x + 1, y - 1);
        g.setColor(colorSombra);
        g.drawString(texto, x + 2, y + 2);
        g.setColor(colorPrincipal);
        g.drawString(texto, x, y);
        g.setFont(anterior);
    }

    private void dibujarMuralStencil(Graphics2D g, int x, int y, int w, int h, boolean mirarDerecha) {
        g.setColor(new Color(0, 0, 0, 48));
        g.fillOval(x + 4, y + h - 10, w - 8, 8);
        g.setPaint(new GradientPaint(x, y, new Color(24, 28, 38, 210), x, y + h, new Color(12, 14, 18, 204)));
        g.fillRect(x, y, w, h);

        int mascaraY = y + h / 2 + 2;
        for (int i = 0; i < 160; i++) {
            int px = x + 4 + (i * 17) % Math.max(1, w - 10);
            int py = y + 4 + (i * 29) % Math.max(1, h - 10);
            int tam = 5 + (i % 5);
            boolean zonaPanuelo = py > mascaraY - 14 || py < y + 24;
            g.setColor(zonaPanuelo
                ? new Color(188 + (i % 36), 148 + (i % 30), 52 + (i % 24), 120)
                : new Color(64 + (i % 16), 78 + (i % 18), 98 + (i % 14), 62));
            g.fillOval(px, py, tam, tam);
            g.setColor(zonaPanuelo
                ? new Color(54, 88, 100, 128)
                : new Color(16, 26, 34, 88));
            g.fillOval(px + 1, py + 1, Math.max(2, tam - 2), Math.max(2, tam - 2));
        }

        int caraCentroX = x + w / 2;
        int caraCentroY = y + h / 2 - 4;
        int caraW = (int) (w * 0.74);
        int caraH = (int) (h * 0.74);

        g.setColor(new Color(36, 24, 22, 160));
        g.fillOval(caraCentroX - caraW / 2 - 2, caraCentroY - caraH / 2 + 2, caraW + 4, caraH);
        g.setPaint(new GradientPaint(
            caraCentroX - caraW / 2,
            caraCentroY,
            new Color(142, 84, 48, 214),
            caraCentroX + caraW / 2,
            caraCentroY,
            new Color(234, 170, 76, 224)
        ));
        g.fillOval(caraCentroX - caraW / 2, caraCentroY - caraH / 2, caraW, caraH);

        g.setColor(new Color(64, 38, 30, 126));
        g.fillOval(caraCentroX - caraW / 2 + 8, caraCentroY - caraH / 2 + 14, caraW / 3, caraH - 34);
        g.fillOval(caraCentroX + caraW / 6, caraCentroY - caraH / 2 + 14, caraW / 3, caraH - 34);

        int ojoY = y + h / 3 + 10;
        dibujarOjoMural(g, x + w / 4 - 20, ojoY, 40, 22, mirarDerecha);
        dibujarOjoMural(g, x + (w * 3 / 4) - 20, ojoY, 40, 22, !mirarDerecha);

        g.setPaint(new GradientPaint(
            caraCentroX,
            y + 18,
            new Color(252, 192, 96, 180),
            caraCentroX,
            y + h - 28,
            new Color(138, 80, 46, 92)
        ));
        g.fillRoundRect(caraCentroX - 10, y + 18, 20, h - 40, 10, 10);
        g.setColor(new Color(40, 26, 20, 150));
        g.drawLine(caraCentroX, y + 12, caraCentroX, y + h - 12);

        int[] panueloX = { x + 8, caraCentroX - 8, x + w - 8, x + w - 16, caraCentroX, x + 16 };
        int[] panueloY = { mascaraY + 2, mascaraY - 8, mascaraY + 2, y + h - 18, y + h - 2, y + h - 18 };
        g.setColor(new Color(26, 40, 72, 214));
        g.fillPolygon(panueloX, panueloY, panueloX.length);
        g.setColor(new Color(14, 18, 34, 164));
        g.drawLine(x + 10, mascaraY + 4, x + w - 10, mascaraY + 4);

        for (int i = 0; i < 20; i++) {
            int px = x + 12 + (i * 15) % Math.max(1, w - 24);
            int py = mascaraY + 8 + (i * 12) % Math.max(1, h / 2 - 18);
            int tam = 8 + i % 5;
            g.setColor(new Color(206, 168, 82, 164));
            g.fillOval(px, py, tam, tam);
            g.setColor(new Color(82, 114, 124, 138));
            g.fillOval(px + 2, py + 2, Math.max(3, tam - 4), Math.max(3, tam - 4));
        }

        g.setColor(new Color(16, 12, 12, 150));
        for (int i = 0; i < 5; i++) {
            int px = x + 8 + i * (w / 5);
            g.drawLine(px, y, px - 3, y + h);
            g.drawLine(px + 1, y + 10, px - 2, y + h - 14);
        }
        g.setColor(new Color(214, 204, 182, 28));
        g.drawRect(x, y, w, h);
    }

    private void dibujarOjoMural(Graphics2D g, int x, int y, int w, int h, boolean mirarDerecha) {
        g.setColor(new Color(22, 14, 12, 220));
        g.fillOval(x - 4, y - 5, w + 8, h + 10);
        g.setColor(new Color(248, 232, 210, 228));
        g.fillOval(x, y, w, h);
        int irisX = mirarDerecha ? x + w / 2 - 3 : x + w / 2 - 8;
        g.setColor(new Color(198, 112, 34, 232));
        g.fillOval(irisX, y + 2, w / 3, h - 4);
        g.setColor(new Color(24, 18, 16, 236));
        g.fillOval(irisX + w / 9 - 1, y + h / 2 - 5, 10, 10);
        g.setColor(new Color(255, 255, 255, 140));
        g.fillOval(irisX + w / 9 + 1, y + h / 2 - 4, 3, 3);
        g.setColor(new Color(18, 18, 20, 220));
        g.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(x - 3, y - 7, w + 6, h + 2, 180, 180);
        g.drawArc(x - 1, y + 6, w + 2, h - 1, 0, -180);
        g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x - 2, y + 2, x + w / 3, y - 4);
        g.drawLine(x + w - 2, y + 2, x + w * 2 / 3, y - 4);
    }

    private void dibujarEspectadoresBarda(Graphics2D g, int muroY, boolean superior) {
        int baseY = superior ? muroY - 20 : muroY - 22;
        int[] xs = superior
            ? new int[] { 138, 170, 212, 248, 296, 334, 382, 606, 648, 690, 738, 890, 934, 980, 1022 }
            : new int[] { 126, 148, 192, 236, 278, 322, 566, 610, 660, 706, 844, 892, 944, 990, 1034 };
        for (int i = 0; i < xs.length; i++) {
            Color ropa = i % 3 == 0 ? new Color(64, 128, 188) : (i % 3 == 1 ? new Color(174, 82, 62) : new Color(92, 142, 78));
            Color cabello = i % 2 == 0 ? new Color(26, 30, 40) : new Color(54, 34, 28);
            Color piel = i % 3 == 0 ? new Color(238, 188, 148) : (i % 3 == 1 ? new Color(224, 176, 138) : new Color(206, 154, 118));
            dibujarEspectadorBarda(g, xs[i], baseY + (i % 2), i % 5, ropa, cabello, piel);
        }
    }

    private void dibujarFanGrada(Graphics2D g, int x, int y, int accion, Color ropa, Color piel, boolean superior) {
        int cabezaX = x;
        int cabezaY = y;
        int torsoX = x - 3;
        int torsoY = y + 7;
        int pieY = torsoY + 10;
        g.setColor(new Color(0, 0, 0, 42));
        g.fillOval(x - 6, pieY + 5, 14, 3);
        g.setColor(piel);
        g.fillOval(cabezaX, cabezaY, 6, 6);
        g.setColor(new Color(38, 26, 24));
        g.fillArc(cabezaX, cabezaY - 1, 6, 4, 0, 180);
        g.setColor(ropa);
        g.fillRoundRect(torsoX, torsoY, 10, 8, 4, 4);
        g.setColor(new Color(18, 18, 22, 180));
        g.drawRoundRect(torsoX, torsoY, 10, 8, 4, 4);
        int hombroY = torsoY + 2;
        int brazoIzqY = accion == 0 || accion == 3 ? hombroY - 3 : hombroY + 2;
        int brazoDerY = accion == 1 || accion == 3 ? hombroY - 3 : hombroY + 2;
        g.setColor(piel);
        g.drawLine(torsoX + 1, hombroY, torsoX - 3, brazoIzqY);
        g.drawLine(torsoX + 9, hombroY, torsoX + 13, brazoDerY);
        if (accion == 4) {
            g.setColor(superior ? new Color(255, 214, 96) : new Color(106, 214, 232));
            g.fillRect(torsoX + 12, hombroY - 5, 2, 8);
            g.fillRect(torsoX + 14, hombroY - 5, 6, 4);
        }
        if (accion == 5) {
            g.setColor(new Color(248, 248, 248, 170));
            g.drawLine(torsoX - 4, hombroY + 1, torsoX + 14, hombroY + 1);
        }
        g.setColor(new Color(42, 42, 46));
        g.drawLine(torsoX + 2, torsoY + 7, torsoX + 1, pieY);
        g.drawLine(torsoX + 8, torsoY + 7, torsoX + 9, pieY);
    }

    private Color colorPublico(int semilla) {
        return switch (Math.floorMod(semilla, 6)) {
            case 0 -> new Color(64, 128, 188);
            case 1 -> new Color(174, 82, 62);
            case 2 -> new Color(92, 142, 78);
            case 3 -> new Color(246, 198, 78);
            case 4 -> new Color(120, 98, 188);
            default -> new Color(82, 182, 164);
        };
    }

    private Color colorPielPublico(int semilla) {
        return switch (Math.floorMod(semilla, 4)) {
            case 0 -> new Color(238, 188, 148);
            case 1 -> new Color(224, 176, 138);
            case 2 -> new Color(206, 154, 118);
            default -> new Color(176, 122, 88);
        };
    }

    private void dibujarEspectadorBarda(Graphics2D g, int x, int y, int accion, Color ropa, Color cabello, Color piel) {
        int balanceo = (int) Math.round(Math.sin((relojUI + x) * 0.08) * 2.0);
        int cabezaTam = 13;
        int torsoW = 18;
        int torsoH = 20;
        int cabezaX = x;
        int cabezaY = y + balanceo;
        int torsoX = x - 3;
        int torsoY = cabezaY + 10;
        int hombroY = torsoY + 5;
        int caderaY = torsoY + torsoH - 1;

        g.setColor(new Color(0, 0, 0, 42));
        g.fillOval(x - 10, y + 34, 24, 5);

        g.setColor(new Color(88, 92, 102, 226));
        g.fillRect(x - 6, y + 29, 24, 5);

        g.setPaint(new GradientPaint(cabezaX, cabezaY, piel.brighter(), cabezaX, cabezaY + cabezaTam, piel.darker()));
        g.fillOval(cabezaX, cabezaY, cabezaTam, cabezaTam);
        g.setColor(cabello);
        g.fillArc(cabezaX - 1, cabezaY - 1, cabezaTam + 2, 9, 0, 180);
        g.setColor(new Color(18, 18, 22, 164));
        g.drawOval(cabezaX, cabezaY, cabezaTam, cabezaTam);

        g.setPaint(new GradientPaint(torsoX, torsoY, ropa.brighter(), torsoX, torsoY + torsoH, ropa.darker()));
        g.fillRoundRect(torsoX, torsoY, torsoW, torsoH, 10, 10);
        g.setColor(new Color(255, 255, 255, 86));
        g.drawRoundRect(torsoX, torsoY, torsoW, torsoH, 10, 10);
        g.setColor(new Color(24, 24, 28, 190));
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int brazoIzqX = torsoX - 4;
        int brazoDerX = torsoX + torsoW + 4;
        int manoIzqY = hombroY + 6;
        int manoDerY = hombroY + 6;

        if (accion == 0) {
            g.setColor(piel);
            g.drawLine(torsoX + 2, hombroY, brazoIzqX, manoIzqY);
            g.drawLine(torsoX + torsoW - 2, hombroY, brazoDerX, manoDerY);
            g.setColor(new Color(236, 236, 240, 170));
            g.drawLine(brazoDerX + 1, manoDerY - 6, brazoDerX + 1, manoDerY - 2);
            g.setColor(new Color(180, 180, 188, 120));
            g.drawOval(brazoDerX - 1, manoDerY - 12, 6, 8);
        } else if (accion == 1) {
            g.setColor(piel);
            g.drawLine(torsoX + 2, hombroY, brazoIzqX, manoIzqY - 2);
            g.drawLine(torsoX + torsoW - 2, hombroY, brazoDerX - 2, manoDerY + 2);
            g.setColor(new Color(92, 62, 42, 210));
            g.fillRoundRect(brazoIzqX - 1, manoIzqY - 6, 6, 5, 3, 3);
            g.setColor(new Color(208, 176, 94, 180));
            g.fillOval(brazoIzqX + 1, manoIzqY - 5, 2, 2);
        } else if (accion == 2) {
            g.setColor(piel);
            g.drawLine(torsoX + 2, hombroY, torsoX - 1, hombroY + 8);
            g.drawLine(torsoX + torsoW - 2, hombroY, brazoDerX + 1, manoDerY);
            g.setColor(new Color(70, 120, 180, 220));
            g.fillRoundRect(brazoDerX - 1, manoDerY - 7, 7, 9, 3, 3);
            g.setColor(new Color(214, 236, 255, 110));
            g.drawLine(brazoDerX, manoDerY - 4, brazoDerX + 4, manoDerY - 4);
        } else if (accion == 3) {
            int gesto = (int) Math.round(Math.sin((relojUI + x) * 0.22) * 2.0);
            g.setColor(piel);
            g.drawLine(torsoX + 2, hombroY, brazoIzqX, manoIzqY - gesto);
            g.drawLine(torsoX + torsoW - 2, hombroY, brazoDerX, manoDerY + gesto);
        } else {
            int trago = (int) Math.round(Math.sin((relojUI + x) * 0.18) * 2.0);
            g.setColor(piel);
            g.drawLine(torsoX + 2, hombroY, brazoIzqX + 1, manoIzqY + 1);
            g.drawLine(torsoX + torsoW - 2, hombroY, brazoDerX - 2, manoDerY - trago);
            g.setColor(new Color(228, 96, 84, 210));
            g.fillRoundRect(brazoDerX - 1, manoDerY - 8 - trago, 4, 8, 2, 2);
        }

        g.setColor(new Color(34, 34, 38, 190));
        g.drawLine(torsoX + 4, caderaY, torsoX + 2, y + 36);
        g.drawLine(torsoX + torsoW - 4, caderaY, torsoX + torsoW + 2, y + 36);
        g.setColor(new Color(18, 18, 20, 210));
        g.drawLine(torsoX - 1, y + 37, torsoX + 5, y + 37);
        g.drawLine(torsoX + torsoW - 3, y + 37, torsoX + torsoW + 3, y + 37);
    }

    private void dibujarBanca(Graphics2D g) {
        GeometriaCancha cancha = ConfiguracionJuego.MAPA_CANCHA;
        int bancaX = ConfiguracionJuego.ANCHO_PANEL / 2 - 136;
        int bancaY = cancha.getCampoYMin() - 92;
        sprites.dibujarBanca(g, bancaX, bancaY);
    }

    private void dibujarCancha(Graphics2D g, GeometriaCancha cancha, int frameAnimacion) {
        sprites.dibujarCancha(g, cancha, frameAnimacion);
    }

    private void dibujarPorteria(Graphics2D g, GeometriaCancha cancha, boolean local) {
        Rectangle porteria = cancha.getPorteria(local);
        int xMarco = porteria.x;
        int yMarco = porteria.y;
        int fondo = porteria.width;
        int anchoMarco = Math.max(12, porteria.width - 4);
        int altoMarco = porteria.height;

        g.setColor(new Color(232, 232, 232));
        g.fillRect(xMarco, yMarco, anchoMarco, altoMarco);
        g.fillRect(local ? xMarco - fondo : xMarco + anchoMarco, yMarco, fondo, altoMarco);
        g.setColor(new Color(185, 195, 206, 165));

        for (int i = 0; i < altoMarco; i += 14) {
            int xRed = local ? xMarco - fondo : xMarco + anchoMarco;
            g.drawLine(xMarco + (local ? 0 : anchoMarco), yMarco + i, xRed + (local ? fondo : 0), yMarco + i + 6);
        }
        for (int i = 0; i <= fondo; i += 7) {
            int x = local ? xMarco - i : xMarco + anchoMarco + i;
            g.drawLine(x, yMarco, x, yMarco + altoMarco);
        }
    }

    private void dibujarShowMedioTiempo(Graphics2D g, MotorJuego motor) {
        GeometriaCancha cancha = motor.getCancha();
        int cx = cancha.getCentroX();
        int cy = cancha.getCentroY();
        double progreso = motor.getProgresoMedioTiempo();
        int pulso = (int) Math.round(Math.sin(relojUI * 0.12) * 8.0);

        g.setColor(new Color(255, 226, 158, 22));
        g.fillOval(cx - 126, cy - 78, 252, 156);
        g.setColor(new Color(255, 210, 116, 36));
        g.fillOval(cx - 94, cy - 42, 188, 84);
        g.setColor(new Color(18, 16, 18, 120));
        g.fillOval(cx - 112, cy + 42, 224, 24);

        dibujarBocinaMedioTiempo(g, cx - 134, cy + 2);
        dibujarBocinaMedioTiempo(g, cx + 104, cy + 2);
        dibujarBailarinaMedioTiempo(g, cx - 52, cy - 20 + pulso / 3, new Color(240, 88, 132), new Color(255, 214, 120), 0.0);
        dibujarBailarinaMedioTiempo(g, cx + 8, cy - 26 - pulso / 4, new Color(98, 214, 255), new Color(255, 168, 92), Math.PI / 2.5);
        dibujarBailarinaMedioTiempo(g, cx + 68, cy - 18 + pulso / 3, new Color(170, 112, 255), new Color(255, 218, 144), Math.PI);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(255, 234, 184, 214));
        g.drawString("MEDIO TIEMPO", cx - 74, cancha.getCampoYMin() + 34);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(244, 244, 244, 188));
        g.drawString("Show en el centro | Los equipos descansan e hidratan", cx - 152, cancha.getCampoYMin() + 52);

        int brilloX = cx - 86 + (int) Math.round(Math.sin(relojUI * 0.08) * 18.0);
        g.setColor(new Color(255, 255, 255, 46));
        g.fillRoundRect(brilloX, cy - 68, 34, 104, 18, 18);
        g.fillRoundRect(brilloX + 122, cy - 68, 34, 104, 18, 18);

        int confetiBaseY = cancha.getCampoYMin() + 22;
        for (int i = 0; i < 12; i++) {
            int px = cx - 128 + i * 22;
            int py = confetiBaseY + (int) ((relojUI * 1.6 + i * 14) % 34);
            g.setColor(new Color(220 - i * 8, 120 + (i * 10) % 120, 170 + (i * 7) % 70, 110));
            g.fillOval(px, py, 4, 4);
        }

        int barraW = 160;
        int barraX = cx - barraW / 2;
        int barraY = cancha.getCampoYMax() - 20;
        g.setColor(new Color(0, 0, 0, 110));
        g.fillRoundRect(barraX, barraY, barraW, 8, 8, 8);
        g.setColor(new Color(255, 208, 106, 180));
        g.fillRoundRect(barraX, barraY, Math.max(8, (int) Math.round(barraW * progreso)), 8, 8, 8);
    }

    private void dibujarBocinaMedioTiempo(Graphics2D g, int x, int y) {
        g.setColor(new Color(18, 18, 22, 176));
        g.fillRoundRect(x, y, 30, 48, 10, 10);
        g.setColor(new Color(74, 80, 92, 184));
        g.drawRoundRect(x, y, 30, 48, 10, 10);
        g.setColor(new Color(28, 28, 30, 214));
        g.fillOval(x + 6, y + 8, 18, 18);
        g.fillOval(x + 8, y + 28, 14, 14);
        g.setColor(new Color(120, 126, 138, 90));
        g.drawOval(x + 6, y + 8, 18, 18);
        g.drawOval(x + 8, y + 28, 14, 14);
    }

    private void dibujarBailarinaMedioTiempo(Graphics2D g, int x, int y, Color ropa, Color detalle, double fase) {
        Graphics2D g2 = (Graphics2D) g.create();
        int sway = (int) Math.round(Math.sin(relojUI * 0.18 + fase) * 6.0);
        int alzaBrazo = (int) Math.round(Math.cos(relojUI * 0.22 + fase) * 9.0);
        int cabezaTam = 12;
        int torsoW = 18;
        int torsoH = 28;

        g2.setColor(new Color(0, 0, 0, 52));
        g2.fillOval(x - 10, y + 58, 28, 8);

        g2.setPaint(new GradientPaint(x, y, new Color(255, 226, 194), x, y + cabezaTam, new Color(224, 174, 138)));
        g2.fillOval(x, y, cabezaTam, cabezaTam);
        g2.setColor(new Color(54, 30, 28, 180));
        g2.fillArc(x - 1, y - 2, cabezaTam + 2, 10, 0, 180);
        g2.setColor(new Color(20, 18, 18, 180));
        g2.fillOval(x + 3, y + 6, 2, 2);
        g2.fillOval(x + 7, y + 6, 2, 2);
        g2.drawArc(x + 3, y + 8, 5, 3, 180, 180);

        int torsoX = x - 3 + sway / 3;
        int torsoY = y + cabezaTam - 1;
        g2.setPaint(new GradientPaint(torsoX, torsoY, ropa.brighter(), torsoX, torsoY + torsoH, ropa.darker()));
        g2.fillRoundRect(torsoX, torsoY, torsoW, torsoH, 10, 10);
        g2.setColor(detalle);
        g2.fillRoundRect(torsoX + torsoW / 2 - 2, torsoY + 2, 4, torsoH - 4, 3, 3);
        g2.setColor(new Color(255, 242, 222, 210));
        g2.drawLine(torsoX + 3, torsoY + 4, torsoX + torsoW - 4, torsoY + 4);

        g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(236, 188, 152));
        g2.drawLine(torsoX + 3, torsoY + 8, torsoX - 8, torsoY - 4 - alzaBrazo);
        g2.drawLine(torsoX + torsoW - 3, torsoY + 8, torsoX + torsoW + 10, torsoY + 2 + alzaBrazo / 2);

        g2.setColor(new Color(42, 28, 62, 200));
        int faldaY = torsoY + torsoH - 2;
        int[] xs = { torsoX + 2, torsoX + torsoW / 2, torsoX + torsoW - 2, torsoX + torsoW + 5, torsoX - 5 };
        int[] ys = { faldaY, faldaY - 4, faldaY, faldaY + 14, faldaY + 14 };
        g2.fillPolygon(xs, ys, xs.length);

        g2.setColor(ropa.darker());
        g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(torsoX + 5, faldaY + 10, torsoX + 1 - sway / 5, y + 56);
        g2.drawLine(torsoX + torsoW - 5, faldaY + 10, torsoX + torsoW + 5 + sway / 5, y + 56);
        g2.setColor(detalle);
        g2.drawLine(torsoX + 1 - sway / 5, y + 48, torsoX + 1 - sway / 5, y + 54);
        g2.drawLine(torsoX + torsoW + 5 + sway / 5, y + 48, torsoX + torsoW + 5 + sway / 5, y + 54);
        g2.setColor(new Color(18, 18, 18));
        g2.fillRoundRect(torsoX - 5 - sway / 5, y + 53, 9, 4, 4, 4);
        g2.fillRoundRect(torsoX + torsoW + 1 + sway / 5, y + 53, 9, 4, 4, 4);
        g2.dispose();
    }

    private void dibujarMarcadoresCancha(Graphics2D g, MotorJuego motor) {
        GeometriaCancha cancha = motor.getCancha();
        int xIzq = cancha.getCampoXMin() + 28;
        int xDer = cancha.getCampoXMax() - 212;
        int y = cancha.getCampoYMin() + 18;

        g.setColor(new Color(0, 0, 0, 104));
        g.fillRoundRect(xIzq, y, 184, 34, 14, 14);
        g.fillRoundRect(xDer, y, 184, 34, 14, 14);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(255, 118, 94));
        g.drawString("LOCAL " + motor.getGolesLocal(), xIzq + 14, y + 23);
        g.setColor(new Color(118, 210, 255));
        g.drawString("RIVAL " + motor.getGolesRival(), xDer + 14, y + 23);

        int pulso = (int) (Math.sin(relojUI * 0.05) * 5.0);
        g.setColor(new Color(255, 230, 120, 55));
        g.fillOval(cancha.getCentroX() - 132 + pulso, cancha.getCampoYMin() + 14, 264, 46);
    }

    private void dibujarJugadoresPorProfundidad(Graphics2D g, MotorJuego motor) {
        List<Jugador> orden = new ArrayList<>();
        for (Jugador jugador : motor.getTodosJugadores()) {
            orden.add(jugador);
        }
        orden.add(motor.getArbitro());
        orden.sort(Comparator.comparingInt(Jugador::getY));

        for (Jugador jugador : orden) {
            sprites.dibujarJugador(g, jugador);
        }
    }

    private void dibujarEtiquetasJugadores(Graphics2D g, MotorJuego motor) {
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        for (Jugador jugador : motor.getTodosJugadores()) {
            dibujarEtiquetaJugador(g, jugador, new Color(250, 248, 236), new Color(0, 0, 0, 140));
        }
        dibujarEtiquetaJugador(g, motor.getArbitro(), new Color(255, 233, 120), new Color(0, 0, 0, 138));
    }

    private void dibujarHUD(Graphics2D g, MotorJuego motor, EntradaJuego entrada) {
        dibujarTarjetaMarcador(g, motor);
        dibujarTarjetaEstado(g, motor);
        dibujarBarraCargaAccion(g, entrada, motor.isModoEspectador());
    }

    private void dibujarIndicadorArbitral(Graphics2D g, MotorJuego motor) {
        String accion = motor.getEstadoArbitrajeTexto();
        if (accion == null || accion.isEmpty()) {
            return;
        }

        Jugador arbitro = motor.getArbitro();
        int frames = Math.max(1, motor.getFramesAccionArbitro());
        int pulso = (int) Math.round(Math.sin(relojUI * 0.20) * 6.0);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        int w = Math.max(52, fm.stringWidth(accion) + 14);
        int x = arbitro.getX() + arbitro.getAncho() / 2 - w / 2;
        int y = arbitro.getY() - 28 - pulso / 2;
        int h = 18;
        int alphaBase = Math.max(72, Math.min(210, 120 + frames * 3));

        Color fondo = new Color(ConfiguracionJuego.HUD_PANEL.getRed(), ConfiguracionJuego.HUD_PANEL.getGreen(), ConfiguracionJuego.HUD_PANEL.getBlue(), Math.max(48, alphaBase));
        Color borde = new Color(ConfiguracionJuego.HUD_ACCENT.getRed(), ConfiguracionJuego.HUD_ACCENT.getGreen(), ConfiguracionJuego.HUD_ACCENT.getBlue(), Math.min(255, alphaBase + 36));
        Color texto = new Color(ConfiguracionJuego.HUD_TEXT.getRed(), ConfiguracionJuego.HUD_TEXT.getGreen(), ConfiguracionJuego.HUD_TEXT.getBlue(), Math.min(255, alphaBase + 28));
        if ("FALTA".equals(accion)) {
            fondo = new Color(ConfiguracionJuego.HUD_DANGER.getRed(), ConfiguracionJuego.HUD_DANGER.getGreen(), ConfiguracionJuego.HUD_DANGER.getBlue(), alphaBase);
            borde = new Color(255, 132, 112, Math.min(255, alphaBase + 32));
            texto = new Color(255, 220, 214, Math.min(255, alphaBase + 28));
        } else if ("AMARILLA".equals(accion)) {
            fondo = new Color(ConfiguracionJuego.HUD_WARN.getRed(), ConfiguracionJuego.HUD_WARN.getGreen(), ConfiguracionJuego.HUD_WARN.getBlue(), alphaBase);
            borde = new Color(255, 220, 90, Math.min(255, alphaBase + 32));
            texto = new Color(255, 246, 190, Math.min(255, alphaBase + 28));
        } else if ("ROJA".equals(accion)) {
            fondo = new Color(ConfiguracionJuego.HUD_DANGER.getRed(), ConfiguracionJuego.HUD_DANGER.getGreen(), ConfiguracionJuego.HUD_DANGER.getBlue(), alphaBase);
            borde = new Color(255, 94, 94, Math.min(255, alphaBase + 32));
            texto = new Color(255, 230, 230, Math.min(255, alphaBase + 28));
        } else if ("GOL".equals(accion)) {
            fondo = new Color(ConfiguracionJuego.HUD_ACCENT.getRed() / 2, ConfiguracionJuego.HUD_ACCENT.getGreen() / 2, ConfiguracionJuego.HUD_ACCENT.getBlue() / 2, alphaBase);
            borde = new Color(118, 246, 172, Math.min(255, alphaBase + 32));
            texto = new Color(224, 255, 236, Math.min(255, alphaBase + 28));
        }

        g.setColor(fondo);
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(borde);
        g.drawRoundRect(x, y, w, h, 10, 10);
        g.setColor(texto);
        g.drawString(accion, x + (w - fm.stringWidth(accion)) / 2, y + 13);

        // Animacion de tarjeta si corresponde
        if ("AMARILLA".equals(accion) || "ROJA".equals(accion)) {
            dibujarAnimacionTarjeta(g, motor, arbitro, accion, motor.getFramesAccionArbitro());
            // dibujar icono de tarjeta al lado si cabe
            BufferedImage icon = GestorSprites.getInstancia().getIconTarjeta("ROJA".equals(accion));
            if (icon != null) {
                int ix = x - icon.getWidth() - 8;
                int iy = y - 4;
                if (ix < 6) ix = x + w - icon.getWidth() - 6;
                g.drawImage(icon, ix, iy, null);
            }
        }
    }

    private void dibujarAnimacionTarjeta(Graphics2D g, MotorJuego motor, Jugador arbitro, String accion, int framesRestantes) {
        int duracionEstim = (int) Math.round(ConfiguracionJuego.FPS * 1.6); // coincide con DURACION_ACCION_ARBITRO_LARGA_FRAMES
        double progreso = 1.0 - (framesRestantes / (double) Math.max(1, duracionEstim));
        progreso = Math.max(0.0, Math.min(1.0, progreso));
        // easing suave
        double ease = 1.0 - Math.pow(1.0 - progreso, 3.0);

        int cardW = 22;
        int cardH = 34;
        int baseX = arbitro.getX() + arbitro.getAncho() / 2;
        int startY = arbitro.getY() + arbitro.getAlto() / 2;
        int targetY = arbitro.getY() - 48;
        int cardX = baseX + 10;
        int cardY = (int) Math.round(startY + (targetY - startY) * ease);

        Color cardColor = "AMARILLA".equals(accion)
            ? new Color(255, 210, 32, 238)
            : new Color(220, 36, 36, 238);

        // Sombra
        g.setColor(new Color(0, 0, 0, Math.max(24, (int) (80 * progreso))));
        g.fillRoundRect(cardX + 4, cardY + 6, cardW, cardH, 6, 6);

        AffineTransform antes = g.getTransform();
        double rot = Math.toRadians((1.0 - ease) * 40.0 - 8.0);
        g.translate(cardX + cardW / 2.0, cardY + cardH / 2.0);
        g.rotate(rot);
        g.setColor(cardColor);
        g.fillRoundRect(-cardW / 2, -cardH / 2, cardW, cardH, 8, 8);
        g.setColor(new Color(255, 255, 255, Math.max(48, (int) (160 * progreso))));
        g.drawRoundRect(-cardW / 2, -cardH / 2, cardW, cardH, 8, 8);
        g.setTransform(antes);
    }

    private void dibujarParticulasJuego(Graphics2D g, MotorJuego motor) {
        int cxBalon = (int) Math.round(motor.getBalon().getCentroX());
        int cyBalon = (int) Math.round(motor.getBalon().getCentroY() - motor.getBalon().getAltura());
        double rapidez = motor.getBalon().getRapidez();
        if (rapidez > 1.6) {
            for (int i = 0; i < 10; i++) {
                double ang = (relojUI * 0.18) + i * (Math.PI * 2.0 / 10.0);
                int px = cxBalon + (int) Math.round(Math.cos(ang) * (8 + rapidez * 1.8));
                int py = cyBalon + (int) Math.round(Math.sin(ang) * (6 + rapidez));
                int a = Math.max(24, Math.min(120, (int) Math.round(rapidez * 24) - i * 6));
                g.setColor(new Color(255, 255, 255, a));
                g.fillOval(px, py, 3, 3);
            }
        }

        for (Jugador jugador : motor.getTodosJugadores()) {
            if (!jugador.tieneTurboActivo()) {
                continue;
            }
            int baseX = jugador.getX() + jugador.getAncho() / 2;
            int baseY = jugador.getY() + jugador.getAlto() - 6;
            for (int i = 0; i < 6; i++) {
                int fase = relojUI + i * 9;
                int px = baseX - jugador.getDireccionX() * (4 + i * 4) + (fase % 5) - 2;
                int py = baseY - Math.abs((fase % 7) - 3);
                g.setColor(new Color(78, 255, 196, 90 - i * 12));
                g.fillOval(px, py, 3, 3);
            }
        }
    }

    private void dibujarTarjetaGlass(Graphics2D g, int x, int y, int ancho, int alto, Color a, Color b) {
        g.setPaint(new GradientPaint(x, y, a, x + ancho, y + alto, b));
        g.fillRoundRect(x, y, ancho, alto, 18, 18);
        g.setColor(new Color(ConfiguracionJuego.HUD_TEXT.getRed(), ConfiguracionJuego.HUD_TEXT.getGreen(), ConfiguracionJuego.HUD_TEXT.getBlue(), 66));
        g.drawRoundRect(x, y, ancho, alto, 18, 18);
        g.setColor(new Color(ConfiguracionJuego.HUD_TEXT.getRed(), ConfiguracionJuego.HUD_TEXT.getGreen(), ConfiguracionJuego.HUD_TEXT.getBlue(), 28));
        g.fillRoundRect(x + 8, y + 8, ancho - 16, 16, 12, 12);
    }

    private void dibujarTarjetaMarcador(Graphics2D g, MotorJuego motor) {
        int x = 16;
        int y = 16;
        int ancho = 182;
        int alto = 64;
        // Usar paleta HUD para la tarjeta del marcador
        Color a = new Color(ConfiguracionJuego.HUD_PANEL.getRed(), ConfiguracionJuego.HUD_PANEL.getGreen(), ConfiguracionJuego.HUD_PANEL.getBlue(), 188);
        Color b = new Color(Math.max(0, ConfiguracionJuego.HUD_PANEL.getRed() - 8), Math.max(0, ConfiguracionJuego.HUD_PANEL.getGreen() - 12), Math.max(0, ConfiguracionJuego.HUD_PANEL.getBlue() - 6), 182);
        dibujarTarjetaGlass(g, x, y, ancho, alto, a, b);

        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(ConfiguracionJuego.HUD_ACCENT);
        g.drawString("MARCADOR", x + 12, y + 17);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(ConfiguracionJuego.HUD_TEXT);
        g.drawString(recortarTexto(motor.getNombreEquipoLocal(), 9), x + 12, y + 30);
        g.drawString(recortarTexto(motor.getNombreEquipoRival(), 9), x + 110, y + 30);

        g.setColor(ConfiguracionJuego.HUD_DANGER);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString(String.valueOf(motor.getGolesLocal()), x + 16, y + 58);

        g.setColor(ConfiguracionJuego.HUD_TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("-", x + 48, y + 56);

        g.setColor(ConfiguracionJuego.TEAM_RIVAL);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString(String.valueOf(motor.getGolesRival()), x + 80, y + 58);

        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.setColor(ConfiguracionJuego.HUD_ACCENT);
        g.drawString(motor.getAbreviaturaEquipoLocal(), x + 12, y + 43);
        g.setColor(ConfiguracionJuego.TEAM_RIVAL);
        g.drawString(motor.getAbreviaturaEquipoRival(), x + 110, y + 43);

        g.setColor(ConfiguracionJuego.HUD_TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString(motor.getTiempoPartidoTexto(), x + ancho - 50, y + 26);

        if (motor.isModoEspectador()) {
            g.setColor(ConfiguracionJuego.HUD_ACCENT);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.drawString("ESP", x + ancho - 30, y + 17);
        }
    }

    private void dibujarTarjetaEstado(Graphics2D g, MotorJuego motor) {
        int ancho = 214;
        int x = ConfiguracionJuego.ANCHO_PANEL - ancho - 18;
        int y = 16;
        int alto = 82;
        // Usar paleta HUD para la tarjeta de estado
        Color a = new Color(ConfiguracionJuego.HUD_PANEL.getRed(), ConfiguracionJuego.HUD_PANEL.getGreen(), ConfiguracionJuego.HUD_PANEL.getBlue(), 182);
        Color b = new Color(Math.max(0, ConfiguracionJuego.HUD_PANEL.getRed() - 6), Math.max(0, ConfiguracionJuego.HUD_PANEL.getGreen() - 8), Math.max(0, ConfiguracionJuego.HUD_PANEL.getBlue() - 10), 176);
        dibujarTarjetaGlass(g, x, y, ancho, alto, a, b);

        Jugador principal = motor.getJugadorPrincipal();
        String lineaNombre = recortarTexto(principal.getNombre(), 16) + "  #" + principal.getNumeroCamiseta();
        String lineaJuego = "Balon: " + recortarTexto(motor.getPoseedorTexto(), 16);
        String lineaEstado = recortarTexto(describirEstadoPartido(motor), 24);
        String lineaDisciplina = "Disciplina: " + resumirDisciplina(principal);

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(ConfiguracionJuego.HUD_TEXT);
        g.drawString(motor.isModoEspectador() ? "MODO ESPECTADOR" : "JUGADOR ACTIVO", x + 12, y + 18);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(ConfiguracionJuego.HUD_TEXT);
        g.drawString(lineaNombre, x + 12, y + 34);
        // Icono de bota si turbo activo
        BufferedImage iconB = sprites.getIconBota();
        if (iconB != null && principal.tieneTurboActivo()) {
            int ix = x + ancho - iconB.getWidth() - 12;
            int iy = y + 10;
            g.drawImage(iconB, ix, iy, null);
        }
        g.drawString(lineaJuego, x + 12, y + 49);
        g.drawString(lineaEstado, x + 12, y + 64);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(ConfiguracionJuego.HUD_ACCENT);
        g.drawString(lineaDisciplina, x + 12, y + 74);

        int barraX = x + 138;
        int barraY = y + 20;
        int barraW = ancho - 150;
        int barraH = 7;
        double energia = motor.getStaminaPrincipalPorcentaje() / 100.0;
        int relleno = (int) Math.round(barraW * energia);
        Color colorEnergia = energia > 0.55 ? ConfiguracionJuego.HUD_ACCENT : (energia > 0.25 ? ConfiguracionJuego.HUD_WARN : ConfiguracionJuego.HUD_DANGER);

        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.setColor(ConfiguracionJuego.HUD_ACCENT);
        g.drawString("ENERGIA", x + 138, y + 16);
        g.setColor(ConfiguracionJuego.HUD_TEXT);
        g.drawString(motor.getStaminaPrincipalPorcentaje() + "%", x + ancho - 34, y + 16);

        g.setColor(new Color(0, 0, 0, 190));
        g.fillRoundRect(barraX, barraY, barraW, barraH, 8, 8);
        g.setColor(colorEnergia);
        g.fillRoundRect(barraX, barraY, Math.max(3, relleno), barraH, 8, 8);
        g.setColor(new Color(ConfiguracionJuego.HUD_TEXT.getRed(), ConfiguracionJuego.HUD_TEXT.getGreen(), ConfiguracionJuego.HUD_TEXT.getBlue(), 90));
        g.drawRoundRect(barraX, barraY, barraW, barraH, 8, 8);

        // Dibujar icono de hidratacion cerca de la barra
        BufferedImage iconH = sprites.getIconHidratacion();
        if (iconH != null) {
            int ix = barraX - iconH.getWidth() - 8;
            int iy = barraY - ((iconH.getHeight() - barraH) / 2);
            if (ix < x + 12) ix = x + 12;
            g.drawImage(iconH, ix, iy, null);
        }

    }

    private String describirEstadoFisico(MotorJuego motor) {
        int energia = motor.getStaminaPrincipalPorcentaje();
        if (energia >= 75) {
            return "Ritmo alto";
        }
        if (energia >= 45) {
            return "Ritmo medio";
        }
        if (energia >= 25) {
            return "Cansado";
        }
        return "Al limite";
    }

    private String describirEstadoPartido(MotorJuego motor) {
        String arbitro = motor.getEstadoArbitrajeTexto();
        if (arbitro != null && !arbitro.isEmpty()) {
            return "Arbitro " + arbitro;
        }
        String saque = motor.getTextoSaque();
        if (saque != null && !saque.isEmpty()) {
            return recortarTexto(saque, 16);
        }
        return "Juego abierto";
    }

    private String resumirDisciplina(Jugador jugador) {
        return jugador.getTarjetasAmarillas() + "A/"
            + (jugador.tieneTarjetaRoja() ? "1R" : "0R")
            + " | F" + jugador.getFaltasCometidas();
    }

    private void dibujarNarracionEnPantalla(Graphics2D g, MotorJuego motor) {
        String narracion = motor.getNarracionActual();
        String textoSaque = motor.getTextoSaque();
        boolean tieneNarracion = narracion != null && !narracion.isEmpty() && motor.getFramesNarracion() > 0;
        boolean tieneEvento = textoSaque != null && !textoSaque.isEmpty() && motor.getFramesTextoSaque() > 0;
        if (!tieneNarracion && !tieneEvento) {
            return;
        }
        int ancho = 420;
        int alto = tieneNarracion && tieneEvento ? 56 : 44;
        int x = ConfiguracionJuego.ANCHO_PANEL / 2 - ancho / 2;
        int y = ConfiguracionJuego.ALTO_PANEL - alto - 22;
        int alphaPanel = tieneNarracion ? 140 : 120;

        // Panel más ligero y compacto para no tapar la acción.
        g.setColor(new Color(8, 12, 18, alphaPanel));
        g.fillRoundRect(x, y, ancho, alto, 14, 14);
        g.setColor(new Color(255, 255, 255, 48));
        g.drawRoundRect(x, y, ancho, alto, 14, 14);

        int brilloX = x + (int) ((relojUI * 6L) % Math.max(1, ancho + 120)) - 120;
        g.setColor(new Color(255, 255, 255, 18));
        g.fillRoundRect(brilloX, y + 6, 92, alto - 12, 12, 12);

        int cursorY = y + 18;
        if (tieneEvento) {
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(255, 208, 112));
            g.drawString("EVENTO", x + 16, cursorY);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.setColor(new Color(255, 244, 214));
            g.drawString(recortarTexto(textoSaque, 48), x + 84, cursorY + 1);
            cursorY += 28;
        }
        if (tieneNarracion) {
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(118, 214, 255));
            g.drawString("NARRADOR", x + 16, cursorY);
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.setColor(new Color(234, 246, 255));
            g.drawString(recortarTexto(narracion, 52), x + 98, cursorY + 1);
        }
    }

    private void dibujarConfetti(Graphics2D g, int cx, int cy, int cantidad) {
        Random rnd = new Random(relojUI * 31L + 7);
        for (int i = 0; i < cantidad; i++) {
            int dx = (int) Math.round(Math.sin((relojUI + i * 7) * 0.07) * 160 + (rnd.nextDouble() - 0.5) * 60);
            int dy = (int) Math.round(((relojUI + i * 5) % 120) - 40 + rnd.nextInt(40));
            int x = cx + dx;
            int y = cy + dy - 28;
            int w = 6 + rnd.nextInt(6);
            int h = 6 + rnd.nextInt(6);
                Color[] paleta = new Color[] {
                    new Color(ConfiguracionJuego.HUD_ACCENT.getRed(), ConfiguracionJuego.HUD_ACCENT.getGreen(), ConfiguracionJuego.HUD_ACCENT.getBlue(), 200),
                    new Color(118, 214, 255, 200),
                    new Color(ConfiguracionJuego.HUD_WARN.getRed(), ConfiguracionJuego.HUD_WARN.getGreen(), ConfiguracionJuego.HUD_WARN.getBlue(), 200),
                    new Color(140, 255, 180, 200)
                };
                Color c = paleta[rnd.nextInt(paleta.length)];
            g.setColor(c);
            g.fillOval(x, y, w, h);
        }
    }

    private void dibujarBarraCargaAccion(Graphics2D g, EntradaJuego entrada, boolean modoEspectador) {
        if (modoEspectador) {
            return;
        }
        if (entrada == null || (!entrada.estaCargandoPase() && !entrada.estaCargandoTiro())) {
            return;
        }

        int ancho = 172;
        int x = ConfiguracionJuego.ANCHO_PANEL - ancho - 18;
        int y = 76;
        int alto = 14;
        double factor = entrada.getFactorCargaActiva();
        double progreso = (factor - 0.35) / (1.00 - 0.35);
        progreso = Math.max(0.0, Math.min(1.0, progreso));
        int anchoRelleno = (int) Math.round(ancho * progreso);

        // Evitar solapamiento con la tarjeta de estado en la esquina superior derecha.
        java.awt.Rectangle rectBarra = new java.awt.Rectangle(x, y, ancho, alto);
        java.awt.Rectangle rectTarjetaEstado = new java.awt.Rectangle(ConfiguracionJuego.ANCHO_PANEL - 214 - 18, 16, 214, 82);
        if (rectBarra.intersects(rectTarjetaEstado)) {
            y = rectTarjetaEstado.y + rectTarjetaEstado.height + 8; // desplazar justo debajo
        }

        g.setColor(new Color(20, 20, 20, 190));
        g.fillRoundRect(x, y, ancho, alto, 10, 10);
        g.setColor(new Color(255, 255, 255, 70));
        g.drawRoundRect(x, y, ancho, alto, 10, 10);

        Color color = entrada.estaCargandoTiro() ? new Color(255, 116, 72) : new Color(72, 190, 255);
        g.setPaint(new GradientPaint(x, y, color.brighter(), x, y + alto, color.darker()));
        g.fillRoundRect(x + 1, y + 1, Math.max(2, anchoRelleno - 2), alto - 2, 8, 8);

        int brilloX = x + (int) ((relojUI * 4L) % Math.max(1, ancho));
        g.setColor(new Color(255, 255, 255, 55));
        g.drawLine(brilloX, y + 2, brilloX + 10, y + alto - 2);

        g.setColor(new Color(245, 245, 245));
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString(entrada.getEtiquetaCargaActiva() + " " + (int) Math.round(progreso * 100) + "%", x + 4, y - 3);
    }

    private String recortarTexto(String texto, int maxChars) {
        if (texto == null || texto.length() <= maxChars) {
            return texto == null ? "" : texto;
        }
        return texto.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private void dibujarEtiquetaJugador(Graphics2D g, Jugador jugador, Color colorTexto, Color colorFondo) {
        String nombre = jugador.getNombre();
        FontMetrics metrics = g.getFontMetrics();
        int anchoTexto = metrics.stringWidth(nombre);
        int x = jugador.getX() + jugador.getAncho() / 2 - anchoTexto / 2;
        int y = jugador.getY() - 10;
        if (y < 96) {
            y = jugador.getY() + jugador.getAlto() + 14;
        }

        g.setColor(colorFondo);
        g.fillRoundRect(x - 7, y - 12, anchoTexto + 14, 17, 9, 9);
        g.setColor(new Color(255, 255, 255, 42));
        g.drawRoundRect(x - 7, y - 12, anchoTexto + 14, 17, 9, 9);
        g.setColor(colorTexto);
        g.drawString(nombre, x, y);
    }

    private void dibujarAvisoHUD(Graphics2D g, String texto, int x, int y) {
        g.setColor(new Color(255, 219, 94));
        g.drawString(texto, x, y);
    }

    private void dibujarAnimacionSorteoMoneda(Graphics2D g, MotorJuego motor) {
        if (!motor.isSorteoMonedaActivo()) {
            return;
        }

        int cajaAncho = 500;
        int cajaAlto = 246;
        int xCaja = ConfiguracionJuego.ANCHO_PANEL / 2 - cajaAncho / 2;
        int yCaja = 78;

        g.drawImage(sprites.getPanelEvento(), xCaja, yCaja, cajaAncho, cajaAlto, null);
        dibujarEfectosCeremoniaInicio(g, xCaja, yCaja, cajaAncho, cajaAlto, motor);

        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.setColor(new Color(250, 237, 205));
        g.drawString("Saque inicial - sorteo con moneda", xCaja + 64, yCaja + 34);

        int frame = motor.getFramesAnimacionMoneda();
        boolean revelado = motor.isGanadorSorteoRevelado();
        double giro = revelado ? (motor.isMonedaFueCara() ? 1.0 : -1.0) : Math.sin(frame * 0.42);
        int monedaH = 86;
        int monedaW = Math.max(8, (int) Math.round(86 * Math.abs(giro)));
        int monedaX = ConfiguracionJuego.ANCHO_PANEL / 2 - monedaW / 2;
        int monedaY = yCaja + 56;
        boolean ladoCara = revelado ? motor.isMonedaFueCara() : giro >= 0;

        g.setColor(new Color(255, 220, 120, 44));
        g.fillOval(monedaX - 12, monedaY - 12, monedaW + 24, monedaH + 24);
        g.setColor(ladoCara ? new Color(250, 208, 84) : new Color(214, 220, 232));
        g.fillOval(monedaX, monedaY, monedaW, monedaH);
        g.setColor(ladoCara ? new Color(194, 130, 42) : new Color(132, 146, 166));
        g.drawOval(monedaX, monedaY, monedaW, monedaH);
        g.setColor(new Color(255, 255, 255, 114));
        g.drawOval(monedaX + 2, monedaY + 2, Math.max(2, monedaW - 4), monedaH - 4);
        if (monedaW > 26) {
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.setColor(new Color(30, 30, 30, 180));
            g.drawString(ladoCara ? "C" : "X", ConfiguracionJuego.ANCHO_PANEL / 2 - 10, monedaY + 54);
        }

        int segundos = (int) Math.ceil(motor.getFramesSorteoMoneda() / (double) ConfiguracionJuego.FPS);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(245, 245, 245));
        String estado = motor.isCeremoniaInicioActiva()
            ? "Equipos entrando a la cancha..."
            : (revelado ? "Resultado confirmado por el arbitro" : "El arbitro lanza la moneda... " + segundos + "s");
        g.drawString(estado, xCaja + 84, yCaja + 160);

        int barraX = xCaja + 62;
        int barraY = yCaja + 174;
        int barraW = cajaAncho - 124;
        int barraH = 10;
        double progreso = motor.isCeremoniaInicioActiva()
            ? progresoEntrada(Math.max(1, ConfiguracionJuego.FPS))
            : 1.0 - Math.max(0.0, Math.min(1.0, motor.getFramesSorteoMoneda() / (double) (ConfiguracionJuego.FPS * 2)));
        g.setColor(new Color(18, 18, 18, 180));
        g.fillRoundRect(barraX, barraY, barraW, barraH, 8, 8);
        g.setColor(new Color(255, 196, 92));
        g.fillRoundRect(barraX, barraY, Math.max(3, (int) Math.round(barraW * progreso)), barraH, 8, 8);

        String resultado = motor.getResultadoMoneda();
        if (revelado && resultado != null && !resultado.isEmpty()) {
            String lado = motor.isMonedaFueCara() ? "CARA" : "CRUZ";
            String saque = motor.isPrimerSaqueLocal() ? "SACA LOCAL" : "SACA RIVAL";
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            g.setColor(new Color(255, 236, 142));
            g.drawString(lado, xCaja + 72, yCaja + 204);
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.setColor(new Color(245, 245, 245));
            g.drawString(saque, xCaja + 220, yCaja + 204);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(255, 230, 168));
            g.drawString(resultado, xCaja + 48, yCaja + 228);
        } else if (motor.isCeremoniaInicioActiva()) {
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.setColor(new Color(255, 228, 128));
            g.drawString("Preparando el sorteo y el saque...", xCaja + 108, yCaja + 206);
        }
    }

    private void dibujarEfectosCeremoniaInicio(Graphics2D g, int xCaja, int yCaja, int cajaAncho, int cajaAlto, MotorJuego motor) {
        if (!motor.isCeremoniaInicioActiva()) {
            return;
        }
        int cx = xCaja + cajaAncho / 2;
        int top = yCaja + 12;
        g.setColor(new Color(255, 214, 120, 22));
        int[] xsIzq = { xCaja + 46, xCaja + 136, cx - 14 };
        int[] ysIzq = { top, yCaja + cajaAlto - 34, yCaja + cajaAlto - 34 };
        g.fillPolygon(xsIzq, ysIzq, 3);
        g.setColor(new Color(118, 214, 255, 18));
        int[] xsDer = { xCaja + cajaAncho - 46, xCaja + cajaAncho - 136, cx + 14 };
        int[] ysDer = { top, yCaja + cajaAlto - 34, yCaja + cajaAlto - 34 };
        g.fillPolygon(xsDer, ysDer, 3);
        for (int i = 0; i < 18; i++) {
            int px = xCaja + 30 + (i * 23) % (cajaAncho - 60);
            int py = yCaja + 24 + (int) ((relojUI * 1.8 + i * 11) % 52);
            g.setColor(new Color(255 - (i % 3) * 20, 182 + (i % 4) * 12, 88 + (i % 5) * 18, 92));
            g.fillOval(px, py, 4 + i % 3, 4 + i % 3);
        }
        int flash = (int) Math.round((Math.sin(relojUI * 0.24) + 1.0) * 18.0);
        g.setColor(new Color(255, 255, 255, 24 + flash));
        g.drawOval(cx - 72, yCaja + 66, 144, 78);
        g.drawOval(cx - 92, yCaja + 56, 184, 98);
    }

    private void dibujarOverlayEstado(Graphics2D g, EstadoJuego estado, String mensaje, MotorJuego motor, boolean inicioModoEspectador) {
        if (estado == EstadoJuego.JUGANDO) {
            return;
        }
        if (estado == EstadoJuego.INICIO) {
            dibujarPantallaInicio(g, inicioModoEspectador);
            return;
        }

        if (estado == EstadoJuego.PAUSADO) {
            dibujarPantallaPausa(g, motor);
            return;
        }
        if (estado == EstadoJuego.GOL || estado == EstadoJuego.FALTA) {
            dibujarPantallaEventoTemporal(g, estado, mensaje, motor);
            return;
        }
        dibujarPantallaFinal(g, estado, motor);
    }

    private void dibujarFondoOverlay(Graphics2D g, int alphaBase) {
        g.setColor(new Color(0, 0, 0, Math.max(0, Math.min(220, alphaBase))));
        g.fillRect(0, 0, ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL);

        int particulas = 24;
        for (int i = 0; i < particulas; i++) {
            int semilla = i * 73;
            int x = Math.abs((relojUI * (i + 2) + semilla) % ConfiguracionJuego.ANCHO_PANEL);
            int y = Math.abs(((relojUI * (i + 1) / 2) + semilla * 11) % ConfiguracionJuego.ALTO_PANEL);
            int radio = 2 + (i % 4);
            g.setColor(new Color(255, 255, 255, 16 + (i % 5) * 4));
            g.fillOval(x, y, radio, radio);
        }
    }

    private void dibujarTarjetaCentralAnimada(Graphics2D g, int cx, int cy, int baseW, int baseH, Color cA, Color cB) {
        double entrada = progresoEntrada(22);
        double escala = 0.88 + entrada * 0.12;
        int w = (int) Math.round(baseW * escala);
        int h = (int) Math.round(baseH * escala);
        int x = cx - w / 2;
        int y = cy - h / 2 - (int) Math.round((1.0 - entrada) * 18.0);

        g.setPaint(new GradientPaint(x, y, cA, x + w, y + h, cB));
        g.fillRoundRect(x, y, w, h, 24, 24);
        g.setColor(new Color(255, 255, 255, 78));
        g.drawRoundRect(x, y, w, h, 24, 24);

        int brilloX = x + (int) ((relojUI * 6L) % Math.max(1, w + 140)) - 140;
        g.setColor(new Color(255, 255, 255, 36));
        g.fillRoundRect(brilloX, y + 8, 120, h - 16, 20, 20);
    }

    private void dibujarPantallaPausa(Graphics2D g, MotorJuego motor) {
        dibujarFondoOverlay(g, 182);
        int cx = ConfiguracionJuego.ANCHO_PANEL / 2;
        int cy = ConfiguracionJuego.ALTO_PANEL / 2;
        dibujarTarjetaCentralAnimada(g, cx, cy, 520, 206, new Color(70, 126, 196), new Color(40, 82, 142));

        g.setColor(new Color(250, 250, 250));
        g.setFont(new Font("SansSerif", Font.BOLD, 40));
        g.drawString("PAUSA", cx - 90, cy - 34);

        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString("La reta sigue pendiente", cx - 140, cy + 4);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        String detalle = motor.isModoEspectador() ? "P continuar | F2 jugador | R reiniciar" : "P continuar | F2 espectador | R reiniciar";
        g.drawString(detalle, cx - 168, cy + 42);
    }

    private void dibujarPantallaEventoTemporal(Graphics2D g, EstadoJuego estado, String mensaje, MotorJuego motor) {
        // Fondo más tenue para que el jugador y la cancha sigan visibles.
        dibujarFondoOverlay(g, 120);
        int cx = ConfiguracionJuego.ANCHO_PANEL / 2;
        int cy = ConfiguracionJuego.ALTO_PANEL / 2;
        Jugador goleador = estado == EstadoJuego.GOL ? motor.getUltimoJugadorGoleador() : null;
        boolean mostrarFichaGoleador = estado == EstadoJuego.GOL && goleador != null;
        int tarjetaW = mostrarFichaGoleador ? 620 : 480;

        Color cA = estado == EstadoJuego.GOL ? new Color(255, 213, 95) : new Color(255, 166, 88);
        Color cB = estado == EstadoJuego.GOL ? new Color(236, 162, 62) : new Color(210, 118, 64);
        g.drawImage(sprites.getPanelEvento(), cx - tarjetaW / 2, cy - 96, tarjetaW, 192, null);

        dibujarRafagaEvento(g, cx, cy, estado == EstadoJuego.GOL ? new Color(255, 246, 198, 72) : new Color(255, 220, 180, 64));

        if (estado == EstadoJuego.GOL) {
            dibujarConfetti(g, cx, cy - 48, 28);
        }

        int tarjetaX = cx - tarjetaW / 2;
        int textoX = tarjetaX + 24;
        g.setColor(new Color(30, 26, 18));
        g.setFont(new Font("SansSerif", Font.BOLD, 40));
        g.drawString(estado == EstadoJuego.GOL ? "GOL" : "FALTA", textoX, cy - 34);

        String subtitulo;
        if (estado == EstadoJuego.GOL) {
            subtitulo = motor.getResumenUltimoGol().isEmpty()
                ? (mensaje == null || mensaje.isEmpty() ? "Se reinicia la jugada" : mensaje)
                : (mensaje == null || mensaje.isEmpty() ? "Gol" : mensaje) + " de " + motor.getUltimoGoleador();
        } else {
            subtitulo = mensaje == null || mensaje.isEmpty() ? "Reanudacion del juego" : mensaje;
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString(recortarTexto(subtitulo, mostrarFichaGoleador ? 28 : 34), textoX, cy + 2);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("Marcador: " + motor.getGolesLocal() + " - " + motor.getGolesRival(), textoX, cy + 36);

        if (mostrarFichaGoleador) {
            dibujarFichaGoleador(g, tarjetaX + tarjetaW - 242, cy - 82, 200, 156, goleador, motor.isUltimoGolAutogol());

            // Dibuja mini-figuras de compañeros celebrando a la izquierda de la tarjeta.
            Jugador[] locales = motor.getLocales();
            Jugador[] rivales = motor.getRivales();
            Jugador[] equipo = null;
            for (Jugador j : locales) { if (j == goleador) { equipo = locales; break; } }
            if (equipo == null) {
                for (Jugador j : rivales) { if (j == goleador) { equipo = rivales; break; } }
            }
            if (equipo != null) {
                int miniW = 72;
                int miniH = 68;
                int startX = tarjetaX + 18;
                int startY = cy - 88;
                int drawn = 0;
                for (int i = 0; i < equipo.length && drawn < 3; i++) {
                    Jugador j = equipo[i];
                    if (j == null || j == goleador) continue;
                    dibujarMiniJugadorEvento(g, startX + drawn * (miniW + 8), startY, miniW, miniH, j, true);
                    drawn++;
                }
            }
        }
    }

    private void dibujarFichaGoleador(Graphics2D g, int x, int y, int w, int h, Jugador goleador, boolean autogol) {
        g.setColor(new Color(16, 18, 24, 108));
        g.fillRoundRect(x + 4, y + 5, w, h, 20, 20);
        g.setPaint(new GradientPaint(x, y, new Color(255, 255, 255, 92), x, y + h, new Color(255, 241, 208, 36)));
        g.fillRoundRect(x, y, w, h, 20, 20);
        g.setColor(new Color(255, 255, 255, 110));
        g.drawRoundRect(x, y, w, h, 20, 20);

        g.setColor(new Color(44, 34, 18, 190));
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.drawString(autogol ? "AUTOR" : "GOLEADOR", x + 16, y + 24);

        g.setColor(new Color(255, 255, 255, 34));
        g.fillRoundRect(x + 16, y + 34, w - 32, 96, 16, 16);
        dibujarMiniJugadorEvento(g, x + 52, y + 28, 124, 118, goleador, !autogol);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(28, 24, 20));
        g.drawString(recortarTexto(goleador.getNombre(), 14), x + 14, y + h - 24);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(74, 56, 28));
        g.drawString(autogol ? "La toco en contra" : "Salio a festejarlo", x + 14, y + h - 8);
    }

    private void dibujarMiniJugadorEvento(Graphics2D g, int x, int y, int w, int h, Jugador jugador, boolean celebrar) {
        jugador.dibujarFiguraEvento(g, x, y, w, h, celebrar);
    }

    private void dibujarRafagaEvento(Graphics2D g, int cx, int cy, Color color) {
        g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 16; i++) {
            double angulo = (Math.PI * 2.0 * i / 16.0) + relojUI * 0.02;
            int x1 = cx + (int) Math.round(Math.cos(angulo) * 120.0);
            int y1 = cy + (int) Math.round(Math.sin(angulo) * 76.0);
            int x2 = cx + (int) Math.round(Math.cos(angulo) * 164.0);
            int y2 = cy + (int) Math.round(Math.sin(angulo) * 102.0);
            g.setColor(color);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private void dibujarPantallaFinal(Graphics2D g, EstadoJuego estado, MotorJuego motor) {
        dibujarFondoOverlay(g, 190);
        int cx = ConfiguracionJuego.ANCHO_PANEL / 2;
        int cy = ConfiguracionJuego.ALTO_PANEL / 2;

        String titulo;
        String subtitulo;
        Color cA;
        Color cB;
        if (estado == EstadoJuego.VICTORIA) {
            titulo = "GANASTE LA RETA";
            subtitulo = "Marcador final: " + motor.getGolesLocal() + " - " + motor.getGolesRival();
            cA = new Color(120, 228, 140);
            cB = new Color(64, 170, 94);
        } else if (estado == EstadoJuego.EMPATE) {
            titulo = "EMPATE";
            subtitulo = "Tiempo cumplido: " + motor.getGolesLocal() + " - " + motor.getGolesRival();
            cA = new Color(172, 202, 250);
            cB = new Color(112, 146, 220);
        } else {
            titulo = "DERROTA";
            subtitulo = "Te ganaron la reta: " + motor.getGolesLocal() + " - " + motor.getGolesRival();
            cA = new Color(255, 166, 130);
            cB = new Color(214, 98, 82);
        }

        g.drawImage(sprites.getPanelEvento(), cx - 280, cy - 112, 560, 224, null);

        g.setColor(new Color(28, 24, 20));
        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        g.drawString(titulo, cx - 190, cy - 36);
        g.setFont(new Font("SansSerif", Font.PLAIN, 21));
        g.drawString(subtitulo, cx - 202, cy + 6);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("R para nuevo partido", cx - 102, cy + 46);
    }

    private void dibujarPantallaInicio(Graphics2D g, boolean inicioModoEspectador) {
        double entrada = progresoEntrada(34);
        // Fondo dinámico más vivo para una entrada más llamativa
        float hue = (float) (0.55 + Math.sin(relojUI * 0.012) * 0.06);
        Color bgA = Color.getHSBColor(hue, 0.46f, 0.12f);
        Color bgB = Color.getHSBColor((float) (hue + 0.08f), 0.82f, 0.28f);
        g.setPaint(new java.awt.GradientPaint(0, 0, bgA, ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL, bgB));
        g.fillRect(0, 0, ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL);
        dibujarFondoOverlay(g, alpha(145 + entrada * 18.0));

        int xBase = ConfiguracionJuego.ANCHO_PANEL / 2 - 360;
        int yBase = 84 + (int) Math.round(Math.sin(relojUI * 0.04) * 4.0);
        int ancho = 720;
        int alto = 486;
        int margen = 24;
        int panelX = xBase + margen;
        int panelY = yBase + 164;
        int panelW = ancho - margen * 2;
        int panelH = 252;
        int botonY = yBase + alto - 72;

        g.drawImage(sprites.getPanelMenu(), xBase, yBase, ancho, alto, null);

        // Logo grande animado encima del menu
        dibujarLogoInicio(g, ConfiguracionJuego.ANCHO_PANEL / 2, yBase + 64);

        dibujarParticulasMenu(g, xBase, yBase, ancho, alto);
        dibujarDecoracionBienvenida(g, xBase, yBase, ancho, alto);

        g.setColor(new Color(255, 227, 148));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("LA CANCHITA", xBase + 24, yBase + 36);

        int flotacionTitulo = (int) Math.round(Math.sin(relojUI * 0.06) * 3.0);
        g.setColor(new Color(12, 12, 12, 120));
        g.setFont(new Font("SansSerif", Font.BOLD, 44));
        g.drawString("Futbol Urbano", xBase + 26, yBase + 92 + flotacionTitulo);
        g.setColor(new Color(245, 245, 245));
        g.drawString("Futbol Urbano", xBase + 22, yBase + 88 + flotacionTitulo);

        g.setColor(new Color(218, 234, 242));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("Liga MX callejera | 5 vs 5", xBase + 24, yBase + 120);

        dibujarCintasInicio(g, xBase + 26, yBase + 132, ancho - 52);

        g.setColor(new Color(255, 255, 255, 35));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 18, 18);
        g.setColor(new Color(255, 255, 255, 66));
        g.drawRoundRect(panelX, panelY, panelW, panelH, 18, 18);

        dibujarControlesAnimados(g, panelX, panelY, panelW, panelH, inicioModoEspectador);
        dibujarBotonInicioAnimado(g, panelX, botonY, panelW, 52);

        // Indicador e interfaz de musica (clickable)
        boolean musicaOn = MusicaInicio.getInstancia().isPlaying() && !MusicaInicio.getInstancia().isMuted();
        int iconX = MUSIC_ICON_X;
        int iconY = MUSIC_ICON_Y;
        int bx = iconX + MUSIC_ICON_PAD_X;
        int by = iconY + MUSIC_ICON_PAD_Y;
        g.setColor(new Color(0, 0, 0, 96));
        g.fillRoundRect(bx, by, MUSIC_ICON_W, MUSIC_ICON_H, 10, 10);

        // Dibuja icono de altavoz
        int sx = bx + 8;
        int sy = by + (MUSIC_ICON_H - 14) / 2;
        int sw = 12;
        int sh = 14;
        g.setColor(new Color(245, 245, 245, 220));
        g.fillRect(sx, sy + 4, 4, 6);
        int[] px = { sx + 4, sx + 4 + 8, sx + 4 };
        int[] py = { sy, sy + 7, sy + 14 };
        g.fillPolygon(px, py, 3);

        // Barras animadas (indicador de reproducción)
        int barsX = sx + sw + 8;
        int barsY = by + (MUSIC_ICON_H - 12) / 2;
        int barsW = 6;
        int gap = 4;
        for (int i = 0; i < 4; i++) {
            double fase = relojUI * 0.08 + i * 0.9;
            int h = 4 + (int) (Math.abs(Math.sin(fase)) * 12.0 * (musicaOn ? 1.0 : 0.28));
            int byBar = barsY + (12 - h);
            g.setColor(musicaOn ? new Color(88, 220, 120) : new Color(206, 206, 206, 160));
            g.fillRoundRect(barsX + i * (barsW + gap), byBar, barsW, h, 3, 3);
        }

        // Texto indicador
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(musicaOn ? new Color(88, 220, 120) : new Color(206, 206, 206));
        String textoMus = "M - Música: " + (musicaOn ? "ON" : "OFF");
        g.drawString(textoMus, bx + 56, by + 16);

        // Slider de volumen dentro del cuadro de control (fondo, porción llena y knob)
        java.awt.Rectangle sliderRect = getMusicSliderRect();
        MusicaInicio mi = MusicaInicio.getInstancia();
        double vol = mi.getVolumen();
        int slx = sliderRect.x;
        int sly = sliderRect.y;
        int srw = sliderRect.width;
        int srh = sliderRect.height;
        // Fondo de la barra con suave gradiente
        java.awt.GradientPaint fondo = new java.awt.GradientPaint(slx, sly, new Color(34, 36, 40, 220), slx + srw, sly, new Color(28, 30, 34, 220));
        g.setPaint(fondo);
        g.fillRoundRect(slx, sly, srw, srh, srh, srh);
        // Porción llena con gradiente
        int filled = (int) Math.round(srw * Math.max(0.0, Math.min(1.0, vol)));
        java.awt.GradientPaint relleno = new java.awt.GradientPaint(slx, sly, new Color(102, 230, 142), slx + Math.max(8, filled), sly, new Color(64, 196, 110));
        g.setPaint(relleno);
        g.fillRoundRect(slx, sly, Math.max(4, filled), srh, srh, srh);
        // Knob (centrado en el extremo de la porción llena)
        int knobCenterX = slx + Math.max(3, Math.min(srw - 3, filled));
        int knobW = srh + 8;
        int knobH = srh + 8;
        int knobX = knobCenterX - knobW / 2;
        int knobY = sly - (knobH - srh) / 2;
        // Sombra del knob
        g.setColor(new Color(0, 0, 0, 96));
        g.fillOval(knobX + 1, knobY + 2, knobW, knobH);
        // Cuerpo exterior del knob
        Color knobOuter = sliderActive ? new Color(18, 18, 22) : new Color(28, 30, 34);
        g.setColor(knobOuter);
        g.fillOval(knobX, knobY, knobW, knobH);
        // Brillo interior
        java.awt.GradientPaint knobGrad = new java.awt.GradientPaint(knobX, knobY, new Color(255, 255, 255, 220), knobX, knobY + knobH, new Color(220, 220, 220, 48));
        g.setPaint(knobGrad);
        g.fillOval(knobX + 2, knobY + 2, knobW - 4, knobH - 4);
        // Borde sutil
        g.setColor(new Color(0, 0, 0, 120));
        g.setStroke(new BasicStroke(1f));
        g.drawOval(knobX, knobY, knobW, knobH);
        // Glow si hover/active
        if (sliderHover || sliderActive) {
            g.setColor(new Color(88, 220, 120, 48));
            for (int i = 0; i < 3; i++) {
                int pad = 3 + i * 3;
                g.drawOval(knobX - pad, knobY - pad, knobW + pad * 2, knobH + pad * 2);
            }
        }
        // Texto porcentaje a la derecha del slider
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(220, 220, 220, 200));
        String pct = Math.max(0, Math.min(100, (int) Math.round(vol * 100))) + "%";
        g.drawString(pct, slx + srw + 8, sly + srh);
    }

    private void dibujarLogoInicio(Graphics2D g, int cx, int cy) {
        int size = 150;
        AffineTransform prev = g.getTransform();
        g.translate(cx, cy);
        double rot = Math.sin(relojUI * 0.018) * 0.06;
        g.rotate(rot);

        int half = size / 2;
        Color a = new Color(255, 198, 88);
        Color b = new Color(255, 110, 72);
        g.setPaint(new GradientPaint(-half, -half, a, half, half, b));
        g.fillOval(-half, -half, size, size);
        g.setColor(new Color(24, 24, 30, 180));
        g.drawOval(-half, -half, size, size);

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(24, 24, 30));
        String title = "LA CANCHITA";
        java.awt.FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(title);
        g.drawString(title, -tw / 2, 6);

        g.setTransform(prev);
    }

    private void dibujarDecoracionBienvenida(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(255, 188, 88, 26));
        g.fillOval(x - 48, y + 18, 210, 210);
        g.setColor(new Color(88, 210, 255, 22));
        g.fillOval(x + w - 164, y + 26, 176, 176);

        dibujarFocoInicio(g, x + 88, y + 18, 138, h - 36, new Color(255, 214, 122, 24));
        dibujarFocoInicio(g, x + w - 124, y + 14, -128, h - 36, new Color(114, 214, 255, 20));

        dibujarBalonDecorativoInicio(g, x + w - 150, y + 66, 74);
        dibujarJugadorDecorativoInicio(g, x + 54, y + h - 154, true, new Color(32, 132, 218), new Color(244, 244, 244), new Color(18, 58, 132));
        dibujarJugadorDecorativoInicio(g, x + w - 164, y + h - 154, false, new Color(38, 162, 92), new Color(244, 244, 244), new Color(190, 48, 56));

        dibujarStickerInicio(g, x + 28, y + 54, 124, 32, "BARRIO", new Color(255, 208, 104), new Color(68, 42, 12));
        dibujarStickerInicio(g, x + w - 186, y + 182, 158, 32, "FUTBOL CALLEJERO", new Color(118, 220, 255), new Color(16, 52, 78));
    }

    private void dibujarFocoInicio(Graphics2D g, int x, int y, int apertura, int alto, Color color) {
        int[] xs = { x, x + apertura, x + apertura / 2 };
        int[] ys = { y, y + alto, y + alto };
        g.setColor(color);
        g.fillPolygon(xs, ys, 3);
    }

    private void dibujarBalonDecorativoInicio(Graphics2D g, int x, int y, int tam) {
        g.setColor(new Color(0, 0, 0, 56));
        g.fillOval(x + 10, y + tam - 6, tam - 16, 12);
        g.setPaint(new GradientPaint(x, y, new Color(255, 255, 255, 240), x + tam, y + tam, new Color(188, 194, 204, 220)));
        g.fillOval(x, y, tam, tam);
        g.setColor(new Color(18, 20, 24, 220));
        g.drawOval(x, y, tam, tam);
        int centroX = x + tam / 2;
        int centroY = y + tam / 2;
        int panel = tam / 4;
        g.fillOval(centroX - panel / 2, centroY - panel / 2, panel, panel);
        g.drawLine(centroX, y + 8, centroX, centroY - panel / 2);
        g.drawLine(centroX, centroY + panel / 2, centroX, y + tam - 8);
        g.drawLine(x + 10, centroY, centroX - panel / 2, centroY);
        g.drawLine(centroX + panel / 2, centroY, x + tam - 10, centroY);
        g.setColor(new Color(255, 255, 255, 150));
        g.fillOval(x + 12, y + 12, tam / 4, tam / 5);
    }

    private void dibujarJugadorDecorativoInicio(Graphics2D g, int x, int y, boolean miraDerecha, Color cuerpo, Color borde, Color detalle) {
        int dir = miraDerecha ? 1 : -1;
        g.setColor(new Color(0, 0, 0, 56));
        g.fillOval(x + 8, y + 106, 42, 10);

        g.setColor(new Color(245, 208, 172));
        g.fillOval(x + 18 + dir * 2, y, 22, 22);
        g.setColor(borde);
        g.drawOval(x + 18 + dir * 2, y, 22, 22);

        g.setPaint(new GradientPaint(x + 10, y + 20, cuerpo.brighter(), x + 10, y + 78, cuerpo.darker()));
        g.fillRoundRect(x + 10, y + 22, 38, 44, 16, 16);
        g.setColor(borde);
        g.drawRoundRect(x + 10, y + 22, 38, 44, 16, 16);
        g.setColor(detalle);
        g.fillRoundRect(x + 27, y + 24, 6, 40, 4, 4);

        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(245, 208, 172));
        g.drawLine(x + 14, y + 34, x + 2, y + 54);
        g.drawLine(x + 44, y + 34, x + 58, y + 18);
        g.setColor(detalle.darker());
        g.drawLine(x + 24, y + 64, x + 18, y + 102);
        g.drawLine(x + 34, y + 64, x + 42, y + 102);
        g.setColor(new Color(24, 24, 28));
        g.drawLine(x + 14, y + 104, x + 22, y + 104);
        g.drawLine(x + 38, y + 104, x + 46, y + 104);
    }

    private void dibujarStickerInicio(Graphics2D g, int x, int y, int w, int h, String texto, Color borde, Color fondo) {
        g.setColor(new Color(0, 0, 0, 42));
        g.fillRoundRect(x + 3, y + 3, w, h, 12, 12);
        g.setColor(fondo);
        g.fillRoundRect(x, y, w, h, 12, 12);
        g.setColor(borde);
        g.drawRoundRect(x, y, w, h, 12, 12);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(248, 246, 238));
        g.drawString(texto, x + (w - fm.stringWidth(texto)) / 2, y + 21);
    }

    private void dibujarCintasInicio(Graphics2D g, int x, int y, int w) {
        g.setPaint(new GradientPaint(x, y, new Color(255, 186, 76, 118), x + w, y, new Color(255, 112, 72, 118)));
        g.fillRoundRect(x, y, w / 2 - 8, 8, 8, 8);
        g.setPaint(new GradientPaint(x + w / 2 + 8, y, new Color(86, 206, 255, 110), x + w, y, new Color(102, 255, 196, 110)));
        g.fillRoundRect(x + w / 2 + 8, y, w / 2 - 8, 8, 8, 8);
    }

    private void dibujarParticulasMenu(Graphics2D g, int x, int y, int w, int h) {
        for (int i = 0; i < 28; i++) {
            int semilla = i * 97;
            int px = x + Math.abs(((relojUI * (i + 1)) + semilla) % Math.max(1, w - 8));
            int py = y + Math.abs(((relojUI * (i + 2) / 2) + semilla * 3) % Math.max(1, h - 8));
            int r = 2 + i % 3;
            g.setColor(new Color(255, 255, 255, 18 + (i % 4) * 6));
            g.fillOval(px, py, r, r);
        }
    }

    private void dibujarControlesAnimados(Graphics2D g, int panelX, int panelY, int panelW, int panelH, boolean inicioModoEspectador) {
        int columnaGap = 18;
        int bloqueW = (panelW - 40 - columnaGap) / 2;
        int bloqueH = panelH - 34;
        int xControles = panelX + 20;
        int xAcciones = xControles + bloqueW + columnaGap;
        int y = panelY + 12;

        g.setColor(new Color(12, 16, 22, 90));
        g.fillRoundRect(xControles, y, bloqueW, bloqueH, 14, 14);
        g.fillRoundRect(xAcciones, y, bloqueW, bloqueH, 14, 14);
        g.setColor(new Color(255, 255, 255, 40));
        g.drawRoundRect(xControles, y, bloqueW, bloqueH, 14, 14);
        g.drawRoundRect(xAcciones, y, bloqueW, bloqueH, 14, 14);

        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(248, 236, 196));
        g.drawString("Controles", xControles + 14, y + 24);
        g.drawString("Acciones", xAcciones + 14, y + 24);

        dibujarItemControl(g, xControles + 14, y + 46, "WASD / Flechas", "Movimiento");
        dibujarItemControl(g, xControles + 14, y + 74, "SHIFT", "Sprint");
        dibujarItemControl(g, xControles + 14, y + 102, "TAB / <- ->", "Elegir modo");
        dibujarItemControl(g, xControles + 14, y + 130, "F2", "Cambiar en partido");
        dibujarItemControl(g, xControles + 14, y + 158, "P / R", "Pausa o reinicio");

        dibujarItemControl(g, xAcciones + 14, y + 46, "SPACE", "Pase con carga");
        dibujarItemControl(g, xAcciones + 14, y + 74, "X", "Tiro con carga");
        dibujarItemControl(g, xAcciones + 14, y + 102, "Roba", "Presiona y anticipa");
        dibujarItemControl(g, xAcciones + 14, y + 130, "Turbo", "Aprovecha bonus");
        dibujarItemControl(g, xAcciones + 14, y + 158, "Banca", "Stamina (usos limitados)");

        dibujarSelectorModoInicio(g, panelX + 26, panelY + panelH - 62, panelW - 52, 38, inicioModoEspectador);

        g.setColor(new Color(208, 228, 244, 180));
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Tip: espectador viene por defecto; ENTER inicia con la opcion elegida.", panelX + 24, panelY + panelH - 10);
    }

    private void dibujarSelectorModoInicio(Graphics2D g, int x, int y, int w, int h, boolean espectador) {
        int mitad = (w - 10) / 2;
        dibujarOpcionModoInicio(g, x, y, mitad, h, "ESPECTADOR", espectador, new Color(86, 198, 255), new Color(22, 58, 86));
        dibujarOpcionModoInicio(g, x + mitad + 10, y, mitad, h, "JUGADOR", !espectador, new Color(255, 190, 92), new Color(86, 54, 20));
    }

    private void dibujarOpcionModoInicio(Graphics2D g, int x, int y, int w, int h, String texto, boolean activa, Color brillo, Color sombra) {
        g.setColor(new Color(8, 12, 18, activa ? 170 : 90));
        g.fillRoundRect(x, y, w, h, 14, 14);
        g.setColor(activa ? brillo : new Color(255, 255, 255, 44));
        g.drawRoundRect(x, y, w, h, 14, 14);
        if (activa) {
            g.setColor(new Color(brillo.getRed(), brillo.getGreen(), brillo.getBlue(), 44));
            g.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 12, 12);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(sombra.getRed(), sombra.getGreen(), sombra.getBlue(), 160));
        g.drawString(texto, x + (w - fm.stringWidth(texto)) / 2 + 1, y + 22);
        g.setColor(activa ? new Color(248, 248, 242) : new Color(196, 204, 214));
        g.drawString(texto, x + (w - fm.stringWidth(texto)) / 2, y + 21);
    }

    private void dibujarItemControl(Graphics2D g, int x, int y, String tecla, String descripcion) {
        g.setColor(new Color(255, 255, 255, 30));
        g.fillRoundRect(x, y - 13, 116, 20, 10, 10);
        g.setColor(new Color(255, 255, 255, 55));
        g.drawRoundRect(x, y - 13, 116, 20, 10, 10);
        g.setColor(new Color(255, 234, 168));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(tecla, x + 8, y + 1);
        g.setColor(new Color(226, 236, 246, 220));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(descripcion, x + 124, y + 1);
    }

    private void dibujarBotonInicioAnimado(Graphics2D g, int x, int y, int w, int h) {
        int pulso = (int) Math.round((Math.sin(relojUI * 0.08) + 1.0) * 4.0);
        g.setPaint(new GradientPaint(x, y, new Color(255, 184, 78), x + w, y + h, new Color(255, 130, 72)));
        g.fillRoundRect(x - pulso / 2, y - pulso / 2, w + pulso, h + pulso, 16, 16);
        g.setColor(new Color(26, 20, 12));
        g.drawRoundRect(x - pulso / 2, y - pulso / 2, w + pulso, h + pulso, 16, 16);

        int brilloX = x + (int) ((relojUI * 7L) % Math.max(1, w + 120)) - 120;
        g.setColor(new Color(255, 255, 255, 56));
        g.fillRoundRect(brilloX, y + 4, 110, h - 8, 12, 12);

        g.setColor(new Color(26, 20, 12));
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        String texto = "ENTER para comenzar la reta";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, x + (w - fm.stringWidth(texto)) / 2, y + 34);
    }
}
