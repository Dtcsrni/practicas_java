package juego.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import juego.core.ConfiguracionJuego;
import juego.core.EntradaJuego;
import juego.core.EstadoJuego;
import juego.core.GeometriaCancha;
import juego.core.MaquinaEstadosJuego;
import juego.core.MotorJuego;
import juego.entidades.Jugador;

// Renderizador 2D del partido: fondo, cancha, entidades, HUD y menus animados.
public class RenderJuego {
    private EstadoJuego ultimoEstado = EstadoJuego.INICIO;
    private int framesEnEstado = 0;
    private int relojUI = 0;

    public void dibujarEscena(Graphics g, MotorJuego motor, MaquinaEstadosJuego maquinaEstados, EntradaJuego entrada, int frameAnimacion) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        relojUI++;
        actualizarEstadoVisual(maquinaEstados.getEstadoActual());

        dibujarCielo(g2);
        dibujarEntornoUrbano(g2);
        dibujarCancha(g2, motor.getCancha(), frameAnimacion);
        dibujarBanca(g2);

        motor.getHidratacionBanca().dibujar(g2);
        motor.getTurbo().dibujar(g2);

        dibujarJugadoresPorProfundidad(g2, motor);
        motor.getBalon().dibujar(g2);
        dibujarParticulasJuego(g2, motor);
        dibujarEtiquetasJugadores(g2, motor);
        dibujarIndicadorArbitral(g2, motor);
        dibujarNarracionEnPantalla(g2, motor);

        dibujarHUD(g2, motor, entrada);
        dibujarAnimacionSorteoMoneda(g2, motor);
        dibujarOverlayEstado(g2, maquinaEstados.getEstadoActual(), maquinaEstados.getMensajeTemporal(), motor);
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

    private void dibujarEntornoUrbano(Graphics2D g) {
        GeometriaCancha cancha = ConfiguracionJuego.MAPA_CANCHA;
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

        dibujarBardasYGraffitis(g);

        dibujarGradas(g);

        g.setColor(new Color(0, 0, 0, 68));
        g.fillRect(0, cancha.getCampoYMin() - 14, ConfiguracionJuego.ANCHO_PANEL, 10);
        g.fillRect(0, cancha.getCampoYMax() + 4, ConfiguracionJuego.ANCHO_PANEL, 10);
    }

    private void dibujarGradas(Graphics2D g) {
        int alto = 68;
        g.setColor(new Color(18, 18, 18, 165));
        g.fillRect(66, 0, ConfiguracionJuego.ANCHO_PANEL - 132, alto);
        g.fillRect(66, ConfiguracionJuego.ALTO_PANEL - alto, ConfiguracionJuego.ANCHO_PANEL - 132, alto);

        for (int x = 74; x < ConfiguracionJuego.ANCHO_PANEL - 74; x += 16) {
            int ritmo = (x + relojUI) % 26;
            int topY = 14 + (ritmo % 3);
            int botY = ConfiguracionJuego.ALTO_PANEL - 24 - (ritmo % 3);
            g.setColor(new Color(80 + (x % 110), 90 + (x % 70), 110 + (x % 50), 130));
            g.fillOval(x, topY, 6, 6);
            g.setColor(new Color(190, 126 + (x % 90), 78 + (x % 60), 118));
            g.fillOval(x + 6, topY + 1, 5, 5);
            g.setColor(new Color(90 + (x % 90), 140 + (x % 80), 190, 126));
            g.fillOval(x, botY, 6, 6);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(new Color(248, 187, 96));
        g.drawString("SECTOR NORTE", 84, 42);
        g.setColor(new Color(106, 214, 232));
        g.drawString("TRIBUNA BARRIAL", ConfiguracionJuego.ANCHO_PANEL - 256, 42);
    }

    private void dibujarBardasYGraffitis(Graphics2D g) {
        int muroYTop = 56;
        int muroYBottom = ConfiguracionJuego.ALTO_PANEL - 92;
        int muroH = 34;

        g.setColor(new Color(92, 96, 104, 180));
        g.fillRect(66, muroYTop, ConfiguracionJuego.ANCHO_PANEL - 132, muroH);
        g.fillRect(66, muroYBottom, ConfiguracionJuego.ANCHO_PANEL - 132, muroH);
        g.setColor(new Color(64, 66, 74, 170));
        g.drawRect(66, muroYTop, ConfiguracionJuego.ANCHO_PANEL - 132, muroH);
        g.drawRect(66, muroYBottom, ConfiguracionJuego.ANCHO_PANEL - 132, muroH);

        for (int x = 76; x < ConfiguracionJuego.ANCHO_PANEL - 70; x += 26) {
            g.setColor(new Color(130, 134, 142, 70));
            g.drawLine(x, muroYTop + 2, x + 8, muroYTop + muroH - 4);
            g.drawLine(x, muroYBottom + 2, x + 8, muroYBottom + muroH - 4);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.setColor(new Color(236, 88, 126, 210));
        g.drawString("BARRIO VIVE", 120, muroYTop + 25);
        g.setColor(new Color(82, 220, 188, 214));
        g.drawString("JUEGA LIMPIO", ConfiguracionJuego.ANCHO_PANEL - 300, muroYTop + 25);

        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(new Color(248, 206, 86, 204));
        g.drawString("LA CANCHITA", 126, muroYBottom + 24);
        g.setColor(new Color(108, 170, 255, 204));
        g.drawString("NO FALLES EL PASE", ConfiguracionJuego.ANCHO_PANEL - 336, muroYBottom + 24);
    }

    private void dibujarBanca(Graphics2D g) {
        GeometriaCancha cancha = ConfiguracionJuego.MAPA_CANCHA;
        int bancaW = 214;
        int bancaH = 24;
        int bancaX = ConfiguracionJuego.ANCHO_PANEL / 2 - bancaW / 2;
        int bancaY = cancha.getCampoYMin() - bancaH + 4;

        g.setColor(new Color(0, 0, 0, 76));
        g.fillRoundRect(bancaX + 3, bancaY + 5, bancaW, bancaH, 12, 12);

        g.setPaint(new GradientPaint(bancaX, bancaY, new Color(42, 92, 128), bancaX, bancaY + bancaH, new Color(22, 56, 86)));
        g.fillRoundRect(bancaX, bancaY, bancaW, bancaH, 12, 12);
        g.setColor(new Color(186, 224, 244, 110));
        g.drawRoundRect(bancaX, bancaY, bancaW, bancaH, 12, 12);

        g.setColor(new Color(196, 228, 246, 170));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("BANCA", bancaX + 18, bancaY + 16);
        g.drawString("HIDRATACION", bancaX + bancaW - 106, bancaY + 16);
    }

    private void dibujarCancha(Graphics2D g, GeometriaCancha cancha, int frameAnimacion) {
        Rectangle campo = cancha.getCampo();
        Rectangle areaGrandeLocal = cancha.getAreaGrande(true);
        Rectangle areaGrandeRival = cancha.getAreaGrande(false);
        Rectangle areaChicaLocal = cancha.getAreaChica(true);
        Rectangle areaChicaRival = cancha.getAreaChica(false);
        int x = campo.x;
        int y = campo.y;
        int w = campo.width;
        int h = campo.height;

        g.setPaint(new GradientPaint(x, y, new Color(98, 102, 108), x, y + h, new Color(78, 82, 90)));
        g.fillRoundRect(x, y, w, h, 22, 22);
        g.setColor(new Color(58, 60, 66));
        g.fillRoundRect(x - 6, y - 6, w + 12, h + 12, 24, 24);

        for (int i = 0; i < 260; i++) {
            int semilla = i * 49 + relojUI;
            int px = x + Math.abs((semilla * 23) % Math.max(1, w - 4));
            int py = y + Math.abs((semilla * 17) % Math.max(1, h - 4));
            int tono = 96 + ((i * 7) % 26);
            g.setColor(new Color(tono, tono, tono + 4, 52));
            g.fillRect(px, py, 2, 2);
        }

        for (int i = 0; i < 26; i++) {
            int gx = x + 20 + (i * 44) % Math.max(1, w - 40);
            int gy = y + 20 + (i * 71) % Math.max(1, h - 40);
            int largo = 14 + (i % 4) * 8;
            g.setColor(new Color(56, 58, 62, 130));
            g.drawLine(gx, gy, gx + largo, gy + (i % 2 == 0 ? 5 : -5));
        }

        g.setStroke(new BasicStroke(3.5f));
        g.setColor(new Color(236, 236, 236, 224));
        g.drawRect(x, y, w, h);
        g.drawLine(cancha.getCentroX(), y, cancha.getCentroX(), y + h);
        g.drawOval(
            cancha.getCentroX() - cancha.getRadioCirculoCentral(),
            cancha.getCentroY() - cancha.getRadioCirculoCentral(),
            cancha.getRadioCirculoCentral() * 2,
            cancha.getRadioCirculoCentral() * 2
        );
        g.drawRect(areaGrandeLocal.x, areaGrandeLocal.y, areaGrandeLocal.width, areaGrandeLocal.height);
        g.drawRect(areaGrandeRival.x, areaGrandeRival.y, areaGrandeRival.width, areaGrandeRival.height);
        g.drawRect(areaChicaLocal.x, areaChicaLocal.y, areaChicaLocal.width, areaChicaLocal.height);
        g.drawRect(areaChicaRival.x, areaChicaRival.y, areaChicaRival.width, areaChicaRival.height);
        g.fillOval(cancha.getCentroX() - 5, cancha.getCentroY() - 5, 10, 10);
        g.fillOval(cancha.getPuntoPenalX(true) - 4, cancha.getPuntoPenalY() - 4, 8, 8);
        g.fillOval(cancha.getPuntoPenalX(false) - 4, cancha.getPuntoPenalY() - 4, 8, 8);

        g.setColor(new Color(232, 232, 232, 176));
        int radioArco = cancha.getRadioArcoArea();
        g.drawArc(
            cancha.getPuntoPenalX(true) - radioArco,
            cancha.getPuntoPenalY() - radioArco,
            radioArco * 2,
            radioArco * 2,
            308,
            104
        );
        g.drawArc(
            cancha.getPuntoPenalX(false) - radioArco,
            cancha.getPuntoPenalY() - radioArco,
            radioArco * 2,
            radioArco * 2,
            128,
            104
        );

        g.setColor(new Color(224, 224, 224, 64));
        for (int linea = y + 54; linea < y + h; linea += 78) {
            g.drawLine(x + 22, linea, x + w - 22, linea);
        }

        int parpadeo = (int) Math.abs(Math.sin(frameAnimacion * 0.05) * 12.0);
        g.setColor(new Color(255, 194, 92, 60 + parpadeo));
        g.drawRoundRect(x + 2, y + 2, w - 4, h - 4, 20, 20);

        dibujarPorteria(g, cancha, true);
        dibujarPorteria(g, cancha, false);
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
            jugador.dibujar(g);
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
        int x = arbitro.getX() + arbitro.getAncho() / 2 - 26;
        int y = arbitro.getY() - 28 - pulso / 2;
        int w = 52;
        int h = 18;
        int alphaBase = Math.max(72, Math.min(210, 120 + frames * 3));

        Color fondo = new Color(20, 24, 30, alphaBase);
        Color borde = new Color(255, 240, 160, Math.min(255, alphaBase + 36));
        Color texto = new Color(255, 246, 188, Math.min(255, alphaBase + 28));
        if ("FALTA".equals(accion)) {
            fondo = new Color(72, 22, 20, alphaBase);
            borde = new Color(255, 132, 112, Math.min(255, alphaBase + 32));
            texto = new Color(255, 220, 214, Math.min(255, alphaBase + 28));
        } else if ("GOL".equals(accion)) {
            fondo = new Color(22, 64, 36, alphaBase);
            borde = new Color(118, 246, 172, Math.min(255, alphaBase + 32));
            texto = new Color(224, 255, 236, Math.min(255, alphaBase + 28));
        }

        g.setColor(fondo);
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(borde);
        g.drawRoundRect(x, y, w, h, 10, 10);
        g.setColor(texto);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString(accion, x + 7, y + 13);
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
        g.setColor(new Color(255, 255, 255, 66));
        g.drawRoundRect(x, y, ancho, alto, 18, 18);
        g.setColor(new Color(255, 255, 255, 28));
        g.fillRoundRect(x + 8, y + 8, ancho - 16, 16, 12, 12);
    }

    private void dibujarTarjetaMarcador(Graphics2D g, MotorJuego motor) {
        int x = 18;
        int y = 10;
        int ancho = 328;
        int alto = 78;
        dibujarTarjetaGlass(g, x, y, ancho, alto, new Color(8, 20, 30, 188), new Color(24, 48, 42, 182));

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(255, 228, 170));
        g.drawString("MARCADOR", x + 14, y + 19);

        g.setColor(new Color(232, 88, 68));
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.drawString(String.valueOf(motor.getGolesLocal()), x + 14, y + 56);

        g.setColor(new Color(245, 245, 245));
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("-", x + 50, y + 53);

        g.setColor(new Color(70, 180, 245));
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.drawString(String.valueOf(motor.getGolesRival()), x + 66, y + 56);

        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(244, 244, 244));
        g.drawString("Tiempo: " + motor.getTiempoPartidoTexto(), x + 108, y + 36);
        g.drawString("Posesion: " + motor.getPoseedorTexto(), x + 108, y + 58);

        if (motor.isModoEspectador()) {
            g.setColor(new Color(255, 221, 126));
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.drawString("ESPECTADOR", x + 236, y + 18);
        }
    }

    private void dibujarTarjetaEstado(Graphics2D g, MotorJuego motor) {
        int x = 354;
        int y = 10;
        int ancho = 454;
        int alto = 78;
        dibujarTarjetaGlass(g, x, y, ancho, alto, new Color(10, 18, 24, 182), new Color(18, 34, 42, 176));

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(236, 236, 236));
        g.drawString("Stamina: " + motor.getStaminaPrincipalPorcentaje() + "%", x + 12, y + 30);
        g.drawString("Bonus: " + motor.getPuntosBonus() + " / " + motor.getPuntosBonusRival() + "  H2O: " + motor.getUsosHidratacionBancaRestantes(), x + 12, y + 50);
        g.drawString("Balon: " + String.format("%.1f", motor.getBalon().getAltura()) + "m", x + 12, y + 69);

        int barraX = x + 142;
        int barraY = y + 16;
        int barraW = 144;
        int barraH = 12;
        double energia = motor.getStaminaPrincipalPorcentaje() / 100.0;
        int relleno = (int) Math.round(barraW * energia);
        Color colorEnergia = energia > 0.55 ? new Color(86, 218, 126) : (energia > 0.25 ? new Color(255, 196, 84) : new Color(255, 102, 84));

        g.setColor(new Color(16, 16, 16, 190));
        g.fillRoundRect(barraX, barraY, barraW, barraH, 8, 8);
        g.setColor(colorEnergia);
        g.fillRoundRect(barraX, barraY, Math.max(3, relleno), barraH, 8, 8);
        g.setColor(new Color(255, 255, 255, 90));
        g.drawRoundRect(barraX, barraY, barraW, barraH, 8, 8);

        g.setColor(new Color(208, 228, 244, 210));
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        String aviso = motor.getNarracionActual() != null && !motor.getNarracionActual().isEmpty()
            ? "Narrador: " + motor.getNarracionActual()
            : (motor.getTextoSaque() != null ? motor.getTextoSaque() : "");
        if (aviso != null && !aviso.isEmpty()) {
            g.drawString(recortarTexto(aviso, 45), x + 298, y + 30);
        }
        g.setColor(new Color(210, 230, 240, 186));
        g.drawString(motor.isModoEspectador() ? "F2 jugador | P pausa" : "SHIFT correr | SPACE pase | X tiro | C barrida", x + 298, y + 50);
    }

    private void dibujarNarracionEnPantalla(Graphics2D g, MotorJuego motor) {
        String narracion = motor.getNarracionActual();
        String textoSaque = motor.getTextoSaque();
        boolean tieneNarracion = narracion != null && !narracion.isEmpty() && motor.getFramesNarracion() > 0;
        boolean tieneEvento = textoSaque != null && !textoSaque.isEmpty() && motor.getFramesTextoSaque() > 0;
        if (!tieneNarracion && !tieneEvento) {
            return;
        }

        int x = ConfiguracionJuego.ANCHO_PANEL / 2 - 260;
        int y = ConfiguracionJuego.ALTO_PANEL - 116;
        int ancho = 520;
        int alto = tieneNarracion && tieneEvento ? 68 : 46;
        int alphaPanel = tieneNarracion ? 216 : 190;

        g.setColor(new Color(8, 12, 18, alphaPanel));
        g.fillRoundRect(x, y, ancho, alto, 18, 18);
        g.setColor(new Color(255, 255, 255, 72));
        g.drawRoundRect(x, y, ancho, alto, 18, 18);

        int brilloX = x + (int) ((relojUI * 5L) % Math.max(1, ancho + 140)) - 120;
        g.setColor(new Color(255, 255, 255, 26));
        g.fillRoundRect(brilloX, y + 6, 112, alto - 12, 14, 14);

        int cursorY = y + 20;
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

    private void dibujarBarraCargaAccion(Graphics2D g, EntradaJuego entrada, boolean modoEspectador) {
        if (modoEspectador) {
            return;
        }
        if (entrada == null || (!entrada.estaCargandoPase() && !entrada.estaCargandoTiro())) {
            return;
        }

        int x = 826;
        int y = 16;
        int ancho = 190;
        int alto = 14;
        double factor = entrada.getFactorCargaActiva();
        double progreso = (factor - 0.35) / (1.00 - 0.35);
        progreso = Math.max(0.0, Math.min(1.0, progreso));
        int anchoRelleno = (int) Math.round(ancho * progreso);

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

        int cajaAncho = 420;
        int cajaAlto = 214;
        int xCaja = ConfiguracionJuego.ANCHO_PANEL / 2 - cajaAncho / 2;
        int yCaja = 78;

        g.setColor(new Color(8, 14, 20, 205));
        g.fillRoundRect(xCaja, yCaja, cajaAncho, cajaAlto, 18, 18);
        g.setColor(new Color(255, 255, 255, 60));
        g.drawRoundRect(xCaja, yCaja, cajaAncho, cajaAlto, 18, 18);

        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.setColor(new Color(250, 237, 205));
        g.drawString("Saque inicial - sorteo con moneda", xCaja + 64, yCaja + 34);

        int frame = motor.getFramesAnimacionMoneda();
        double giro = Math.sin(frame * 0.42);
        int monedaH = 86;
        int monedaW = Math.max(8, (int) Math.round(86 * Math.abs(giro)));
        int monedaX = ConfiguracionJuego.ANCHO_PANEL / 2 - monedaW / 2;
        int monedaY = yCaja + 56;
        boolean ladoCara = giro >= 0;

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
        g.drawString("El arbitro lanza la moneda... " + segundos + "s", xCaja + 96, yCaja + 164);

        int barraX = xCaja + 62;
        int barraY = yCaja + 176;
        int barraW = cajaAncho - 124;
        int barraH = 10;
        double progreso = 1.0 - Math.max(0.0, Math.min(1.0, motor.getFramesSorteoMoneda() / (double) (ConfiguracionJuego.FPS * 3)));
        g.setColor(new Color(18, 18, 18, 180));
        g.fillRoundRect(barraX, barraY, barraW, barraH, 8, 8);
        g.setColor(new Color(255, 196, 92));
        g.fillRoundRect(barraX, barraY, Math.max(3, (int) Math.round(barraW * progreso)), barraH, 8, 8);

        String resultado = motor.getResultadoMoneda();
        if (resultado != null && !resultado.isEmpty() && !resultado.equals("La moneda esta girando...")) {
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(255, 228, 128));
            g.drawString(resultado, xCaja + 66, yCaja + 200);
        }
    }

    private void dibujarOverlayEstado(Graphics2D g, EstadoJuego estado, String mensaje, MotorJuego motor) {
        if (estado == EstadoJuego.JUGANDO) {
            return;
        }
        if (estado == EstadoJuego.INICIO) {
            dibujarPantallaInicio(g);
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
        dibujarFondoOverlay(g, 176);
        int cx = ConfiguracionJuego.ANCHO_PANEL / 2;
        int cy = ConfiguracionJuego.ALTO_PANEL / 2;

        Color cA = estado == EstadoJuego.GOL ? new Color(255, 213, 95) : new Color(255, 166, 88);
        Color cB = estado == EstadoJuego.GOL ? new Color(236, 162, 62) : new Color(210, 118, 64);
        dibujarTarjetaCentralAnimada(g, cx, cy, 540, 216, cA, cB);

        dibujarRafagaEvento(g, cx, cy, estado == EstadoJuego.GOL ? new Color(255, 246, 198, 90) : new Color(255, 220, 180, 84));

        g.setColor(new Color(30, 26, 18));
        g.setFont(new Font("SansSerif", Font.BOLD, 42));
        g.drawString(estado == EstadoJuego.GOL ? "GOL" : "FALTA", cx - 78, cy - 38);

        String subtitulo;
        if (estado == EstadoJuego.GOL) {
            subtitulo = motor.getResumenUltimoGol().isEmpty()
                ? (mensaje == null || mensaje.isEmpty() ? "Se reinicia la jugada" : mensaje)
                : (mensaje == null || mensaje.isEmpty() ? "Gol" : mensaje) + " de " + motor.getUltimoGoleador();
        } else {
            subtitulo = mensaje == null || mensaje.isEmpty() ? "Reanudacion del juego" : mensaje;
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString(subtitulo, cx - 216, cy + 2);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("Marcador: " + motor.getGolesLocal() + " - " + motor.getGolesRival(), cx - 114, cy + 42);
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

        dibujarTarjetaCentralAnimada(g, cx, cy, 560, 224, cA, cB);

        g.setColor(new Color(28, 24, 20));
        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        g.drawString(titulo, cx - 190, cy - 36);
        g.setFont(new Font("SansSerif", Font.PLAIN, 21));
        g.drawString(subtitulo, cx - 202, cy + 6);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("R para nuevo partido", cx - 102, cy + 46);
    }

    private void dibujarPantallaInicio(Graphics2D g) {
        double entrada = progresoEntrada(34);
        dibujarFondoOverlay(g, alpha(145 + entrada * 18.0));

        int xBase = ConfiguracionJuego.ANCHO_PANEL / 2 - 360;
        int yBase = 84 + (int) Math.round(Math.sin(relojUI * 0.04) * 4.0);
        int ancho = 720;
        int alto = 486;

        g.setPaint(new GradientPaint(xBase, yBase, new Color(14, 22, 28, 236), xBase + ancho, yBase + alto, new Color(22, 54, 46, 232)));
        g.fillRoundRect(xBase, yBase, ancho, alto, 28, 28);
        g.setColor(new Color(255, 255, 255, 78));
        g.drawRoundRect(xBase, yBase, ancho, alto, 28, 28);

        dibujarParticulasMenu(g, xBase, yBase, ancho, alto);

        g.setColor(new Color(255, 227, 148));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("LA CANCHITA", xBase + 24, yBase + 36);

        int flotacionTitulo = (int) Math.round(Math.sin(relojUI * 0.06) * 3.0);
        g.setColor(new Color(12, 12, 12, 120));
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        g.drawString("Futbol Urbano", xBase + 28, yBase + 102 + flotacionTitulo);
        g.setColor(new Color(245, 245, 245));
        g.drawString("Futbol Urbano", xBase + 24, yBase + 98 + flotacionTitulo);

        g.setColor(new Color(218, 234, 242));
        g.setFont(new Font("SansSerif", Font.PLAIN, 21));
        g.drawString("Pachuca vs Seleccion Mexicana | 5 vs 5", xBase + 24, yBase + 136);

        int panelX = xBase + 24;
        int panelY = yBase + 160;
        int panelW = ancho - 48;
        int panelH = 238;
        g.setColor(new Color(255, 255, 255, 35));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 18, 18);
        g.setColor(new Color(255, 255, 255, 66));
        g.drawRoundRect(panelX, panelY, panelW, panelH, 18, 18);

        dibujarControlesAnimados(g, panelX, panelY, panelW, panelH);
        dibujarBotonInicioAnimado(g, xBase + 24, yBase + alto - 80, ancho - 48, 52);
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

    private void dibujarControlesAnimados(Graphics2D g, int panelX, int panelY, int panelW, int panelH) {
        int columnaGap = 18;
        int bloqueW = (panelW - 40 - columnaGap) / 2;
        int bloqueH = panelH - 24;
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
        dibujarItemControl(g, xControles + 14, y + 102, "F2", "Espectador");
        dibujarItemControl(g, xControles + 14, y + 130, "P", "Pausa");
        dibujarItemControl(g, xControles + 14, y + 158, "R", "Reiniciar");

        dibujarItemControl(g, xAcciones + 14, y + 46, "SPACE", "Pase con carga");
        dibujarItemControl(g, xAcciones + 14, y + 74, "X", "Tiro con carga");
        dibujarItemControl(g, xAcciones + 14, y + 102, "Roba", "Presiona y anticipa");
        dibujarItemControl(g, xAcciones + 14, y + 130, "Turbo", "Aprovecha bonus");
        dibujarItemControl(g, xAcciones + 14, y + 158, "Banca", "Stamina (usos limitados)");

        g.setColor(new Color(208, 228, 244, 180));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString("Tip: administra stamina; la hidratacion de banca no es infinita.", panelX + 24, panelY + panelH - 16);
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
        g.drawString("ENTER para comenzar la reta", x + 176, y + 34);
    }
}
