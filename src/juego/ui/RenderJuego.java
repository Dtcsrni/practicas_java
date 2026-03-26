package juego.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.BasicStroke;
import java.awt.RenderingHints;

import juego.core.ConfiguracionJuego;
import juego.core.EstadoJuego;
import juego.core.MaquinaEstadosJuego;
import juego.core.MotorJuego;
import juego.entidades.Jugador;

// Renderizador 2D del juego.
// Su unica responsabilidad es dibujar; no altera estado de logica.
public class RenderJuego {
    public void dibujarEscena(Graphics g, MotorJuego motor, MaquinaEstadosJuego maquinaEstados, int frameAnimacion) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Capas base.
        dibujarEntornoUrbano(g2, frameAnimacion);
        dibujarCancha(g2, frameAnimacion);
        dibujarDecoracionUrbana(g2, frameAnimacion);
        dibujarAtmosfera(g2, frameAnimacion);

        // Entidades de juego.
        motor.getBalon().dibujar(g2);
        motor.getMonedaEspecial().dibujar(g2);
        motor.getTurbo().dibujar(g2);

        for (Jugador local : motor.getLocales()) {
            local.dibujar(g2);
        }
        for (Jugador rival : motor.getRivales()) {
            rival.dibujar(g2);
        }
        dibujarEtiquetasJugadores(g2, motor);

        dibujarHUD(g2, motor);
        dibujarOverlayEstado(g2, maquinaEstados.getEstadoActual(), maquinaEstados.getMensajeTemporal(), motor);
        g2.dispose();
    }

    private void dibujarEntornoUrbano(Graphics2D g, int frameAnimacion) {
        // Fondo general y bordes del entorno.
        g.setPaint(new GradientPaint(0, 0, new Color(12, 20, 28), 0, ConfiguracionJuego.ALTO_PANEL, new Color(45, 36, 24)));
        g.fillRect(0, 0, ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL);

        g.setColor(new Color(255, 191, 110, 58));
        g.fillOval(ConfiguracionJuego.ANCHO_PANEL - 320, -120, 360, 280);
        g.setColor(new Color(255, 230, 170, 22));
        g.fillOval(ConfiguracionJuego.ANCHO_PANEL - 260, -80, 220, 180);

        // Muros laterales de la canchita.
        g.setColor(new Color(76, 72, 67));
        g.fillRect(0, 0, 36, ConfiguracionJuego.ALTO_PANEL);
        g.fillRect(ConfiguracionJuego.ANCHO_PANEL - 36, 0, 36, ConfiguracionJuego.ALTO_PANEL);
        g.setColor(new Color(54, 48, 42));
        g.fillRect(36, 0, 24, ConfiguracionJuego.ALTO_PANEL);
        g.fillRect(ConfiguracionJuego.ANCHO_PANEL - 60, 0, 24, ConfiguracionJuego.ALTO_PANEL);

        dibujarGradas(g, frameAnimacion);

        // Malla superior/inferior.
        g.setColor(new Color(95, 95, 95));
        int desplazamiento = frameAnimacion % 16;
        for (int x = -16; x < ConfiguracionJuego.ANCHO_PANEL + 16; x += 16) {
            g.drawLine(x + desplazamiento, 0, x + 8 + desplazamiento, 10);
            g.drawLine(x - desplazamiento, ConfiguracionJuego.ALTO_PANEL - 1, x + 8 - desplazamiento, ConfiguracionJuego.ALTO_PANEL - 10);
        }

        g.setColor(new Color(12, 12, 12, 100));
        g.fillRect(0, ConfiguracionJuego.CAMPO_Y_MIN - 14, ConfiguracionJuego.ANCHO_PANEL, 10);
        g.fillRect(0, ConfiguracionJuego.CAMPO_Y_MAX + 4, ConfiguracionJuego.ANCHO_PANEL, 10);
    }

    private void dibujarCancha(Graphics2D g, int frameAnimacion) {
        // Pintura de area jugable y lineas principales.
        g.setColor(new Color(44, 132, 58));
        g.fillRect(
            ConfiguracionJuego.CAMPO_X_MIN - 6,
            ConfiguracionJuego.CAMPO_Y_MIN - 6,
            (ConfiguracionJuego.CAMPO_X_MAX - ConfiguracionJuego.CAMPO_X_MIN) + 12,
            (ConfiguracionJuego.CAMPO_Y_MAX - ConfiguracionJuego.CAMPO_Y_MIN) + 12
        );

        int desplazamiento = (frameAnimacion / 2) % 80;
        for (int x = ConfiguracionJuego.CAMPO_X_MIN - desplazamiento; x < ConfiguracionJuego.CAMPO_X_MAX; x += 80) {
            if (((x - ConfiguracionJuego.CAMPO_X_MIN) / 80) % 2 == 0) {
                g.setColor(new Color(39, 118, 53));
                g.fillRect(x, ConfiguracionJuego.CAMPO_Y_MIN - 6, 80, (ConfiguracionJuego.CAMPO_Y_MAX - ConfiguracionJuego.CAMPO_Y_MIN) + 12);
            }
        }

        g.setPaint(new GradientPaint(
            ConfiguracionJuego.CAMPO_X_MIN,
            ConfiguracionJuego.CAMPO_Y_MIN,
            new Color(255, 255, 255, 34),
            ConfiguracionJuego.CAMPO_X_MAX,
            ConfiguracionJuego.CAMPO_Y_MAX,
            new Color(255, 255, 255, 4)
        ));
        g.fillRoundRect(
            ConfiguracionJuego.CAMPO_X_MIN,
            ConfiguracionJuego.CAMPO_Y_MIN,
            ConfiguracionJuego.CAMPO_X_MAX - ConfiguracionJuego.CAMPO_X_MIN,
            ConfiguracionJuego.CAMPO_Y_MAX - ConfiguracionJuego.CAMPO_Y_MIN,
            24,
            24
        );

        g.setColor(new Color(255, 255, 255, 40));
        g.fillOval(ConfiguracionJuego.ANCHO_PANEL / 2 - 155, ConfiguracionJuego.ALTO_PANEL / 2 - 110, 310, 220);

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f));
        g.drawRect(
            ConfiguracionJuego.CAMPO_X_MIN,
            ConfiguracionJuego.CAMPO_Y_MIN,
            ConfiguracionJuego.CAMPO_X_MAX - ConfiguracionJuego.CAMPO_X_MIN,
            ConfiguracionJuego.CAMPO_Y_MAX - ConfiguracionJuego.CAMPO_Y_MIN
        );
        g.drawLine(
            ConfiguracionJuego.ANCHO_PANEL / 2,
            ConfiguracionJuego.CAMPO_Y_MIN,
            ConfiguracionJuego.ANCHO_PANEL / 2,
            ConfiguracionJuego.CAMPO_Y_MAX
        );
        g.drawOval(ConfiguracionJuego.ANCHO_PANEL / 2 - 56, ConfiguracionJuego.ALTO_PANEL / 2 - 56, 112, 112);
        g.drawRect(ConfiguracionJuego.CAMPO_X_MIN, ConfiguracionJuego.Y_PORTERIA + 34, 116, ConfiguracionJuego.ALTO_PORTERIA - 68);
        g.drawRect(ConfiguracionJuego.CAMPO_X_MAX - 116, ConfiguracionJuego.Y_PORTERIA + 34, 116, ConfiguracionJuego.ALTO_PORTERIA - 68);
        g.drawRect(ConfiguracionJuego.CAMPO_X_MIN, ConfiguracionJuego.Y_PORTERIA + 14, 46, ConfiguracionJuego.ALTO_PORTERIA - 28);
        g.drawRect(ConfiguracionJuego.CAMPO_X_MAX - 46, ConfiguracionJuego.Y_PORTERIA + 14, 46, ConfiguracionJuego.ALTO_PORTERIA - 28);
        g.fillOval(ConfiguracionJuego.ANCHO_PANEL / 2 - 5, ConfiguracionJuego.ALTO_PANEL / 2 - 5, 10, 10);

        g.setColor(new Color(255, 255, 255, 80));
        for (int y = ConfiguracionJuego.CAMPO_Y_MIN + 48; y < ConfiguracionJuego.CAMPO_Y_MAX; y += 72) {
            g.drawLine(ConfiguracionJuego.CAMPO_X_MIN + 18, y, ConfiguracionJuego.CAMPO_X_MAX - 18, y);
        }

        g.setColor(new Color(255, 255, 255, 150));
        g.fillOval(ConfiguracionJuego.ANCHO_PANEL / 2 - 3, ConfiguracionJuego.ALTO_PANEL / 2 - 3, 6, 6);
        g.fillOval(ConfiguracionJuego.CAMPO_X_MIN + 92, ConfiguracionJuego.ALTO_PANEL / 2 - 3, 6, 6);
        g.fillOval(ConfiguracionJuego.CAMPO_X_MAX - 98, ConfiguracionJuego.ALTO_PANEL / 2 - 3, 6, 6);

        g.setColor(new Color(255, 255, 255, 95));
        g.drawArc(ConfiguracionJuego.CAMPO_X_MIN + 84, ConfiguracionJuego.ALTO_PANEL / 2 - 76, 72, 152, 300, 120);
        g.drawArc(ConfiguracionJuego.CAMPO_X_MAX - 156, ConfiguracionJuego.ALTO_PANEL / 2 - 76, 72, 152, 120, 120);

        g.setColor(new Color(255, 255, 255, 14));
        for (int x = ConfiguracionJuego.CAMPO_X_MIN + 24; x < ConfiguracionJuego.CAMPO_X_MAX; x += 22) {
            int offset = (x + frameAnimacion) % 14;
            g.drawLine(x, ConfiguracionJuego.CAMPO_Y_MIN + offset, x + 10, ConfiguracionJuego.CAMPO_Y_MAX - offset);
        }

        dibujarPorteria(g, true);
        dibujarPorteria(g, false);
    }

    private void dibujarPorteria(Graphics2D g, boolean local) {
        int xMarco = local ? ConfiguracionJuego.CAMPO_X_MIN - 16 : ConfiguracionJuego.CAMPO_X_MAX - 2;
        int yMarco = ConfiguracionJuego.Y_PORTERIA + 10;
        int fondo = 28;
        int anchoMarco = 18;
        int altoMarco = ConfiguracionJuego.ALTO_PORTERIA - 20;

        g.setColor(new Color(232, 232, 232));
        g.fillRect(xMarco, yMarco, anchoMarco, altoMarco);
        g.fillRect(local ? xMarco - fondo : xMarco + anchoMarco, yMarco, fondo, altoMarco);
        g.setColor(new Color(210, 210, 210, 140));
        for (int i = 0; i < altoMarco; i += 14) {
            int xRed = local ? xMarco - fondo : xMarco + anchoMarco;
            g.drawLine(xMarco + (local ? 0 : anchoMarco), yMarco + i, xRed + (local ? fondo : 0), yMarco + i + 6);
        }
        for (int i = 0; i <= fondo; i += 7) {
            int x = local ? xMarco - i : xMarco + anchoMarco + i;
            g.drawLine(x, yMarco, x, yMarco + altoMarco);
        }
    }

    private void dibujarDecoracionUrbana(Graphics2D g, int frameAnimacion) {
        // Grafiti simple estilo urbano mexicano.
        g.setColor(new Color(196, 68, 155));
        g.setFont(new Font("Dialog", Font.BOLD, 24));
        g.drawString("BARRIO FC", 42, 48);
        g.setColor(new Color(53, 214, 196));
        g.drawString("LA CANCHITA", ConfiguracionJuego.ANCHO_PANEL - 208, 48);

        g.setColor(new Color(18, 18, 18, 90));
        g.fillRoundRect(78, 46, 148, 42, 12, 12);
        g.fillRoundRect(ConfiguracionJuego.ANCHO_PANEL - 246, 46, 176, 42, 12, 12);

        int pulso = (int) (Math.sin(frameAnimacion * 0.08) * 6.0);
        g.setColor(new Color(255, 200, 30));
        g.setFont(new Font("Dialog", Font.BOLD, 14));
        g.drawString("No tirar basura", 40, ConfiguracionJuego.ALTO_PANEL - 18);
        g.setColor(new Color(255, 120, 60));
        g.drawString("Respeta la reta", ConfiguracionJuego.ANCHO_PANEL - 170, ConfiguracionJuego.ALTO_PANEL - 18);
        g.setColor(new Color(255, 255, 255, 34));
        g.fillOval(80 + pulso, ConfiguracionJuego.ALTO_PANEL - 76, 56, 24);
        g.fillOval(ConfiguracionJuego.ANCHO_PANEL - 146 - pulso, ConfiguracionJuego.ALTO_PANEL - 76, 56, 24);
    }

    private void dibujarAtmosfera(Graphics2D g, int frameAnimacion) {
        int pulso = (int) (Math.sin(frameAnimacion * 0.03) * 10.0);
        g.setPaint(new GradientPaint(
            0,
            ConfiguracionJuego.CAMPO_Y_MIN,
            new Color(255, 255, 255, 0),
            0,
            ConfiguracionJuego.CAMPO_Y_MAX,
            new Color(0, 0, 0, 36)
        ));
        g.fillRect(
            ConfiguracionJuego.CAMPO_X_MIN,
            ConfiguracionJuego.CAMPO_Y_MIN,
            ConfiguracionJuego.CAMPO_X_MAX - ConfiguracionJuego.CAMPO_X_MIN,
            ConfiguracionJuego.CAMPO_Y_MAX - ConfiguracionJuego.CAMPO_Y_MIN
        );

        g.setColor(new Color(255, 248, 220, 28));
        g.fillOval(120 + pulso, 70, 260, 120);
        g.fillOval(ConfiguracionJuego.ANCHO_PANEL - 380 - pulso, 70, 260, 120);
    }

    private void dibujarGradas(Graphics2D g, int frameAnimacion) {
        int gradasAlto = 58;
        g.setColor(new Color(28, 28, 28, 120));
        g.fillRect(60, 0, ConfiguracionJuego.ANCHO_PANEL - 120, gradasAlto);
        g.fillRect(60, ConfiguracionJuego.ALTO_PANEL - gradasAlto, ConfiguracionJuego.ANCHO_PANEL - 120, gradasAlto);

        for (int x = 72; x < ConfiguracionJuego.ANCHO_PANEL - 72; x += 34) {
            int pulso = (int) (Math.sin((frameAnimacion + x) * 0.04) * 3.0);
            g.setColor(new Color(220, 80 + (x % 90), 70, 150));
            g.fillOval(x, 14 + pulso, 10, 10);
            g.setColor(new Color(70, 190, 220, 140));
            g.fillOval(x + 8, ConfiguracionJuego.ALTO_PANEL - 26 - pulso, 10, 10);
        }
    }

    private void dibujarEtiquetasJugadores(Graphics2D g, MotorJuego motor) {
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        for (Jugador jugador : motor.getTodosJugadores()) {
            String nombre = jugador.getNombre();
            FontMetrics metrics = g.getFontMetrics();
            int anchoTexto = metrics.stringWidth(nombre);
            int x = jugador.getX() + jugador.getAncho() / 2 - anchoTexto / 2;
            int y = jugador.getY() - 10;
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(x - 6, y - 11, anchoTexto + 12, 16, 8, 8);
            g.setColor(new Color(255, 246, 228));
            g.drawString(nombre, x, y);
        }
    }

    private void dibujarHUD(Graphics2D g, MotorJuego motor) {
        // HUD informativo para marcador y estado basico.
        g.setPaint(new GradientPaint(18, 18, new Color(6, 10, 13, 210), 460, 230, new Color(20, 40, 34, 180)));
        g.fillRoundRect(24, 22, 470, 232, 22, 22);
        g.setColor(new Color(255, 255, 255, 52));
        g.drawRoundRect(24, 22, 470, 232, 22, 22);

        g.setColor(new Color(252, 244, 217));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("MARCADOR " + motor.getGolesLocal() + " - " + motor.getGolesRival(), 42, 54);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.drawString("Meta: " + ConfiguracionJuego.META_GOLES + " goles", 42, 84);
        g.drawString("Jugador: X=" + motor.getJugadorPrincipal().getX() + "  Y=" + motor.getJugadorPrincipal().getY(), 42, 112);
        g.drawString("Balon: " + motor.getPoseedorTexto(), 42, 140);
        g.drawString("Bonus: " + motor.getPuntosBonus(), 42, 168);
        g.drawString("Ultimo gol: " + motor.getResumenUltimoGol(), 42, 196);
        g.drawString("Altura balon: " + String.format("%.1f", motor.getBalon().getAltura()), 42, 224);
        g.drawString("Controles: mantén SPACE para pase y X para tiro", 42, 248);
        if (motor.getTextoSaque() != null && !motor.getTextoSaque().isEmpty()) {
            g.setColor(new Color(255, 219, 94));
            g.drawString("Aviso: " + motor.getTextoSaque(), 250, 84);
        }
    }

    private void dibujarOverlayEstado(Graphics2D g, EstadoJuego estado, String mensaje, MotorJuego motor) {
        // Overlays de menu, pausa y resultado.
        if (estado == EstadoJuego.JUGANDO) {
            return;
        }

        String titulo;
        String subtitulo;
        String detalle;

        if (estado == EstadoJuego.INICIO) {
            titulo = "Futbol Urbano";
            subtitulo = "6 jugadores: 3 vs 3";
            detalle = "ENTER iniciar | P pausa | R reiniciar";
        } else if (estado == EstadoJuego.PAUSADO) {
            titulo = "Pausa";
            subtitulo = "La reta sigue pendiente";
            detalle = "P continuar | R reiniciar";
        } else if (estado == EstadoJuego.GOL) {
            titulo = "Gol";
            subtitulo = motor.getResumenUltimoGol().isEmpty()
                ? (mensaje.isEmpty() ? "Se reinicia la jugada" : mensaje)
                : mensaje + " de " + motor.getUltimoGoleador();
            detalle = "Marcador: " + motor.getGolesLocal() + " - " + motor.getGolesRival();
        } else if (estado == EstadoJuego.VICTORIA) {
            titulo = "Ganaste la reta";
            subtitulo = "Marcador final: " + motor.getGolesLocal() + " - " + motor.getGolesRival();
            detalle = "R para nuevo partido";
        } else {
            titulo = "Derrota";
            subtitulo = "Te ganaron la reta: " + motor.getGolesLocal() + " - " + motor.getGolesRival();
            detalle = "R para revancha";
        }

        g.setColor(new Color(0, 0, 0, 185));
        g.fillRect(0, 0, ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL);

        g.setPaint(new GradientPaint(180, 130, new Color(255, 224, 103), 700, 320, new Color(230, 165, 75)));
        g.fillRoundRect(180, 130, 520, 190, 24, 24);
        g.setColor(new Color(28, 22, 14));
        g.drawRoundRect(180, 130, 520, 190, 24, 24);

        g.setColor(new Color(25, 25, 25));
        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        g.drawString(titulo, 220, 194);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString(subtitulo, 220, 236);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString(detalle, 220, 280);
    }
}
