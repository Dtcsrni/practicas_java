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

    // Objetivo del partido.
    public static final int META_GOLES = 8;
    // Duracion real de un partido jugable (en segundos de simulacion).
    public static final int DURACION_PARTIDO_SEGUNDOS = 7 * 60;
    // El reloj mostrado se escala a minutos "futbol" para aproximar 90'.
    public static final int MINUTOS_REGLAMENTARIOS = 90;
    // Geometria de la porteria.
    public static final int ANCHO_PORTERIA = 14;
    public static final int ALTO_PORTERIA = 188;
    public static final int Y_PORTERIA = (ALTO_PANEL - ALTO_PORTERIA) / 2;
    // Limites jugables dentro del panel.
    public static final int CAMPO_X_MIN = 42;
    public static final int CAMPO_Y_MIN = 34;
    public static final int CAMPO_X_MAX = ANCHO_PANEL - 42;
    public static final int CAMPO_Y_MAX = ALTO_PANEL - 34;

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
    public static final int POS_X_BASE_LOCAL = 160;
    public static final int POS_X_BASE_RIVAL = ANCHO_PANEL - 210;
    public static final int POS_Y_PORTERO = ALTO_PANEL / 2 - 26;
    public static final int POS_Y_CAMPO_ARRIBA = 270;
    public static final int POS_Y_CAMPO_ABAJO = 530;
}
