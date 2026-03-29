package juego.core;

import java.awt.Color;

// Configuracion central del partido, la cancha y la ventana.
public final class ConfiguracionJuego {
    private ConfiguracionJuego() {
        // Solo expone constantes.
    }

    // Loop principal.
    public static final int FPS = 60;
    // Resolucion base del panel. Se mantiene claramente por debajo del maximo Full HD.
    public static final int ANCHO_PANEL = 1280;
    public static final int ALTO_PANEL = 720;
    public static final GeometriaCancha MAPA_CANCHA = GeometriaCancha.crearReglamentariaEscalada(ANCHO_PANEL, ALTO_PANEL);

    // Paleta y colores reutilizables para HUD y renderizado de cancha
    public static final Color HUD_BG = new Color(16, 18, 24);
    public static final Color HUD_PANEL = new Color(28, 30, 34, 220);
    public static final Color HUD_TEXT = new Color(245, 245, 245);
    public static final Color HUD_ACCENT = new Color(88, 220, 120);
    public static final Color HUD_WARN = new Color(255, 166, 88);
    public static final Color HUD_DANGER = new Color(220, 36, 36);
    public static final Color TEAM_RIVAL = new Color(70, 180, 245);
    public static final Color HUD_INACTIVE = new Color(206, 206, 206);

    // Colores de césped y líneas de cancha
    public static final Color CESPED_A = new Color(92, 168, 96);
    public static final Color CESPED_B = new Color(44, 102, 56);
    public static final Color LINEA_CANCHA = new Color(240, 240, 236);

    // Objetivo del partido.
    public static final int META_GOLES = 8;
    // Duracion real de un partido jugable (en segundos de simulacion).
    public static final int DURACION_PARTIDO_SEGUNDOS = 5 * 60;
    // El reloj mostrado se escala a minutos "futbol" para aproximar 90'.
    public static final int MINUTOS_REGLAMENTARIOS = 90;
    // Geometria de la porteria.
    public static final int ANCHO_PORTERIA = MAPA_CANCHA.getProfundidadPorteria();
    public static final int ALTO_PORTERIA = MAPA_CANCHA.getAlturaPorteria();
    public static final int Y_PORTERIA = MAPA_CANCHA.getPorteriaY();
    // Limites jugables dentro del panel.
    public static final int CAMPO_X_MIN = MAPA_CANCHA.getCampoXMin();
    public static final int CAMPO_Y_MIN = MAPA_CANCHA.getCampoYMin();
    public static final int CAMPO_X_MAX = MAPA_CANCHA.getCampoXMax();
    public static final int CAMPO_Y_MAX = MAPA_CANCHA.getCampoYMax();

    // Duracion de mensajes temporales.
    public static final int FRAMES_MENSAJE_GOL = FPS;
    public static final int FRAMES_MENSAJE_FALTA = (int) (FPS * 1.2);

    // Hidratacion en banca (recarga cuando un jugador agotado llega al punto).
    public static final int COOLDOWN_HIDRATACION_BANCA = FPS * 3;
    public static final double RECARGA_HIDRATACION_BANCA = 18.0;
    public static final int USOS_HIDRATACION_BANCA = 9;
    public static final int INTERVALO_TURBO = FPS * 10;
    public static final int DURACION_TURBO_EN_ESCENARIO = FPS * 6;

    // Posiciones iniciales de la formacion 3 vs 3.
    public static final int ANCHO_CAMPO = CAMPO_X_MAX - CAMPO_X_MIN;
    public static final int ALTO_CAMPO = CAMPO_Y_MAX - CAMPO_Y_MIN;
    public static final int POS_X_BASE_LOCAL = CAMPO_X_MIN + Math.max(92, (int) Math.round(ANCHO_CAMPO * 0.15));
    public static final int POS_X_BASE_RIVAL = CAMPO_X_MAX - Math.max(118, (int) Math.round(ANCHO_CAMPO * 0.15));
    public static final int POS_Y_PORTERO = MAPA_CANCHA.getCentroY() - 21;
    public static final int POS_Y_CAMPO_ARRIBA = CAMPO_Y_MIN + Math.max(72, (int) Math.round(ALTO_CAMPO * 0.28));
    public static final int POS_Y_CAMPO_ABAJO = CAMPO_Y_MIN + Math.max(214, (int) Math.round(ALTO_CAMPO * 0.60));
    public static final int OFFSET_X_APOYO_LOCAL = Math.max(38, (int) Math.round(ANCHO_CAMPO * 0.05));
    public static final int OFFSET_X_EXTREMO_LOCAL = Math.max(70, (int) Math.round(ANCHO_CAMPO * 0.085));
    public static final int OFFSET_X_MEDIA_LOCAL = Math.max(18, (int) Math.round(ANCHO_CAMPO * 0.03));
    public static final int OFFSET_X_RIVAL_UNO = Math.max(24, (int) Math.round(ANCHO_CAMPO * 0.04));
    public static final int OFFSET_X_EXTREMO_RIVAL = Math.max(40, (int) Math.round(ANCHO_CAMPO * 0.055));
    public static final int OFFSET_X_MEDIA_RIVAL = Math.max(8, (int) Math.round(ANCHO_CAMPO * 0.016));
    public static final int OFFSET_Y_EXTREMO = Math.max(50, (int) Math.round(ALTO_CAMPO * 0.17));
    public static final int OFFSET_Y_MEDIA = Math.max(42, (int) Math.round(ALTO_CAMPO * 0.14));
    public static final int[] Y_ENTRADA_LOCALES = {
        POS_Y_PORTERO,
        CAMPO_Y_MIN + Math.max(52, (int) Math.round(ALTO_CAMPO * 0.18)),
        CAMPO_Y_MIN + Math.max(132, (int) Math.round(ALTO_CAMPO * 0.36)),
        CAMPO_Y_MIN + Math.max(212, (int) Math.round(ALTO_CAMPO * 0.56)),
        CAMPO_Y_MIN + Math.max(292, (int) Math.round(ALTO_CAMPO * 0.78))
    };
    public static final int[] Y_ENTRADA_RIVALES = {
        POS_Y_PORTERO,
        CAMPO_Y_MIN + Math.max(292, (int) Math.round(ALTO_CAMPO * 0.78)),
        CAMPO_Y_MIN + Math.max(212, (int) Math.round(ALTO_CAMPO * 0.56)),
        CAMPO_Y_MIN + Math.max(132, (int) Math.round(ALTO_CAMPO * 0.36)),
        CAMPO_Y_MIN + Math.max(52, (int) Math.round(ALTO_CAMPO * 0.18))
    };
}
