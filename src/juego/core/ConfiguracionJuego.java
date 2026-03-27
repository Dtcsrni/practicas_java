package juego.core;

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

    // Objetivo del partido.
    public static final int META_GOLES = 8;
    // Duracion real de un partido jugable (en segundos de simulacion).
    public static final int DURACION_PARTIDO_SEGUNDOS = 7 * 60;
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
    public static final int POS_X_BASE_LOCAL = CAMPO_X_MIN + Math.max(92, (int) Math.round((CAMPO_X_MAX - CAMPO_X_MIN) * 0.16));
    public static final int POS_X_BASE_RIVAL = CAMPO_X_MAX - Math.max(126, (int) Math.round((CAMPO_X_MAX - CAMPO_X_MIN) * 0.18));
    public static final int POS_Y_PORTERO = MAPA_CANCHA.getCentroY() - 21;
    public static final int POS_Y_CAMPO_ARRIBA = CAMPO_Y_MIN + Math.max(118, (int) Math.round((CAMPO_Y_MAX - CAMPO_Y_MIN) * 0.34));
    public static final int POS_Y_CAMPO_ABAJO = CAMPO_Y_MIN + Math.max(320, (int) Math.round((CAMPO_Y_MAX - CAMPO_Y_MIN) * 0.76));
}
