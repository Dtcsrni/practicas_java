package juego.core;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.Random;

import juego.entidades.Balon;
import juego.entidades.EntidadJuego;
import juego.entidades.HidratacionBanca;
import juego.entidades.Jugador;
import juego.entidades.Turbo;
import juego.sonido.TipoSonido;

// Nucleo de la simulacion: mueve jugadores, resuelve posesion y actualiza el partido.
public class MotorJuego {
    private static final boolean REGLAS_CALLEJERAS = true;
    private static final String[] NOMBRES_LOCALES = {
        // Referencias recientes/recordadas de Pachuca.
        "Lozano", "E. Gutierrez", "V. Guzman", "L. Chavez", "E. Sanchez",
        "K. Alvarez", "Ustari", "F. Jara", "N. Ibanez", "Rondon"
    };
    private static final String[] NOMBRES_RIVALES = {
        // Referencias de Seleccion Mexicana.
        "Ochoa", "Montes", "Gallardo", "Edson", "Orbelin",
        "R. Jimenez", "S. Gimenez", "A. Vega", "J. Quinones", "Cesar Huerta"
    };
    private static final String[] NOMBRES_ARBITRO = {
        "Arbitro", "Silva", "Ramos", "Cedillo", "Luna"
    };
    private static final String[] FRASES_GOL = {
        "⚽ Golazo de %s para %s",
        "🔥 %s la manda a guardar para %s",
        "💥 Definicion brutal de %s (%s)",
        "🎯 %s rompe el arco y festeja %s",
        "🚀 %s define con categoria para %s",
        "🥅 %s aparece y castiga para %s",
        "📣 Se grita fuerte: %s marca para %s"
    };
    private static final String[] FRASES_ROBO = {
        "🛡️ %s roba limpio y sale jugando",
        "👣 Gran quite de %s",
        "⚔️ %s mete pie y recupera",
        "🧲 %s se queda con la pelota",
        "🔒 %s cierra el paso y recupera",
        "💨 %s anticipa y sale disparado"
    };
    private static final int DURACION_NARRACION_FRAMES = ConfiguracionJuego.FPS * 3;
    private static final int COOLDOWN_NARRACION_FRAMES = ConfiguracionJuego.FPS / 2;
    // Estos umbrales afinan el "feeling" del juego:
    // control del balon, fuerza de pases/tiros, atajadas y agresividad de NPCs.
    private static final double FUERZA_PASE_MIN = 2.8;
    private static final double FUERZA_PASE_MAX = 4.6;
    private static final double FUERZA_TIRO_MIN = 4.8;
    private static final double FUERZA_TIRO_MAX = 7.1;
    private static final double VELOCIDAD_MAXIMA_PARA_CONTROL = 2.25;
    private static final double DISTANCIA_MAXIMA_CONTROL = 42.0;
    private static final double DISTANCIA_MAXIMA_POSESION = 82.0;
    private static final double ALTURA_MAXIMA_CONTROL = 8.0;
    private static final double ALTURA_MAXIMA_GOL = 30.0;
    private static final double ALTURA_MINIMA_TRAVESANO = 28.0;
    private static final double ALTURA_MAXIMA_TRAVESANO = 38.0;
    private static final double ALTURA_MAXIMA_ATAJADA = 32.0;
    private static final double DISTANCIA_ATAJADA_PORTERO = 46.0;
    private static final double DISTANCIA_DESVIO_PORTERO = 66.0;
    private static final double DISTANCIA_SALIDA_PORTERO = 248.0;
    private static final double ALCANCE_X_PORTERO = 214.0;
    private static final double ALCANCE_Y_PORTERO = 148.0;
    private static final int LOOKAHEAD_ATAJADA_FRAMES = 14;
    private static final double FACTOR_ATAJADA_MIN = 0.52;
    private static final double FACTOR_ATAJADA_MAX = 0.74;
    private static final int RECUPERACION_LANZADA_PORTERO_FRAMES = ConfiguracionJuego.FPS / 2;
    private static final double GRAVEDAD_BALON = 0.36;
    private static final int MARGEN_REANUDACION = 72;
    private static final double ARRASTRE_BALON = 0.34;
    private static final double APORTE_MOVIMIENTO_POSEEDOR = 0.20;
    private static final double IMPULSO_BASE_MOVIMIENTO = 0.34;
    private static final int COOLDOWN_DECISION_NPC = 18;
    private static final int RETRASO_SAQUE_FRAMES = 24;
    private static final int RETRASO_SAQUE_GOL_FRAMES = ConfiguracionJuego.FPS / 2;
    private static final int FRAMES_BOTE_INICIAL = ConfiguracionJuego.FPS;
    private static final int FRAME_SOLTAR_BOTE_INICIAL = ConfiguracionJuego.FPS / 3;
    private static final int DURACION_ACCION_ARBITRO_CORTA_FRAMES = ConfiguracionJuego.FPS / 2;
    private static final int DURACION_ACCION_ARBITRO_MEDIA_FRAMES = ConfiguracionJuego.FPS;
    private static final int DURACION_ACCION_ARBITRO_LARGA_FRAMES = (int) (ConfiguracionJuego.FPS * 1.6);
    private static final int DURACION_BARRIDA_FRAMES = 8;
    private static final int COOLDOWN_BARRIDA_FRAMES = ConfiguracionJuego.FPS;
    private static final double DISTANCIA_BARRIDA_MAX = 78.0;
    private static final double DISTANCIA_IMPACTO_BARRIDA = 44.0;
    private static final double IMPULSO_BARRIDA_BASE = 4.3;
    private static final double COSTO_STAMINA_BARRIDA = 7.5;
    private static final double DISTANCIA_PRESION_ALTA = 86.0;
    private static final double DISTANCIA_PASE_SEGURA = 250.0;
    private static final double DISTANCIA_PASE_NPC = 170.0;
    private static final double DISTANCIA_TIRO_CLARA = 210.0;
    private static final int LOOKAHEAD_BALON_LIBRE_FRAMES = 28;
    private static final double FRICCION_ESTIMADA_BALON_LIBRE = 0.979;
    private static final double FRICCION_ESTIMADA_BALON_AEREO = 0.993;
    private static final double VELOCIDAD_VERTICAL_MINIMA_CONTROL = 1.65;
    private static final double ALTURA_MAXIMA_CONTROL_DESCENSO = 15.0;
    private static final int LOOKAHEAD_ATERRIZAJE_BALON_FRAMES = 44;
    private static final double PESO_ANTICIPACION_BALON = 0.74;
    private static final double DISTANCIA_MAXIMA_MARCA_PASE = 260.0;
    private static final double FACTOR_ORDEN_TACTICO = 1.22;
    private static final int FRAMES_BLOQUEO_POSEEDOR = 30;
    private static final double MAX_ADELANTO_GOLPEO = 12.0;
    private static final int RETARDO_REACCION_ATAJADA_FRAMES = 4;
    private static final int DURACION_SORTEO_MONEDA_FRAMES = ConfiguracionJuego.FPS * 3;
    private static final int DURACION_CEREMONIA_INICIO_FRAMES = (int) (ConfiguracionJuego.FPS * 1.8);
    private static final int VELOCIDAD_REUBICACION_SUAVE = 6;
    private static final int DURACION_PARTIDO_FRAMES = ConfiguracionJuego.DURACION_PARTIDO_SEGUNDOS * ConfiguracionJuego.FPS;
    private static final int FRAMES_UMBRAL_BAJA_DINAMICA = ConfiguracionJuego.FPS * 2;
    private static final int FRAMES_RITMO_ALTO = ConfiguracionJuego.FPS * 3;
    private static final int FRAMES_TRANSICION_EQUIPO = ConfiguracionJuego.FPS * 3;
    private static final int MOVIMIENTO_COLECTIVO_MINIMO = 15;
    private static final double RAPIDEZ_BALON_MINIMA_DINAMICA = 0.65;
    private static final double IMPULSO_REACTIVACION_BALON = 2.3;
    private static final int MARGEN_OFFSIDE_NPC = 12;
    private static final int Y_APERTURA_SUPERIOR = ConfiguracionJuego.CAMPO_Y_MIN + 130;
    private static final int Y_APERTURA_INFERIOR = ConfiguracionJuego.CAMPO_Y_MAX - 130;
    private static final int FRAMES_MAX_CONDUCCION_NPC = (int) (ConfiguracionJuego.FPS * 1.6);
    private static final int FRAMES_MAX_POSESION_SIN_ACCION_NPC = ConfiguracionJuego.FPS * 3;
    private static final int MARGEN_OBJETIVO_INACTIVO_NPC = 6;

    private final Random aleatorio;
    private final GeometriaCancha cancha;
    private final Rectangle arcoIzquierdo;
    private final Rectangle arcoDerecho;

    private final Jugador porteroLocal;
    private final Jugador jugadorPrincipal;
    private final Jugador aliadoLocal;
    private final Jugador extremoLocal;
    private final Jugador mediaLocal;

    private final Jugador porteroRival;
    private final Jugador rivalUno;
    private final Jugador rivalDos;
    private final Jugador extremoRival;
    private final Jugador mediaRival;
    private final Jugador arbitro;
    private final Jugador[] jugadoresLocales;
    private final Jugador[] jugadoresRivales;
    private final Jugador[] todosJugadores;
    private final Jugador[] actoresConObjetivo;
    private final int[] objetivoJugadorX;
    private final int[] objetivoJugadorY;
    private final boolean[] objetivoJugadorActivo;
    private final int[] framesBarridaActiva;
    private final int[] cooldownBarridaJugadorFrames;
    private final double[] barridaDireccionX;
    private final double[] barridaDireccionY;

    private final Balon balon;
    private final HidratacionBanca hidratacionBanca;
    private final Turbo turbo;
    private final ArrayDeque<TipoSonido> sonidosPendientes;

    private Jugador poseedorBalon;
    private boolean poseedorEsLocal;
    private boolean balonLibre;

    private int movPrincipalX;
    private int movPrincipalY;
    private int movAliadoX;
    private int movAliadoY;
    private int movRivalUnoX;
    private int movRivalUnoY;
    private int movRivalDosX;
    private int movRivalDosY;
    private int movExtremoLocalX;
    private int movExtremoLocalY;
    private int movExtremoRivalX;
    private int movExtremoRivalY;
    private int movMediaLocalX;
    private int movMediaLocalY;
    private int movMediaRivalX;
    private int movMediaRivalY;
    private int movPorteroLocalX;
    private int movPorteroLocalY;
    private int movPorteroRivalX;
    private int movPorteroRivalY;
    private int movArbitroX;
    private int movArbitroY;
    private boolean sprintPrincipal;
    private boolean sprintAliado;
    private boolean sprintRivalUno;
    private boolean sprintRivalDos;
    private boolean sprintExtremoLocal;
    private boolean sprintExtremoRival;
    private boolean sprintMediaLocal;
    private boolean sprintMediaRival;
    private boolean sprintPorteroLocal;
    private boolean sprintPorteroRival;

    private int golesLocal;
    private int golesRival;
    private int puntosBonus;
    private int puntosBonusRival;
    private int contadorAparicionTurbo;
    private int framesTurboRestantesEnEscenario;
    private int cooldownRoboFrames;
    private int cooldownCapturaLibreFrames;
    private int cooldownDecisionNpcFrames;
    private int cooldownDecisionAliadoFrames;
    private int cooldownDecisionRivalUnoFrames;
    private int cooldownDecisionRivalDosFrames;
    private int cooldownDecisionExtremoLocalFrames;
    private int cooldownDecisionExtremoRivalFrames;
    private int cooldownDecisionMediaLocalFrames;
    private int cooldownDecisionMediaRivalFrames;
    private int cooldownAtajadaPorteroFrames;
    private int recuperacionPorteroLocalFrames;
    private int recuperacionPorteroRivalFrames;
    private int framesPartidoJugados;
    private int framesDesdeUltimoDisparo;
    private double errorLecturaPorteroLocal;
    private double errorLecturaPorteroRival;
    private int cooldownLecturaPorteroLocalFrames;
    private int cooldownLecturaPorteroRivalFrames;
    private int framesPrioridadSaquePortero;
    private Jugador ultimoPateador;
    private int bloqueoRecapturaUltimoPateadorFrames;
    private int framesAnimacion;
    private int framesMomentumLocal;
    private int framesMomentumRival;
    private int framesVentanaRecepcionPase;
    private boolean ultimoToqueLocal;
    private boolean ultimoPaseLocal;
    private Jugador ultimoPasador;
    private String textoSaque;
    private int framesTextoSaque;
    private TipoReanudacion tipoReanudacionPendiente;
    private Jugador ejecutorReanudacion;
    private boolean saquePendienteLocal;
    private int saquePendienteX;
    private int saquePendienteY;
    private int framesRetrasoSaque;
    private int framesEsperaReanudacion;
    private boolean boteInicialPendiente;
    private boolean boteInicialSoltado;
    private boolean boteInicialLocal;
    private int framesBoteInicial;
    private boolean balonEnManos;
    private boolean sorteoMonedaActivo;
    private boolean ceremoniaInicioActiva;
    private boolean partidoFinalizadoPorTiempo;
    private boolean escenaFinalActiva;
    private boolean modoEspectador;
    private boolean hidratacionAgotadaAnunciada;
    private int framesSorteoMoneda;
    private int framesCeremoniaInicio;
    private int framesAnimacionMoneda;
    private boolean primerSaqueLocal;
    private boolean monedaFueCara;
    private boolean ganadorSorteoRevelado;
    private String resultadoMoneda;
    private int framesPoseedorAtascado;
    private Jugador poseedorControlAccionNpc;
    private int framesPoseedorSinAccionNpc;
    private String ultimoGoleador;
    private String ultimoEquipoGoleador;
    private String narracionActual;
    private int framesNarracion;
    private int cooldownNarracion;
    private EstadoArbitraje estadoArbitrajeActual;
    private int framesAccionArbitro;
    private double objetivoArbitroX;
    private double objetivoArbitroY;
    private int framesSinDinamismo;
    private int framesRitmoAlto;
    private int framesTransicionLocal;
    private int framesTransicionRival;
    private PlanAtaque planAtaqueLocal;
    private PlanAtaque planAtaqueRival;
    private PlanDefensa planDefensaLocal;
    private PlanDefensa planDefensaRival;
    private int framesPlanLocal;
    private int framesPlanRival;
    private EventoJuego eventoTransitorio;

    public MotorJuego() {
        // Geometria de gol usada para goles, travesano y saques.
        aleatorio = new Random();
        cancha = ConfiguracionJuego.MAPA_CANCHA;
        // Los arcos coinciden con la linea final jugable.
        arcoIzquierdo = new Rectangle(cancha.getPorteria(true));
        arcoDerecho = new Rectangle(cancha.getPorteria(false));

        // Plantillas fijas del 5 vs 5.
        porteroLocal = new Jugador(
            "Memo",
            20,
            ConfiguracionJuego.POS_Y_PORTERO,
            30,
            42,
            3,
            new Color(255, 170, 30),
            Color.WHITE,
            new Color(150, 70, 0)
        );
        jugadorPrincipal = new Jugador(
            "Leo",
            ConfiguracionJuego.POS_X_BASE_LOCAL,
            ConfiguracionJuego.POS_Y_CAMPO_ARRIBA,
            34,
            44,
            4,
            new Color(220, 70, 50),
            new Color(245, 235, 220),
            new Color(90, 20, 20)
        );
        aliadoLocal = new Jugador(
            "Rafa",
            ConfiguracionJuego.POS_X_BASE_LOCAL + 45,
            ConfiguracionJuego.POS_Y_CAMPO_ABAJO,
            32,
            42,
            4,
            new Color(210, 85, 60),
            new Color(245, 235, 220),
            new Color(90, 20, 20)
        );
        extremoLocal = new Jugador(
            "Nico",
            ConfiguracionJuego.POS_X_BASE_LOCAL + 78,
            ConfiguracionJuego.POS_Y_CAMPO_ARRIBA + 84,
            32,
            42,
            4,
            new Color(200, 95, 65),
            new Color(245, 235, 220),
            new Color(90, 20, 20)
        );
        mediaLocal = new Jugador(
            "Erick",
            ConfiguracionJuego.POS_X_BASE_LOCAL + 24,
            ConfiguracionJuego.POS_Y_CAMPO_ABAJO + 72,
            32,
            42,
            4,
            new Color(206, 98, 62),
            new Color(245, 235, 220),
            new Color(90, 20, 20)
        );

        porteroRival = new Jugador(
            "Bruno",
            ConfiguracionJuego.ANCHO_PANEL - 50,
            ConfiguracionJuego.POS_Y_PORTERO,
            30,
            42,
            3,
            new Color(30, 200, 220),
            Color.WHITE,
            new Color(0, 80, 95)
        );
        rivalUno = new Jugador(
            "Tono",
            ConfiguracionJuego.POS_X_BASE_RIVAL - 30,
            ConfiguracionJuego.POS_Y_CAMPO_ARRIBA,
            32,
            42,
            4,
            new Color(60, 120, 190),
            Color.WHITE,
            new Color(10, 25, 60)
        );
        rivalDos = new Jugador(
            "Dario",
            ConfiguracionJuego.POS_X_BASE_RIVAL,
            ConfiguracionJuego.POS_Y_CAMPO_ABAJO,
            32,
            42,
            4,
            new Color(70, 135, 200),
            Color.WHITE,
            new Color(10, 25, 60)
        );
        extremoRival = new Jugador(
            "C. Huerta",
            ConfiguracionJuego.POS_X_BASE_RIVAL - 44,
            ConfiguracionJuego.POS_Y_CAMPO_ARRIBA + 94,
            32,
            42,
            4,
            new Color(76, 142, 206),
            Color.WHITE,
            new Color(10, 25, 60)
        );
        mediaRival = new Jugador(
            "Romo",
            ConfiguracionJuego.POS_X_BASE_RIVAL - 10,
            ConfiguracionJuego.POS_Y_CAMPO_ABAJO + 68,
            32,
            42,
            4,
            new Color(80, 146, 210),
            Color.WHITE,
            new Color(10, 25, 60)
        );
        arbitro = new Jugador(
            "Arbitro",
            ConfiguracionJuego.ANCHO_PANEL / 2 - 14,
            ConfiguracionJuego.ALTO_PANEL / 2 - 18,
            28,
            40,
            3,
            new Color(34, 34, 34),
            new Color(245, 245, 245),
            new Color(255, 214, 64)
        );
        jugadoresLocales = new Jugador[] { porteroLocal, jugadorPrincipal, aliadoLocal, extremoLocal, mediaLocal };
        jugadoresRivales = new Jugador[] { porteroRival, rivalUno, rivalDos, extremoRival, mediaRival };
        todosJugadores = new Jugador[] {
            porteroLocal, jugadorPrincipal, aliadoLocal, extremoLocal, mediaLocal,
            porteroRival, rivalUno, rivalDos, extremoRival, mediaRival
        };
        actoresConObjetivo = new Jugador[] {
            porteroLocal, jugadorPrincipal, aliadoLocal, extremoLocal, mediaLocal,
            porteroRival, rivalUno, rivalDos, extremoRival, mediaRival, arbitro
        };
        objetivoJugadorX = new int[actoresConObjetivo.length];
        objetivoJugadorY = new int[actoresConObjetivo.length];
        objetivoJugadorActivo = new boolean[actoresConObjetivo.length];
        framesBarridaActiva = new int[todosJugadores.length];
        cooldownBarridaJugadorFrames = new int[todosJugadores.length];
        barridaDireccionX = new double[todosJugadores.length];
        barridaDireccionY = new double[todosJugadores.length];

        balon = new Balon(ConfiguracionJuego.ANCHO_PANEL / 2 - 10, ConfiguracionJuego.ALTO_PANEL / 2 - 10, 20);
        int hidratacionX = ConfiguracionJuego.ANCHO_PANEL / 2 - 26;
        int hidratacionY = ConfiguracionJuego.CAMPO_Y_MIN + 10;
        hidratacionBanca = new HidratacionBanca(hidratacionX, hidratacionY, 52, 28);
        turbo = new Turbo();
        sonidosPendientes = new ArrayDeque<>();
        reiniciarPartido();
    }

    public void reiniciarPartido() {
        // Reinicio completo del partido y sus contadores.
        golesLocal = 0;
        golesRival = 0;
        puntosBonus = 0;
        puntosBonusRival = 0;
        contadorAparicionTurbo = 0;
        framesTurboRestantesEnEscenario = 0;
        cooldownRoboFrames = 0;
        cooldownCapturaLibreFrames = 0;
        cooldownDecisionNpcFrames = 0;
        cooldownDecisionAliadoFrames = 0;
        cooldownDecisionRivalUnoFrames = 0;
        cooldownDecisionRivalDosFrames = 0;
        cooldownDecisionExtremoLocalFrames = 0;
        cooldownDecisionExtremoRivalFrames = 0;
        cooldownDecisionMediaLocalFrames = 0;
        cooldownDecisionMediaRivalFrames = 0;
        cooldownAtajadaPorteroFrames = 0;
        recuperacionPorteroLocalFrames = 0;
        recuperacionPorteroRivalFrames = 0;
        framesPartidoJugados = 0;
        framesDesdeUltimoDisparo = RETARDO_REACCION_ATAJADA_FRAMES + 12;
        errorLecturaPorteroLocal = 0.0;
        errorLecturaPorteroRival = 0.0;
        cooldownLecturaPorteroLocalFrames = 0;
        cooldownLecturaPorteroRivalFrames = 0;
        framesPrioridadSaquePortero = 0;
        tipoReanudacionPendiente = TipoReanudacion.NINGUNA;
        ejecutorReanudacion = null;
        framesRetrasoSaque = 0;
        framesEsperaReanudacion = 0;
        boteInicialPendiente = false;
        boteInicialSoltado = false;
        boteInicialLocal = true;
        framesBoteInicial = 0;
        balonEnManos = false;
        sorteoMonedaActivo = false;
        ceremoniaInicioActiva = false;
        partidoFinalizadoPorTiempo = false;
        escenaFinalActiva = false;
        hidratacionAgotadaAnunciada = false;
        framesSorteoMoneda = 0;
        framesCeremoniaInicio = 0;
        framesAnimacionMoneda = 0;
        primerSaqueLocal = true;
        monedaFueCara = true;
        ganadorSorteoRevelado = false;
        resultadoMoneda = "";
        framesPoseedorAtascado = 0;
        ultimoPateador = null;
        bloqueoRecapturaUltimoPateadorFrames = 0;
        framesAnimacion = 0;
        framesMomentumLocal = 0;
        framesMomentumRival = 0;
        framesVentanaRecepcionPase = 0;
        ultimoToqueLocal = true;
        ultimoPaseLocal = true;
        ultimoPasador = null;
        textoSaque = "";
        framesTextoSaque = 0;
        limpiarReaccionesFinales();
        ultimoGoleador = "";
        ultimoEquipoGoleador = "";
        narracionActual = "";
        framesNarracion = 0;
        cooldownNarracion = 0;
        estadoArbitrajeActual = EstadoArbitraje.OBSERVA;
        framesAccionArbitro = 0;
        objetivoArbitroX = ConfiguracionJuego.ANCHO_PANEL / 2.0;
        objetivoArbitroY = ConfiguracionJuego.ALTO_PANEL / 2.0 - 44.0;
        framesSinDinamismo = 0;
        framesRitmoAlto = 0;
        planAtaqueLocal = PlanAtaque.POSICIONAL;
        planAtaqueRival = PlanAtaque.POSICIONAL;
        planDefensaLocal = PlanDefensa.BLOQUE_MEDIO;
        planDefensaRival = PlanDefensa.BLOQUE_MEDIO;
        framesPlanLocal = 0;
        framesPlanRival = 0;
        eventoTransitorio = EventoJuego.NINGUNO;
        for (int i = 0; i < framesBarridaActiva.length; i++) {
            framesBarridaActiva[i] = 0;
            cooldownBarridaJugadorFrames[i] = 0;
            barridaDireccionX[i] = 0.0;
            barridaDireccionY[i] = 0.0;
        }
        limpiarObjetivosJugadores();
        hidratacionBanca.reiniciar();
        turbo.desactivar();
        configurarPlantillasPartido();
        iniciarSorteoInicial();
    }

    private void configurarPlantillasPartido() {
        // En cada partido rota nombres ("roster"), atributos y uniformes.
        asignarNombresPartido();
        asignarAtributosBalanceados();
        asignarPerfilArbitro();
        aplicarUniformesPartido();
    }

    private void asignarNombresPartido() {
        porteroLocal.setNombre(nombreAleatorioDistinto(NOMBRES_LOCALES));
        jugadorPrincipal.setNombre(nombreAleatorioDistinto(NOMBRES_LOCALES, porteroLocal.getNombre()));
        aliadoLocal.setNombre(nombreAleatorioDistinto(NOMBRES_LOCALES, porteroLocal.getNombre(), jugadorPrincipal.getNombre()));
        extremoLocal.setNombre(nombreAleatorioDistinto(NOMBRES_LOCALES, porteroLocal.getNombre(), jugadorPrincipal.getNombre(), aliadoLocal.getNombre()));
        mediaLocal.setNombre(nombreAleatorioDistinto(NOMBRES_LOCALES, porteroLocal.getNombre(), jugadorPrincipal.getNombre(), aliadoLocal.getNombre(), extremoLocal.getNombre()));

        porteroRival.setNombre(nombreAleatorioDistinto(NOMBRES_RIVALES));
        rivalUno.setNombre(nombreAleatorioDistinto(NOMBRES_RIVALES, porteroRival.getNombre()));
        rivalDos.setNombre(nombreAleatorioDistinto(NOMBRES_RIVALES, porteroRival.getNombre(), rivalUno.getNombre()));
        extremoRival.setNombre(nombreAleatorioDistinto(NOMBRES_RIVALES, porteroRival.getNombre(), rivalUno.getNombre(), rivalDos.getNombre()));
        mediaRival.setNombre(nombreAleatorioDistinto(NOMBRES_RIVALES, porteroRival.getNombre(), rivalUno.getNombre(), rivalDos.getNombre(), extremoRival.getNombre()));

        arbitro.setNombre(nombreAleatorioDistinto(NOMBRES_ARBITRO));
    }

    private String nombreAleatorioDistinto(String[] pool, String... usados) {
        String elegido = pool[aleatorio.nextInt(pool.length)];
        int intentos = 0;
        while (nombreYaUsado(elegido, usados) && intentos < 18) {
            elegido = pool[aleatorio.nextInt(pool.length)];
            intentos++;
        }
        return elegido;
    }

    private boolean nombreYaUsado(String nombre, String[] usados) {
        if (usados == null) {
            return false;
        }
        for (String usado : usados) {
            if (usado != null && usado.equals(nombre)) {
                return true;
            }
        }
        return false;
    }

    private void asignarAtributosBalanceados() {
        // Balancea por pares: si uno sube, el opuesto baja en la misma magnitud.
        asignarParBalanceado(porteroLocal, porteroRival, 2, 102.0, 58, 42, 76);
        asignarParBalanceado(jugadorPrincipal, rivalUno, 3, 100.0, 52, 61, 56);
        asignarParBalanceado(aliadoLocal, rivalDos, 3, 100.0, 52, 64, 60);
        asignarParBalanceado(extremoLocal, extremoRival, 3, 98.0, 54, 58, 53);
        asignarParBalanceado(mediaLocal, mediaRival, 3, 99.0, 55, 66, 63);
    }

    private void asignarPerfilArbitro() {
        arbitro.setVelocidad(3);
        arbitro.setStaminaMax(110.0);
        arbitro.recargarStaminaCompleta();
        arbitro.setInteligencia(52 + aleatorio.nextInt(25));
        arbitro.setEntrada(40 + aleatorio.nextInt(12));
        arbitro.setDisciplina(48 + aleatorio.nextInt(28));
        arbitro.limpiarDisciplina();
    }

    private void asignarParBalanceado(Jugador a, Jugador b, int velocidadBase, double staminaBase, int inteligenciaBase, int entradaBase, int disciplinaBase) {
        int deltaVel = aleatorio.nextInt(3) - 1; // -1..1
        double deltaStamina = aleatorio.nextDouble() * 12.0 - 6.0; // -6..6
        int deltaInteligencia = aleatorio.nextInt(13) - 6; // -6..6
        int deltaEntrada = aleatorio.nextInt(13) - 6; // -6..6
        int deltaDisciplina = aleatorio.nextInt(13) - 6; // -6..6

        int velA = Math.max(2, Math.min(4, velocidadBase + deltaVel));
        int velB = Math.max(2, Math.min(4, velocidadBase - deltaVel));
        double stamA = Math.max(88.0, Math.min(112.0, staminaBase + deltaStamina));
        double stamB = Math.max(88.0, Math.min(112.0, staminaBase - deltaStamina));
        int intelA = Math.max(40, Math.min(80, inteligenciaBase + deltaInteligencia));
        int intelB = Math.max(40, Math.min(80, inteligenciaBase - deltaInteligencia));
        int entradaA = Math.max(38, Math.min(82, entradaBase + deltaEntrada));
        int entradaB = Math.max(38, Math.min(82, entradaBase - deltaEntrada));
        int disciplinaA = Math.max(38, Math.min(82, disciplinaBase + deltaDisciplina));
        int disciplinaB = Math.max(38, Math.min(82, disciplinaBase - deltaDisciplina));

        a.setVelocidad(velA);
        b.setVelocidad(velB);
        a.setStaminaMax(stamA);
        b.setStaminaMax(stamB);
        a.recargarStaminaCompleta();
        b.recargarStaminaCompleta();
        a.setInteligencia(intelA);
        b.setInteligencia(intelB);
        a.setEntrada(entradaA);
        b.setEntrada(entradaB);
        a.setDisciplina(disciplinaA);
        b.setDisciplina(disciplinaB);
        a.limpiarDisciplina();
        b.limpiarDisciplina();
    }

    private void aplicarUniformesPartido() {
        // Local: Pachuca (azul/blanco). Rival: Seleccion Mexicana (verde/blanco/rojo).
        aplicarUniformePachuca(jugadoresLocales);
        aplicarUniformeSeleccionMexicana(jugadoresRivales);
        arbitro.setColores(new Color(34, 34, 34), new Color(245, 245, 245), new Color(255, 214, 64));
    }

    private void aplicarUniformePachuca(Jugador[] equipo) {
        int variacion = aleatorio.nextInt(18) - 9;
        for (int i = 0; i < equipo.length; i++) {
            Jugador jugador = equipo[i];
            boolean esPorteroEquipo = esPortero(jugador);
            Color cuerpo;
            Color borde;
            Color detalle;
            if (esPorteroEquipo) {
                cuerpo = new Color(clamp(255 + variacion), clamp(173 + variacion), clamp(35 + variacion));
                borde = new Color(245, 245, 245);
                detalle = new Color(clamp(130 + variacion), clamp(65 + variacion), 0);
            } else {
                cuerpo = new Color(clamp(28 + variacion), clamp(102 + variacion), clamp(196 + variacion));
                borde = new Color(245, 245, 245);
                detalle = i % 2 == 0
                    ? new Color(clamp(232 + variacion), clamp(232 + variacion), clamp(232 + variacion))
                    : new Color(clamp(16 + variacion), clamp(50 + variacion), clamp(120 + variacion));
            }
            jugador.setColores(cuerpo, borde, detalle);
        }
    }

    private void aplicarUniformeSeleccionMexicana(Jugador[] equipo) {
        int variacion = aleatorio.nextInt(18) - 9;
        for (int i = 0; i < equipo.length; i++) {
            Jugador jugador = equipo[i];
            boolean esPorteroEquipo = esPortero(jugador);
            Color cuerpo;
            Color borde;
            Color detalle;
            if (esPorteroEquipo) {
                cuerpo = new Color(clamp(70 + variacion), clamp(170 + variacion), clamp(210 + variacion));
                borde = new Color(245, 245, 245);
                detalle = new Color(clamp(0 + variacion), clamp(72 + variacion), clamp(95 + variacion));
            } else {
                cuerpo = new Color(clamp(22 + variacion), clamp(132 + variacion), clamp(78 + variacion));
                borde = new Color(245, 245, 245);
                detalle = i % 2 == 0
                    ? new Color(clamp(186 + variacion), clamp(30 + variacion), clamp(48 + variacion))
                    : new Color(clamp(235 + variacion), clamp(235 + variacion), clamp(235 + variacion));
            }
            jugador.setColores(cuerpo, borde, detalle);
        }
    }

    private int clamp(int valor) {
        return Math.max(0, Math.min(255, valor));
    }

    private void limpiarObjetivosJugadores() {
        for (int i = 0; i < objetivoJugadorActivo.length; i++) {
            objetivoJugadorActivo[i] = false;
        }
    }

    private int indiceActorObjetivo(Jugador jugador) {
        for (int i = 0; i < actoresConObjetivo.length; i++) {
            if (actoresConObjetivo[i] == jugador) {
                return i;
            }
        }
        return -1;
    }

    private void fijarObjetivoJugadorCentro(Jugador jugador, int centroX, int centroY) {
        int indice = indiceActorObjetivo(jugador);
        if (indice < 0) {
            return;
        }
        int xMin = ConfiguracionJuego.CAMPO_X_MIN + 6;
        int xMax = ConfiguracionJuego.CAMPO_X_MAX - jugador.getAncho() - 6;
        int yMin = ConfiguracionJuego.CAMPO_Y_MIN + 6;
        int yMax = ConfiguracionJuego.CAMPO_Y_MAX - jugador.getAlto() - 6;
        int x = Math.max(xMin, Math.min(xMax, centroX - jugador.getAncho() / 2));
        int y = Math.max(yMin, Math.min(yMax, centroY - jugador.getAlto() / 2));
        objetivoJugadorX[indice] = x + jugador.getAncho() / 2;
        objetivoJugadorY[indice] = y + jugador.getAlto() / 2;
        objetivoJugadorActivo[indice] = true;
    }

    private void fijarObjetivoJugadorPosicion(Jugador jugador, int x, int y) {
        fijarObjetivoJugadorCentro(jugador, x + jugador.getAncho() / 2, y + jugador.getAlto() / 2);
    }

    private boolean aplicarObjetivosJugadores(int velocidadMaxima) {
        boolean huboMovimiento = false;
        for (int i = 0; i < actoresConObjetivo.length; i++) {
            if (!objetivoJugadorActivo[i]) {
                continue;
            }
            Jugador jugador = actoresConObjetivo[i];
            // Durante una reanudacion el ejecutor ya tiene su propio movimiento dedicado.
            // Evita que el suavizado de formacion lo desplace fuera del punto de saque.
            if (esEjecutorPendiente(jugador)) {
                continue;
            }
            int objetivoX = objetivoJugadorX[i] - jugador.getAncho() / 2;
            int objetivoY = objetivoJugadorY[i] - jugador.getAlto() / 2;
            int dx = calcularPaso(jugador.getX(), objetivoX, velocidadMaxima);
            int dy = calcularPaso(jugador.getY(), objetivoY, velocidadMaxima);
            if (dx == 0 && dy == 0) {
                objetivoJugadorActivo[i] = false;
                continue;
            }
            jugador.mover(dx, dy);
            limitarEntidadAlPanel(jugador);
            registrarMovimientoSuavizado(jugador, dx, dy);
            huboMovimiento = true;
        }
        return huboMovimiento;
    }

    private void registrarMovimientoSuavizado(Jugador jugador, int dx, int dy) {
        if (jugador == jugadorPrincipal) {
            movPrincipalX = dx;
            movPrincipalY = dy;
        } else if (jugador == aliadoLocal) {
            movAliadoX = dx;
            movAliadoY = dy;
        } else if (jugador == rivalUno) {
            movRivalUnoX = dx;
            movRivalUnoY = dy;
        } else if (jugador == rivalDos) {
            movRivalDosX = dx;
            movRivalDosY = dy;
        } else if (jugador == extremoLocal) {
            movExtremoLocalX = dx;
            movExtremoLocalY = dy;
        } else if (jugador == extremoRival) {
            movExtremoRivalX = dx;
            movExtremoRivalY = dy;
        } else if (jugador == mediaLocal) {
            movMediaLocalX = dx;
            movMediaLocalY = dy;
        } else if (jugador == mediaRival) {
            movMediaRivalX = dx;
            movMediaRivalY = dy;
        } else if (jugador == porteroLocal) {
            movPorteroLocalX = dx;
            movPorteroLocalY = dy;
        } else if (jugador == porteroRival) {
            movPorteroRivalX = dx;
            movPorteroRivalY = dy;
        } else if (jugador == arbitro) {
            movArbitroX = dx;
            movArbitroY = dy;
        }
    }

    private boolean hayObjetivosPendientes() {
        for (boolean activo : objetivoJugadorActivo) {
            if (activo) {
                return true;
            }
        }
        return false;
    }

    private void prepararObjetivosFormacionBase() {
        fijarObjetivoJugadorPosicion(porteroLocal, 20, ConfiguracionJuego.POS_Y_PORTERO);
        fijarObjetivoJugadorPosicion(jugadorPrincipal, ConfiguracionJuego.POS_X_BASE_LOCAL, ConfiguracionJuego.POS_Y_CAMPO_ARRIBA);
        fijarObjetivoJugadorPosicion(aliadoLocal, ConfiguracionJuego.POS_X_BASE_LOCAL + 45, ConfiguracionJuego.POS_Y_CAMPO_ABAJO);
        fijarObjetivoJugadorPosicion(extremoLocal, ConfiguracionJuego.POS_X_BASE_LOCAL + 78, ConfiguracionJuego.POS_Y_CAMPO_ARRIBA + 84);
        fijarObjetivoJugadorPosicion(mediaLocal, ConfiguracionJuego.POS_X_BASE_LOCAL + 24, ConfiguracionJuego.POS_Y_CAMPO_ABAJO + 72);

        fijarObjetivoJugadorPosicion(porteroRival, ConfiguracionJuego.ANCHO_PANEL - 50, ConfiguracionJuego.POS_Y_PORTERO);
        fijarObjetivoJugadorPosicion(rivalUno, ConfiguracionJuego.POS_X_BASE_RIVAL - 30, ConfiguracionJuego.POS_Y_CAMPO_ARRIBA);
        fijarObjetivoJugadorPosicion(rivalDos, ConfiguracionJuego.POS_X_BASE_RIVAL, ConfiguracionJuego.POS_Y_CAMPO_ABAJO);
        fijarObjetivoJugadorPosicion(extremoRival, ConfiguracionJuego.POS_X_BASE_RIVAL - 44, ConfiguracionJuego.POS_Y_CAMPO_ARRIBA + 94);
        fijarObjetivoJugadorPosicion(mediaRival, ConfiguracionJuego.POS_X_BASE_RIVAL - 10, ConfiguracionJuego.POS_Y_CAMPO_ABAJO + 68);

        fijarObjetivoJugadorPosicion(
            arbitro,
            ConfiguracionJuego.ANCHO_PANEL / 2 - arbitro.getAncho() / 2,
            ConfiguracionJuego.ALTO_PANEL / 2 - arbitro.getAlto() / 2 - 44
        );
    }

    private void prepararEntradaCeremonial() {
        limpiarObjetivosJugadores();

        // Llegada breve: cada equipo entra desde su lateral sin teletransportarse a su posicion final.
        int[] yLocal = { ConfiguracionJuego.POS_Y_PORTERO, 186, 294, 414, 542 };
        int[] yRival = { ConfiguracionJuego.POS_Y_PORTERO, 542, 414, 294, 186 };
        Jugador[] locales = { porteroLocal, jugadorPrincipal, aliadoLocal, extremoLocal, mediaLocal };
        Jugador[] rivales = { porteroRival, rivalUno, rivalDos, extremoRival, mediaRival };

        for (int i = 0; i < locales.length; i++) {
            Jugador local = locales[i];
            local.setX(ConfiguracionJuego.CAMPO_X_MIN - local.getAncho() - 90 - i * 12);
            local.setY(yLocal[i]);
        }
        for (int i = 0; i < rivales.length; i++) {
            Jugador rival = rivales[i];
            rival.setX(ConfiguracionJuego.CAMPO_X_MAX + 90 + i * 12);
            rival.setY(yRival[i]);
        }
        arbitro.setX(ConfiguracionJuego.ANCHO_PANEL / 2 - arbitro.getAncho() / 2);
        arbitro.setY(ConfiguracionJuego.CAMPO_Y_MIN - arbitro.getAlto() - 36);

        prepararObjetivosFormacionBase();
    }

    private void iniciarSorteoInicial() {
        sorteoMonedaActivo = true;
        framesSorteoMoneda = DURACION_SORTEO_MONEDA_FRAMES;
        framesAnimacionMoneda = 0;
        ceremoniaInicioActiva = true;
        framesCeremoniaInicio = DURACION_CEREMONIA_INICIO_FRAMES;
        monedaFueCara = aleatorio.nextBoolean();
        primerSaqueLocal = aleatorio.nextBoolean();
        ganadorSorteoRevelado = false;
        resultadoMoneda = "🪙 La moneda esta girando...";
        tipoReanudacionPendiente = TipoReanudacion.NINGUNA;
        framesRetrasoSaque = 0;
        framesEsperaReanudacion = 0;
        poseedorBalon = null;
        balonLibre = true;
        balonEnManos = false;
        balon.setPosicion(
            ConfiguracionJuego.ANCHO_PANEL / 2.0 - balon.getAncho() / 2.0,
            ConfiguracionJuego.ALTO_PANEL / 2.0 - balon.getAlto() / 2.0
        );
        balon.detener();
        prepararEntradaCeremonial();
    }

    private void resolverSorteoInicial() {
        sorteoMonedaActivo = false;
        resultadoMoneda = "🪙 Resultado: " + (monedaFueCara ? "Cara" : "Cruz")
            + " | Saca: " + (primerSaqueLocal ? "Local" : "Rival");
        mostrarTextoSaque(resultadoMoneda);
        iniciarBoteInicial(primerSaqueLocal);
    }

    public EventoJuego actualizar(EntradaJuego entrada) {
        framesAnimacion++;
        eventoTransitorio = EventoJuego.NINGUNO;

        if (escenaFinalActiva) {
            actualizarEscenaFinal();
            actualizarTemporizadoresGlobales();
            return EventoJuego.NINGUNO;
        }

        if (sorteoMonedaActivo) {
            if (ceremoniaInicioActiva) {
                aplicarObjetivosJugadores(VELOCIDAD_REUBICACION_SUAVE);
                actualizarEstadoJugadores();
                if (framesCeremoniaInicio > 0) {
                    framesCeremoniaInicio--;
                }
                resultadoMoneda = "🚶 Equipos entrando a la cancha...";
                if (framesCeremoniaInicio <= 0 || !hayObjetivosPendientes()) {
                    ceremoniaInicioActiva = false;
                    resultadoMoneda = "🪙 La moneda esta girando...";
                }
                actualizarTemporizadoresGlobales();
                return EventoJuego.NINGUNO;
            }

            moverArbitro();
            arbitro.actualizarEstado(false, Math.abs(movArbitroX) + Math.abs(movArbitroY));
            arbitro.actualizarAnimacion(movArbitroX, movArbitroY);
            framesAnimacionMoneda++;
            if (!ganadorSorteoRevelado && framesSorteoMoneda <= ConfiguracionJuego.FPS) {
                ganadorSorteoRevelado = true;
                resultadoMoneda = "🪙 Resultado: " + (monedaFueCara ? "Cara" : "Cruz")
                    + " | Saca: " + (primerSaqueLocal ? "Local" : "Rival");
            }
            if (framesSorteoMoneda > 0) {
                framesSorteoMoneda--;
            } else {
                resolverSorteoInicial();
            }
            actualizarTemporizadoresGlobales();
            return EventoJuego.NINGUNO;
        }

        if (!partidoFinalizadoPorTiempo && framesPartidoJugados < DURACION_PARTIDO_FRAMES) {
            framesPartidoJugados++;
        }

        if (boteInicialPendiente) {
            actualizarBoteInicial();
            actualizarTemporizadoresGlobales();
            return EventoJuego.NINGUNO;
        }

        if (framesRetrasoSaque > 0) {
            framesRetrasoSaque--;
            actualizarPlanesNpc();
            moverPrincipal(entrada);
            moverAliadoLocal();
            moverExtremos();
            moverMediocampistas();
            moverRivales();
            moverPorteros();
            moverArbitro();
            aplicarMovimientoBarridasActivas();
            aplicarObjetivosJugadores(VELOCIDAD_REUBICACION_SUAVE);
            actualizarEstadoJugadores();
            actualizarBalonEnPreparacionReanudacion();
            if (framesRetrasoSaque == 0) {
                ejecutarReanudacionPendiente();
            }
            actualizarTemporizadoresGlobales();
            return EventoJuego.NINGUNO;
        }

        // Cada frame sigue siempre la misma secuencia:
        // mover jugadores -> resolver estados -> actualizar balon/posesion
        // -> verificar goles/salidas -> procesar bonus y cooldowns.
        // 1) Movimiento de jugadores.
        actualizarPlanesNpc();
        moverPrincipal(entrada);
        moverAliadoLocal();
        moverExtremos();
        moverMediocampistas();
        moverRivales();
        moverPorteros();
        moverArbitro();
        aplicarMovimientoBarridasActivas();
        resolverSolapamientoJugadores();

        // 2) Estados temporales y animaciones.
        actualizarEstadoJugadores();

        // 3) Posesion, disparo y fisica del balon.
        actualizarPosesionYBalon(entrada);
        asegurarDinamismoPartido();
        if (eventoTransitorio != EventoJuego.NINGUNO) {
            return eventoTransitorio;
        }

        // 4) Goles y salidas por lineas.
        EventoJuego eventoGol = verificarGol();
        if (eventoGol != EventoJuego.NINGUNO) {
            return eventoGol;
        }

        if (!partidoFinalizadoPorTiempo && framesPartidoJugados >= DURACION_PARTIDO_FRAMES) {
            partidoFinalizadoPorTiempo = true;
            if (golesLocal > golesRival) {
                activarEscenaFinal(true, false);
                registrarSonido(TipoSonido.VICTORIA);
                return EventoJuego.VICTORIA;
            }
            if (golesRival > golesLocal) {
                activarEscenaFinal(false, false);
                registrarSonido(TipoSonido.DERROTA);
                return EventoJuego.DERROTA;
            }
            activarEscenaFinal(false, true);
            registrarSonido(TipoSonido.SAQUE);
            return EventoJuego.EMPATE;
        }

        // 5) Bonus y temporizadores auxiliares.
        actualizarHidratacionBanca();
        actualizarTurbo();
        actualizarTemporizadoresGlobales();
        return EventoJuego.NINGUNO;
    }

    private void actualizarEscenaFinal() {
        movPrincipalX = 0;
        movPrincipalY = 0;
        movAliadoX = 0;
        movAliadoY = 0;
        movExtremoLocalX = 0;
        movExtremoLocalY = 0;
        movMediaLocalX = 0;
        movMediaLocalY = 0;
        movPorteroLocalX = 0;
        movPorteroLocalY = 0;
        movRivalUnoX = 0;
        movRivalUnoY = 0;
        movRivalDosX = 0;
        movRivalDosY = 0;
        movExtremoRivalX = 0;
        movExtremoRivalY = 0;
        movMediaRivalX = 0;
        movMediaRivalY = 0;
        movPorteroRivalX = 0;
        movPorteroRivalY = 0;
        movArbitroX = 0;
        movArbitroY = 0;
        sprintPrincipal = false;
        sprintAliado = false;
        sprintExtremoLocal = false;
        sprintMediaLocal = false;
        sprintPorteroLocal = false;
        sprintRivalUno = false;
        sprintRivalDos = false;
        sprintExtremoRival = false;
        sprintMediaRival = false;
        sprintPorteroRival = false;

        if (poseedorBalon != null) {
            poseedorBalon = null;
            balonLibre = true;
            balonEnManos = false;
        }
        actualizarEstadoJugadores();
    }

    private void activarEscenaFinal(boolean victoriaLocal, boolean empate) {
        escenaFinalActiva = true;
        framesRetrasoSaque = 0;
        tipoReanudacionPendiente = TipoReanudacion.NINGUNA;
        ejecutorReanudacion = null;
        boteInicialPendiente = false;
        boteInicialSoltado = false;
        framesBoteInicial = 0;
        limpiarObjetivosJugadores();
        if (empate) {
            limpiarReaccionesFinales();
            return;
        }
        activarReaccionEquipo(jugadoresLocales, victoriaLocal);
        activarReaccionEquipo(jugadoresRivales, !victoriaLocal);
        arbitro.limpiarReaccionFinal();
    }

    private void activarReaccionEquipo(Jugador[] equipo, boolean celebran) {
        for (Jugador jugador : equipo) {
            if (jugador == null || jugador.estaExpulsado()) {
                continue;
            }
            if (celebran) {
                jugador.activarCelebracionFinal();
            } else {
                jugador.activarDerrotaFinal();
            }
        }
    }

    private void limpiarReaccionesFinales() {
        for (Jugador jugador : getTodosJugadores()) {
            if (jugador != null) {
                jugador.limpiarReaccionFinal();
            }
        }
    }

    private void moverPrincipal(EntradaJuego entrada) {
        if (jugadorPrincipal.estaExpulsado() || jugadorPrincipal.estaDerribado()) {
            movPrincipalX = 0;
            movPrincipalY = 0;
            sprintPrincipal = false;
            return;
        }
        if (esEjecutorPendiente(jugadorPrincipal)) {
            int objetivoX = calcularXEjecutorPendiente(jugadorPrincipal, true);
            int objetivoY = calcularYEjecutorPendiente(jugadorPrincipal);
            sprintPrincipal = jugadorPrincipal.puedeSprintar();
            int velocidad = jugadorPrincipal.getVelocidadMovimiento(sprintPrincipal);
            movPrincipalX = calcularPaso(jugadorPrincipal.getX(), objetivoX, velocidad);
            movPrincipalY = calcularPaso(jugadorPrincipal.getY(), objetivoY, velocidad);
            jugadorPrincipal.mover(movPrincipalX, movPrincipalY);
            limitarEntidadAlPanel(jugadorPrincipal);
            return;
        }

        if (modoEspectador) {
            moverPrincipalComoNpc();
            return;
        }

        // El jugador humano es el unico cuya direccion sale directo del teclado.
        sprintPrincipal = entrada.estaCorriendo() && jugadorPrincipal.puedeSprintar();
        int velocidad = jugadorPrincipal.getVelocidadMovimiento(sprintPrincipal);
        movPrincipalX = entrada.calcularDeltaX(velocidad);
        movPrincipalY = entrada.calcularDeltaY(velocidad);
        jugadorPrincipal.mover(movPrincipalX, movPrincipalY);
        limitarEntidadAlPanel(jugadorPrincipal);
    }

    private void moverPrincipalComoNpc() {
        if (jugadorPrincipal.estaDerribado()) {
            movPrincipalX = 0;
            movPrincipalY = 0;
            sprintPrincipal = false;
            return;
        }
        int objetivoX;
        int objetivoY;
        if (modoAperturaNpc()) {
            objetivoX = calcularXDesmarqueNpc(jugadorPrincipal);
            objetivoY = calcularYDesmarqueNpc(jugadorPrincipal);
        } else if (!balonLibre && poseedorBalon == jugadorPrincipal) {
            objetivoX = ConfiguracionJuego.CAMPO_X_MAX - 188;
            objetivoY = calcularCarrilAtaqueY(jugadorPrincipal, true);
        } else if (!balonLibre && poseedorEsLocal) {
            objetivoX = calcularXApoyoOfensivo(poseedorBalon, true, false);
            objetivoY = calcularCarrilApoyo(poseedorBalon, true, false);
        } else if (!balonLibre) {
            Jugador presionador = seleccionarPresionadorEquipo(true);
            if (presionador == jugadorPrincipal) {
                objetivoX = poseedorBalon.getX() - 18;
                objetivoY = poseedorBalon.getY();
            } else {
                int[] marca = calcularObjetivoMarcaPase(jugadorPrincipal, poseedorBalon);
                objetivoX = marca[0];
                objetivoY = marca[1];
            }
        } else {
            boolean persigue = perseguidorBalonLibre(true) == jugadorPrincipal;
            if (persigue) {
                int[] intercepcion = calcularObjetivoIntercepcionBalonLibre(jugadorPrincipal, true);
                objetivoX = intercepcion[0];
                objetivoY = intercepcion[1];
            } else {
                objetivoX = calcularXApoyoLibre(true, false);
                objetivoY = calcularYApoyoLibre(true, false);
            }
        }

        int[] objetivoNormalizado = normalizarObjetivoNpc(jugadorPrincipal, true, objetivoX, objetivoY, false);
        objetivoX = objetivoNormalizado[0];
        objetivoY = objetivoNormalizado[1];
        sprintPrincipal = debeSprintarNpc(jugadorPrincipal, objetivoX, objetivoY, false);
        int velocidad = jugadorPrincipal.getVelocidadMovimiento(sprintPrincipal);
        movPrincipalX = calcularPaso(jugadorPrincipal.getX(), objetivoX, velocidad);
        movPrincipalY = calcularPaso(jugadorPrincipal.getY(), objetivoY, velocidad);
        jugadorPrincipal.mover(movPrincipalX, movPrincipalY);
        limitarEntidadAlPanel(jugadorPrincipal);
    }

    private void moverAliadoLocal() {
        if (aliadoLocal.estaExpulsado() || aliadoLocal.estaDerribado()) {
            movAliadoX = 0;
            movAliadoY = 0;
            sprintAliado = false;
            return;
        }
        if (esEjecutorPendiente(aliadoLocal)) {
            int objetivoX = calcularXEjecutorPendiente(aliadoLocal, true);
            int objetivoY = calcularYEjecutorPendiente(aliadoLocal);
            sprintAliado = aliadoLocal.puedeSprintar();
            int velocidadAliado = aliadoLocal.getVelocidadMovimiento(sprintAliado);
            movAliadoX = calcularPaso(aliadoLocal.getX(), objetivoX, velocidadAliado);
            movAliadoY = calcularPaso(aliadoLocal.getY(), objetivoY, velocidadAliado);
            aliadoLocal.mover(movAliadoX, movAliadoY);
            limitarEntidadAlPanel(aliadoLocal);
            return;
        }

        if (modoAperturaNpc()) {
            int objetivoX = calcularXDesmarqueNpc(aliadoLocal);
            int objetivoY = calcularYDesmarqueNpc(aliadoLocal);
            sprintAliado = debeSprintarNpc(aliadoLocal, objetivoX, objetivoY, false);
            int velocidadAliado = aliadoLocal.getVelocidadMovimiento(sprintAliado);
            movAliadoX = calcularPaso(aliadoLocal.getX(), objetivoX, velocidadAliado);
            movAliadoY = calcularPaso(aliadoLocal.getY(), objetivoY, velocidadAliado);
            aliadoLocal.mover(movAliadoX, movAliadoY);
            limitarEntidadAlPanel(aliadoLocal);
            return;
        }

        // El aliado alterna entre apoyo interior, desmarque y repliegue.
        int objetivoX;
        int objetivoY;
        if (!balonLibre && poseedorBalon == aliadoLocal) {
            objetivoX = ConfiguracionJuego.CAMPO_X_MAX - 195;
            objetivoY = calcularCarrilAtaqueY(aliadoLocal, true);
        } else if (!balonLibre && poseedorEsLocal) {
            objetivoX = calcularXApoyoOfensivo(poseedorBalon, true, false);
            objetivoY = calcularCarrilApoyo(poseedorBalon, true, true);
        } else if (!balonLibre) {
            // En defensa alterna entre presionar y cortar una linea de pase peligrosa.
            double distanciaPrincipal = distanciaEntre(jugadorPrincipal, poseedorBalon);
            double distanciaAliado = distanciaEntre(aliadoLocal, poseedorBalon);
            boolean principalPresiona = distanciaPrincipal < 96.0 && distanciaPrincipal + 18.0 < distanciaAliado;
            if (principalPresiona) {
                int[] marca = calcularObjetivoMarcaPase(aliadoLocal, poseedorBalon);
                objetivoX = marca[0];
                objetivoY = marca[1];
            } else {
                objetivoX = poseedorBalon.getX() - 16;
                objetivoY = poseedorBalon.getY();
            }
        } else {
            boolean persigue = perseguidorBalonLibre(true) == aliadoLocal;
            if (persigue) {
                int[] intercepcion = calcularObjetivoIntercepcionBalonLibre(aliadoLocal, true);
                objetivoX = intercepcion[0];
                objetivoY = intercepcion[1];
            } else {
                objetivoX = calcularXApoyoLibre(true, false);
                objetivoY = calcularYApoyoLibre(true, false);
            }
        }

        int[] objetivoNormalizado = normalizarObjetivoNpc(aliadoLocal, true, objetivoX, objetivoY, false);
        objetivoX = objetivoNormalizado[0];
        objetivoY = objetivoNormalizado[1];
        sprintAliado = debeSprintarNpc(aliadoLocal, objetivoX, objetivoY, false);
        int velocidadAliado = aliadoLocal.getVelocidadMovimiento(sprintAliado);
        movAliadoX = calcularPaso(aliadoLocal.getX(), objetivoX, velocidadAliado);
        movAliadoY = calcularPaso(aliadoLocal.getY(), objetivoY, velocidadAliado);
        aliadoLocal.mover(movAliadoX, movAliadoY);
        limitarEntidadAlPanel(aliadoLocal);
    }

    private void moverExtremos() {
        moverExtremo(extremoLocal, true);
        moverExtremo(extremoRival, false);
    }

    private void moverMediocampistas() {
        moverMediocampista(mediaLocal, true);
        moverMediocampista(mediaRival, false);
    }

    private void moverMediocampista(Jugador medio, boolean equipoLocal) {
        if (medio.estaExpulsado() || medio.estaDerribado()) {
            if (medio == mediaLocal) {
                sprintMediaLocal = false;
                movMediaLocalX = 0;
                movMediaLocalY = 0;
            } else {
                sprintMediaRival = false;
                movMediaRivalX = 0;
                movMediaRivalY = 0;
            }
            return;
        }
        int objetivoX;
        int objetivoY;
        if (esEjecutorPendiente(medio)) {
            objetivoX = calcularXEjecutorPendiente(medio, equipoLocal);
            objetivoY = calcularYEjecutorPendiente(medio);
        } else if (modoAperturaNpc()) {
            objetivoX = calcularXDesmarqueNpc(medio);
            objetivoY = calcularYDesmarqueNpc(medio);
        } else if (!balonLibre && poseedorBalon == medio) {
            objetivoX = equipoLocal ? ConfiguracionJuego.CAMPO_X_MAX - 198 : ConfiguracionJuego.CAMPO_X_MIN + 198;
            objetivoY = calcularCarrilAtaqueY(medio, equipoLocal);
        } else if (!balonLibre && poseedorEsLocal == equipoLocal) {
            objetivoX = calcularXApoyoOfensivo(poseedorBalon, equipoLocal, false);
            objetivoY = calcularCarrilApoyo(poseedorBalon, equipoLocal, false);
        } else if (!balonLibre) {
            int[] marca = calcularObjetivoMarcaPase(medio, poseedorBalon);
            objetivoX = marca[0];
            objetivoY = marca[1];
        } else {
            Jugador perseguidor = perseguidorBalonLibre(equipoLocal);
            if (perseguidor == medio) {
                int[] intercepcion = calcularObjetivoIntercepcionBalonLibre(medio, equipoLocal);
                objetivoX = intercepcion[0];
                objetivoY = intercepcion[1];
            } else {
                objetivoX = calcularXApoyoLibre(equipoLocal, false);
                objetivoY = calcularYApoyoLibre(equipoLocal, false);
            }
        }

        int[] objetivoNormalizado = normalizarObjetivoNpc(medio, equipoLocal, objetivoX, objetivoY, false);
        objetivoX = objetivoNormalizado[0];
        objetivoY = objetivoNormalizado[1];
        boolean sprint = debeSprintarNpc(medio, objetivoX, objetivoY, false);
        int velocidad = medio.getVelocidadMovimiento(sprint);
        int movX = calcularPaso(medio.getX(), objetivoX, velocidad);
        int movY = calcularPaso(medio.getY(), objetivoY, velocidad);
        medio.mover(movX, movY);
        limitarEntidadAlPanel(medio);

        if (medio == mediaLocal) {
            sprintMediaLocal = sprint;
            movMediaLocalX = movX;
            movMediaLocalY = movY;
        } else {
            sprintMediaRival = sprint;
            movMediaRivalX = movX;
            movMediaRivalY = movY;
        }
    }

    private void moverExtremo(Jugador extremo, boolean equipoLocal) {
        if (extremo.estaExpulsado() || extremo.estaDerribado()) {
            if (extremo == extremoLocal) {
                sprintExtremoLocal = false;
                movExtremoLocalX = 0;
                movExtremoLocalY = 0;
            } else {
                sprintExtremoRival = false;
                movExtremoRivalX = 0;
                movExtremoRivalY = 0;
            }
            return;
        }
        int objetivoX;
        int objetivoY;
        if (esEjecutorPendiente(extremo)) {
            objetivoX = calcularXEjecutorPendiente(extremo, equipoLocal);
            objetivoY = calcularYEjecutorPendiente(extremo);
        } else if (modoAperturaNpc()) {
            objetivoX = calcularXDesmarqueNpc(extremo);
            objetivoY = calcularYDesmarqueNpc(extremo);
        } else if (!balonLibre && poseedorBalon == extremo) {
            objetivoX = equipoLocal ? ConfiguracionJuego.CAMPO_X_MAX - 165 : ConfiguracionJuego.CAMPO_X_MIN + 165;
            objetivoY = calcularCarrilAtaqueY(extremo, equipoLocal);
        } else if (!balonLibre && poseedorEsLocal == equipoLocal) {
            objetivoX = calcularXApoyoOfensivo(poseedorBalon, equipoLocal, true);
            objetivoY = calcularCarrilApoyo(poseedorBalon, equipoLocal, true);
        } else if (!balonLibre) {
            Jugador presionador = seleccionarPresionadorEquipo(equipoLocal);
            if (presionador == extremo) {
                objetivoX = poseedorBalon.getX() + (equipoLocal ? -16 : 16);
                objetivoY = poseedorBalon.getY();
            } else {
                int[] marca = calcularObjetivoMarcaPase(extremo, poseedorBalon);
                objetivoX = marca[0];
                objetivoY = marca[1];
            }
        } else {
            Jugador perseguidor = perseguidorBalonLibre(equipoLocal);
            if (perseguidor == extremo) {
                int[] intercepcion = calcularObjetivoIntercepcionBalonLibre(extremo, equipoLocal);
                objetivoX = intercepcion[0];
                objetivoY = intercepcion[1];
            } else {
                objetivoX = calcularXApoyoLibre(equipoLocal, true);
                objetivoY = calcularYApoyoLibre(equipoLocal, true);
            }
        }

        int[] objetivoNormalizado = normalizarObjetivoNpc(extremo, equipoLocal, objetivoX, objetivoY, true);
        objetivoX = objetivoNormalizado[0];
        objetivoY = objetivoNormalizado[1];
        boolean sprint = debeSprintarNpc(extremo, objetivoX, objetivoY, false);
        int velocidad = extremo.getVelocidadMovimiento(sprint);
        int movX = calcularPaso(extremo.getX(), objetivoX, velocidad);
        int movY = calcularPaso(extremo.getY(), objetivoY, velocidad);
        extremo.mover(movX, movY);
        limitarEntidadAlPanel(extremo);

        if (extremo == extremoLocal) {
            sprintExtremoLocal = sprint;
            movExtremoLocalX = movX;
            movExtremoLocalY = movY;
        } else {
            sprintExtremoRival = sprint;
            movExtremoRivalX = movX;
            movExtremoRivalY = movY;
        }
    }

    private void moverRivales() {
        if (rivalUno.estaExpulsado() || rivalUno.estaDerribado()) {
            sprintRivalUno = false;
            movRivalUnoX = 0;
            movRivalUnoY = 0;
        }
        if (rivalDos.estaExpulsado() || rivalDos.estaDerribado()) {
            sprintRivalDos = false;
            movRivalDosX = 0;
            movRivalDosY = 0;
        }
        // Los rivales aplican una estructura simple:
        // uno presiona o conduce, el otro da apoyo o cierra el carril libre.
        int objetivoRivalUnoX;
        int objetivoRivalUnoY;
        int objetivoRivalDosX;
        int objetivoRivalDosY;

        if (esEjecutorPendiente(rivalUno)) {
            objetivoRivalUnoX = calcularXEjecutorPendiente(rivalUno, false);
            objetivoRivalUnoY = calcularYEjecutorPendiente(rivalUno);
        } else if (modoAperturaNpc()) {
            objetivoRivalUnoX = calcularXDesmarqueNpc(rivalUno);
            objetivoRivalUnoY = calcularYDesmarqueNpc(rivalUno);
        } else if (!balonLibre && poseedorEsLocal) {
            Jugador presionador = seleccionarDefensorPresionanteRival();
            if (presionador == rivalUno) {
                objetivoRivalUnoX = poseedorBalon.getX() - 14;
                objetivoRivalUnoY = poseedorBalon.getY();
            } else {
                int[] marca = calcularObjetivoMarcaPase(rivalUno, poseedorBalon);
                objetivoRivalUnoX = marca[0];
                objetivoRivalUnoY = marca[1];
            }
        } else if (balonLibre) {
            Jugador perseguidor = perseguidorBalonLibre(false);
            if (perseguidor == rivalUno) {
                int[] intercepcion = calcularObjetivoIntercepcionBalonLibre(rivalUno, false);
                objetivoRivalUnoX = intercepcion[0];
                objetivoRivalUnoY = intercepcion[1];
            } else {
                objetivoRivalUnoX = calcularXApoyoLibre(false, false);
                objetivoRivalUnoY = calcularYApoyoLibre(false, false);
            }
        } else if (poseedorBalon == rivalUno) {
            objetivoRivalUnoX = ConfiguracionJuego.CAMPO_X_MIN + 132;
            objetivoRivalUnoY = calcularCarrilAtaqueY(rivalUno, false);
        } else if (poseedorBalon == rivalDos) {
            objetivoRivalUnoX = calcularXApoyoOfensivo(rivalDos, false, false);
            objetivoRivalUnoY = calcularCarrilApoyo(rivalDos, false, false);
        } else {
            objetivoRivalUnoX = calcularXApoyoOfensivo(poseedorBalon, false, false);
            objetivoRivalUnoY = calcularCarrilApoyo(poseedorBalon, false, false);
        }

        if (esEjecutorPendiente(rivalDos)) {
            objetivoRivalDosX = calcularXEjecutorPendiente(rivalDos, false);
            objetivoRivalDosY = calcularYEjecutorPendiente(rivalDos);
        } else if (modoAperturaNpc()) {
            objetivoRivalDosX = calcularXDesmarqueNpc(rivalDos);
            objetivoRivalDosY = calcularYDesmarqueNpc(rivalDos);
        } else if (!balonLibre && poseedorEsLocal) {
            Jugador presionador = seleccionarDefensorPresionanteRival();
            if (presionador == rivalDos) {
                objetivoRivalDosX = poseedorBalon.getX() - 16;
                objetivoRivalDosY = poseedorBalon.getY();
            } else {
                int[] marca = calcularObjetivoMarcaPase(rivalDos, poseedorBalon);
                objetivoRivalDosX = marca[0];
                objetivoRivalDosY = marca[1];
            }
        } else if (balonLibre) {
            Jugador perseguidor = perseguidorBalonLibre(false);
            if (perseguidor == rivalUno) {
                objetivoRivalDosX = calcularXApoyoLibre(false, true);
                objetivoRivalDosY = calcularYApoyoLibre(false, true);
            } else {
                int[] intercepcion = calcularObjetivoIntercepcionBalonLibre(rivalDos, false);
                objetivoRivalDosX = intercepcion[0];
                objetivoRivalDosY = intercepcion[1];
            }
        } else if (poseedorBalon == rivalUno) {
            objetivoRivalDosX = calcularXApoyoOfensivo(rivalUno, false, true);
            objetivoRivalDosY = calcularCarrilApoyo(rivalUno, false, true);
        } else if (poseedorBalon == rivalDos) {
            objetivoRivalDosX = ConfiguracionJuego.CAMPO_X_MIN + 132;
            objetivoRivalDosY = calcularCarrilAtaqueY(rivalDos, false);
        } else {
            objetivoRivalDosX = calcularXApoyoOfensivo(poseedorBalon, false, true);
            objetivoRivalDosY = calcularCarrilApoyo(poseedorBalon, false, true);
        }

        int[] objetivoRivalUnoNormalizado = normalizarObjetivoNpc(rivalUno, false, objetivoRivalUnoX, objetivoRivalUnoY, false);
        objetivoRivalUnoX = objetivoRivalUnoNormalizado[0];
        objetivoRivalUnoY = objetivoRivalUnoNormalizado[1];
        int[] objetivoRivalDosNormalizado = normalizarObjetivoNpc(rivalDos, false, objetivoRivalDosX, objetivoRivalDosY, false);
        objetivoRivalDosX = objetivoRivalDosNormalizado[0];
        objetivoRivalDosY = objetivoRivalDosNormalizado[1];
        if (!rivalUno.estaExpulsado() && !rivalUno.estaDerribado()) {
            sprintRivalUno = debeSprintarNpc(rivalUno, objetivoRivalUnoX, objetivoRivalUnoY, false);
            int velocidadRivalUno = rivalUno.getVelocidadMovimiento(sprintRivalUno);
            movRivalUnoX = calcularPaso(rivalUno.getX(), objetivoRivalUnoX, velocidadRivalUno);
            movRivalUnoY = calcularPaso(rivalUno.getY(), objetivoRivalUnoY, velocidadRivalUno);
            rivalUno.mover(movRivalUnoX, movRivalUnoY);
            limitarEntidadAlPanel(rivalUno);
        }

        if (!rivalDos.estaExpulsado() && !rivalDos.estaDerribado()) {
            sprintRivalDos = debeSprintarNpc(rivalDos, objetivoRivalDosX, objetivoRivalDosY, false);
            int velocidadRivalDos = rivalDos.getVelocidadMovimiento(sprintRivalDos);
            movRivalDosX = calcularPaso(rivalDos.getX(), objetivoRivalDosX, velocidadRivalDos);
            movRivalDosY = calcularPaso(rivalDos.getY(), objetivoRivalDosY, velocidadRivalDos);
            rivalDos.mover(movRivalDosX, movRivalDosY);
            limitarEntidadAlPanel(rivalDos);
        }
    }

    private void moverPorteros() {
        // El portero ya no sigue solo la Y de la jugada:
        // intenta cerrar el angulo al arco y se adelanta si detecta una amenaza real.
        if (recuperacionPorteroLocalFrames > 0) {
            movPorteroLocalX = 0;
            movPorteroLocalY = 0;
        }
        if (recuperacionPorteroRivalFrames > 0) {
            movPorteroRivalX = 0;
            movPorteroRivalY = 0;
        }
        int xObjetivoLocal;
        int yObjetivoLocal;
        if (esEjecutorPendiente(porteroLocal)) {
            xObjetivoLocal = calcularXEjecutorPendiente(porteroLocal, true);
            yObjetivoLocal = calcularYEjecutorPendiente(porteroLocal);
        } else {
            xObjetivoLocal = calcularXObjetivoPortero(porteroLocal, true);
            yObjetivoLocal = calcularYObjetivoPortero(porteroLocal, true);
        }
        if (recuperacionPorteroLocalFrames <= 0) {
            sprintPorteroLocal = debeSprintarPortero(porteroLocal, true, xObjetivoLocal, yObjetivoLocal);
            int velocidadLocal = calcularVelocidadPortero(porteroLocal, true);
            movPorteroLocalX = calcularPaso(porteroLocal.getX(), xObjetivoLocal, velocidadLocal);
            movPorteroLocalY = calcularPaso(porteroLocal.getY(), yObjetivoLocal, velocidadLocal);
            porteroLocal.mover(movPorteroLocalX, movPorteroLocalY);
        }

        int xObjetivoRival;
        int yObjetivoRival;
        if (esEjecutorPendiente(porteroRival)) {
            xObjetivoRival = calcularXEjecutorPendiente(porteroRival, false);
            yObjetivoRival = calcularYEjecutorPendiente(porteroRival);
        } else {
            xObjetivoRival = calcularXObjetivoPortero(porteroRival, false);
            yObjetivoRival = calcularYObjetivoPortero(porteroRival, false);
        }
        if (recuperacionPorteroRivalFrames <= 0) {
            sprintPorteroRival = debeSprintarPortero(porteroRival, false, xObjetivoRival, yObjetivoRival);
            int velocidadRival = calcularVelocidadPortero(porteroRival, false);
            movPorteroRivalX = calcularPaso(porteroRival.getX(), xObjetivoRival, velocidadRival);
            movPorteroRivalY = calcularPaso(porteroRival.getY(), yObjetivoRival, velocidadRival);
            porteroRival.mover(movPorteroRivalX, movPorteroRivalY);
        }

        limitarPorteroEnZona(porteroLocal, true);
        limitarPorteroEnZona(porteroRival, false);
    }

    private void moverArbitro() {
        // El arbitro alterna entre seguimiento normal y acciones de arbitraje.
        int objetivoX;
        int objetivoY;
        if (arbitroDebeLlevarBalonAlCentro()) {
            java.awt.Point centro = cancha.getPuntoSaqueInicial();
            objetivoX = centro.x - arbitro.getAncho() / 2;
            objetivoY = centro.y - arbitro.getAlto() / 2 - 10;
        } else if (framesAccionArbitro > 0) {
            objetivoX = (int) Math.round(objetivoArbitroX - arbitro.getAncho() / 2.0);
            objetivoY = (int) Math.round(objetivoArbitroY - arbitro.getAlto() / 2.0);
        } else {
            objetivoX = ConfiguracionJuego.ANCHO_PANEL / 2 - arbitro.getAncho() / 2;
            objetivoY = ConfiguracionJuego.ALTO_PANEL / 2 - arbitro.getAlto() / 2;
            if (!sorteoMonedaActivo) {
                objetivoX += (int) ((balon.getCentroX() - ConfiguracionJuego.ANCHO_PANEL / 2.0) * 0.12);
                objetivoY += (int) ((balon.getCentroY() - ConfiguracionJuego.ALTO_PANEL / 2.0) * 0.10);
            }
        }
        int velocidad = Math.max(1, arbitro.getVelocidadBase() - 1 + (framesAccionArbitro > 0 ? 1 : 0));
        movArbitroX = calcularPaso(arbitro.getX(), objetivoX, velocidad);
        movArbitroY = calcularPaso(arbitro.getY(), objetivoY, velocidad);
        arbitro.mover(movArbitroX, movArbitroY);
        limitarEntidadAlPanel(arbitro);
    }

    private void resolverSolapamientoJugadores() {
        // Evita que los jugadores se "monten" y queden atascados visualmente.
        Jugador[] jugadores = getTodosJugadores();
        for (int i = 0; i < jugadores.length; i++) {
            Jugador a = jugadores[i];
            for (int j = i + 1; j < jugadores.length; j++) {
                Jugador b = jugadores[j];
                if (!intersecanRectangulos(
                    a.getX(), a.getY(), a.getAncho(), a.getAlto(),
                    b.getX(), b.getY(), b.getAncho(), b.getAlto()
                )) {
                    continue;
                }

                int centroAX = a.getX() + a.getAncho() / 2;
                int centroAY = a.getY() + a.getAlto() / 2;
                int centroBX = b.getX() + b.getAncho() / 2;
                int centroBY = b.getY() + b.getAlto() / 2;
                int dx = centroAX - centroBX;
                int dy = centroAY - centroBY;
                if (dx == 0 && dy == 0) {
                    dx = aleatorio.nextBoolean() ? 1 : -1;
                    dy = aleatorio.nextBoolean() ? 1 : -1;
                }

                int empujeX = dx > 0 ? 2 : -2;
                int empujeY = dy > 0 ? 2 : -2;
                if (esPortero(a)) {
                    empujeX = 0;
                }
                if (esPortero(b)) {
                    empujeX = 0;
                }

                a.setX(a.getX() + empujeX);
                a.setY(a.getY() + empujeY);
                b.setX(b.getX() - empujeX);
                b.setY(b.getY() - empujeY);
                limitarEntidadAlPanel(a);
                limitarEntidadAlPanel(b);
                if (esPortero(a)) {
                    limitarPorteroEnZona(a, esJugadorLocal(a));
                }
                if (esPortero(b)) {
                    limitarPorteroEnZona(b, esJugadorLocal(b));
                }
            }
        }
    }

    private void actualizarPosesionYBalon(EntradaJuego entrada) {
        gestionarBarridas(entrada);

        // Evita bloqueos en esquinas cuando la pelota se queda muerta.
        resolverAtascoEnEsquina();
        actualizarTrackerPosesionNpc();

        if (balonLibre) {
            // Sin poseedor, el balon usa fisica pura y cualquiera puede capturarlo.
            framesPoseedorAtascado = 0;
            balon.actualizarFisica(ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL);
            if (intentarAtajadaPortero()) {
                return;
            }
            intentarCapturaBalonLibre();
            return;
        }

        // Con poseedor, primero hay disputa por robo y luego decision de accion.
        resolverBloqueoPoseedor();
        intentarRobo();
        boolean accion = ejecutarAccionBalonSiAplica(entrada) || ejecutarAccionNpcSiAplica();
        if (!accion) {
            // Si nadie pasa ni tira, el balon sigue "pegado" al conductor con un arrastre suave.
            arrastrarBalonConPoseedor();
        }
    }

    private void actualizarTrackerPosesionNpc() {
        if (balonLibre || poseedorBalon == null || (!modoEspectador && poseedorBalon == jugadorPrincipal)) {
            poseedorControlAccionNpc = null;
            framesPoseedorSinAccionNpc = 0;
            return;
        }

        if (poseedorControlAccionNpc != poseedorBalon) {
            poseedorControlAccionNpc = poseedorBalon;
            framesPoseedorSinAccionNpc = 0;
            return;
        }

        if (framesPoseedorSinAccionNpc < 9999) {
            framesPoseedorSinAccionNpc++;
        }
    }

    private void asegurarDinamismoPartido() {
        if (sorteoMonedaActivo || framesRetrasoSaque > 0 || tipoReanudacionPendiente != TipoReanudacion.NINGUNA) {
            framesSinDinamismo = 0;
            return;
        }

        int movimientoColectivo = 0;
        for (Jugador jugador : getTodosJugadores()) {
            if (esPortero(jugador)) {
                continue;
            }
            movimientoColectivo += velocidadMovimiento(jugador);
        }
        boolean balonActivo = !balonLibre
            || balon.getRapidez() > RAPIDEZ_BALON_MINIMA_DINAMICA
            || balon.getAltura() > 1.5;
        boolean hayDisputa = poseedorBalon != null
            && rivalMasCercanoA(poseedorBalon, esJugadorLocal(poseedorBalon)) != null
            && distanciaRivalMasCercano(poseedorBalon, esJugadorLocal(poseedorBalon)) < 88.0;

        if (movimientoColectivo >= MOVIMIENTO_COLECTIVO_MINIMO || balonActivo || hayDisputa) {
            framesSinDinamismo = Math.max(0, framesSinDinamismo - 2);
            return;
        }

        framesSinDinamismo++;
        if (framesSinDinamismo < FRAMES_UMBRAL_BAJA_DINAMICA) {
            return;
        }

        framesSinDinamismo = 0;
        framesRitmoAlto = FRAMES_RITMO_ALTO;
        reactivarRitmoPartido();
    }

    private void reactivarRitmoPartido() {
        if (balonLibre) {
            double centroJugadoresX = 0.0;
            double centroJugadoresY = 0.0;
            int total = 0;
            for (Jugador jugador : getTodosJugadores()) {
                centroJugadoresX += jugador.getX() + jugador.getAncho() / 2.0;
                centroJugadoresY += jugador.getY() + jugador.getAlto() / 2.0;
                total++;
            }
            if (total > 0) {
                centroJugadoresX /= total;
                centroJugadoresY /= total;
            }
            double dx = balon.getCentroX() - centroJugadoresX;
            double dy = balon.getCentroY() - centroJugadoresY;
            double norma = Math.hypot(dx, dy);
            if (norma < 0.001) {
                dx = aleatorio.nextBoolean() ? 1.0 : -1.0;
                dy = aleatorio.nextBoolean() ? 0.5 : -0.5;
                norma = Math.hypot(dx, dy);
            }
            balon.impulsar((dx / norma) * IMPULSO_REACTIVACION_BALON, (dy / norma) * (IMPULSO_REACTIVACION_BALON * 0.85), 0.8);
            cooldownCapturaLibreFrames = Math.min(cooldownCapturaLibreFrames, 4);
        } else if (poseedorBalon != null) {
            if (poseedorBalon != jugadorPrincipal || modoEspectador) {
                setCooldownDecisionNpc(poseedorBalon, 0);
                framesPoseedorAtascado = Math.max(framesPoseedorAtascado, FRAMES_BLOQUEO_POSEEDOR - 2);
            }
            Jugador presionLocal = seleccionarPresionadorEquipo(true);
            Jugador presionRival = seleccionarPresionadorEquipo(false);
            setCooldownDecisionNpc(presionLocal, 0);
            setCooldownDecisionNpc(presionRival, 0);
            cooldownRoboFrames = Math.min(cooldownRoboFrames, ConfiguracionJuego.FPS / 8);
        }
        narrar("🔥 El partido sube de ritmo", false);
    }

    private void resolverBloqueoPoseedor() {
        if (poseedorBalon == null) {
            framesPoseedorAtascado = 0;
            return;
        }
        if (poseedorBalon == jugadorPrincipal) {
            framesPoseedorAtascado = 0;
            return;
        }

        Jugador defensorCercano = rivalMasCercanoA(poseedorBalon, esJugadorLocal(poseedorBalon));
        double distanciaPresion = defensorCercano == null ? Double.MAX_VALUE : distanciaEntre(poseedorBalon, defensorCercano);
        int movimiento = Math.abs(movimientoXDe(poseedorBalon)) + Math.abs(movimientoYDe(poseedorBalon));
        boolean cercaArco = distanciaHorizontalAlArco(poseedorBalon) < 220.0;
        boolean contraLimiteAtaque = esJugadorLocal(poseedorBalon)
            ? poseedorBalon.getX() + poseedorBalon.getAncho() >= ConfiguracionJuego.CAMPO_X_MAX - 4
            : poseedorBalon.getX() <= ConfiguracionJuego.CAMPO_X_MIN + 4;
        boolean enBloqueo = movimiento <= 1 && (balonEnManos || distanciaPresion < 72.0 || cercaArco || contraLimiteAtaque);
        if (!enBloqueo) {
            framesPoseedorAtascado = Math.max(0, framesPoseedorAtascado - 3);
            return;
        }

        framesPoseedorAtascado++;
        if (framesPoseedorAtascado < FRAMES_BLOQUEO_POSEEDOR) {
            return;
        }
        framesPoseedorAtascado = 0;

        // Si la IA queda trabada en una marca cerrada, despeja para reactivar la jugada.
        if (balonEnManos) {
            if (esPortero(poseedorBalon)) {
                Jugador receptorLargo = seleccionarReceptorLargoPortero(poseedorBalon);
                double[] direccion = receptorLargo != null
                    ? direccionPaseAnticipadoNpc(poseedorBalon, receptorLargo)
                    : direccionAlArcoContrario(poseedorBalon);
                lanzarBalonDesdePoseedor(direccion, FUERZA_PASE_MAX * 0.95, 2.6);
            } else {
                Jugador receptor = seleccionarReceptorNpc(poseedorBalon);
                double[] direccion = receptor != null
                    ? direccionPaseAnticipadoNpc(poseedorBalon, receptor)
                    : direccionAlArcoContrario(poseedorBalon);
                lanzarBalonDesdePoseedor(direccion, FUERZA_PASE_MAX * 0.86, 1.8);
            }
            registrarSonido(TipoSonido.SAQUE);
        } else {
            boolean forzarTiro = cercaArco || contraLimiteAtaque;
            if (forzarTiro) {
                double[] direccion = direccionTiroNpc(poseedorBalon);
                lanzarBalonDesdePoseedor(direccion, FUERZA_TIRO_MAX * 0.94, 3.2);
                registrarSonido(TipoSonido.TIRO);
            } else {
                double[] direccion = direccionAlArcoContrario(poseedorBalon);
                double variacion = poseedorBalon.getY() < ConfiguracionJuego.ALTO_PANEL / 2 ? 0.24 : -0.24;
                direccion[1] += variacion;
                double norma = Math.hypot(direccion[0], direccion[1]);
                if (norma > 0.0001) {
                    direccion[0] /= norma;
                    direccion[1] /= norma;
                }
                lanzarBalonDesdePoseedor(direccion, FUERZA_PASE_MAX * 0.82, 1.6);
                registrarSonido(TipoSonido.PASE);
            }
        }
            ultimoToqueLocal = esJugadorLocal(poseedorBalon);
            ultimoPateador = poseedorBalon;
            poseedorBalon = null;
            balonLibre = true;
            balonEnManos = false;
            cooldownCapturaLibreFrames = 8;
            cooldownRoboFrames = ConfiguracionJuego.FPS / 4;
            poseedorControlAccionNpc = null;
            framesPoseedorSinAccionNpc = 0;
    }

    private void resolverAtascoEnEsquina() {
        if (balonLibre) {
            double velocidad = Math.abs(balon.getVelocidadX()) + Math.abs(balon.getVelocidadY());
            if (velocidad < 0.18 && estaEnEsquina(balon.getX(), balon.getY(), 55)) {
                double impulsoX = balon.getCentroX() < ConfiguracionJuego.ANCHO_PANEL / 2.0 ? 1.6 : -1.6;
                double impulsoY = balon.getCentroY() < ConfiguracionJuego.ALTO_PANEL / 2.0 ? 1.2 : -1.2;
                balon.impulsar(impulsoX, impulsoY, 1.2);
            }
            return;
        }

        // Un NPC atrapado en la esquina despeja para reactivar la jugada.
        if (poseedorBalon != null && poseedorBalon != jugadorPrincipal && estaEnEsquina(poseedorBalon.getX(), poseedorBalon.getY(), 65)) {
            boolean equipoLocal = esJugadorLocal(poseedorBalon);
            double dirX = equipoLocal ? 2.8 : -2.8;
            double dirY = poseedorBalon.getY() < ConfiguracionJuego.ALTO_PANEL / 2 ? 1.1 : -1.1;
            pegarBalonAlPoseedor();
            registrarSonido(TipoSonido.SAQUE);
            balon.impulsar(dirX, dirY, 1.8);
            balonLibre = true;
            poseedorBalon = null;
            balonEnManos = false;
            cooldownRoboFrames = ConfiguracionJuego.FPS / 3;
            cooldownCapturaLibreFrames = 5;
            ultimoPateador = null;
            bloqueoRecapturaUltimoPateadorFrames = 0;
            poseedorControlAccionNpc = null;
            framesPoseedorSinAccionNpc = 0;
        }
    }

    private void gestionarBarridas(EntradaJuego entrada) {
        if (framesRetrasoSaque > 0 || tipoReanudacionPendiente != TipoReanudacion.NINGUNA) {
            if (!modoEspectador && entrada != null) {
                entrada.consumirBarrida();
            }
            return;
        }

        if (poseedorBalon != null && !balonEnManos) {
            if (!modoEspectador && entrada != null && entrada.consumirBarrida()) {
                iniciarBarridaJugador(jugadorPrincipal, entrada.getDireccionAccionX(), entrada.getDireccionAccionY());
            }
            intentarBarridasNpc();
            resolverImpactosBarridaContraPoseedor();
            return;
        }

        if (!modoEspectador && entrada != null && entrada.consumirBarrida()) {
            iniciarBarridaJugador(jugadorPrincipal, entrada.getDireccionAccionX(), entrada.getDireccionAccionY());
        }
        resolverImpactosBarridaBalonLibre();
    }

    private void iniciarBarridaJugador(Jugador jugador, int direccionX, int direccionY) {
        if (!puedeIniciarBarrida(jugador)) {
            return;
        }
        double norma = Math.hypot(direccionX, direccionY);
        if (norma < 0.0001) {
            direccionX = jugador.getDireccionX();
            direccionY = jugador.getDireccionY();
            norma = Math.hypot(direccionX, direccionY);
        }
        if (norma < 0.0001) {
            direccionX = esJugadorLocal(jugador) ? 1 : -1;
            direccionY = 0;
            norma = 1.0;
        }
        activarBarrida(jugador, direccionX / norma, direccionY / norma);
    }

    private void intentarBarridasNpc() {
        if (poseedorBalon == null || balonEnManos || esPortero(poseedorBalon)) {
            return;
        }
        Jugador[] defensores = poseedorEsLocal ? getRivales() : getLocales();
        for (Jugador defensor : defensores) {
            if (!puedeIniciarBarrida(defensor)) {
                continue;
            }
            double distancia = distanciaEntre(defensor, poseedorBalon);
            if (distancia > DISTANCIA_BARRIDA_MAX) {
                continue;
            }
            double energia = energiaRelativa(defensor);
            if (energia < 0.18) {
                continue;
            }

            double amenaza = Math.max(0.0, (260.0 - distanciaHorizontalAlArco(poseedorBalon)) / 260.0);
            double presion = Math.max(0.0, (DISTANCIA_BARRIDA_MAX - distancia) / DISTANCIA_BARRIDA_MAX);
            double ventajaDefensiva = defensor.getInteligencia() * 0.004 + energia * 0.05;
            double probabilidad = 0.014 + amenaza * 0.10 + presion * 0.13 + ventajaDefensiva;
            if (defensor == seleccionarPresionadorEquipo(esJugadorLocal(defensor))) {
                probabilidad += 0.06;
            }
            probabilidad = Math.max(0.0, Math.min(0.42, probabilidad));
            if (aleatorio.nextDouble() >= probabilidad) {
                continue;
            }

            double objetivoX = poseedorBalon.getX() + poseedorBalon.getAncho() / 2.0;
            double objetivoY = poseedorBalon.getY() + poseedorBalon.getAlto() / 2.0;
            double origenX = defensor.getX() + defensor.getAncho() / 2.0;
            double origenY = defensor.getY() + defensor.getAlto() / 2.0;
            double dx = objetivoX - origenX;
            double dy = objetivoY - origenY;
            double norma = Math.hypot(dx, dy);
            if (norma < 0.0001) {
                continue;
            }
            activarBarrida(defensor, dx / norma, dy / norma);
            // Solo una barrida NPC por frame para no volver caotico el contacto.
            break;
        }
    }

    private void resolverImpactosBarridaContraPoseedor() {
        if (poseedorBalon == null || balonEnManos || esPortero(poseedorBalon)) {
            return;
        }

        Jugador poseedorActual = poseedorBalon;
        for (Jugador jugador : getTodosJugadores()) {
            if (jugador == poseedorActual || esJugadorLocal(jugador) == esJugadorLocal(poseedorActual)) {
                continue;
            }
            int indice = indiceJugador(jugador);
            if (indice < 0 || framesBarridaActiva[indice] <= 0) {
                continue;
            }

            double distancia = distanciaEntre(jugador, poseedorActual);
            if (distancia > DISTANCIA_IMPACTO_BARRIDA) {
                continue;
            }

            double probabilidadExito = calcularProbabilidadExitoBarrida(jugador, poseedorActual, distancia);
            boolean exito = aleatorio.nextDouble() < probabilidadExito;
            double severidad = calcularSeveridadBarrida(jugador, poseedorActual, distancia, exito);
            double probabilidadFalta = calcularProbabilidadFaltaBarrida(jugador, poseedorActual, distancia, severidad, exito);
            framesBarridaActiva[indice] = 0;
            barridaDireccionX[indice] = 0.0;
            barridaDireccionY[indice] = 0.0;

            if (aleatorio.nextDouble() < probabilidadFalta) {
                boolean faltaMarcada = sancionarFaltaPorBarrida(jugador, poseedorActual, severidad, exito);
                if (faltaMarcada) {
                    break;
                }
            }

            if (!exito) {
                continue;
            }

            boolean controlLimpio = distanciaRivalMasCercano(jugador, esJugadorLocal(jugador)) > 56.0
                && aleatorio.nextDouble() < 0.58;
            if (controlLimpio) {
                tomarPosesion(jugador, esJugadorLocal(jugador));
            } else {
                liberarBalonTrasBarrida(jugador, poseedorActual);
            }
            activarAccionArbitro(
                EstadoArbitraje.APLICA_VENTAJA,
                DURACION_ACCION_ARBITRO_CORTA_FRAMES,
                jugador.getX() + jugador.getAncho() / 2.0,
                jugador.getY() + jugador.getAlto() / 2.0,
                false
            );
            cooldownRoboFrames = Math.max(cooldownRoboFrames, ConfiguracionJuego.FPS / 4);
            registrarSonido(TipoSonido.ROBO);
            narrarRobo(jugador);
            break;
        }
    }

    private void resolverImpactosBarridaBalonLibre() {
        if (!balonLibre || balon.getAltura() > ALTURA_MAXIMA_CONTROL_DESCENSO) {
            return;
        }
        for (Jugador jugador : getTodosJugadores()) {
            int indice = indiceJugador(jugador);
            if (indice < 0 || framesBarridaActiva[indice] <= 0) {
                continue;
            }
            if (distanciaAlBalon(jugador) > DISTANCIA_IMPACTO_BARRIDA) {
                continue;
            }
            double[] direccion = direccionAlArcoContrario(jugador);
            double impulsoX = direccion[0] * 2.4 + barridaDireccionX[indice] * 1.8;
            double impulsoY = direccion[1] * 2.1 + barridaDireccionY[indice] * 1.5;
            balon.impulsar(impulsoX, impulsoY, 0.8);
            ultimoToqueLocal = esJugadorLocal(jugador);
            ultimoPateador = jugador;
            cooldownCapturaLibreFrames = Math.max(cooldownCapturaLibreFrames, 7);
            framesBarridaActiva[indice] = 0;
            barridaDireccionX[indice] = 0.0;
            barridaDireccionY[indice] = 0.0;
            registrarSonido(TipoSonido.ROBO);
            break;
        }
    }

    private double calcularProbabilidadExitoBarrida(Jugador barrendero, Jugador poseedor, double distancia) {
        double ventajaDistancia = Math.max(0.0, (DISTANCIA_IMPACTO_BARRIDA - distancia) / DISTANCIA_IMPACTO_BARRIDA) * 0.34;
        double energia = energiaRelativa(barrendero);
        double energiaPoseedor = energiaRelativa(poseedor);
        double inerciaBarrendero = velocidadMovimiento(barrendero) * 0.09;
        double inerciaPoseedor = velocidadMovimiento(poseedor) * 0.08;
        double lectura = (barrendero.getInteligencia() - poseedor.getInteligencia()) * 0.004;
        double base = 0.38 + ventajaDistancia + energia * 0.18 + inerciaBarrendero + lectura;
        base -= energiaPoseedor * 0.14 + inerciaPoseedor;
        return Math.max(0.16, Math.min(0.86, base));
    }

    private void liberarBalonTrasBarrida(Jugador barrendero, Jugador poseedorAnterior) {
        double x = (barrendero.getX() + poseedorAnterior.getX()) / 2.0;
        double y = (barrendero.getY() + poseedorAnterior.getY()) / 2.0;
        balon.setPosicion(x, y);
        balon.detener();
        double[] direccion = direccionAlArcoContrario(barrendero);
        balon.impulsar(direccion[0] * 2.8, direccion[1] * 2.1, 0.9);
        balonLibre = true;
        poseedorBalon = null;
        balonEnManos = false;
        ultimoToqueLocal = esJugadorLocal(barrendero);
        ultimoPateador = barrendero;
        cooldownCapturaLibreFrames = Math.max(cooldownCapturaLibreFrames, 8);
    }

    private boolean sancionarFaltaPorBarrida(Jugador infractor, Jugador victima, double severidad, boolean barridaLimpia) {
        TipoTarjeta tarjeta = resolverTarjetaFalta(infractor, victima, severidad, barridaLimpia);
        aplicarConsecuenciasFisicasFalta(infractor, victima, severidad, tarjeta, barridaLimpia);
        if (!arbitroMarcaFalta(infractor, victima, severidad, tarjeta)) {
            activarAccionArbitro(
                EstadoArbitraje.APLICA_VENTAJA,
                DURACION_ACCION_ARBITRO_MEDIA_FRAMES,
                victima.getX() + victima.getAncho() / 2.0,
                victima.getY() + victima.getAlto() / 2.0,
                false
            );
            narrar("🎭 El arbitro deja seguir tras la barrida sobre " + victima.getNombre(), true);
            return false;
        }

        boolean saqueLocal = esJugadorLocal(victima);
        int x = (int) Math.round(victima.getX() + victima.getAncho() / 2.0);
        int y = (int) Math.round(victima.getY() + victima.getAlto() / 2.0);
        java.awt.Point punto = cancha.normalizarPuntoLibreIndirecto(x, y);
        Jugador ejecutor = seleccionarCobradorCampo(saqueLocal, punto.x, punto.y);
        programarReanudacion(
            TipoReanudacion.LIBRE_INDIRECTO,
            ejecutor,
            saqueLocal,
            punto.x,
            punto.y,
            "🛑 Falta de " + infractor.getNombre()
        );
        infractor.registrarFaltaCometida();
        String mensajeTarjeta = aplicarTarjetaFalta(infractor, tarjeta);
        EstadoArbitraje estadoArbitraje = switch (tarjeta) {
            case AMARILLA -> EstadoArbitraje.TARJETA_AMARILLA;
            case ROJA -> EstadoArbitraje.TARJETA_ROJA;
            default -> EstadoArbitraje.MARCA_FALTA;
        };
        activarAccionArbitro(estadoArbitraje, DURACION_ACCION_ARBITRO_LARGA_FRAMES, punto.x, punto.y, true);
        eventoTransitorio = saqueLocal ? EventoJuego.FALTA_A_FAVOR : EventoJuego.FALTA_EN_CONTRA;
        registrarSonido(TipoSonido.SAQUE);
        narrar(
            mensajeTarjeta == null
                ? "🛑 El arbitro marca falta de " + infractor.getNombre()
                : "🛑 El arbitro marca falta de " + infractor.getNombre() + " y muestra " + mensajeTarjeta,
            true
        );
        return true;
    }

    private double calcularSeveridadBarrida(Jugador barrendero, Jugador poseedor, double distancia, boolean exito) {
        double cercania = Math.max(0.0, (DISTANCIA_IMPACTO_BARRIDA - distancia) / DISTANCIA_IMPACTO_BARRIDA);
        double inercia = Math.min(1.0, velocidadMovimiento(barrendero) / 8.0);
        double tecnicaDefensiva = Math.max(0.0, (barrendero.getEntrada() - 50) / 35.0);
        double disciplina = Math.max(0.0, (58 - barrendero.getDisciplina()) / 28.0);
        double lectura = Math.max(0.0, (50 - barrendero.getInteligencia()) / 30.0);
        double cansancio = Math.max(0.0, (0.42 - energiaRelativa(barrendero)) * 1.4);
        boolean llegaPorDetras = esJugadorLocal(poseedor)
            ? barrendero.getX() < poseedor.getX() - 6
            : barrendero.getX() > poseedor.getX() + 6;
        double severidad = 0.20
            + cercania * 0.28
            + inercia * 0.16
            + disciplina * 0.16
            + lectura * 0.10
            + cansancio * 0.08
            + (llegaPorDetras ? 0.12 : 0.0)
            - tecnicaDefensiva * 0.18;
        if (!exito) {
            severidad += 0.10;
        }
        return Math.max(0.08, Math.min(1.18, severidad));
    }

    private double calcularProbabilidadFaltaBarrida(Jugador barrendero, Jugador poseedor, double distancia, double severidad, boolean exito) {
        double ventajaDistancia = Math.max(0.0, (DISTANCIA_IMPACTO_BARRIDA * 0.82 - distancia) / DISTANCIA_IMPACTO_BARRIDA);
        double tecnica = (barrendero.getEntrada() - 50) * 0.0045;
        double disciplina = (50 - barrendero.getDisciplina()) * 0.0048;
        double lectura = (50 - barrendero.getInteligencia()) * 0.0032;
        double ventajaPoseedor = (poseedor.getInteligencia() - barrendero.getInteligencia()) * 0.0025;
        double base = 0.06 + severidad * 0.44 + ventajaDistancia * 0.12 + disciplina + lectura + ventajaPoseedor - tecnica;
        if (!exito) {
            base += 0.10;
        }
        return Math.max(0.05, Math.min(0.72, base));
    }

    private TipoTarjeta resolverTarjetaFalta(Jugador infractor, Jugador victima, double severidad, boolean barridaLimpia) {
        double distanciaArco = distanciaHorizontalAlArco(victima);
        double amenazaGol = Math.max(0.0, (210.0 - distanciaArco) / 210.0);
        double reincidencia = infractor.getFaltasCometidas() * 0.07 + infractor.getTarjetasAmarillas() * 0.20;
        double indisciplina = Math.max(0.0, (56 - infractor.getDisciplina()) / 26.0);
        double torpeza = Math.max(0.0, (54 - infractor.getEntrada()) / 28.0);
        double scoreAmarilla = severidad + amenazaGol * 0.18 + reincidencia + indisciplina * 0.34 + torpeza * 0.18;
        double scoreRoja = severidad + amenazaGol * 0.26 + reincidencia * 0.70 + indisciplina * 0.46 + torpeza * 0.24;
        if (!barridaLimpia) {
            scoreAmarilla += 0.08;
            scoreRoja += 0.10;
        }
        if (scoreRoja >= 1.18 || (scoreRoja >= 1.04 && aleatorio.nextDouble() < 0.34)) {
            return TipoTarjeta.ROJA;
        }
        if (scoreAmarilla >= 0.78 || (scoreAmarilla >= 0.64 && aleatorio.nextDouble() < 0.52)) {
            return TipoTarjeta.AMARILLA;
        }
        return TipoTarjeta.NINGUNA;
    }

    private boolean arbitroMarcaFalta(Jugador infractor, Jugador victima, double severidad, TipoTarjeta tarjeta) {
        double lectura = (arbitro.getInteligencia() - 50) * 0.007;
        double autoridad = (arbitro.getDisciplina() - 50) * 0.005;
        double reincidencia = infractor.getFaltasCometidas() * 0.03 + infractor.getTarjetasAmarillas() * 0.09;
        double teatro = Math.max(0.0, (58 - victima.getDisciplina()) * 0.0025);
        double base = 0.24 + severidad * 0.62 + lectura + autoridad + reincidencia + teatro;
        if (tarjeta == TipoTarjeta.AMARILLA) {
            base += 0.10;
        } else if (tarjeta == TipoTarjeta.ROJA) {
            base += 0.22;
        }
        if (severidad >= 0.92) {
            base = Math.max(base, 0.93);
        }
        return aleatorio.nextDouble() < Math.max(0.16, Math.min(0.98, base));
    }

    private void aplicarConsecuenciasFisicasFalta(Jugador infractor, Jugador victima, double severidad, TipoTarjeta tarjeta, boolean barridaLimpia) {
        double[] direccion = direccionHaciaJugador(infractor, victima);
        int framesDerribo = calcularFramesDerriboFalta(severidad, tarjeta, barridaLimpia);
        victima.derribar(framesDerribo, direccion[0], direccion[1]);

        double castigoStamina = 5.0 + severidad * 9.0 + (tarjeta == TipoTarjeta.ROJA ? 4.0 : 0.0);
        if (!barridaLimpia) {
            castigoStamina += 2.5;
        }
        victima.gastarStamina(castigoStamina);

        if (hayLesionPorFalta(victima, severidad, tarjeta)) {
            int framesLesion = ConfiguracionJuego.FPS * (4 + aleatorio.nextInt(6));
            victima.aplicarLesionTemporal(framesLesion);
            mostrarTextoSaque("🤕 Dolorido: " + victima.getNombre());
            narrar("🤕 " + victima.getNombre() + " queda resentido tras la falta", true);
        } else if (framesDerribo >= ConfiguracionJuego.FPS / 2) {
            mostrarTextoSaque("🛏️ " + victima.getNombre() + " queda tendido");
        }
    }

    private int calcularFramesDerriboFalta(double severidad, TipoTarjeta tarjeta, boolean barridaLimpia) {
        int base = 12 + (int) Math.round(severidad * 22.0);
        if (!barridaLimpia) {
            base += 5;
        }
        if (tarjeta == TipoTarjeta.AMARILLA) {
            base += 5;
        } else if (tarjeta == TipoTarjeta.ROJA) {
            base += 10;
        }
        return Math.max(10, Math.min(ConfiguracionJuego.FPS + 8, base));
    }

    private boolean hayLesionPorFalta(Jugador victima, double severidad, TipoTarjeta tarjeta) {
        double vulnerabilidad = Math.max(0.0, (0.42 - energiaRelativa(victima)) * 0.45);
        double base = 0.02 + Math.max(0.0, severidad - 0.58) * 0.20 + vulnerabilidad;
        if (tarjeta == TipoTarjeta.ROJA) {
            base += 0.07;
        } else if (tarjeta == TipoTarjeta.AMARILLA) {
            base += 0.03;
        }
        return aleatorio.nextDouble() < Math.max(0.01, Math.min(0.24, base));
    }

    private String aplicarTarjetaFalta(Jugador infractor, TipoTarjeta tarjeta) {
        if (tarjeta == TipoTarjeta.NINGUNA) {
            return null;
        }
        if (tarjeta == TipoTarjeta.ROJA) {
            infractor.recibirRojaDirecta();
            aplicarExpulsionJugador(infractor);
            mostrarTextoSaque("🟥 Roja para " + infractor.getNombre());
            return "roja";
        }

        boolean expulsion = infractor.recibirAmarilla();
        if (expulsion) {
            aplicarExpulsionJugador(infractor);
            mostrarTextoSaque("🟨🟥 Segunda amarilla y roja para " + infractor.getNombre());
            return "segunda amarilla y roja";
        }
        mostrarTextoSaque("🟨 Amarilla para " + infractor.getNombre());
        return "amarilla";
    }

    private void aplicarExpulsionJugador(Jugador jugador) {
        int indice = indiceJugador(jugador);
        if (indice >= 0) {
            framesBarridaActiva[indice] = 0;
            cooldownBarridaJugadorFrames[indice] = Math.max(cooldownBarridaJugadorFrames[indice], DURACION_PARTIDO_FRAMES);
            barridaDireccionX[indice] = 0.0;
            barridaDireccionY[indice] = 0.0;
        }
        if (poseedorBalon == jugador) {
            poseedorBalon = null;
            balonLibre = true;
            balonEnManos = false;
            cooldownCapturaLibreFrames = Math.max(cooldownCapturaLibreFrames, 10);
        }
        int destinoX = esJugadorLocal(jugador) ? -jugador.getAncho() - 80 : ConfiguracionJuego.ANCHO_PANEL + 80;
        int destinoY = ConfiguracionJuego.ALTO_PANEL / 2 + indiceRolCampo(jugador, esJugadorLocal(jugador)) * 18;
        jugador.setX(destinoX);
        jugador.setY(destinoY);
    }

    private boolean puedeIniciarBarrida(Jugador jugador) {
        if (jugador == null || jugador.estaExpulsado() || esPortero(jugador) || poseedorBalon == jugador || esEjecutorPendiente(jugador) || balonEnManos) {
            return false;
        }
        int indice = indiceJugador(jugador);
        if (indice < 0) {
            return false;
        }
        if (framesBarridaActiva[indice] > 0 || cooldownBarridaJugadorFrames[indice] > 0) {
            return false;
        }
        return jugador.getStamina() >= 14.0;
    }

    private void activarBarrida(Jugador jugador, double dirX, double dirY) {
        int indice = indiceJugador(jugador);
        if (indice < 0) {
            return;
        }
        framesBarridaActiva[indice] = DURACION_BARRIDA_FRAMES;
        cooldownBarridaJugadorFrames[indice] = COOLDOWN_BARRIDA_FRAMES;
        barridaDireccionX[indice] = dirX;
        barridaDireccionY[indice] = dirY;
        jugador.gastarStamina(COSTO_STAMINA_BARRIDA);
        jugador.activarAnimacionBarrida(DURACION_BARRIDA_FRAMES + 8, dirX, dirY);
        registrarSonido(TipoSonido.ROBO);
        if (jugador == jugadorPrincipal) {
            mostrarTextoSaque("🛝 Barrida de " + jugador.getNombre());
        }
    }

    private void aplicarMovimientoBarridasActivas() {
        for (Jugador jugador : getTodosJugadores()) {
            int indice = indiceJugador(jugador);
            if (indice < 0 || framesBarridaActiva[indice] <= 0) {
                continue;
            }
            double factor = Math.max(0.25, framesBarridaActiva[indice] / (double) DURACION_BARRIDA_FRAMES);
            int pasoX = (int) Math.round(barridaDireccionX[indice] * IMPULSO_BARRIDA_BASE * factor);
            int pasoY = (int) Math.round(barridaDireccionY[indice] * IMPULSO_BARRIDA_BASE * factor);
            if (pasoX == 0 && pasoY == 0) {
                continue;
            }
            jugador.mover(pasoX, pasoY);
            limitarEntidadAlPanel(jugador);
            if (esPortero(jugador)) {
                limitarPorteroEnZona(jugador, esJugadorLocal(jugador));
            }
            acumularMovimientoJugador(jugador, pasoX, pasoY);
        }
    }

    private void acumularMovimientoJugador(Jugador jugador, int extraX, int extraY) {
        if (jugador == jugadorPrincipal) {
            movPrincipalX += extraX;
            movPrincipalY += extraY;
            return;
        }
        if (jugador == aliadoLocal) {
            movAliadoX += extraX;
            movAliadoY += extraY;
            return;
        }
        if (jugador == extremoLocal) {
            movExtremoLocalX += extraX;
            movExtremoLocalY += extraY;
            return;
        }
        if (jugador == mediaLocal) {
            movMediaLocalX += extraX;
            movMediaLocalY += extraY;
            return;
        }
        if (jugador == porteroLocal) {
            movPorteroLocalX += extraX;
            movPorteroLocalY += extraY;
            return;
        }
        if (jugador == rivalUno) {
            movRivalUnoX += extraX;
            movRivalUnoY += extraY;
            return;
        }
        if (jugador == rivalDos) {
            movRivalDosX += extraX;
            movRivalDosY += extraY;
            return;
        }
        if (jugador == extremoRival) {
            movExtremoRivalX += extraX;
            movExtremoRivalY += extraY;
            return;
        }
        if (jugador == mediaRival) {
            movMediaRivalX += extraX;
            movMediaRivalY += extraY;
            return;
        }
        if (jugador == porteroRival) {
            movPorteroRivalX += extraX;
            movPorteroRivalY += extraY;
        }
    }

    private int indiceJugador(Jugador jugador) {
        for (int i = 0; i < todosJugadores.length; i++) {
            if (todosJugadores[i] == jugador) {
                return i;
            }
        }
        return -1;
    }

    private boolean estaEnEsquina(int x, int y, int margen) {
        boolean esquinaIzqSup = x <= margen && y <= margen;
        boolean esquinaDerSup = x >= ConfiguracionJuego.ANCHO_PANEL - margen && y <= margen;
        boolean esquinaIzqInf = x <= margen && y >= ConfiguracionJuego.ALTO_PANEL - margen;
        boolean esquinaDerInf = x >= ConfiguracionJuego.ANCHO_PANEL - margen && y >= ConfiguracionJuego.ALTO_PANEL - margen;
        return esquinaIzqSup || esquinaDerSup || esquinaIzqInf || esquinaDerInf;
    }

    private void intentarRobo() {
        if (cooldownRoboFrames > 0 || poseedorBalon == null) {
            return;
        }
        if (framesRetrasoSaque > 0 || tipoReanudacionPendiente != TipoReanudacion.NINGUNA) {
            return;
        }
        if (balonEnManos && esPortero(poseedorBalon)) {
            return;
        }

        // El robo no es instantaneo: exige cercania, compara inercia/velocidad
        // y suma una pequena variacion aleatoria para que las disputas no sean identicas.
        Jugador[] candidatos = poseedorEsLocal ? getRivales() : getLocales();
        for (Jugador candidato : candidatos) {
            if (candidato.estaExpulsado()) {
                continue;
            }
            if (!estanEnRangoDeRobo(candidato, poseedorBalon)) {
                continue;
            }

            double distancia = distanciaEntre(candidato, poseedorBalon);
            double bonoCercania = Math.max(0.0, 34.0 - distancia) * 0.22;
            double inerciaAtacante = velocidadMovimiento(candidato);
            double inerciaPoseedor = velocidadMovimiento(poseedorBalon);
            double fuerzaAtacante = candidato.getVelocidad() * 0.82 + inerciaAtacante * 1.18 + bonoCercania;
            double fuerzaPoseedor = poseedorBalon.getVelocidad() * 0.68 + inerciaPoseedor * 0.95 + 2.4;
            double tirada = aleatorio.nextDouble() * 2.4;

            if (fuerzaAtacante + tirada >= fuerzaPoseedor) {
                poseedorBalon = candidato;
                poseedorEsLocal = esJugadorLocal(candidato);
                ultimoToqueLocal = poseedorEsLocal;
                framesPoseedorAtascado = 0;
                poseedorControlAccionNpc = candidato;
                framesPoseedorSinAccionNpc = 0;
                cooldownRoboFrames = (int) (ConfiguracionJuego.FPS * 0.42);
                activarTransicionOfensiva(poseedorEsLocal, FRAMES_TRANSICION_EQUIPO + ConfiguracionJuego.FPS / 2);
                activarAccionArbitro(
                    EstadoArbitraje.APLICA_VENTAJA,
                    DURACION_ACCION_ARBITRO_CORTA_FRAMES,
                    candidato.getX() + candidato.getAncho() / 2.0,
                    candidato.getY() + candidato.getAlto() / 2.0,
                    false
                );
                registrarSonido(TipoSonido.ROBO);
                narrarRobo(candidato);
                break;
            }
        }
    }

    private void intentarCapturaBalonLibre() {
        // Evita recaptura instantanea justo despues de un pase o tiro.
        if (cooldownCapturaLibreFrames > 0) {
            return;
        }

        // Control base: piso. Control avanzado: balon descendente a media altura.
        boolean controlEnPiso = balon.getAltura() <= ALTURA_MAXIMA_CONTROL && balon.estaControlableEnPiso();
        boolean controlEnDescenso = puedeDomarBalonEnDescenso();
        if (!controlEnPiso && !controlEnDescenso) {
            return;
        }

        double rapidezMaxima = controlEnDescenso ? VELOCIDAD_MAXIMA_PARA_CONTROL + 0.45 : VELOCIDAD_MAXIMA_PARA_CONTROL;
        if (balon.getRapidez() > rapidezMaxima) {
            return;
        }

        Jugador mejorCandidato = null;
        double mejorPuntaje = Double.MAX_VALUE;
        for (Jugador jugador : getTodosJugadores()) {
            if (jugador.estaExpulsado()) {
                continue;
            }
            if (jugador == ultimoPateador && bloqueoRecapturaUltimoPateadorFrames > 0) {
                continue;
            }
            boolean contacto = intersecanRectangulos(
                jugador.getX(),
                jugador.getY(),
                jugador.getAncho(),
                jugador.getAlto(),
                balon.getX(),
                balon.getY(),
                balon.getAncho(),
                balon.getAlto()
            );
            double distancia = distanciaAlBalon(jugador);
            boolean controlProximo = distancia <= DISTANCIA_MAXIMA_CONTROL * 0.72;
            if (estaEnZonaDeControl(jugador) && (contacto || controlProximo)) {
                double penalizacionAgrupamiento = penalizacionAgrupamiento(jugador);
                double puntaje = distancia + penalizacionAgrupamiento;
                if (puntaje < mejorPuntaje) {
                    mejorPuntaje = puntaje;
                    mejorCandidato = jugador;
                }
            }
        }

        if (mejorCandidato == null) {
            return;
        }

        tomarPosesion(mejorCandidato, esJugadorLocal(mejorCandidato));
    }

    private boolean puedeDomarBalonEnDescenso() {
        if (balon.getAltura() > ALTURA_MAXIMA_CONTROL_DESCENSO || balon.getAltura() < ALTURA_MAXIMA_CONTROL) {
            return false;
        }
        return balon.getVelocidadZ() < -VELOCIDAD_VERTICAL_MINIMA_CONTROL;
    }

    private boolean ejecutarAccionBalonSiAplica(EntradaJuego entrada) {
        if (modoEspectador) {
            return false;
        }
        boolean pase = entrada.consumirPase();
        boolean tiro = entrada.consumirTiro();
        double factorPase = pase ? entrada.consumirFactorPase() : 0.0;
        double factorTiro = tiro ? entrada.consumirFactorTiro() : 0.0;
        if (balonEnManos && poseedorBalon != null && !esPortero(poseedorBalon) && tiro) {
            // En saque de banda no existe "tiro": se transforma en pase/lanzamiento.
            tiro = false;
            pase = true;
            factorPase = Math.max(factorPase, factorTiro > 0.0 ? factorTiro : 0.70);
        }
        if (!pase && !tiro) {
            return false;
        }

        // Las acciones del usuario solo valen con posesion local.
        if (poseedorBalon == null || !poseedorEsLocal) {
            return false;
        }

        arrastrarBalonConPoseedor();
        Jugador pateador = poseedorBalon;
        balonEnManos = false;
        if (tiro) {
            ejecutarTiro(entrada, factorTiro);
            registrarSonido(TipoSonido.TIRO);
        } else {
            double[] direccionPase = obtenerDireccionNormalizada(entrada, poseedorBalon);
            ejecutarPase(direccionPase, factorPase);
            registrarIntentoPase(pateador, esJugadorLocal(pateador));
            registrarSonido(TipoSonido.PASE);
        }
        balonLibre = true;
        ultimoToqueLocal = true;
        poseedorBalon = null;
        cooldownRoboFrames = ConfiguracionJuego.FPS / 3;
        cooldownCapturaLibreFrames = tiro ? 12 : 10;
        ultimoPateador = pateador;
        bloqueoRecapturaUltimoPateadorFrames = tiro
            ? (factorTiro >= 0.75 ? 20 : 16)
            : (factorPase >= 0.75 ? 14 : 10);
        return true;
    }

    private boolean ejecutarAccionNpcSiAplica() {
        if (poseedorBalon == null || (!modoEspectador && poseedorBalon == jugadorPrincipal)) {
            return false;
        }

        // IA ofensiva: compara si conviene tirar ahora o pasar para un mejor tiro.
        Jugador poseedor = poseedorBalon;
        Jugador presionCercana = rivalMasCercanoA(poseedor, esJugadorLocal(poseedor));
        Jugador receptor = seleccionarReceptorNpc(poseedor);
        double distanciaPresion = presionCercana == null ? Double.MAX_VALUE : distanciaEntre(poseedor, presionCercana);
        double distanciaArco = distanciaHorizontalAlArco(poseedor);
        boolean enZonaDeTiro = estaEnZonaDeTiro(poseedor);
        boolean bajoPresion = distanciaPresion < DISTANCIA_PRESION_ALTA;
        boolean esPortero = poseedor == porteroLocal || poseedor == porteroRival;
        boolean bloqueoEnZonaGol = !esPortero && estaAtascadoCercaDelArco(poseedor, distanciaPresion);
        boolean forzarAccionPorPosesionLarga = framesPoseedorSinAccionNpc >= FRAMES_MAX_POSESION_SIN_ACCION_NPC;
        boolean zonaDefinicion = !esPortero
            && distanciaArco < 185.0
            && Math.abs((poseedor.getY() + poseedor.getAlto() / 2.0)
                - (ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2.0)) < 125.0;
        if (getCooldownDecisionNpc(poseedor) > 0
            && !zonaDefinicion
            && !bloqueoEnZonaGol
            && !balonEnManos
            && !bajoPresion
            && distanciaArco > 360.0
            && !forzarAccionPorPosesionLarga) {
            return false;
        }
        boolean prioridadSaquePortero = esPortero && framesPrioridadSaquePortero > 0;
        if (esPortero
            && getRecuperacionPorteroFrames(poseedor) > 0
            && !balonEnManos
            && !prioridadSaquePortero
            && !forzarAccionPorPosesionLarga) {
            return false;
        }
        double sesgoTiro = sesgoTiroNpc(poseedor);
        double sesgoPase = sesgoPaseNpc(poseedor);
        double energiaPoseedor = energiaRelativa(poseedor);
        boolean enTransicion = enTransicionOfensiva(esJugadorLocal(poseedor));
        double ambicionGanadora = calcularAmbicionGanadora(esJugadorLocal(poseedor));
        double momentumEquipo = factorMomentumEquipo(esJugadorLocal(poseedor));
        sesgoTiro -= (1.0 - energiaPoseedor) * 14.0;
        sesgoPase += (1.0 - energiaPoseedor) * 10.0;
        sesgoTiro += ambicionGanadora * 16.0;
        sesgoPase -= ambicionGanadora * 8.0;
        sesgoTiro += momentumEquipo * 11.0;
        sesgoPase -= momentumEquipo * 3.5;
        if (enTransicion) {
            sesgoTiro += 8.0;
            sesgoPase -= 6.0;
        }
        // Agresividad base: todos los equipos priorizan progresar y finalizar jugadas.
        sesgoTiro += 10.0;
        sesgoPase -= 8.0;
        double scoreTiro = calcularScoreTiroNpc(poseedor, distanciaPresion, enZonaDeTiro);
        double scorePase = calcularScorePaseParaTiro(poseedor, receptor, distanciaPresion);
        boolean paseParaTiroMejor = receptor != null && scorePase + sesgoPase > scoreTiro + 34.0 + sesgoTiro;
        Jugador receptorLargo = seleccionarReceptorLargoNpc(poseedor);
        boolean paseLargoTactico = receptorLargo != null
            && !bajoPresion
            && distanciaArco > 210.0
            && energiaPoseedor > 0.34
            && aleatorio.nextDouble() < (enTransicion ? 0.48 : 0.34);
        boolean buscaRemate = debeBuscarRemateNpc(poseedor, distanciaArco, enZonaDeTiro, bajoPresion, scoreTiro, scorePase);
        boolean tiroSorpresa = !esPortero
            && enTransicion
            && !bajoPresion
            && distanciaArco > 210.0
            && distanciaArco < 390.0
            && scoreTiro >= scorePase - 18.0
            && aleatorio.nextDouble() < 0.22;
        boolean paseFiltrado = !esPortero
            && enTransicion
            && receptor != null
            && !bajoPresion
            && distanciaArco > 170.0
            && (distanciaHorizontalAlArco(poseedor) - distanciaHorizontalAlArco(receptor)) > 28.0
            && aleatorio.nextDouble() < 0.28;
        if (energiaPoseedor < 0.18 && receptor != null && !zonaDefinicion && !bloqueoEnZonaGol) {
            buscaRemate = false;
            paseParaTiroMejor = true;
        }
        if ((ambicionGanadora > 0.30 || momentumEquipo > 0.30) && distanciaArco < 390.0 && !esPortero) {
            buscaRemate = true;
        }
        boolean conducir = debeConducirAntesDePasar(poseedor, receptor, distanciaPresion, distanciaArco, bajoPresion, scoreTiro, scorePase);
        boolean conduccionSinProgreso = velocidadMovimiento(poseedor) <= 1 && distanciaPresion > 86.0 && distanciaArco > 230.0;
        if (conducir
            && !enTransicion
            && distanciaArco > 300.0
            && framesPoseedorSinAccionNpc < FRAMES_MAX_CONDUCCION_NPC
            && !conduccionSinProgreso
            && !forzarAccionPorPosesionLarga) {
            return false;
        }

        double[] direccion;
        double fuerza;
        double elevacion;
        double factorPotencia = 0.62;
        double distanciaPaseObjetivo = receptor == null ? 0.0 : distanciaEntre(poseedor, receptor);
        double riesgoLineaPase = 0.0;
        boolean esTiro;

        if (balonEnManos && !esPortero) {
            receptor = seleccionarReceptorLargoPortero(poseedor);
            distanciaPaseObjetivo = receptor == null ? 0.0 : distanciaEntre(poseedor, receptor);
            riesgoLineaPase = receptor == null ? 0.0 : riesgoIntercepcionPase(poseedor, receptor, esJugadorLocal(poseedor));
            factorPotencia = calcularFactorPaseNpc(poseedor, receptor, false, energiaPoseedor, true);
            direccion = receptor != null ? direccionPaseAnticipadoNpc(poseedor, receptor) : direccionAlArcoContrario(poseedor);
            fuerza = interpolarFuerza(FUERZA_PASE_MIN, FUERZA_PASE_MAX, factorPotencia);
            elevacion = calcularElevacionPaseNpc(factorPotencia, distanciaPaseObjetivo, true);
            esTiro = false;
            registrarSonido(TipoSonido.SAQUE);
        } else if (esPortero) {
            Jugador receptorSeguroPortero = seleccionarReceptorSeguroPortero(poseedor);
            Jugador receptorLargoPortero = seleccionarReceptorLargoPortero(poseedor);
            boolean salidaBajoPresion = poseedorBalon != null
                && amenazaDeDisparoDelPoseedor(esJugadorLocal(poseedor))
                && distanciaRivalMasCercano(poseedor, esJugadorLocal(poseedor)) < 120.0;
            Jugador receptorElegido = (!salidaBajoPresion && receptorSeguroPortero != null) ? receptorSeguroPortero : receptorLargoPortero;
            receptor = receptorElegido;
            distanciaPaseObjetivo = receptorElegido == null ? 0.0 : distanciaEntre(poseedor, receptorElegido);
            riesgoLineaPase = receptorElegido == null ? 0.0 : riesgoIntercepcionPase(poseedor, receptorElegido, esJugadorLocal(poseedor));
            factorPotencia = calcularFactorPaseNpc(poseedor, receptorElegido, salidaBajoPresion, energiaPoseedor, true);
            direccion = receptorElegido != null ? direccionPaseAnticipadoNpc(poseedor, receptorElegido) : direccionAlArcoContrario(poseedor);
            fuerza = interpolarFuerza(FUERZA_PASE_MIN, FUERZA_PASE_MAX, factorPotencia);
            elevacion = calcularElevacionPaseNpc(factorPotencia, distanciaPaseObjetivo, true);
            esTiro = false;
            registrarSonido(TipoSonido.SAQUE);
        } else if (zonaDefinicion) {
            factorPotencia = calcularFactorTiroNpc(distanciaArco, bajoPresion, true, energiaPoseedor);
            direccion = direccionTiroNpc(poseedor);
            fuerza = interpolarFuerza(FUERZA_TIRO_MIN, FUERZA_TIRO_MAX, factorPotencia);
            elevacion = calcularElevacionTiroNpc(factorPotencia, true);
            esTiro = true;
            registrarSonido(TipoSonido.TIRO);
        } else if (bloqueoEnZonaGol) {
            // Si se atasca cerca del area, prioriza rematar para destrabar la jugada.
            factorPotencia = calcularFactorTiroNpc(distanciaArco, true, true, energiaPoseedor);
            direccion = direccionTiroNpc(poseedor);
            fuerza = interpolarFuerza(FUERZA_TIRO_MIN, FUERZA_TIRO_MAX, factorPotencia);
            elevacion = calcularElevacionTiroNpc(factorPotencia, true);
            esTiro = true;
            registrarSonido(TipoSonido.TIRO);
        } else if (tiroSorpresa) {
            factorPotencia = Math.min(1.0, calcularFactorTiroNpc(distanciaArco, bajoPresion, enZonaDeTiro, energiaPoseedor) + 0.06);
            direccion = direccionTiroNpc(poseedor);
            fuerza = interpolarFuerza(FUERZA_TIRO_MIN, FUERZA_TIRO_MAX, factorPotencia);
            elevacion = calcularElevacionTiroNpc(factorPotencia, enZonaDeTiro);
            esTiro = true;
            registrarSonido(TipoSonido.TIRO);
        } else if (buscaRemate) {
            factorPotencia = calcularFactorTiroNpc(distanciaArco, bajoPresion, enZonaDeTiro, energiaPoseedor);
            direccion = direccionTiroNpc(poseedor);
            factorPotencia += Math.max(-0.05, Math.min(0.05, sesgoTiro / 180.0));
            fuerza = interpolarFuerza(FUERZA_TIRO_MIN, FUERZA_TIRO_MAX, factorPotencia);
            elevacion = calcularElevacionTiroNpc(factorPotencia, enZonaDeTiro);
            esTiro = true;
            registrarSonido(TipoSonido.TIRO);
        } else if (paseFiltrado) {
            distanciaPaseObjetivo = distanciaEntre(poseedor, receptor);
            riesgoLineaPase = riesgoIntercepcionPase(poseedor, receptor, esJugadorLocal(poseedor));
            factorPotencia = Math.max(0.74, calcularFactorPaseNpc(poseedor, receptor, false, energiaPoseedor, false));
            direccion = direccionPaseAnticipadoNpc(poseedor, receptor);
            fuerza = interpolarFuerza(FUERZA_PASE_MIN, FUERZA_PASE_MAX, factorPotencia);
            elevacion = calcularElevacionPaseNpc(factorPotencia, distanciaPaseObjetivo, false) + 0.25;
            esTiro = false;
            registrarSonido(TipoSonido.PASE);
        } else if (paseParaTiroMejor && !bajoPresion) {
            distanciaPaseObjetivo = receptor == null ? 0.0 : distanciaEntre(poseedor, receptor);
            riesgoLineaPase = receptor == null ? 0.0 : riesgoIntercepcionPase(poseedor, receptor, esJugadorLocal(poseedor));
            factorPotencia = calcularFactorPaseNpc(poseedor, receptor, false, energiaPoseedor, false);
            direccion = direccionPaseAnticipadoNpc(poseedor, receptor);
            fuerza = interpolarFuerza(FUERZA_PASE_MIN, FUERZA_PASE_MAX, factorPotencia);
            elevacion = calcularElevacionPaseNpc(factorPotencia, distanciaPaseObjetivo, false);
            esTiro = false;
            registrarSonido(TipoSonido.PASE);
        } else if (paseLargoTactico) {
            receptor = receptorLargo;
            distanciaPaseObjetivo = distanciaEntre(poseedor, receptorLargo);
            riesgoLineaPase = riesgoIntercepcionPase(poseedor, receptorLargo, esJugadorLocal(poseedor));
            factorPotencia = Math.max(0.78, calcularFactorPaseNpc(poseedor, receptorLargo, false, energiaPoseedor, false));
            direccion = direccionPaseAnticipadoNpc(poseedor, receptorLargo);
            fuerza = interpolarFuerza(FUERZA_PASE_MIN, FUERZA_PASE_MAX, factorPotencia);
            elevacion = calcularElevacionPaseNpc(factorPotencia, distanciaPaseObjetivo, false);
            esTiro = false;
            registrarSonido(TipoSonido.PASE);
        } else if (
            enZonaDeTiro
                || (bajoPresion && distanciaArco < 265.0 && Math.abs((poseedor.getY() + poseedor.getAlto() / 2.0)
                    - (ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2.0)) < 132.0)
                || distanciaArco < DISTANCIA_TIRO_CLARA + sesgoTiro * 0.45
                || (!bajoPresion && distanciaArco < 380.0 && scoreTiro > scorePase - 18.0 && aleatorio.nextDouble() < 0.52)
        ) {
            factorPotencia = calcularFactorTiroNpc(distanciaArco, bajoPresion, enZonaDeTiro, energiaPoseedor);
            direccion = direccionTiroNpc(poseedor);
            factorPotencia += Math.max(-0.05, Math.min(0.05, sesgoTiro / 180.0));
            fuerza = interpolarFuerza(FUERZA_TIRO_MIN, FUERZA_TIRO_MAX, factorPotencia);
            elevacion = calcularElevacionTiroNpc(factorPotencia, enZonaDeTiro);
            esTiro = true;
            registrarSonido(TipoSonido.TIRO);
        } else if (receptor != null) {
            distanciaPaseObjetivo = distanciaEntre(poseedor, receptor);
            riesgoLineaPase = riesgoIntercepcionPase(poseedor, receptor, esJugadorLocal(poseedor));
            factorPotencia = calcularFactorPaseNpc(poseedor, receptor, bajoPresion, energiaPoseedor, false);
            direccion = direccionPaseAnticipadoNpc(poseedor, receptor);
            fuerza = interpolarFuerza(FUERZA_PASE_MIN, FUERZA_PASE_MAX, factorPotencia);
            elevacion = calcularElevacionPaseNpc(factorPotencia, distanciaPaseObjetivo, false);
            esTiro = false;
            registrarSonido(TipoSonido.PASE);
        } else {
            // Fallback: ante cualquier ambiguedad, siempre libera la jugada.
            if (!esPortero && enZonaDeTiro && distanciaArco < 180.0) {
                factorPotencia = calcularFactorTiroNpc(distanciaArco, true, enZonaDeTiro, energiaPoseedor);
                direccion = direccionTiroNpc(poseedor);
                fuerza = interpolarFuerza(FUERZA_TIRO_MIN, FUERZA_TIRO_MAX, factorPotencia);
                elevacion = calcularElevacionTiroNpc(factorPotencia, enZonaDeTiro);
                esTiro = true;
                registrarSonido(TipoSonido.TIRO);
            } else {
                factorPotencia = calcularFactorPaseNpc(poseedor, null, true, energiaPoseedor, false);
                riesgoLineaPase = receptor == null ? 0.0 : riesgoIntercepcionPase(poseedor, receptor, esJugadorLocal(poseedor));
                direccion = receptor != null ? direccionPaseAnticipadoNpc(poseedor, receptor) : direccionAlArcoContrario(poseedor);
                fuerza = interpolarFuerza(FUERZA_PASE_MIN, FUERZA_PASE_MAX, factorPotencia);
                elevacion = calcularElevacionPaseNpc(factorPotencia, distanciaPaseObjetivo, false);
                esTiro = false;
                registrarSonido(TipoSonido.PASE);
            }
        }

        if (!esTiro && aplicarFueraDeJuegoSiCorresponde(poseedor, direccion, esJugadorLocal(poseedor))) {
            return true;
        }

        elevacion = ajustarElevacionAccionNpc(
            poseedor,
            receptor,
            esTiro,
            elevacion,
            distanciaPresion,
            distanciaPaseObjetivo,
            enZonaDeTiro,
            riesgoLineaPase
        );

        balonEnManos = false;
        if (!esTiro) {
            registrarIntentoPase(poseedor, esJugadorLocal(poseedor));
        }
        lanzarBalonDesdePoseedor(direccion, fuerza, elevacion);
        balonLibre = true;
        ultimoToqueLocal = esJugadorLocal(poseedor);
        poseedorBalon = null;
        if (esPortero) {
            framesPrioridadSaquePortero = 0;
        }
        cooldownRoboFrames = ConfiguracionJuego.FPS / 3;
        cooldownCapturaLibreFrames = calcularCooldownCapturaNpc(esTiro, esPortero, factorPotencia, distanciaPaseObjetivo, elevacion);
        setCooldownDecisionNpc(
            poseedor,
            Math.max(0, (COOLDOWN_DECISION_NPC / 2) + ajusteCooldownDecisionNpc(poseedor) + ajusteDecisionPorEnergia(poseedor))
        );
        ultimoPateador = poseedor;
        bloqueoRecapturaUltimoPateadorFrames = esPortero ? 18 : 20;
        poseedorControlAccionNpc = null;
        framesPoseedorSinAccionNpc = 0;
        return true;
    }

    private boolean estaAtascadoCercaDelArco(Jugador poseedor, double distanciaPresion) {
        if (distanciaHorizontalAlArco(poseedor) > 245.0) {
            return false;
        }
        if (velocidadMovimiento(poseedor) > 2) {
            return false;
        }
        boolean bajoMarcajeFuerte = distanciaPresion < 58.0;
        boolean enBandaProfunda = poseedor.getY() < ConfiguracionJuego.CAMPO_Y_MIN + 86
            || poseedor.getY() + poseedor.getAlto() > ConfiguracionJuego.CAMPO_Y_MAX - 86;
        return bajoMarcajeFuerte || enBandaProfunda;
    }

    private boolean debeConducirAntesDePasar(
        Jugador poseedor,
        Jugador receptor,
        double distanciaPresion,
        double distanciaArco,
        boolean bajoPresion,
        double scoreTiro,
        double scorePase
    ) {
        if (poseedor == porteroLocal || poseedor == porteroRival || balonEnManos) {
            return false;
        }
        if (bajoPresion || distanciaPresion < 78.0) {
            return false;
        }
        if (distanciaArco < 175.0) {
            return false;
        }
        if (energiaRelativa(poseedor) < 0.26) {
            return false;
        }
        double espacioConduccion = distanciaRivalMasCercano(poseedor, esJugadorLocal(poseedor));
        double mejoraPase = receptor == null
            ? -1000.0
            : distanciaHorizontalAlArco(poseedor) - distanciaHorizontalAlArco(receptor);
        boolean paseNoMejora = receptor == null || scorePase < scoreTiro + 10.0 || mejoraPase < 30.0;
        return espacioConduccion > 84.0 && paseNoMejora;
    }

    private boolean debeBuscarRemateNpc(
        Jugador poseedor,
        double distanciaArco,
        boolean enZonaDeTiro,
        boolean bajoPresion,
        double scoreTiro,
        double scorePase
    ) {
        if (poseedor == porteroLocal || poseedor == porteroRival) {
            return false;
        }
        boolean delantero = esDelanteroNpc(poseedor);
        boolean extremo = esExtremoNpc(poseedor);
        boolean mediocampista = esMediocampistaNpc(poseedor);
        // Todos los NPC de campo priorizan buscar gol cuando hay oportunidad real.
        if (enZonaDeTiro && distanciaArco < (delantero ? 360.0 : extremo ? 330.0 : 315.0)) {
            return true;
        }
        if (bajoPresion && distanciaArco < (delantero ? 320.0 : extremo ? 292.0 : 276.0)) {
            return true;
        }
        double umbralAceptable = mediocampista ? -8.0 : extremo ? -12.0 : -18.0;
        double umbralMedia = mediocampista ? 2.0 : extremo ? -2.0 : -8.0;
        boolean oportunidadAceptable = distanciaArco < (delantero ? 382.0 : extremo ? 350.0 : 332.0)
            && scoreTiro >= scorePase + umbralAceptable;
        boolean oportunidadMedianamenteBuena = distanciaArco < (delantero ? 420.0 : extremo ? 388.0 : 360.0)
            && scoreTiro >= scorePase + umbralMedia;
        return oportunidadAceptable || oportunidadMedianamenteBuena;
    }

    private double calcularScoreTiroNpc(Jugador tirador, double distanciaPresion, boolean enZonaDeTiro) {
        double distanciaArco = distanciaHorizontalAlArco(tirador);
        double riesgoBloqueo = riesgoIntercepcionTiro(tirador);
        double factorInteligencia = (tirador.getInteligencia() - 50) / 50.0;
        Jugador porteroOponente = esJugadorLocal(tirador) ? porteroRival : porteroLocal;
        double porteroDescolocado = Math.abs(
            (porteroOponente.getY() + porteroOponente.getAlto() / 2.0)
                - (ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2.0)
        );
        double score = enZonaDeTiro ? 34.0 : -16.0;
        score += Math.max(0.0, 320.0 - distanciaArco) * 0.27;
        score += Math.min(40.0, distanciaPresion) * 0.16;
        score += Math.max(0.0, porteroDescolocado - 28.0) * 0.12;
        score -= Math.max(0.0, distanciaArco - 210.0) * (0.18 + Math.max(0.0, factorInteligencia) * 0.08);
        score += factorInteligencia * 3.2;
        score -= riesgoBloqueo * 20.0;
        score += 10.0;
        return score;
    }

    private double calcularScorePaseParaTiro(Jugador pasador, Jugador receptor, double distanciaPresionPasador) {
        if (receptor == null) {
            return -1000.0;
        }
        double distanciaPase = distanciaEntre(pasador, receptor);
        if (distanciaPase > DISTANCIA_PASE_SEGURA) {
            return -1000.0;
        }

        double distanciaArcoPasador = distanciaHorizontalAlArco(pasador);
        double distanciaArcoReceptor = distanciaHorizontalAlArco(receptor);
        double separacionReceptor = distanciaRivalMasCercano(receptor, esJugadorLocal(receptor));
        double riesgoIntercepcion = riesgoIntercepcionPase(pasador, receptor, esJugadorLocal(pasador));
        double factorInteligencia = (pasador.getInteligencia() - 50) / 50.0;
        double score = 0.0;
        score += Math.max(0.0, distanciaArcoPasador - distanciaArcoReceptor) * 0.32;
        score += Math.min(80.0, separacionReceptor) * 0.22;
        score += Math.max(0.0, distanciaPresionPasador - 12.0) * 0.08;
        score -= Math.max(0.0, distanciaPase - DISTANCIA_PASE_NPC) * 0.22;
        if (distanciaArcoReceptor >= distanciaArcoPasador - 12.0) {
            // Evita secuencias de toques laterales que no acercan al arco.
            score -= 40.0;
        }
        if (distanciaPase < 78.0 && distanciaPresionPasador > 70.0) {
            // Evita pases cortos innecesarios cuando hay espacio para conducir.
            score -= 26.0;
        }
        if (distanciaPase < 96.0 && distanciaArcoReceptor >= distanciaArcoPasador - 18.0) {
            score -= 24.0;
        }
        score -= riesgoIntercepcion * (120.0 + Math.max(0.0, factorInteligencia) * 28.0);
        score += factorInteligencia * 6.0;
        if (estaEnZonaDeTiro(receptor)) {
            score += 20.0;
        }
        return score;
    }

    private boolean estanEnRangoDeRobo(Jugador atacante, Jugador poseedor) {
        // El robo no depende solo de solape exacto: permite "meter pie" en corta distancia.
        Rectangle zonaRobo = new Rectangle(
            poseedor.getX() - 18,
            poseedor.getY() - 14,
            poseedor.getAncho() + 36,
            poseedor.getAlto() + 28
        );
        if (atacante.getBounds().intersects(zonaRobo)) {
            return true;
        }
        double distancia = distanciaEntre(atacante, poseedor);
        double alcance = 48.0 + Math.min(12.0, atacante.getVelocidad() * 1.6);
        return distancia <= alcance;
    }

    private void ejecutarTiro(EntradaJuego entrada, double factorCarga) {
        // El tiro siempre prioriza el arco rival y usa el input como ajuste fino.
        double[] direccion = obtenerDireccionTiro(entrada, poseedorBalon);
        double fuerza = interpolarFuerza(FUERZA_TIRO_MIN, FUERZA_TIRO_MAX, factorCarga);
        // Tiro mas raso para que viaje rapido y sea mas dificil de atajar.
        double elevacion = 2.0 + factorCarga * 2.9;
        lanzarBalonDesdePoseedor(direccion, fuerza, elevacion);
    }

    private void ejecutarPase(EntradaJuego entrada, double factorCarga) {
        // El pase usa la misma mecanica de carga, con elevacion mas baja.
        double[] direccion = obtenerDireccionNormalizada(entrada, poseedorBalon);
        ejecutarPase(direccion, factorCarga);
    }

    private void ejecutarPase(double[] direccion, double factorCarga) {
        // Variante para reutilizar direccion cuando se valida fuera de juego.
        double fuerza = interpolarFuerza(FUERZA_PASE_MIN, FUERZA_PASE_MAX, factorCarga);
        // El pase tiende a viajar mas raso para favorecer la recepcion.
        double elevacion = 1.0 + factorCarga * 1.6;
        lanzarBalonDesdePoseedor(direccion, fuerza, elevacion);
    }

    private double interpolarFuerza(double min, double max, double factorCarga) {
        double factor = Math.max(0.0, Math.min(1.0, factorCarga));
        return min + (max - min) * factor;
    }

    private double calcularFactorPaseNpc(
        Jugador pasador,
        Jugador receptor,
        boolean bajoPresion,
        double energiaPoseedor,
        boolean saquePortero
    ) {
        double distancia = receptor == null ? 215.0 : distanciaEntre(pasador, receptor);
        double base;
        if (distancia < 110.0) {
            // Pase corto.
            base = 0.46;
        } else if (distancia < 210.0) {
            // Pase medio.
            base = 0.60;
        } else {
            // Pase largo.
            base = 0.84;
        }
        if (bajoPresion) {
            base += 0.08;
        }
        if (saquePortero) {
            base += 0.10;
        }
        // Cansado: evita golpear siempre al maximo.
        base -= (1.0 - energiaPoseedor) * 0.10;
        return Math.max(0.28, Math.min(0.98, base));
    }

    private double calcularFactorTiroNpc(double distanciaArco, boolean bajoPresion, boolean enZonaDeTiro, double energiaPoseedor) {
        double base;
        if (distanciaArco < 150.0) {
            // Cerca: mas colocado que potente.
            base = 0.66;
        } else if (distanciaArco < 245.0) {
            // Media distancia.
            base = 0.78;
        } else {
            // Lejos: necesita mas fuerza.
            base = 0.92;
        }
        if (bajoPresion) {
            base += 0.06;
        }
        if (!enZonaDeTiro) {
            base += 0.04;
        }
        base -= (1.0 - energiaPoseedor) * 0.08;
        return Math.max(0.52, Math.min(1.00, base));
    }

    private double calcularElevacionPaseNpc(double factorPotencia, double distanciaPase, boolean saquePortero) {
        double elevacion = 0.8 + factorPotencia * 1.8;
        if (distanciaPase > 200.0 || saquePortero) {
            elevacion += 0.5;
        }
        return Math.max(0.8, Math.min(3.8, elevacion));
    }

    private double calcularElevacionTiroNpc(double factorPotencia, boolean enZonaDeTiro) {
        double elevacion = enZonaDeTiro ? (2.2 + factorPotencia * 1.8) : (2.8 + factorPotencia * 2.0);
        return Math.max(2.0, Math.min(4.8, elevacion));
    }

    private double ajustarElevacionAccionNpc(
        Jugador poseedor,
        Jugador receptor,
        boolean esTiro,
        double elevacionBase,
        double distanciaPresion,
        double distanciaPaseObjetivo,
        boolean enZonaDeTiro,
        double riesgoLineaPase
    ) {
        double elevacion = elevacionBase;
        if (esTiro) {
            double riesgoBloqueo = riesgoIntercepcionTiro(poseedor);
            boolean porteroAdelantado = porteroAdelantadoRival(poseedor);
            if (!enZonaDeTiro && riesgoBloqueo < 0.60) {
                elevacion -= 0.55;
            }
            if (enZonaDeTiro && porteroAdelantado && distanciaPresion > 72.0) {
                elevacion += 0.60;
            }
            if (riesgoBloqueo > 1.0) {
                elevacion += 0.35;
            }
            return Math.max(1.9, Math.min(5.2, elevacion));
        }

        if (receptor == null) {
            return Math.max(0.7, Math.min(4.2, elevacion));
        }
        boolean paseLargo = distanciaPaseObjetivo > 210.0;
        boolean bajoPresion = distanciaPresion < DISTANCIA_PRESION_ALTA;
        if (paseLargo || riesgoLineaPase > 0.82) {
            elevacion += 0.42;
        } else if (distanciaPaseObjetivo < 140.0 && riesgoLineaPase < 0.34 && !bajoPresion) {
            elevacion -= 0.32;
        }
        if (bajoPresion && distanciaPaseObjetivo > 150.0) {
            elevacion += 0.22;
        }
        return Math.max(0.7, Math.min(4.2, elevacion));
    }

    private boolean porteroAdelantadoRival(Jugador atacante) {
        Jugador portero = esJugadorLocal(atacante) ? porteroRival : porteroLocal;
        double lineaPorteria = esJugadorLocal(atacante) ? ConfiguracionJuego.CAMPO_X_MAX : ConfiguracionJuego.CAMPO_X_MIN;
        double centroPortero = portero.getX() + portero.getAncho() / 2.0;
        return Math.abs(centroPortero - lineaPorteria) > 72.0;
    }

    private int calcularCooldownCapturaNpc(boolean esTiro, boolean esPortero, double factorPotencia, double distanciaPase, double elevacion) {
        int base = esTiro ? 13 : 8;
        if (esPortero) {
            base += 3;
        }
        base += (int) Math.round(factorPotencia * 8.0);
        if (!esTiro && distanciaPase > 210.0) {
            base += 3;
        }
        if (elevacion > 2.7) {
            base += 2;
        }
        return Math.max(7, Math.min(22, base));
    }

    private double[] obtenerDireccionNormalizada(EntradaJuego entrada, Jugador ejecutor) {
        double dx = entrada.getDireccionAccionX();
        double dy = entrada.getDireccionAccionY();
        double magnitud = Math.sqrt(dx * dx + dy * dy);
        if (magnitud < 0.0001) {
            // Sin direccion de input, usa orientacion corporal del jugador.
            if (ejecutor != null) {
                return direccionSegunOrientacion(ejecutor);
            }
            return new double[] { 1.0, 0.0 };
        }
        return new double[] { dx / magnitud, dy / magnitud };
    }

    private double[] obtenerDireccionTiro(EntradaJuego entrada, Jugador tirador) {
        if (tirador == null) {
            return obtenerDireccionNormalizada(entrada, null);
        }

        // El tiro no apunta al cursor: construye un objetivo en la porteria rival
        // y combina input con orientacion corporal para que el golpeo siga el gesto del jugador.
        boolean local = esJugadorLocal(tirador);
        double arcoX = local ? ConfiguracionJuego.CAMPO_X_MAX + 10.0 : ConfiguracionJuego.CAMPO_X_MIN - 10.0;
        double centroPorteriaY = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2.0;
        double sesgoVertical = entrada.getDireccionAccionY() * 96.0;
        double sesgoHorizontal = entrada.getDireccionAccionX() * 18.0;
        double orientY = direccionSegunOrientacion(tirador)[1];
        double objetivoPosteY = centroPorteriaY + (orientY >= 0 ? 58.0 : -58.0);
        if (Math.abs(orientY) < 0.2) {
            objetivoPosteY = centroPorteriaY + (aleatorio.nextBoolean() ? 54.0 : -54.0);
        }
        double objetivoBaseX = arcoX + sesgoHorizontal;
        double objetivoBaseY = (centroPorteriaY + sesgoVertical) * 0.45 + objetivoPosteY * 0.55;

        double origenX = tirador.getX() + tirador.getAncho() / 2.0;
        double origenY = tirador.getY() + tirador.getAlto() / 2.0;
        double[] orientacion = direccionSegunOrientacion(tirador);
        double objetivoOrientX = origenX + orientacion[0] * 250.0;
        double objetivoOrientY = origenY + orientacion[1] * 160.0;
        double magnitudInput = Math.sqrt(
            entrada.getDireccionAccionX() * entrada.getDireccionAccionX()
                + entrada.getDireccionAccionY() * entrada.getDireccionAccionY()
        );
        double pesoOrientacion = magnitudInput < 0.2 ? 0.50 : 0.26;
        double objetivoX = objetivoBaseX * (1.0 - pesoOrientacion) + objetivoOrientX * pesoOrientacion;
        double objetivoY = objetivoBaseY * (1.0 - pesoOrientacion) + objetivoOrientY * pesoOrientacion;
        objetivoY = Math.max(
            ConfiguracionJuego.Y_PORTERIA + 8.0,
            Math.min(ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA - 8.0, objetivoY)
        );
        double dx = objetivoX - origenX;
        double dy = objetivoY - origenY;
        double magnitud = Math.sqrt(dx * dx + dy * dy);
        if (magnitud < 0.0001) {
            return new double[] { local ? 1.0 : -1.0, 0.0 };
        }
        return new double[] { dx / magnitud, dy / magnitud };
    }

    private double[] direccionSegunOrientacion(Jugador jugador) {
        int dirX = jugador.getDireccionX();
        int dirY = jugador.getDireccionY();
        double magnitud = Math.sqrt(dirX * dirX + dirY * dirY);
        if (magnitud < 0.0001) {
            return new double[] { esJugadorLocal(jugador) ? 1.0 : -1.0, 0.0 };
        }
        return new double[] { dirX / magnitud, dirY / magnitud };
    }

    private double[] direccionHaciaJugador(Jugador origen, Jugador destino) {
        double dx = (destino.getX() + destino.getAncho() / 2.0) - (origen.getX() + origen.getAncho() / 2.0);
        double dy = (destino.getY() + destino.getAlto() / 2.0) - (origen.getY() + origen.getAlto() / 2.0);
        double magnitud = Math.sqrt(dx * dx + dy * dy);
        if (magnitud < 0.0001) {
            return direccionAlArcoContrario(origen);
        }
        return new double[] { dx / magnitud, dy / magnitud };
    }

    private double[] direccionHaciaPunto(Jugador origen, double destinoX, double destinoY) {
        double origenX = origen.getX() + origen.getAncho() / 2.0;
        double origenY = origen.getY() + origen.getAlto() / 2.0;
        double dx = destinoX - origenX;
        double dy = destinoY - origenY;
        double magnitud = Math.sqrt(dx * dx + dy * dy);
        if (magnitud < 0.0001) {
            return direccionAlArcoContrario(origen);
        }
        return new double[] { dx / magnitud, dy / magnitud };
    }

    private double[] direccionPaseAnticipadoNpc(Jugador origen, Jugador receptor) {
        if (receptor == null) {
            return direccionAlArcoContrario(origen);
        }
        double distancia = distanciaEntre(origen, receptor);
        double framesAdelanto = Math.max(3.0, Math.min(11.0, distancia / 30.0));
        double objetivoX = receptor.getX() + receptor.getAncho() / 2.0 + movimientoXDe(receptor) * framesAdelanto;
        double objetivoY = receptor.getY() + receptor.getAlto() / 2.0 + movimientoYDe(receptor) * framesAdelanto;
        objetivoX = Math.max(ConfiguracionJuego.CAMPO_X_MIN + 14.0, Math.min(ConfiguracionJuego.CAMPO_X_MAX - 14.0, objetivoX));
        objetivoY = Math.max(ConfiguracionJuego.CAMPO_Y_MIN + 14.0, Math.min(ConfiguracionJuego.CAMPO_Y_MAX - 14.0, objetivoY));
        return direccionHaciaPunto(origen, objetivoX, objetivoY);
    }

    private double[] direccionAlArcoContrario(Jugador jugador) {
        double arcoX = esJugadorLocal(jugador) ? ConfiguracionJuego.CAMPO_X_MAX : ConfiguracionJuego.CAMPO_X_MIN;
        double arcoY = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2.0;
        double dx = arcoX - (jugador.getX() + jugador.getAncho() / 2.0);
        double dy = arcoY - (jugador.getY() + jugador.getAlto() / 2.0);
        double magnitud = Math.sqrt(dx * dx + dy * dy);
        if (magnitud < 0.0001) {
            return new double[] { esJugadorLocal(jugador) ? 1.0 : -1.0, 0.0 };
        }
        return new double[] { dx / magnitud, dy / magnitud };
    }

    private double[] direccionTiroNpc(Jugador jugador) {
        double arcoX = esJugadorLocal(jugador) ? ConfiguracionJuego.CAMPO_X_MAX + 6.0 : ConfiguracionJuego.CAMPO_X_MIN - 6.0;
        double centroPorteriaY = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2.0;
        Jugador portero = esJugadorLocal(jugador) ? porteroRival : porteroLocal;
        double yPortero = portero.getY() + portero.getAlto() / 2.0;
        double mitadSuperior = centroPorteriaY - 54.0;
        double mitadInferior = centroPorteriaY + 54.0;
        double objetivoY;
        if (yPortero >= centroPorteriaY) {
            objetivoY = mitadSuperior;
        } else {
            objetivoY = mitadInferior;
        }
        if (aleatorio.nextDouble() < 0.16) {
            objetivoY = centroPorteriaY + (aleatorio.nextBoolean() ? 24.0 : -24.0);
        }
        objetivoY = Math.max(ConfiguracionJuego.Y_PORTERIA + 8.0, Math.min(ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA - 8.0, objetivoY));
        return direccionHaciaPunto(jugador, arcoX, objetivoY);
    }

    private Jugador seleccionarReceptorLocal(Jugador pasador) {
        Jugador[] opciones = getLocales();
        Jugador mejor = null;
        double mejorPuntaje = Double.MAX_VALUE;
        for (Jugador candidato : opciones) {
            if (candidato == pasador || candidato.estaExpulsado()) {
                continue;
            }
            double dx = candidato.getX() - pasador.getX();
            double dy = candidato.getY() - pasador.getY();
            double penalizacionAtras = dx < -5 ? 180.0 : 0.0;
            double puntaje = dx * dx + dy * dy + penalizacionAtras;
            if (puntaje < mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejor = candidato;
            }
        }
        return mejor;
    }

    private Jugador seleccionarReceptorNpc(Jugador pasador) {
        // El receptor se elige por una heuristica:
        // distancia razonable, evitar pase comprometido hacia atras
        // y preferir companeros menos marcados.
        Jugador[] opciones = esJugadorLocal(pasador) ? getLocales() : getRivales();
        Jugador mejor = null;
        double mejorPuntaje = Double.MAX_VALUE;
        Jugador rivalPresion = rivalMasCercanoA(pasador, esJugadorLocal(pasador));
        double distanciaPresionPasador = rivalPresion == null ? Double.MAX_VALUE : distanciaEntre(pasador, rivalPresion);
        boolean bajoPresion = distanciaPresionPasador < DISTANCIA_PRESION_ALTA;
        boolean presionExtrema = distanciaPresionPasador < 56.0;
        boolean pasadorPortero = esPortero(pasador);
        double centroPasadorX = pasador.getX() + pasador.getAncho() / 2.0;
        boolean enCampoRival = esJugadorLocal(pasador)
            ? centroPasadorX > ConfiguracionJuego.ANCHO_PANEL / 2.0
            : centroPasadorX < ConfiguracionJuego.ANCHO_PANEL / 2.0;
        for (Jugador candidato : opciones) {
            if (candidato == pasador || candidato.estaExpulsado()) {
                continue;
            }

            double dx = candidato.getX() - pasador.getX();
            double dy = candidato.getY() - pasador.getY();
            // Avance positivo siempre significa progresar hacia el arco rival.
            double avance = esJugadorLocal(pasador) ? dx : -dx;
            double distancia = Math.sqrt(dx * dx + dy * dy);
            if (distancia > DISTANCIA_PASE_SEGURA) {
                continue;
            }
            if (!pasadorPortero && enCampoRival && !presionExtrema && avance < 0.0) {
                continue;
            }
            // Evita ciclo de pases hacia atras cuando hay margen para progresar.
            if (!pasadorPortero && !bajoPresion && avance < -12.0) {
                continue;
            }

            Jugador marcador = rivalMasCercanoA(candidato, esJugadorLocal(candidato));
            double separacion = marcador == null ? DISTANCIA_PASE_SEGURA : distanciaEntre(candidato, marcador);
            double riesgoLinea = riesgoIntercepcionPase(pasador, candidato, esJugadorLocal(pasador));
            double penalizacionMarca = separacion < DISTANCIA_PRESION_ALTA ? 240.0 : 0.0;
            double penalizacionLinea = riesgoLinea * 260.0;
            double bonificacionZonaTiro = estaEnZonaDeTiro(candidato) ? -70.0 : 0.0;
            double retroceso = Math.max(0.0, -avance);
            double progreso = Math.max(0.0, avance);
            double penalizacionAtras = retroceso * (bajoPresion ? 10.0 : 22.0);
            double penalizacionCortoEsteril = (distancia < 84.0 && progreso < 16.0) ? 320.0 : 0.0;
            double bonificacionProgresion = progreso * 3.6;
            double bonificacionRuptura = progreso > 68.0 ? 150.0 : 0.0;
            double puntaje =
                dx * dx
                    + dy * dy
                    + penalizacionAtras
                    + penalizacionCortoEsteril
                    + penalizacionMarca
                    + penalizacionLinea
                    + bonificacionZonaTiro
                    - bonificacionProgresion
                    - bonificacionRuptura;
            if (puntaje < mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejor = candidato;
            }
        }
        return mejor;
    }

    private double riesgoIntercepcionPase(Jugador pasador, Jugador receptor, boolean pasadorEsLocal) {
        Jugador[] rivales = pasadorEsLocal ? getRivales() : getLocales();
        double x1 = pasador.getX() + pasador.getAncho() / 2.0;
        double y1 = pasador.getY() + pasador.getAlto() / 2.0;
        double x2 = receptor.getX() + receptor.getAncho() / 2.0;
        double y2 = receptor.getY() + receptor.getAlto() / 2.0;
        double longitud = Math.max(1.0, Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));
        double riesgo = 0.0;
        for (Jugador rival : rivales) {
            if (esPortero(rival) || rival.estaExpulsado()) {
                continue;
            }
            double px = rival.getX() + rival.getAncho() / 2.0;
            double py = rival.getY() + rival.getAlto() / 2.0;
            double distancia = distanciaPuntoASegmento(px, py, x1, y1, x2, y2);
            if (distancia < 26.0) {
                riesgo += (26.0 - distancia) / 26.0;
            }
        }
        return Math.min(1.8, riesgo / Math.max(1.0, longitud / 180.0));
    }

    private double riesgoIntercepcionTiro(Jugador tirador) {
        boolean tiradorLocal = esJugadorLocal(tirador);
        Jugador[] bloqueadores = tiradorLocal ? getRivales() : getLocales();
        double x1 = tirador.getX() + tirador.getAncho() / 2.0;
        double y1 = tirador.getY() + tirador.getAlto() / 2.0;
        double x2 = tiradorLocal ? ConfiguracionJuego.CAMPO_X_MAX + 6.0 : ConfiguracionJuego.CAMPO_X_MIN - 6.0;
        double y2 = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2.0;
        double longitud = Math.max(1.0, Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));
        double riesgo = 0.0;
        for (Jugador rival : bloqueadores) {
            if (esPortero(rival) || rival.estaExpulsado()) {
                continue;
            }
            double px = rival.getX() + rival.getAncho() / 2.0;
            double py = rival.getY() + rival.getAlto() / 2.0;
            double distancia = distanciaPuntoASegmento(px, py, x1, y1, x2, y2);
            if (distancia < 36.0) {
                riesgo += (36.0 - distancia) / 36.0;
            }
        }
        return Math.min(2.0, riesgo / Math.max(1.0, longitud / 220.0));
    }

    private double distanciaPuntoASegmento(double px, double py, double x1, double y1, double x2, double y2) {
        double vx = x2 - x1;
        double vy = y2 - y1;
        double wx = px - x1;
        double wy = py - y1;
        double c1 = wx * vx + wy * vy;
        if (c1 <= 0.0) {
            double dx = px - x1;
            double dy = py - y1;
            return Math.sqrt(dx * dx + dy * dy);
        }
        double c2 = vx * vx + vy * vy;
        if (c2 <= c1) {
            double dx = px - x2;
            double dy = py - y2;
            return Math.sqrt(dx * dx + dy * dy);
        }
        double t = c1 / c2;
        double projX = x1 + t * vx;
        double projY = y1 + t * vy;
        double dx = px - projX;
        double dy = py - projY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private Jugador seleccionarReceptorLargoPortero(Jugador portero) {
        Jugador[] opciones = esJugadorLocal(portero) ? getLocales() : getRivales();
        Jugador mejor = null;
        double mejorDistancia = -1.0;
        for (Jugador candidato : opciones) {
            if (candidato == portero || esPortero(candidato) || candidato.estaExpulsado()) {
                continue;
            }
            double distancia = distanciaEntre(portero, candidato);
            if (distancia > mejorDistancia) {
                mejorDistancia = distancia;
                mejor = candidato;
            }
        }
        return mejor;
    }

    private Jugador seleccionarReceptorSeguroPortero(Jugador portero) {
        Jugador[] opciones = esJugadorLocal(portero) ? getLocales() : getRivales();
        Jugador mejor = null;
        double mejorScore = -Double.MAX_VALUE;
        for (Jugador candidato : opciones) {
            if (candidato == portero || esPortero(candidato) || candidato.estaExpulsado()) {
                continue;
            }
            double distancia = distanciaEntre(portero, candidato);
            if (distancia > 210.0) {
                continue;
            }
            double separacion = distanciaRivalMasCercano(candidato, esJugadorLocal(candidato));
            double riesgo = riesgoIntercepcionPase(portero, candidato, esJugadorLocal(portero));
            double avance = distanciaHorizontalAlArco(portero) - distanciaHorizontalAlArco(candidato);
            double score = separacion * 0.42 + avance * 0.24 - riesgo * 120.0 - distancia * 0.06;
            if (score > mejorScore) {
                mejorScore = score;
                mejor = candidato;
            }
        }
        return mejor;
    }

    private Jugador seleccionarReceptorLargoNpc(Jugador pasador) {
        Jugador[] opciones = esJugadorLocal(pasador) ? getLocales() : getRivales();
        Jugador mejor = null;
        double mejorScore = -Double.MAX_VALUE;
        for (Jugador candidato : opciones) {
            if (candidato == pasador || esPortero(candidato) || candidato.estaExpulsado()) {
                continue;
            }
            double distancia = distanciaEntre(pasador, candidato);
            if (distancia < 180.0 || distancia > DISTANCIA_PASE_SEGURA) {
                continue;
            }
            double avance = distanciaHorizontalAlArco(pasador) - distanciaHorizontalAlArco(candidato);
            double riesgo = riesgoIntercepcionPase(pasador, candidato, esJugadorLocal(pasador));
            double separacion = distanciaRivalMasCercano(candidato, esJugadorLocal(candidato));
            double score = avance * 0.38 + distancia * 0.08 + separacion * 0.24 - riesgo * 95.0;
            if (score > mejorScore) {
                mejorScore = score;
                mejor = candidato;
            }
        }
        return mejor;
    }

    private boolean aplicarFueraDeJuegoSiCorresponde(Jugador pasador, double[] direccion, boolean equipoAtacanteLocal) {
        if (REGLAS_CALLEJERAS) {
            return false;
        }
        Jugador receptor = seleccionarReceptorPorDireccion(pasador, direccion, equipoAtacanteLocal);
        if (receptor == null || !estaFueraDeJuego(receptor, equipoAtacanteLocal)) {
            return false;
        }
        sancionarFueraDeJuego(equipoAtacanteLocal, receptor);
        return true;
    }

    private Jugador seleccionarReceptorPorDireccion(Jugador pasador, double[] direccion, boolean equipoAtacanteLocal) {
        Jugador[] candidatos = equipoAtacanteLocal ? getLocales() : getRivales();
        Jugador mejor = null;
        double mejorScore = -Double.MAX_VALUE;
        for (Jugador candidato : candidatos) {
            if (candidato == pasador || candidato.estaExpulsado()) {
                continue;
            }
            double vx = (candidato.getX() + candidato.getAncho() / 2.0) - (pasador.getX() + pasador.getAncho() / 2.0);
            double vy = (candidato.getY() + candidato.getAlto() / 2.0) - (pasador.getY() + pasador.getAlto() / 2.0);
            double distancia = Math.sqrt(vx * vx + vy * vy);
            if (distancia < 24.0 || distancia > 340.0) {
                continue;
            }
            double dot = (direccion[0] * vx + direccion[1] * vy) / Math.max(0.001, distancia);
            if (dot < 0.55) {
                continue;
            }
            double score = dot * 220.0 - distancia * 0.35;
            if (score > mejorScore) {
                mejorScore = score;
                mejor = candidato;
            }
        }
        return mejor;
    }

    private boolean estaFueraDeJuego(Jugador receptor, boolean equipoAtacanteLocal) {
        double receptorX = receptor.getX() + receptor.getAncho() / 2.0;
        double balonX = balon.getCentroX();
        double medio = ConfiguracionJuego.ANCHO_PANEL / 2.0;
        if (equipoAtacanteLocal) {
            if (receptorX <= medio || receptorX <= balonX) {
                return false;
            }
            double linea = calcularLineaSegundoDefensor(false);
            return receptorX > linea;
        }
        if (receptorX >= medio || receptorX >= balonX) {
            return false;
        }
        double linea = calcularLineaSegundoDefensor(true);
        return receptorX < linea;
    }

    private double calcularLineaSegundoDefensor(boolean equipoLocalDefiende) {
        Jugador[] defensores = equipoLocalDefiende ? getLocales() : getRivales();
        double[] xs = new double[defensores.length];
        for (int i = 0; i < defensores.length; i++) {
            xs[i] = defensores[i].getX() + defensores[i].getAncho() / 2.0;
        }
        if (equipoLocalDefiende) {
            double menor = Math.min(xs[0], Math.min(xs[1], xs[2]));
            double segundo = Double.MAX_VALUE;
            for (double x : xs) {
                if (x > menor && x < segundo) {
                    segundo = x;
                }
            }
            return segundo == Double.MAX_VALUE ? menor : segundo;
        }
        double mayor = Math.max(xs[0], Math.max(xs[1], xs[2]));
        double segundo = -Double.MAX_VALUE;
        for (double x : xs) {
            if (x < mayor && x > segundo) {
                segundo = x;
            }
        }
        return segundo == -Double.MAX_VALUE ? mayor : segundo;
    }

    private void sancionarFueraDeJuego(boolean atacanteLocal, Jugador infractor) {
        boolean saqueLocal = !atacanteLocal;
        int x = (int) Math.round(infractor.getX() + infractor.getAncho() / 2.0);
        int y = (int) Math.round(infractor.getY() + infractor.getAlto() / 2.0);
        java.awt.Point punto = cancha.normalizarPuntoLibreIndirecto(x, y);
        Jugador ejecutor = seleccionarCobradorCampo(saqueLocal, punto.x, punto.y);
        programarReanudacion(
            TipoReanudacion.LIBRE_INDIRECTO,
            ejecutor,
            saqueLocal,
            punto.x,
            punto.y,
            "Fuera de juego " + (atacanteLocal ? "local" : "rival")
        );
        activarAccionArbitro(EstadoArbitraje.MARCA_FALTA, DURACION_ACCION_ARBITRO_LARGA_FRAMES, punto.x, punto.y, true);
        registrarSonido(TipoSonido.SAQUE);
        eventoTransitorio = atacanteLocal ? EventoJuego.FALTA_EN_CONTRA : EventoJuego.FALTA_A_FAVOR;
    }

    private void pegarBalonAlPoseedor() {
        if (poseedorBalon == null) {
            return;
        }
        double[] objetivo = calcularObjetivoBalonConPoseedor(poseedorBalon, balonEnManos);
        balon.setPosicion(objetivo[0], objetivo[1]);
        balon.detener();
        limitarEntidadAlPanel(balon);
    }

    private void arrastrarBalonConPoseedor() {
        if (poseedorBalon == null) {
            return;
        }

        // La posesion no teletransporta el balon:
        // lo interpola hacia una posicion objetivo y le suma parte del movimiento del jugador.
        double[] objetivo = calcularObjetivoBalonConPoseedor(poseedorBalon, balonEnManos);
        double objetivoX = objetivo[0];
        double objetivoY = objetivo[1];

        double actualX = balon.getX();
        double actualY = balon.getY();
        double nuevoX = actualX + (objetivoX - actualX) * ARRASTRE_BALON + movimientoXDe(poseedorBalon) * APORTE_MOVIMIENTO_POSEEDOR;
        double nuevoY = actualY + (objetivoY - actualY) * ARRASTRE_BALON + movimientoYDe(poseedorBalon) * APORTE_MOVIMIENTO_POSEEDOR;
        balon.setPosicion(nuevoX, nuevoY);
        limitarEntidadAlPanel(balon);

        // Si la pelota se despega demasiado, la posesion se pierde.
        if (distanciaAlBalon(poseedorBalon) > DISTANCIA_MAXIMA_POSESION) {
            balonLibre = true;
            ultimoToqueLocal = poseedorEsLocal;
            ultimoPateador = poseedorBalon;
            poseedorBalon = null;
            balonEnManos = false;
            cooldownCapturaLibreFrames = 6;
        }
    }

    private double[] calcularObjetivoBalonConPoseedor(Jugador poseedor, boolean enManos) {
        double centroPoseedorX = poseedor.getX() + poseedor.getAncho() / 2.0;
        double centroPoseedorY = poseedor.getY() + poseedor.getAlto() / 2.0;
        if (enManos) {
            double x = poseedor.getX() + poseedor.getAncho() / 2.0 - balon.getAncho() / 2.0;
            double y = poseedor.getY() + Math.max(2, poseedor.getAlto() / 5) - balon.getAlto() / 2.0;
            return new double[] { x, y };
        }

        double dirX = movimientoXDe(poseedor);
        double dirY = movimientoYDe(poseedor);
        if (Math.abs(dirX) + Math.abs(dirY) < 0.01) {
            dirX = poseedor.getDireccionX();
            dirY = poseedor.getDireccionY();
            if (Math.abs(dirX) + Math.abs(dirY) < 0.01) {
                dirX = poseedorEsLocal ? 1.0 : -1.0;
                dirY = 0.0;
            }
        }

        double magnitud = Math.hypot(dirX, dirY);
        dirX /= magnitud;
        dirY /= magnitud;
        double separacionFrontal = poseedor.getAncho() / 2.0 + balon.getRadio() - 1.0;
        double caidaVertical = Math.max(3.0, poseedor.getAlto() * 0.22);
        double objetivoCentroX = centroPoseedorX + dirX * separacionFrontal;
        double objetivoCentroY = centroPoseedorY + dirY * Math.max(4.0, poseedor.getAlto() * 0.16) + caidaVertical;
        return new double[] {
            objetivoCentroX - balon.getAncho() / 2.0,
            objetivoCentroY - balon.getAlto() / 2.0
        };
    }

    private void lanzarBalonDesdePoseedor(double[] direccion, double fuerza, double elevacion) {
        if (poseedorBalon == null) {
            return;
        }

        // El disparo nace delante del jugador para evitar autocolisiones
        // y hereda parte de su velocidad para que correr influya en el golpeo.
        double centroPoseedorX = poseedorBalon.getX() + poseedorBalon.getAncho() / 2.0;
        double centroPoseedorY = poseedorBalon.getY() + poseedorBalon.getAlto() / 2.0;
        double separacion = poseedorBalon.getAncho() / 2.0 + balon.getRadio() + 6.0;
        double objetivoCentroX = centroPoseedorX + direccion[0] * separacion;
        double objetivoCentroY = centroPoseedorY + direccion[1] * separacion;
        double actualCentroX = balon.getCentroX();
        double actualCentroY = balon.getCentroY();
        double deltaX = objetivoCentroX - actualCentroX;
        double deltaY = objetivoCentroY - actualCentroY;
        double distancia = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        double factor = distancia > MAX_ADELANTO_GOLPEO ? MAX_ADELANTO_GOLPEO / distancia : 1.0;
        double nuevaX = actualCentroX + deltaX * factor - balon.getAncho() / 2.0;
        double nuevaY = actualCentroY + deltaY * factor - balon.getAlto() / 2.0;

        balon.setPosicion(nuevaX, nuevaY);
        double impulsoX = direccion[0] * fuerza + movimientoXDe(poseedorBalon) * IMPULSO_BASE_MOVIMIENTO;
        double impulsoY = direccion[1] * fuerza + movimientoYDe(poseedorBalon) * IMPULSO_BASE_MOVIMIENTO;
        balon.detener();
        balon.impulsar(impulsoX, impulsoY, elevacion);
        framesDesdeUltimoDisparo = 0;
    }

    private boolean estaEnZonaDeControl(Jugador jugador) {
        return distanciaAlBalon(jugador) <= DISTANCIA_MAXIMA_CONTROL;
    }

    private double distanciaAlBalon(Jugador jugador) {
        double centroJugadorX = jugador.getX() + jugador.getAncho() / 2.0;
        double centroJugadorY = jugador.getY() + jugador.getAlto() / 2.0;
        double dx = centroJugadorX - balon.getCentroX();
        double dy = centroJugadorY - balon.getCentroY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double distanciaEntre(Jugador origen, Jugador destino) {
        double dx = (origen.getX() + origen.getAncho() / 2.0) - (destino.getX() + destino.getAncho() / 2.0);
        double dy = (origen.getY() + origen.getAlto() / 2.0) - (destino.getY() + destino.getAlto() / 2.0);
        return Math.sqrt(dx * dx + dy * dy);
    }

    private Jugador rivalMasCercanoA(Jugador jugador, boolean jugadorEsLocal) {
        Jugador[] rivales = jugadorEsLocal ? getRivales() : getLocales();
        Jugador mejor = null;
        double mejorDistancia = Double.MAX_VALUE;
        for (Jugador rival : rivales) {
            if (rival.estaExpulsado()) {
                continue;
            }
            double distancia = distanciaEntre(jugador, rival);
            if (distancia < mejorDistancia) {
                mejorDistancia = distancia;
                mejor = rival;
            }
        }
        return mejor;
    }

    private boolean estaEnZonaDeTiro(Jugador jugador) {
        double distanciaHorizontalAlArco = esJugadorLocal(jugador)
            ? ConfiguracionJuego.CAMPO_X_MAX - (jugador.getX() + jugador.getAncho() / 2.0)
            : (jugador.getX() + jugador.getAncho() / 2.0) - ConfiguracionJuego.CAMPO_X_MIN;
        double centroY = jugador.getY() + jugador.getAlto() / 2.0;
        double centroPorteriaY = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2.0;
        return distanciaHorizontalAlArco < 285.0 && Math.abs(centroY - centroPorteriaY) < 150.0;
    }

    private int calcularCarrilAtaqueY(Jugador jugador, boolean equipoLocal) {
        int centroPorteria = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2;
        int ajuste = jugador.getY() < centroPorteria ? -54 : 54;
        int carril = centroPorteria + ajuste;
        int minimo = ConfiguracionJuego.CAMPO_Y_MIN + 30;
        int maximo = ConfiguracionJuego.CAMPO_Y_MAX - jugador.getAlto() - 30;
        return Math.max(minimo, Math.min(maximo, carril));
    }

    private int calcularCarrilApoyo(Jugador poseedor, boolean equipoLocal, boolean extremo) {
        int centroPorteria = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2;
        int signo = poseedor.getY() < centroPorteria ? 1 : -1;
        int offset = extremo ? 148 : 98;
        int carril = poseedor.getY() + signo * offset;
        int minimo = ConfiguracionJuego.CAMPO_Y_MIN + 26;
        int maximo = ConfiguracionJuego.CAMPO_Y_MAX - 56;
        return Math.max(minimo, Math.min(maximo, carril));
    }

    private int calcularYBloqueDefensivo(Jugador poseedor, boolean equipoLocal, boolean extremo) {
        int centroPorteria = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2;
        int referencia = (poseedor.getY() + centroPorteria) / 2;
        int ajuste = extremo ? 84 : -54;
        int y = referencia + (poseedor.getY() < centroPorteria ? ajuste : -ajuste);
        int minimo = ConfiguracionJuego.CAMPO_Y_MIN + 24;
        int maximo = ConfiguracionJuego.CAMPO_Y_MAX - 54;
        return Math.max(minimo, Math.min(maximo, y));
    }

    private int calcularXApoyoOfensivo(Jugador poseedor, boolean equipoLocal, boolean extremo) {
        // Apoyo ofensivo por delante del poseedor para generar progresion real.
        int offset = extremo ? 158 : 104;
        int direccionAtaque = equipoLocal ? 1 : -1;
        int x = poseedor.getX() + direccionAtaque * offset;
        int minimo = ConfiguracionJuego.CAMPO_X_MIN + 32;
        int maximo = ConfiguracionJuego.CAMPO_X_MAX - 80;
        return Math.max(minimo, Math.min(maximo, x));
    }

    private int calcularXApoyoLibre(boolean equipoLocal, boolean extremo) {
        int offset = extremo ? 156 : 102;
        int x = (int) balon.getCentroX() + (equipoLocal ? -offset : offset);
        int minimo = ConfiguracionJuego.CAMPO_X_MIN + 28;
        int maximo = ConfiguracionJuego.CAMPO_X_MAX - 76;
        return Math.max(minimo, Math.min(maximo, x));
    }

    private int calcularYApoyoLibre(boolean equipoLocal, boolean extremo) {
        int centro = (int) balon.getCentroY();
        int offset = extremo ? 150 : 108;
        int direccion = centro < ConfiguracionJuego.ALTO_PANEL / 2 ? 1 : -1;
        int y = centro + direccion * offset;
        if (!equipoLocal) {
            y = centro - direccion * (extremo ? 112 : 78);
        }
        int minimo = ConfiguracionJuego.CAMPO_Y_MIN + 24;
        int maximo = ConfiguracionJuego.CAMPO_Y_MAX - 54;
        return Math.max(minimo, Math.min(maximo, y));
    }

    private int calcularXCobertura(Jugador poseedor, boolean equipoLocal, boolean extremo) {
        int offset = extremo ? 126 : 82;
        int x = poseedor.getX() + (equipoLocal ? -offset : offset);
        int minimo = ConfiguracionJuego.CAMPO_X_MIN + 26;
        int maximo = ConfiguracionJuego.CAMPO_X_MAX - 74;
        return Math.max(minimo, Math.min(maximo, x));
    }

    private double distanciaHorizontalAlArco(Jugador jugador) {
        return esJugadorLocal(jugador)
            ? ConfiguracionJuego.CAMPO_X_MAX - (jugador.getX() + jugador.getAncho() / 2.0)
            : (jugador.getX() + jugador.getAncho() / 2.0) - ConfiguracionJuego.CAMPO_X_MIN;
    }

    private double distanciaRivalMasCercano(Jugador jugador, boolean jugadorEsLocal) {
        Jugador rival = rivalMasCercanoA(jugador, jugadorEsLocal);
        return rival == null ? Double.MAX_VALUE : distanciaEntre(jugador, rival);
    }

    private int[] calcularObjetivoIntercepcionBalonLibre(Jugador jugador, boolean equipoLocal) {
        double[] aterrizaje = predecirAterrizajeBalon();
        boolean balonAereo = balon.estaEnAire() || balon.getAltura() > ALTURA_MAXIMA_CONTROL;
        double xAterrizaje = aterrizaje[0];
        double yAterrizaje = aterrizaje[1];
        double framesAterrizaje = aterrizaje[2];
        double x = balon.getCentroX();
        double y = balon.getCentroY();
        double vx = balon.getVelocidadX();
        double vy = balon.getVelocidadY();
        double mejorX = x;
        double mejorY = y;
        double mejorScore = Double.MAX_VALUE;

        for (int frame = 0; frame <= LOOKAHEAD_BALON_LIBRE_FRAMES; frame++) {
            if (frame > 0) {
                x += vx;
                y += vy;
                vx *= FRICCION_ESTIMADA_BALON_LIBRE;
                vy *= FRICCION_ESTIMADA_BALON_LIBRE;
            }

            double objetivoX = x - jugador.getAncho() / 2.0;
            double objetivoY = y - jugador.getAlto() / 2.0;
            if (balonAereo) {
                double pesoAterrizaje = Math.max(0.0, Math.min(1.0, balon.getAltura() / 20.0));
                double objetivoAereoX = xAterrizaje - jugador.getAncho() / 2.0;
                double objetivoAereoY = yAterrizaje - jugador.getAlto() / 2.0;
                objetivoX = lerp(objetivoX, objetivoAereoX, 0.25 + pesoAterrizaje * 0.50);
                objetivoY = lerp(objetivoY, objetivoAereoY, 0.25 + pesoAterrizaje * 0.50);
            }
            objetivoX = Math.max(ConfiguracionJuego.CAMPO_X_MIN + 8.0, Math.min(ConfiguracionJuego.CAMPO_X_MAX - jugador.getAncho() - 8.0, objetivoX));
            objetivoY = Math.max(ConfiguracionJuego.CAMPO_Y_MIN + 8.0, Math.min(ConfiguracionJuego.CAMPO_Y_MAX - jugador.getAlto() - 8.0, objetivoY));

            double distancia = Math.hypot(objetivoX - jugador.getX(), objetivoY - jugador.getY());
            double costoTiempo = frame * 2.2;
            double costoAgrupamiento = penalizacionAgrupamiento(jugador) * 0.55;
            double framesLlegada = distancia / Math.max(1.2, jugador.getVelocidad() + 0.25);
            double costoSincronizacionAerea = balonAereo ? Math.abs(framesLlegada - framesAterrizaje) * 1.3 : 0.0;
            double score = distancia * PESO_ANTICIPACION_BALON + costoTiempo + costoAgrupamiento + costoSincronizacionAerea;
            if (score < mejorScore) {
                mejorScore = score;
                mejorX = objetivoX;
                mejorY = objetivoY;
            }
        }

        return new int[] { (int) Math.round(mejorX), (int) Math.round(mejorY) };
    }

    private double[] predecirAterrizajeBalon() {
        double x = balon.getCentroX();
        double y = balon.getCentroY();
        double vx = balon.getVelocidadX();
        double vy = balon.getVelocidadY();
        double z = balon.getAltura();
        double vz = balon.getVelocidadZ();
        int frames = 0;

        while (frames < LOOKAHEAD_ATERRIZAJE_BALON_FRAMES) {
            vz -= GRAVEDAD_BALON;
            x += vx;
            y += vy;
            z += vz;
            frames++;

            boolean enAire = z > 0.0 || Math.abs(vz) > 0.18;
            double friccion = enAire ? FRICCION_ESTIMADA_BALON_AEREO : FRICCION_ESTIMADA_BALON_LIBRE;
            vx *= friccion;
            vy *= friccion;

            if (z <= 0.0) {
                z = 0.0;
                break;
            }
        }

        double xClampeada = Math.max(ConfiguracionJuego.CAMPO_X_MIN + 8.0, Math.min(ConfiguracionJuego.CAMPO_X_MAX - 8.0, x));
        double yClampeada = Math.max(ConfiguracionJuego.CAMPO_Y_MIN + 8.0, Math.min(ConfiguracionJuego.CAMPO_Y_MAX - 8.0, y));
        return new double[] { xClampeada, yClampeada, frames };
    }

    private Jugador seleccionarDefensorPresionanteRival() {
        if (poseedorBalon == null) {
            return rivalUno;
        }
        double scoreUno = distanciaEntre(rivalUno, poseedorBalon) - rivalUno.getStamina() * 0.35;
        double scoreDos = distanciaEntre(rivalDos, poseedorBalon) - rivalDos.getStamina() * 0.35;
        double scoreExtremo = distanciaEntre(extremoRival, poseedorBalon) - extremoRival.getStamina() * 0.35;
        double scoreMedia = distanciaEntre(mediaRival, poseedorBalon) - mediaRival.getStamina() * 0.35;
        if (scoreUno <= scoreDos && scoreUno <= scoreExtremo && scoreUno <= scoreMedia) {
            return rivalUno;
        }
        if (scoreDos <= scoreExtremo && scoreDos <= scoreMedia) {
            return rivalDos;
        }
        return scoreExtremo <= scoreMedia ? extremoRival : mediaRival;
    }

    private Jugador seleccionarPresionadorEquipo(boolean equipoLocal) {
        if (poseedorBalon == null) {
            return equipoLocal ? aliadoLocal : rivalUno;
        }
        Jugador[] candidatos = equipoLocal
            ? new Jugador[] { jugadorPrincipal, aliadoLocal, extremoLocal, mediaLocal }
            : new Jugador[] { rivalUno, rivalDos, extremoRival, mediaRival };
        Jugador mejor = null;
        double mejorScore = Double.MAX_VALUE;
        for (Jugador candidato : candidatos) {
            if (candidato.estaExpulsado()) {
                continue;
            }
            double score = distanciaEntre(candidato, poseedorBalon) - candidato.getStamina() * 0.35;
            if (score < mejorScore) {
                mejorScore = score;
                mejor = candidato;
            }
        }
        return mejor == null ? (equipoLocal ? jugadorPrincipal : rivalUno) : mejor;
    }

    private int[] calcularObjetivoMarcaPase(Jugador defensor, Jugador poseedor) {
        Jugador amenaza = seleccionarAmenazaDePase(poseedor, esJugadorLocal(defensor));
        if (amenaza == null) {
            int xCobertura = calcularXCobertura(poseedor, esJugadorLocal(defensor), esExtremo(defensor));
            int yCobertura = calcularYBloqueDefensivo(poseedor, esJugadorLocal(defensor), esExtremo(defensor));
            return new int[] { xCobertura, yCobertura };
        }

        double px = poseedor.getX() + poseedor.getAncho() / 2.0;
        double py = poseedor.getY() + poseedor.getAlto() / 2.0;
        double ax = amenaza.getX() + amenaza.getAncho() / 2.0;
        double ay = amenaza.getY() + amenaza.getAlto() / 2.0;
        double factor = 0.58;
        double objetivoX = px + (ax - px) * factor - defensor.getAncho() / 2.0;
        double objetivoY = py + (ay - py) * factor - defensor.getAlto() / 2.0;
        int margenX = esJugadorLocal(defensor) ? -8 : 8;
        objetivoX += margenX;
        objetivoX = Math.max(ConfiguracionJuego.CAMPO_X_MIN + 12.0, Math.min(ConfiguracionJuego.CAMPO_X_MAX - defensor.getAncho() - 12.0, objetivoX));
        objetivoY = Math.max(ConfiguracionJuego.CAMPO_Y_MIN + 12.0, Math.min(ConfiguracionJuego.CAMPO_Y_MAX - defensor.getAlto() - 12.0, objetivoY));
        return new int[] { (int) Math.round(objetivoX), (int) Math.round(objetivoY) };
    }

    private Jugador seleccionarAmenazaDePase(Jugador poseedor, boolean defensorEsLocal) {
        if (poseedor == null) {
            return null;
        }
        Jugador[] atacantes = defensorEsLocal ? getRivales() : getLocales();
        Jugador mejor = null;
        double mejorPuntaje = -Double.MAX_VALUE;
        for (Jugador atacante : atacantes) {
            if (atacante == poseedor || esPortero(atacante) || atacante.estaExpulsado()) {
                continue;
            }
            double distanciaAlPoseedor = distanciaEntre(atacante, poseedor);
            if (distanciaAlPoseedor > DISTANCIA_MAXIMA_MARCA_PASE) {
                continue;
            }
            double progresion = Math.max(0.0, 420.0 - distanciaHorizontalAlArco(atacante));
            double ventaja = Math.max(0.0, distanciaRivalMasCercano(atacante, esJugadorLocal(atacante)) - 34.0);
            double riesgoLinea = riesgoIntercepcionPase(poseedor, atacante, esJugadorLocal(poseedor));
            double puntaje = progresion * 0.34 + ventaja * 0.24 - distanciaAlPoseedor * 0.11 - riesgoLinea * 42.0;
            if (puntaje > mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejor = atacante;
            }
        }
        return mejor;
    }

    private Jugador perseguidorBalonLibre(boolean equipoLocal) {
        Jugador[] opciones = equipoLocal ? getLocales() : getRivales();
        Jugador mejor = null;
        double mejorPuntaje = Double.MAX_VALUE;
        for (Jugador jugador : opciones) {
            if (jugador == porteroLocal || jugador == porteroRival || jugador.estaExpulsado()) {
                continue;
            }
            int[] intercepcion = calcularObjetivoIntercepcionBalonLibre(jugador, equipoLocal);
            double distanciaIntercepcion = Math.hypot(intercepcion[0] - jugador.getX(), intercepcion[1] - jugador.getY());
            double puntaje = distanciaIntercepcion + penalizacionAgrupamiento(jugador) * 0.65;
            if (puntaje < mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejor = jugador;
            }
        }
        return mejor == null ? (equipoLocal ? jugadorPrincipal : rivalUno) : mejor;
    }

    private double penalizacionAgrupamiento(Jugador jugador) {
        double penalizacion = 0.0;
        boolean local = esJugadorLocal(jugador);
        for (Jugador otro : getTodosJugadores()) {
            if (otro == jugador || otro.estaExpulsado()) {
                continue;
            }
            double distancia = distanciaEntre(jugador, otro);
            if (distancia < 52.0) {
                boolean mismoEquipo = esJugadorLocal(otro) == local;
                penalizacion += mismoEquipo ? 22.0 : 8.0;
            } else if (distancia < 96.0 && esJugadorLocal(otro) == local) {
                penalizacion += 9.0;
            }
        }
        return penalizacion;
    }

    private boolean intentarAtajadaPortero() {
        if (!balonLibre || balon.getAltura() > ALTURA_MAXIMA_ATAJADA || cooldownAtajadaPorteroFrames > 0) {
            return false;
        }

        // Busca una intercepcion futura en los proximos frames.
        return intentarAtajadaPortero(porteroLocal, true) || intentarAtajadaPortero(porteroRival, false);
    }

    private EventoJuego verificarGol() {
        if (saqueDeMetaEnPreparacion()) {
            return EventoJuego.NINGUNO;
        }
        double centroX = balon.getCentroX();
        double centroY = balon.getCentroY();
        boolean balonCruzoLineaDerecha = balonEntroEnArco(false);
        boolean balonCruzoLineaIzquierda = balonEntroEnArco(true);

        // Un gol exige cruzar la linea entre postes y por debajo de cierta altura.
        // Si va demasiado alto, se trata como salida por encima del arco.
        if (balonCruzoLineaDerecha) {
            if (intentarReboteTravesano(false)) {
                return EventoJuego.NINGUNO;
            }
            if (balon.getAltura() > ALTURA_MAXIMA_GOL) {
                resolverSalidaSobreArco(false, centroY);
                return EventoJuego.NINGUNO;
            }
            golesLocal++;
            registrarGol(resolverAutorGol(true), true);
            registrarSonido(TipoSonido.GOL);
            reiniciarJugada(false);
            return EventoJuego.GOL_LOCAL;
        }

        if (balonCruzoLineaIzquierda) {
            if (intentarReboteTravesano(true)) {
                return EventoJuego.NINGUNO;
            }
            if (balon.getAltura() > ALTURA_MAXIMA_GOL) {
                resolverSalidaSobreArco(true, centroY);
                return EventoJuego.NINGUNO;
            }
            golesRival++;
            registrarGol(resolverAutorGol(false), false);
            registrarSonido(TipoSonido.GOL);
            reiniciarJugada(true);
            return EventoJuego.GOL_RIVAL;
        }

        if (balonLibre) {
            manejarBalonFueraDeCancha(centroX, centroY);
        }

        return EventoJuego.NINGUNO;
    }

    private boolean saqueDeMetaEnPreparacion() {
        if (tipoReanudacionPendiente == TipoReanudacion.META) {
            return true;
        }
        return poseedorBalon != null
            && esPortero(poseedorBalon)
            && !balonLibre
            && framesPrioridadSaquePortero > 0;
    }

    private boolean balonEntroEnArco(boolean arcoIzquierdo) {
        double centroX = balon.getCentroX();
        double centroY = balon.getCentroY();
        double radio = balon.getRadio();
        double margenVertical = Math.max(2.0, radio * 0.35);
        boolean dentroPostes = centroY >= ConfiguracionJuego.Y_PORTERIA + margenVertical
            && centroY <= ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA - margenVertical;
        if (!dentroPostes) {
            return false;
        }

        // Exige que al menos una parte sustancial del balon haya cruzado la linea de meta.
        double umbralCruce = Math.max(3.0, radio * 0.45);
        if (arcoIzquierdo) {
            return centroX <= ConfiguracionJuego.CAMPO_X_MIN - umbralCruce;
        }
        return centroX >= ConfiguracionJuego.CAMPO_X_MAX + umbralCruce;
    }

    private boolean intentarReboteTravesano(boolean arcoIzquierdo) {
        // El travesano solo existe en una franja de altura intermedia:
        // mas bajo es gol/atajada, mas alto es salida.
        double alturaBalon = balon.getAltura();
        if (alturaBalon < ALTURA_MINIMA_TRAVESANO || alturaBalon > ALTURA_MAXIMA_TRAVESANO) {
            return false;
        }
        if (!estaEntrePostes(balon.getCentroY())) {
            return false;
        }

        double nuevaX = arcoIzquierdo
            ? ConfiguracionJuego.CAMPO_X_MIN + 6.0
            : ConfiguracionJuego.CAMPO_X_MAX - balon.getAncho() - 6.0;
        balon.setPosicion(nuevaX, balon.getY());
        balon.fijarAltura(ALTURA_MINIMA_TRAVESANO - 2.0);
        balon.fijarVelocidades(
            Math.abs(balon.getVelocidadX()) * (arcoIzquierdo ? 0.82 : -0.82),
            balon.getVelocidadY() * 0.92,
            Math.abs(balon.getVelocidadZ()) * 0.55
        );
        mostrarTextoSaque("🪵 Travesaño");
        registrarSonido(TipoSonido.TIRO);
        return true;
    }

    private void resolverSalidaSobreArco(boolean arcoIzquierdo, double centroY) {
        if (arcoIzquierdo) {
            resolverSalidaLineaDeFondo(false, centroY);
            return;
        }
        resolverSalidaLineaDeFondo(true, centroY);
    }

    private void resolverSalidaLineaDeFondo(boolean ladoDerecho, double centroY) {
        boolean superior = centroY < cancha.getCentroY();
        if (!ladoDerecho) {
            if (ultimoToqueLocal) {
                java.awt.Point corner = superior ? cancha.getCornerSuperior(false) : cancha.getCornerInferior(false);
                asignarSaqueEsquina(false, corner.x, corner.y, "🏳️ Tiro de esquina rival");
            } else {
                asignarSaqueMeta(true, "🥅 Saque de meta local");
            }
            return;
        }

        if (!ultimoToqueLocal) {
            java.awt.Point corner = superior ? cancha.getCornerSuperior(true) : cancha.getCornerInferior(true);
            asignarSaqueEsquina(true, corner.x, corner.y, "🏳️ Tiro de esquina local");
        } else {
            asignarSaqueMeta(false, "🥅 Saque de meta rival");
        }
    }

    private void manejarBalonFueraDeCancha(double centroX, double centroY) {
        GeometriaCancha.LadoSalida ladoSalida = cancha.resolverLadoSalida(centroX, centroY);
        boolean fueraSuperior = ladoSalida == GeometriaCancha.LadoSalida.SUPERIOR;
        boolean fueraInferior = ladoSalida == GeometriaCancha.LadoSalida.INFERIOR;
        boolean fueraIzquierda = ladoSalida == GeometriaCancha.LadoSalida.IZQUIERDA;
        boolean fueraDerecha = ladoSalida == GeometriaCancha.LadoSalida.DERECHA;
        boolean enRangoArco = cancha.estaEntrePostes(centroY);

        // Salida por banda.
        if (fueraSuperior || fueraInferior) {
            boolean saqueLocal = !ultimoToqueLocal;
            int ySaque = fueraSuperior ? cancha.getCampoYMin() : cancha.getCampoYMax();
            int xSaque = cancha.clampXEnLineaLateral(centroX);
            asignarSaqueBanda(saqueLocal, xSaque, ySaque, "↔️ Saque de banda " + (saqueLocal ? "local" : "rival"));
            return;
        }

        // Salida por fondo sin gol: meta o esquina.
        if (fueraIzquierda && !enRangoArco) {
            // Fondo izquierdo defendido por el local.
            if (ultimoToqueLocal) {
                // Ultimo toque local: esquina rival.
                java.awt.Point corner = centroY < cancha.getCentroY() ? cancha.getCornerSuperior(false) : cancha.getCornerInferior(false);
                asignarSaqueEsquina(false, corner.x, corner.y, "🏳️ Tiro de esquina rival");
            } else {
                // Ultimo toque rival: saque de meta local.
                asignarSaqueMeta(true, "🥅 Saque de meta local");
            }
            return;
        }

        if (fueraDerecha && !enRangoArco) {
            // Fondo derecho defendido por el rival.
            if (!ultimoToqueLocal) {
                // Ultimo toque rival: esquina local.
                java.awt.Point corner = centroY < cancha.getCentroY() ? cancha.getCornerSuperior(true) : cancha.getCornerInferior(true);
                asignarSaqueEsquina(true, corner.x, corner.y, "🏳️ Tiro de esquina local");
            } else {
                // Ultimo toque local: saque de meta rival.
                asignarSaqueMeta(false, "🥅 Saque de meta rival");
            }
        }
    }

    private void asignarSaqueBanda(boolean saqueLocal, int x, int y, String mensaje) {
        Jugador ejecutor = seleccionarCobradorCampo(saqueLocal, x, y);
        programarReanudacion(TipoReanudacion.BANDA, ejecutor, saqueLocal, x, y, mensaje);
    }

    private void asignarSaqueMeta(boolean saqueLocal, String mensaje) {
        Jugador ejecutor = saqueLocal ? porteroLocal : porteroRival;
        java.awt.Point punto = cancha.getPuntoSaqueMeta(saqueLocal);
        programarReanudacion(TipoReanudacion.META, ejecutor, saqueLocal, punto.x, punto.y, mensaje);
    }

    private void asignarSaqueEsquina(boolean saqueLocal, int x, double y, String mensaje) {
        Jugador ejecutor = seleccionarCobradorCampo(saqueLocal, x, y);
        programarReanudacion(TipoReanudacion.ESQUINA, ejecutor, saqueLocal, x, (int) y, mensaje);
    }

    private void mostrarTextoSaque(String texto) {
        textoSaque = texto;
        framesTextoSaque = ConfiguracionJuego.FPS * 2;
        narrar(texto, false);
    }

    private void registrarGol(Jugador goleador, boolean equipoLocal) {
        if (goleador == null) {
            ultimoGoleador = "Sin identificar";
        } else if (esJugadorLocal(goleador) == equipoLocal) {
            ultimoGoleador = goleador.getNombre();
        } else {
            ultimoGoleador = "😵 Autogol de " + goleador.getNombre();
        }
        ultimoEquipoGoleador = equipoLocal ? "🏠 Local" : "🚩 Rival";
        activarAccionArbitro(
            EstadoArbitraje.VALIDA_GOL,
            DURACION_ACCION_ARBITRO_LARGA_FRAMES,
            ConfiguracionJuego.ANCHO_PANEL / 2.0,
            ConfiguracionJuego.ALTO_PANEL / 2.0 - 28.0,
            true
        );
        String plantilla = FRASES_GOL[aleatorio.nextInt(FRASES_GOL.length)];
        narrar(String.format(plantilla, ultimoGoleador, ultimoEquipoGoleador), true);
    }

    private void narrarRobo(Jugador jugador) {
        if (jugador == null) {
            return;
        }
        String plantilla = FRASES_ROBO[aleatorio.nextInt(FRASES_ROBO.length)];
        narrar(String.format(plantilla, jugador.getNombre()), false);
    }

    private void narrar(String texto, boolean forzar) {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        if (!forzar && cooldownNarracion > 0) {
            return;
        }
        narracionActual = texto;
        framesNarracion = DURACION_NARRACION_FRAMES;
        cooldownNarracion = COOLDOWN_NARRACION_FRAMES;
    }

    private void activarAccionArbitro(EstadoArbitraje estado, int duracionFrames, double x, double y, boolean forzar) {
        if (!forzar && framesAccionArbitro > 0 && prioridadArbitraje(estado) < prioridadArbitraje(estadoArbitrajeActual)) {
            return;
        }
        estadoArbitrajeActual = estado;
        framesAccionArbitro = Math.max(0, duracionFrames);
        objetivoArbitroX = Math.max(ConfiguracionJuego.CAMPO_X_MIN + 18.0, Math.min(ConfiguracionJuego.CAMPO_X_MAX - 18.0, x));
        objetivoArbitroY = Math.max(ConfiguracionJuego.CAMPO_Y_MIN + 18.0, Math.min(ConfiguracionJuego.CAMPO_Y_MAX - 18.0, y));
    }

    private int prioridadArbitraje(EstadoArbitraje estado) {
        if (estado == EstadoArbitraje.MARCA_FALTA) {
            return 5;
        }
        if (estado == EstadoArbitraje.VALIDA_GOL) {
            return 4;
        }
        if (estado == EstadoArbitraje.COBRA_SAQUE) {
            return 3;
        }
        if (estado == EstadoArbitraje.APLICA_VENTAJA) {
            return 2;
        }
        return 1;
    }

    private Jugador resolverAutorGol(boolean golLocal) {
        if (ultimoPateador != null) {
            return ultimoPateador;
        }
        Jugador[] candidatos = golLocal ? getLocales() : getRivales();
        Jugador mejor = null;
        double mejorDist = Double.MAX_VALUE;
        for (Jugador candidato : candidatos) {
            double dist = distanciaAlBalon(candidato);
            if (dist < mejorDist) {
                mejorDist = dist;
                mejor = candidato;
            }
        }
        return mejor;
    }

    private Jugador seleccionarCobradorCampo(boolean equipoLocal, double y) {
        return seleccionarCobradorCampo(equipoLocal, ConfiguracionJuego.ANCHO_PANEL / 2.0, y);
    }

    private Jugador seleccionarCobradorCampo(boolean equipoLocal, double x, double y) {
        Jugador[] equipo = equipoLocal ? getLocales() : getRivales();
        Jugador mejor = null;
        double mejorDist = Double.MAX_VALUE;
        for (Jugador candidato : equipo) {
            if (esPortero(candidato) || candidato.estaExpulsado()) {
                continue;
            }
            double dx = (candidato.getX() + candidato.getAncho() / 2.0) - x;
            double dy = (candidato.getY() + candidato.getAlto() / 2.0) - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < mejorDist) {
                mejorDist = dist;
                mejor = candidato;
            }
        }
        return mejor != null ? mejor : (equipoLocal ? jugadorPrincipal : rivalUno);
    }

    private void tomarPosesion(Jugador jugador, boolean equipoLocal) {
        if (jugador == null || jugador.estaExpulsado()) {
            return;
        }
        // Tomar posesion congela la fisica libre, asigna equipo con ultimo toque
        // y da un breve margen para evitar robos/decisiones en el mismo frame.
        boolean cambioDeEquipo = poseedorBalon != null && poseedorEsLocal != equipoLocal;
        boolean trasBalonDividido = ultimoPateador != null && ultimoToqueLocal != equipoLocal;
        resolverRecompensaPaseSiCorresponde(jugador, equipoLocal);
        poseedorBalon = jugador;
        poseedorEsLocal = equipoLocal;
        ultimoToqueLocal = equipoLocal;
        balonLibre = false;
        cooldownRoboFrames = ConfiguracionJuego.FPS / 3;
        cooldownCapturaLibreFrames = 0;
        setCooldownDecisionNpc(jugador, COOLDOWN_DECISION_NPC / 2);
        ultimoPateador = null;
        bloqueoRecapturaUltimoPateadorFrames = 0;
        balonEnManos = false;
        framesPoseedorAtascado = 0;
        poseedorControlAccionNpc = jugador;
        framesPoseedorSinAccionNpc = 0;
        if (!esPortero(jugador)) {
            framesPrioridadSaquePortero = 0;
        }
        if (cambioDeEquipo || trasBalonDividido) {
            activarTransicionOfensiva(equipoLocal, FRAMES_TRANSICION_EQUIPO);
        }
        pegarBalonAlPoseedor();
    }

    private void registrarIntentoPase(Jugador pasador, boolean equipoLocal) {
        ultimoPasador = pasador;
        ultimoPaseLocal = equipoLocal;
        framesVentanaRecepcionPase = 68;
    }

    private void resolverRecompensaPaseSiCorresponde(Jugador receptor, boolean equipoLocal) {
        if (framesVentanaRecepcionPase <= 0 || ultimoPasador == null) {
            return;
        }
        if (equipoLocal != ultimoPaseLocal || receptor == ultimoPasador) {
            return;
        }
        otorgarRecompensaEquipo(equipoLocal, 1, "Pase completado", receptor);
        narrar((equipoLocal ? "🏠 Local" : "🚩 Rival") + " encadena pases ✨", false);
        framesVentanaRecepcionPase = 0;
        ultimoPasador = null;
    }

    private void tomarPosesionEnPunto(Jugador jugador, boolean equipoLocal, int balonX, int balonY) {
        tomarPosesion(jugador, equipoLocal);
        balon.setPosicion(balonX - balon.getAncho() / 2.0, balonY - balon.getAlto() / 2.0);
        balon.detener();
        cooldownRoboFrames = ConfiguracionJuego.FPS;
        cooldownCapturaLibreFrames = 10;
        setCooldownDecisionNpc(jugador, COOLDOWN_DECISION_NPC);
    }

    private void iniciarBoteInicial(boolean saqueLocal) {
        reposicionarEquiposParaSaqueInicial(saqueLocal);
        boteInicialPendiente = true;
        boteInicialSoltado = false;
        boteInicialLocal = saqueLocal;
        framesBoteInicial = FRAMES_BOTE_INICIAL;
        tipoReanudacionPendiente = TipoReanudacion.NINGUNA;
        ejecutorReanudacion = null;
        framesRetrasoSaque = 0;
        framesEsperaReanudacion = 0;
        poseedorBalon = null;
        balonLibre = true;
        balonEnManos = false;
        balon.fijarAltura(0.0);
        balon.fijarVelocidades(0.0, 0.0, 0.0);
        java.awt.Point centro = cancha.getPuntoSaqueInicial();
        activarAccionArbitro(
            EstadoArbitraje.COBRA_SAQUE,
            DURACION_ACCION_ARBITRO_MEDIA_FRAMES,
            centro.x,
            centro.y,
            true
        );
        mostrarTextoSaque("🏁 Bote inicial: " + (saqueLocal ? "Local" : "Rival"));
    }

    private void actualizarBoteInicial() {
        java.awt.Point centro = cancha.getPuntoSaqueInicial();
        actualizarPlanesNpc();
        moverPrincipalComoNpc();
        moverAliadoLocal();
        moverExtremos();
        moverMediocampistas();
        moverRivales();
        moverPorteros();
        moverArbitro();
        aplicarMovimientoBarridasActivas();
        aplicarObjetivosJugadores(VELOCIDAD_REUBICACION_SUAVE);
        actualizarEstadoJugadores();

        if (!boteInicialSoltado) {
            if (!arbitroEnCentroParaSaque()) {
                pegarBalonAlArbitro();
            } else {
                balon.setPosicion(centro.x - balon.getAncho() / 2.0, centro.y - balon.getAlto() / 2.0);
                balon.fijarAltura(0.0);
                balon.fijarVelocidades(0.0, 0.0, 0.0);
            }
            if (framesBoteInicial <= FRAME_SOLTAR_BOTE_INICIAL && arbitroEnCentroParaSaque()) {
                Jugador objetivo = boteInicialLocal ? jugadorPrincipal : rivalUno;
                double dx = (objetivo.getX() + objetivo.getAncho() / 2.0) - balon.getCentroX();
                double dy = (objetivo.getY() + objetivo.getAlto() / 2.0) - balon.getCentroY();
                double norma = Math.hypot(dx, dy);
                if (norma < 0.0001) {
                    dx = boteInicialLocal ? -1.0 : 1.0;
                    dy = 0.0;
                    norma = 1.0;
                }
                balon.impulsar((dx / norma) * 0.9, (dy / norma) * 0.35, 3.2);
                boteInicialSoltado = true;
                registrarSonido(TipoSonido.SAQUE);
            }
        } else {
            balon.actualizarFisica(ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL);
        }

        framesBoteInicial--;
        if (framesBoteInicial > 0) {
            return;
        }

        Jugador objetivo = boteInicialLocal ? jugadorPrincipal : rivalUno;
        double dx = (objetivo.getX() + objetivo.getAncho() / 2.0) - balon.getCentroX();
        double dy = (objetivo.getY() + objetivo.getAlto() / 2.0) - balon.getCentroY();
        double norma = Math.hypot(dx, dy);
        if (norma < 0.0001) {
            dx = boteInicialLocal ? -1.0 : 1.0;
            dy = 0.0;
            norma = 1.0;
        }
        if (balon.getRapidez() < 0.9) {
            balon.impulsar((dx / norma) * 1.4, (dy / norma) * 0.5, 0.0);
        }
        poseedorBalon = null;
        balonLibre = true;
        balonEnManos = false;
        ultimoToqueLocal = boteInicialLocal;
        cooldownCapturaLibreFrames = Math.min(cooldownCapturaLibreFrames, 3);
        boteInicialPendiente = false;
        boteInicialSoltado = false;
        framesBoteInicial = 0;
        cooldownRoboFrames = Math.min(cooldownRoboFrames, ConfiguracionJuego.FPS / 8);
        narrar("▶️ Juegue", true);
    }

    private void reiniciarJugada(boolean saqueLocal) {
        // Tras gol, ambos equipos vuelven a su posicion inicial de saque.
        reposicionarEquiposParaSaqueInicial(saqueLocal);
        Jugador cobrador = saqueLocal ? jugadorPrincipal : rivalUno;
        java.awt.Point centro = cancha.getPuntoSaqueInicial();
        programarReanudacion(
            TipoReanudacion.INICIAL,
            cobrador,
            saqueLocal,
            centro.x,
            centro.y,
            saqueLocal ? "Saque inicial local" : "Saque inicial rival"
        );
    }

    private void reposicionarEquiposParaSaqueInicial(boolean saqueLocal) {
        // Equipo que saca: cobrador y apoyo en circulo central.
        // Equipo contrario: bloque en su propio campo.
        int centroX = cancha.getCentroX();
        int centroY = cancha.getCentroY();
        limpiarObjetivosJugadores();
        fijarObjetivoJugadorPosicion(porteroLocal, 20, ConfiguracionJuego.POS_Y_PORTERO);
        fijarObjetivoJugadorPosicion(porteroRival, ConfiguracionJuego.ANCHO_PANEL - 50, ConfiguracionJuego.POS_Y_PORTERO);

        if (saqueLocal) {
            fijarObjetivoJugadorPosicion(jugadorPrincipal, centroX - 40, centroY - jugadorPrincipal.getAlto() / 2);
            fijarObjetivoJugadorPosicion(aliadoLocal, centroX - 100, centroY - aliadoLocal.getAlto() / 2 + 56);
            fijarObjetivoJugadorPosicion(extremoLocal, centroX - 122, centroY - extremoLocal.getAlto() / 2 - 62);
            fijarObjetivoJugadorPosicion(mediaLocal, centroX - 160, centroY - mediaLocal.getAlto() / 2 + 4);

            fijarObjetivoJugadorPosicion(rivalUno, ConfiguracionJuego.POS_X_BASE_RIVAL + 30, ConfiguracionJuego.ALTO_PANEL / 2 - 58);
            fijarObjetivoJugadorPosicion(rivalDos, ConfiguracionJuego.POS_X_BASE_RIVAL + 58, ConfiguracionJuego.ALTO_PANEL / 2 + 34);
            fijarObjetivoJugadorPosicion(extremoRival, ConfiguracionJuego.POS_X_BASE_RIVAL + 84, ConfiguracionJuego.ALTO_PANEL / 2 - 118);
            fijarObjetivoJugadorPosicion(mediaRival, ConfiguracionJuego.POS_X_BASE_RIVAL + 112, ConfiguracionJuego.ALTO_PANEL / 2 + 2);
        } else {
            fijarObjetivoJugadorPosicion(rivalUno, centroX + 8, centroY - rivalUno.getAlto() / 2);
            fijarObjetivoJugadorPosicion(rivalDos, centroX + 68, centroY - rivalDos.getAlto() / 2 - 52);
            fijarObjetivoJugadorPosicion(extremoRival, centroX + 104, centroY - extremoRival.getAlto() / 2 + 62);
            fijarObjetivoJugadorPosicion(mediaRival, centroX + 142, centroY - mediaRival.getAlto() / 2 + 2);

            fijarObjetivoJugadorPosicion(jugadorPrincipal, ConfiguracionJuego.POS_X_BASE_LOCAL - 30, ConfiguracionJuego.ALTO_PANEL / 2 - 58);
            fijarObjetivoJugadorPosicion(aliadoLocal, ConfiguracionJuego.POS_X_BASE_LOCAL - 72, ConfiguracionJuego.ALTO_PANEL / 2 + 34);
            fijarObjetivoJugadorPosicion(extremoLocal, ConfiguracionJuego.POS_X_BASE_LOCAL - 104, ConfiguracionJuego.ALTO_PANEL / 2 - 118);
            fijarObjetivoJugadorPosicion(mediaLocal, ConfiguracionJuego.POS_X_BASE_LOCAL - 136, ConfiguracionJuego.ALTO_PANEL / 2 + 2);
        }
        fijarObjetivoJugadorPosicion(arbitro, cancha.getCentroX() - arbitro.getAncho() / 2, cancha.getCentroY() - arbitro.getAlto() / 2 - 44);
    }

    private void programarReanudacion(TipoReanudacion tipo, Jugador ejecutor, boolean saqueLocal, int x, int y, String mensaje) {
        tipoReanudacionPendiente = tipo;
        ejecutorReanudacion = ejecutor;
        framesVentanaRecepcionPase = 0;
        ultimoPasador = null;
        saquePendienteLocal = saqueLocal;
        saquePendienteX = x;
        saquePendienteY = y;
        framesRetrasoSaque = tipo == TipoReanudacion.INICIAL
            ? Math.max(RETRASO_SAQUE_FRAMES, RETRASO_SAQUE_GOL_FRAMES)
            : RETRASO_SAQUE_FRAMES;
        framesEsperaReanudacion = 0;
        limpiarObjetivosJugadores();
        switch (tipo) {
            case BANDA:
                prepararFormacionSaqueBanda(saqueLocal, ejecutor, x, y);
                break;
            case META:
                prepararFormacionSaqueMeta(saqueLocal);
                break;
            case ESQUINA:
                prepararFormacionSaqueEsquina(saqueLocal, ejecutor, x, y);
                break;
            case LIBRE_INDIRECTO:
                prepararFormacionLibreIndirecto(saqueLocal, ejecutor, x, y);
                break;
            case INICIAL:
                reposicionarEquiposParaSaqueInicial(saqueLocal);
                break;
            default:
                break;
        }
        poseedorBalon = null;
        balonLibre = true;
        balonEnManos = false;
        cooldownRoboFrames = ConfiguracionJuego.FPS;
        cooldownCapturaLibreFrames = Math.max(cooldownCapturaLibreFrames, 8);
        if (ejecutor != null) {
            setCooldownDecisionNpc(ejecutor, ejecutor == jugadorPrincipal ? COOLDOWN_DECISION_NPC : 0);
        }
        double arbitroY = y + (y < ConfiguracionJuego.ALTO_PANEL / 2 ? 48.0 : -48.0);
        activarAccionArbitro(EstadoArbitraje.COBRA_SAQUE, DURACION_ACCION_ARBITRO_MEDIA_FRAMES, x, arbitroY, true);
        mostrarTextoSaque(mensaje);
    }

    private void ejecutarReanudacionPendiente() {
        if (tipoReanudacionPendiente == TipoReanudacion.NINGUNA) {
            return;
        }
        TipoReanudacion tipoActual = tipoReanudacionPendiente;

        if (tipoActual == TipoReanudacion.INICIAL && !arbitroEnCentroParaSaque()) {
            framesRetrasoSaque = 1;
            return;
        }
        if (tipoActual != TipoReanudacion.INICIAL && !balonListoParaReanudacion(saquePendienteX, saquePendienteY)) {
            framesRetrasoSaque = 1;
            return;
        }
        if (tipoActual == TipoReanudacion.INICIAL) {
            iniciarBoteInicial(saquePendienteLocal);
            return;
        }
        if (ejecutorReanudacion != null && !ejecutorListoParaReanudar()) {
            framesEsperaReanudacion++;
            int umbralEspera = Math.max(12, ConfiguracionJuego.FPS / 4);
            if (framesEsperaReanudacion >= umbralEspera) {
                // Si tarda demasiado, reforzamos el objetivo del ejecutor sin recolocarlo de golpe.
                ubicarEjecutorDetrasDelBalon(
                    ejecutorReanudacion,
                    saquePendienteLocal,
                    saquePendienteX,
                    saquePendienteY
                );
                framesEsperaReanudacion = umbralEspera;
            } else {
                framesRetrasoSaque = 1;
                return;
            }
        }
        framesEsperaReanudacion = 0;
        if (poseedorBalon == null && ejecutorReanudacion != null) {
            tomarPosesion(ejecutorReanudacion, saquePendienteLocal);
        }
        balonEnManos = poseedorBalon != null && debeIrEnManosEnReanudacion(tipoActual, poseedorBalon);
        cooldownCapturaLibreFrames = Math.max(cooldownCapturaLibreFrames, 6);
        if (tipoActual == TipoReanudacion.META && poseedorBalon != null && esPortero(poseedorBalon)) {
            ejecutarSaqueLargoPortero(poseedorBalon, saquePendienteLocal);
        } else {
            setCooldownDecisionNpc(poseedorBalon, poseedorBalon == jugadorPrincipal ? COOLDOWN_DECISION_NPC / 2 : 0);
        }
        registrarSonido(TipoSonido.SAQUE);
        tipoReanudacionPendiente = TipoReanudacion.NINGUNA;
        ejecutorReanudacion = null;
    }

    private void actualizarBalonEnPreparacionReanudacion() {
        if (tipoReanudacionPendiente == TipoReanudacion.NINGUNA) {
            arrastrarBalonConPoseedor();
            return;
        }
        if (tipoReanudacionPendiente == TipoReanudacion.INICIAL) {
            pegarBalonAlArbitro();
            return;
        }
        moverBalonHaciaPuntoReanudacion(saquePendienteX, saquePendienteY, 0.18, 7.2);
    }

    private boolean arbitroDebeLlevarBalonAlCentro() {
        return tipoReanudacionPendiente == TipoReanudacion.INICIAL
            || (boteInicialPendiente && !boteInicialSoltado);
    }

    private boolean arbitroEnCentroParaSaque() {
        java.awt.Point centro = cancha.getPuntoSaqueInicial();
        double arbitroCentroX = arbitro.getX() + arbitro.getAncho() / 2.0;
        double arbitroCentroY = arbitro.getY() + arbitro.getAlto() / 2.0;
        return Math.abs(arbitroCentroX - centro.x) <= 8.0
            && Math.abs(arbitroCentroY - (centro.y - 10.0)) <= 10.0;
    }

    private void pegarBalonAlArbitro() {
        double balonX = arbitro.getX() + arbitro.getAncho() / 2.0 - balon.getAncho() / 2.0;
        double balonY = arbitro.getY() + Math.max(2, arbitro.getAlto() / 5) - balon.getAlto() / 2.0;
        balon.setPosicion(balonX, balonY);
        balon.fijarAltura(0.0);
        balon.fijarVelocidades(0.0, 0.0, 0.0);
    }

    private void moverBalonHaciaPuntoReanudacion(int objetivoX, int objetivoY, double factor, double velocidadMaxima) {
        double dx = objetivoX - balon.getCentroX();
        double dy = objetivoY - balon.getCentroY();
        double distancia = Math.hypot(dx, dy);
        if (distancia < 0.35) {
            balon.setPosicion(objetivoX - balon.getAncho() / 2.0, objetivoY - balon.getAlto() / 2.0);
            balon.fijarAltura(0.0);
            balon.fijarVelocidades(0.0, 0.0, 0.0);
            return;
        }

        double paso = Math.min(velocidadMaxima, Math.max(0.55, distancia * factor));
        double nuevoCentroX = balon.getCentroX() + dx / distancia * paso;
        double nuevoCentroY = balon.getCentroY() + dy / distancia * paso;
        balon.setPosicion(nuevoCentroX - balon.getAncho() / 2.0, nuevoCentroY - balon.getAlto() / 2.0);
        balon.fijarAltura(Math.max(0.0, balon.getAltura() * 0.4));
        balon.fijarVelocidades((dx / distancia) * paso * 0.55, (dy / distancia) * paso * 0.55, 0.0);
        limitarEntidadAlPanel(balon);
    }

    private boolean balonListoParaReanudacion(int objetivoX, int objetivoY) {
        double dx = objetivoX - balon.getCentroX();
        double dy = objetivoY - balon.getCentroY();
        return Math.hypot(dx, dy) <= 5.5 && balon.getAltura() <= 0.35 && balon.getRapidez() <= 1.2;
    }

    private void ejecutarSaqueLargoPortero(Jugador portero, boolean saqueLocal) {
        Jugador receptorLargo = seleccionarReceptorLargoPortero(portero);
        double energiaPortero = energiaRelativa(portero);
        double distanciaObjetivo = receptorLargo == null ? 230.0 : distanciaEntre(portero, receptorLargo);
        double factorPotencia = Math.max(0.90, calcularFactorPaseNpc(portero, receptorLargo, false, energiaPortero, true));
        double[] direccion = receptorLargo != null
            ? direccionPaseAnticipadoNpc(portero, receptorLargo)
            : direccionAlArcoContrario(portero);
        double fuerza = interpolarFuerza(FUERZA_PASE_MIN, FUERZA_PASE_MAX, factorPotencia);
        double elevacion = Math.max(2.7, calcularElevacionPaseNpc(factorPotencia, distanciaObjetivo, true) + 0.35);

        balon.setPosicion(saquePendienteX - balon.getAncho() / 2.0, saquePendienteY - balon.getAlto() / 2.0);
        balon.detener();
        balonEnManos = false;
        registrarIntentoPase(portero, saqueLocal);
        lanzarBalonDesdePoseedor(direccion, fuerza, elevacion);
        balonLibre = true;
        ultimoToqueLocal = saqueLocal;
        poseedorBalon = null;
        framesPrioridadSaquePortero = 0;
        cooldownRoboFrames = ConfiguracionJuego.FPS / 3;
        cooldownCapturaLibreFrames = calcularCooldownCapturaNpc(false, true, factorPotencia, distanciaObjetivo, elevacion);
        setCooldownDecisionNpc(portero, 0);
        ultimoPateador = portero;
        bloqueoRecapturaUltimoPateadorFrames = 18;
        poseedorControlAccionNpc = null;
        framesPoseedorSinAccionNpc = 0;
    }

    private boolean debeIrEnManosEnReanudacion(TipoReanudacion tipo, Jugador ejecutor) {
        return tipo == TipoReanudacion.BANDA;
    }

    private boolean ejecutorListoParaReanudar() {
        if (ejecutorReanudacion == null) {
            return true;
        }
        int objetivoX = calcularXEjecutorPendiente(ejecutorReanudacion, saquePendienteLocal);
        int objetivoY = calcularYEjecutorPendiente(ejecutorReanudacion);
        return Math.abs(ejecutorReanudacion.getX() - objetivoX) <= 8
            && Math.abs(ejecutorReanudacion.getY() - objetivoY) <= 8;
    }

    private void actualizarTemporizadoresGlobales() {
        if (framesDesdeUltimoDisparo < 999) {
            framesDesdeUltimoDisparo++;
        }
        if (cooldownRoboFrames > 0) {
            cooldownRoboFrames--;
        }
        if (cooldownCapturaLibreFrames > 0) {
            cooldownCapturaLibreFrames--;
        }
        if (cooldownDecisionNpcFrames > 0) {
            cooldownDecisionNpcFrames--;
        }
        if (cooldownDecisionAliadoFrames > 0) {
            cooldownDecisionAliadoFrames--;
        }
        if (cooldownDecisionRivalUnoFrames > 0) {
            cooldownDecisionRivalUnoFrames--;
        }
        if (cooldownDecisionRivalDosFrames > 0) {
            cooldownDecisionRivalDosFrames--;
        }
        if (cooldownDecisionExtremoLocalFrames > 0) {
            cooldownDecisionExtremoLocalFrames--;
        }
        if (cooldownDecisionExtremoRivalFrames > 0) {
            cooldownDecisionExtremoRivalFrames--;
        }
        if (cooldownDecisionMediaLocalFrames > 0) {
            cooldownDecisionMediaLocalFrames--;
        }
        if (cooldownDecisionMediaRivalFrames > 0) {
            cooldownDecisionMediaRivalFrames--;
        }
        if (cooldownNarracion > 0) {
            cooldownNarracion--;
        }
        if (framesMomentumLocal > 0) {
            framesMomentumLocal--;
        }
        if (framesMomentumRival > 0) {
            framesMomentumRival--;
        }
        if (framesTransicionLocal > 0) {
            framesTransicionLocal--;
        }
        if (framesTransicionRival > 0) {
            framesTransicionRival--;
        }
        if (framesVentanaRecepcionPase > 0) {
            framesVentanaRecepcionPase--;
            if (framesVentanaRecepcionPase == 0) {
                ultimoPasador = null;
            }
        }
        if (framesNarracion > 0) {
            framesNarracion--;
            if (framesNarracion == 0) {
                narracionActual = "";
            }
        }
        if (cooldownAtajadaPorteroFrames > 0) {
            cooldownAtajadaPorteroFrames--;
        }
        if (recuperacionPorteroLocalFrames > 0) {
            recuperacionPorteroLocalFrames--;
        }
        if (recuperacionPorteroRivalFrames > 0) {
            recuperacionPorteroRivalFrames--;
        }
        if (cooldownLecturaPorteroLocalFrames > 0) {
            cooldownLecturaPorteroLocalFrames--;
        }
        if (cooldownLecturaPorteroRivalFrames > 0) {
            cooldownLecturaPorteroRivalFrames--;
        }
        if (framesPrioridadSaquePortero > 0) {
            framesPrioridadSaquePortero--;
        }
        if (bloqueoRecapturaUltimoPateadorFrames > 0) {
            bloqueoRecapturaUltimoPateadorFrames--;
            if (bloqueoRecapturaUltimoPateadorFrames == 0) {
                ultimoPateador = null;
            }
        }
        if (framesTextoSaque > 0) {
            framesTextoSaque--;
            if (framesTextoSaque == 0) {
                textoSaque = "";
            }
        }
        if (framesRitmoAlto > 0) {
            framesRitmoAlto--;
        }
        if (framesAccionArbitro > 0) {
            framesAccionArbitro--;
            if (framesAccionArbitro == 0) {
                estadoArbitrajeActual = EstadoArbitraje.OBSERVA;
            }
        }
        for (int i = 0; i < cooldownBarridaJugadorFrames.length; i++) {
            if (cooldownBarridaJugadorFrames[i] > 0) {
                cooldownBarridaJugadorFrames[i]--;
            }
            if (framesBarridaActiva[i] > 0) {
                framesBarridaActiva[i]--;
                if (framesBarridaActiva[i] == 0) {
                    barridaDireccionX[i] = 0.0;
                    barridaDireccionY[i] = 0.0;
                }
            }
        }
    }

    private enum TipoReanudacion {
        NINGUNA,
        BANDA,
        LIBRE_INDIRECTO,
        META,
        ESQUINA,
        INICIAL
    }

    private enum EstadoArbitraje {
        OBSERVA,
        COBRA_SAQUE,
        MARCA_FALTA,
        TARJETA_AMARILLA,
        TARJETA_ROJA,
        APLICA_VENTAJA,
        VALIDA_GOL
    }

    private enum TipoTarjeta {
        NINGUNA,
        AMARILLA,
        ROJA
    }

    private enum PlanAtaque {
        VERTICAL,
        POSICIONAL,
        CAMBIO_BANDA
    }

    private enum PlanDefensa {
        PRESION_ALTA,
        BLOQUE_MEDIO,
        REPLIEGUE
    }

    private void prepararFormacionSaqueBanda(boolean saqueLocal, Jugador ejecutor, int x, int y) {
        Jugador[] atacantes = saqueLocal ? getLocales() : getRivales();
        Jugador[] defensores = saqueLocal ? getRivales() : getLocales();
        int interiorY = y <= cancha.getCentroY() ? y + 92 : y - 92;
        int apoyoCortoX = x + (saqueLocal ? 92 : -92);
        int apoyoLargoX = x + (saqueLocal ? 188 : -188);

        for (Jugador atacante : atacantes) {
            if (atacante == ejecutor) {
                continue;
            }
            int objetivoX = atacante == atacantes[0] ? apoyoLargoX : apoyoCortoX;
            int objetivoY = atacante == atacantes[0] ? interiorY + 46 : interiorY;
            colocarJugador(atacante, objetivoX, objetivoY);
        }

        colocarDefensasReanudacion(defensores, x + (saqueLocal ? 42 : -42), interiorY);
    }

    private void prepararFormacionSaqueMeta(boolean saqueLocal) {
        Jugador[] atacantes = saqueLocal ? getLocales() : getRivales();
        Jugador[] defensores = saqueLocal ? getRivales() : getLocales();
        Rectangle areaGrande = cancha.getAreaGrande(saqueLocal);
        int salidaX = saqueLocal ? areaGrande.x + areaGrande.width + 36 : areaGrande.x - 36;
        int salidaXLejana = saqueLocal ? areaGrande.x + areaGrande.width + 154 : areaGrande.x - 154;

        for (Jugador atacante : atacantes) {
            if (atacante == (saqueLocal ? porteroLocal : porteroRival)) {
                continue;
            }
            int objetivoX = atacante == atacantes[1] ? salidaX : salidaXLejana;
            int objetivoY = atacante == atacantes[1]
                ? cancha.getPorteriaY() + 28
                : cancha.getPorteriaY() + cancha.getAlturaPorteria() - 28;
            colocarJugador(atacante, objetivoX, objetivoY);
        }

        int presionX = saqueLocal ? areaGrande.x + areaGrande.width + 190 : areaGrande.x - 190;
        int centroY = cancha.getCentroY();
        colocarDefensasReanudacion(defensores, presionX, centroY);
    }

    private void prepararFormacionSaqueEsquina(boolean saqueLocal, Jugador ejecutor, int x, int y) {
        Jugador[] atacantes = saqueLocal ? getLocales() : getRivales();
        Jugador[] defensores = saqueLocal ? getRivales() : getLocales();
        int arcoCentroY = cancha.getCentroY();
        Rectangle areaGrande = cancha.getAreaGrande(!saqueLocal);
        int primerPaloX = saqueLocal ? areaGrande.x + areaGrande.width - 18 : areaGrande.x + 18;
        int segundoPaloX = saqueLocal ? areaGrande.x + areaGrande.width - 88 : areaGrande.x + 88;

        for (Jugador atacante : atacantes) {
            if (atacante == ejecutor) {
                continue;
            }
            int objetivoX = atacante == atacantes[0] ? segundoPaloX : primerPaloX;
            int objetivoY = atacante == atacantes[0] ? arcoCentroY + 42 : arcoCentroY - 22;
            colocarJugador(atacante, objetivoX, objetivoY);
        }

        int marcaX = saqueLocal ? areaGrande.x + areaGrande.width - 44 : areaGrande.x + 44;
        colocarDefensasReanudacion(defensores, marcaX, arcoCentroY);
        ubicarEjecutorDetrasDelBalon(ejecutor, saqueLocal, x, y);
    }

    private void prepararFormacionLibreIndirecto(boolean saqueLocal, Jugador ejecutor, int x, int y) {
        Jugador[] atacantes = saqueLocal ? getLocales() : getRivales();
        Jugador[] defensores = saqueLocal ? getRivales() : getLocales();
        int carrilSuperior = Math.max(cancha.getCampoYMin() + 24, y - 88);
        int carrilInferior = Math.min(cancha.getCampoYMax() - 24, y + 88);
        int apoyoCortoX = x + (saqueLocal ? 84 : -84);
        int apoyoLargoX = x + (saqueLocal ? 156 : -156);

        for (Jugador atacante : atacantes) {
            if (atacante == ejecutor) {
                continue;
            }
            int objetivoX = atacante == atacantes[0] ? apoyoLargoX : apoyoCortoX;
            int objetivoY = atacante == atacantes[0] ? carrilSuperior : carrilInferior;
            colocarJugador(atacante, objetivoX, objetivoY);
        }

        int referenciaX = x + (saqueLocal ? 56 : -56);
        colocarDefensasReanudacion(defensores, referenciaX, y);
        ubicarEjecutorDetrasDelBalon(ejecutor, saqueLocal, x, y);
    }

    private void colocarDefensasReanudacion(Jugador[] defensores, int referenciaX, int referenciaY) {
        int indice = 0;
        for (Jugador defensor : defensores) {
            if (defensor == porteroLocal || defensor == porteroRival) {
                continue;
            }
            int offsetY = indice == 0 ? -54 : 54;
            colocarJugador(defensor, referenciaX, referenciaY + offsetY);
            indice++;
        }
    }

    private void ubicarEjecutorDetrasDelBalonInmediato(Jugador jugador, boolean equipoLocal, int balonX, int balonY) {
        int offsetX = equipoLocal ? jugador.getAncho() - 6 : -balon.getAncho() + 6;
        int objetivoX = (int) Math.round(balonX - offsetX);
        int objetivoY = balonY - jugador.getAlto() / 2;
        fijarObjetivoJugadorPosicion(jugador, objetivoX, objetivoY);
    }

    private void ubicarEjecutorDetrasDelBalon(Jugador jugador, boolean equipoLocal, int balonX, int balonY) {
        ubicarEjecutorDetrasDelBalonInmediato(jugador, equipoLocal, balonX, balonY);
    }

    private boolean esEjecutorPendiente(Jugador jugador) {
        return framesRetrasoSaque > 0
            && tipoReanudacionPendiente != TipoReanudacion.NINGUNA
            && ejecutorReanudacion == jugador
            && jugador != null;
    }

    private int calcularXEjecutorPendiente(Jugador jugador, boolean equipoLocal) {
        int offsetX = equipoLocal ? jugador.getAncho() - 6 : -balon.getAncho() + 6;
        return (int) Math.round(saquePendienteX - offsetX);
    }

    private int calcularYEjecutorPendiente(Jugador jugador) {
        return saquePendienteY - jugador.getAlto() / 2;
    }

    private boolean modoAperturaNpc() {
        boolean porteroConManos = poseedorBalon != null && balonEnManos && esPortero(poseedorBalon);
        return framesRetrasoSaque > 0 || porteroConManos;
    }

    private int calcularXDesmarqueNpc(Jugador jugador) {
        boolean equipoConBalon = framesRetrasoSaque > 0 ? saquePendienteLocal : poseedorEsLocal;
        boolean jugadorLocal = esJugadorLocal(jugador);
        boolean mismoEquipo = jugadorLocal == equipoConBalon;
        int baseAtaque = jugadorLocal ? ConfiguracionJuego.CAMPO_X_MIN + 360 : ConfiguracionJuego.CAMPO_X_MAX - 360;
        int baseDefensa = jugadorLocal ? ConfiguracionJuego.CAMPO_X_MIN + 440 : ConfiguracionJuego.CAMPO_X_MAX - 440;
        int objetivo = mismoEquipo ? baseAtaque : baseDefensa;
        if (esCarrilInferior(jugador)) {
            objetivo += jugadorLocal ? -36 : 36;
        } else {
            objetivo += jugadorLocal ? 36 : -36;
        }
        return Math.max(ConfiguracionJuego.CAMPO_X_MIN + 24, Math.min(ConfiguracionJuego.CAMPO_X_MAX - 24, objetivo));
    }

    private int calcularYDesmarqueNpc(Jugador jugador) {
        if (esCarrilInferior(jugador)) {
            return Y_APERTURA_INFERIOR - jugador.getAlto() / 2;
        }
        return Y_APERTURA_SUPERIOR - jugador.getAlto() / 2;
    }

    private boolean esCarrilInferior(Jugador jugador) {
        return jugador == aliadoLocal || jugador == rivalDos || jugador == mediaLocal || jugador == mediaRival;
    }

    private boolean esExtremo(Jugador jugador) {
        return jugador == extremoLocal || jugador == extremoRival;
    }

    private void colocarJugador(Jugador jugador, int centroX, int centroY) {
        fijarObjetivoJugadorCentro(jugador, centroX, centroY);
    }

    private void registrarSonido(TipoSonido tipoSonido) {
        sonidosPendientes.offer(tipoSonido);
    }

    public TipoSonido consumirSonidoPendiente() {
        return sonidosPendientes.poll();
    }

    private void otorgarRecompensaEquipo(boolean equipoLocal, int puntos, String motivo, Jugador protagonista) {
        if (equipoLocal) {
            puntosBonus += puntos;
            framesMomentumLocal = Math.max(framesMomentumLocal, ConfiguracionJuego.FPS * 5);
        } else {
            puntosBonusRival += puntos;
            framesMomentumRival = Math.max(framesMomentumRival, ConfiguracionJuego.FPS * 5);
        }

        Jugador[] equipo = equipoLocal ? getLocales() : getRivales();
        for (Jugador jugador : equipo) {
            if (esPortero(jugador)) {
                continue;
            }
            jugador.recuperarStamina(0.55 + puntos * 0.28);
        }
        if (protagonista != null) {
            protagonista.recuperarStamina(0.95 + puntos * 0.36);
        }
        if (motivo != null && !motivo.isEmpty()) {
            narrar((equipoLocal ? "🏠 Local" : "🚩 Rival") + ": " + motivo, false);
        }
        aplicarImpulsoEquipoSiDisponible(equipoLocal);
    }

    private void aplicarImpulsoEquipoSiDisponible(boolean equipoLocal) {
        int bonus = equipoLocal ? puntosBonus : puntosBonusRival;
        while (bonus >= 6) {
            bonus -= 6;
            activarImpulsoEquipo(equipoLocal);
        }
        if (equipoLocal) {
            puntosBonus = bonus;
        } else {
            puntosBonusRival = bonus;
        }
    }

    private void activarImpulsoEquipo(boolean equipoLocal) {
        Jugador[] equipo = equipoLocal ? getLocales() : getRivales();
        Jugador[] elegidos = new Jugador[2];
        for (Jugador jugador : equipo) {
            if (esPortero(jugador)) {
                continue;
            }
            if (elegidos[0] == null || jugador.getStamina() < elegidos[0].getStamina()) {
                elegidos[1] = elegidos[0];
                elegidos[0] = jugador;
            } else if (elegidos[1] == null || jugador.getStamina() < elegidos[1].getStamina()) {
                elegidos[1] = jugador;
            }
        }
        for (Jugador elegido : elegidos) {
            if (elegido != null) {
                elegido.activarTurbo(1, ConfiguracionJuego.FPS * 3);
                elegido.recuperarStamina(4.2);
            }
        }
        if (equipoLocal) {
            framesMomentumLocal = Math.max(framesMomentumLocal, ConfiguracionJuego.FPS * 7);
        } else {
            framesMomentumRival = Math.max(framesMomentumRival, ConfiguracionJuego.FPS * 7);
        }
        narrar("💪 Impulso de equipo para " + (equipoLocal ? "Local" : "Rival"), false);
    }

    private Jugador jugadorQueIntersecta(EntidadJuego entidad) {
        for (Jugador jugador : getTodosJugadores()) {
            if (jugador.getBounds().intersects(entidad.getBounds())) {
                return jugador;
            }
        }
        return null;
    }

    private void actualizarHidratacionBanca() {
        // La hidratacion es fija en banca: quien llega agotado recupera energia.
        hidratacionBanca.actualizar();
        if (!hidratacionBanca.estaDisponible()) {
            if (!hidratacionAgotadaAnunciada && hidratacionBanca.getUsosRestantes() <= 0) {
                hidratacionAgotadaAnunciada = true;
                narrar("🚱 Se termino el agua en la banca", true);
            }
            return;
        }

        for (Jugador jugador : getTodosJugadores()) {
            boolean agotado = jugador.estaAgotado() || energiaRelativa(jugador) <= 0.24;
            if (!agotado) {
                continue;
            }
            if (!jugador.getBounds().intersects(hidratacionBanca.getBounds())) {
                continue;
            }
            if (!hidratacionBanca.consumirUso()) {
                return;
            }
            jugador.recuperarStamina(ConfiguracionJuego.RECARGA_HIDRATACION_BANCA);
            hidratacionBanca.activarCooldown(ConfiguracionJuego.COOLDOWN_HIDRATACION_BANCA);
            narrar("💧 " + jugador.getNombre() + " se hidrata en la banca", false);
            break;
        }
    }

    private void actualizarTurbo() {
        // Gestion de turbo tactico (cualquier jugador puede tomarlo).
        contadorAparicionTurbo++;

        if (!turbo.estaActivo()) {
            if (contadorAparicionTurbo >= ConfiguracionJuego.INTERVALO_TURBO) {
                recolocarEntidadBonus(turbo);
                turbo.activar(turbo.getX(), turbo.getY());
                framesTurboRestantesEnEscenario = ConfiguracionJuego.DURACION_TURBO_EN_ESCENARIO;
                contadorAparicionTurbo = 0;
            }
            return;
        }

        framesTurboRestantesEnEscenario--;
        if (framesTurboRestantesEnEscenario <= 0) {
            turbo.desactivar();
            contadorAparicionTurbo = 0;
            return;
        }

        Jugador recolector = jugadorQueIntersecta(turbo);
        if (recolector != null) {
            turbo.aplicarEfecto(recolector);
            Jugador apoyo = companeroMasCercano(recolector);
            if (apoyo != null && apoyo != recolector && !esPortero(apoyo)) {
                apoyo.activarTurbo(1, ConfiguracionJuego.FPS * 2);
            }
            boolean local = esJugadorLocal(recolector);
            otorgarRecompensaEquipo(local, 1, "Turbo tactico", recolector);
            turbo.desactivar();
            contadorAparicionTurbo = 0;
            framesTurboRestantesEnEscenario = 0;
        }
    }

    private Jugador companeroMasCercano(Jugador origen) {
        Jugador[] equipo = esJugadorLocal(origen) ? getLocales() : getRivales();
        Jugador mejor = null;
        double mejorDist = Double.MAX_VALUE;
        for (Jugador candidato : equipo) {
            if (candidato == origen) {
                continue;
            }
            double dist = distanciaEntre(origen, candidato);
            if (dist < mejorDist) {
                mejorDist = dist;
                mejor = candidato;
            }
        }
        return mejor;
    }

    private void recolocarEntidadBonus(EntidadJuego entidad) {
        // Busca una posicion tactica para que el bonus tenga impacto futbolistico real.
        boolean turboBonus = entidad == turbo;
        for (int intentos = 0; intentos < 80; intentos++) {
            int[] punto = sugerirPosicionBonus(turboBonus, intentos);
            entidad.setX(punto[0]);
            entidad.setY(punto[1]);
            if (posicionValidaParaBonus(entidad)) {
                return;
            }
        }

        // Fallback aleatorio.
        for (int intentos = 0; intentos < 30; intentos++) {
            int maxX = ConfiguracionJuego.ANCHO_PANEL - entidad.getAncho();
            int maxY = ConfiguracionJuego.ALTO_PANEL - entidad.getAlto();
            entidad.setX(aleatorio.nextInt(Math.max(1, maxX + 1)));
            entidad.setY(aleatorio.nextInt(Math.max(1, maxY + 1)));
            if (posicionValidaParaBonus(entidad)) {
                return;
            }
        }
    }

    private int[] sugerirPosicionBonus(boolean turboBonus, int intento) {
        int margenX = 36;
        int margenY = 36;
        double baseX = balon.getCentroX();
        double baseY = balon.getCentroY();
        int x;
        int y;

        if (turboBonus) {
            boolean localAtaca = !balonLibre && poseedorBalon != null && poseedorEsLocal;
            int direccion = localAtaca ? 1 : -1;
            if (balonLibre) {
                direccion = aleatorio.nextBoolean() ? 1 : -1;
            }
            x = (int) Math.round(baseX + direccion * (120 + aleatorio.nextInt(170)));
            int[] carriles = new int[] {
                ConfiguracionJuego.CAMPO_Y_MIN + 92,
                ConfiguracionJuego.ALTO_PANEL / 2 - 20,
                ConfiguracionJuego.CAMPO_Y_MAX - 116
            };
            y = carriles[(intento + aleatorio.nextInt(carriles.length)) % carriles.length];
        } else {
            int direccionX = aleatorio.nextBoolean() ? 1 : -1;
            x = (int) Math.round(baseX + direccionX * (80 + aleatorio.nextInt(220)));
            double yRef = baseY < ConfiguracionJuego.ALTO_PANEL / 2.0
                ? ConfiguracionJuego.CAMPO_Y_MAX - 150.0
                : ConfiguracionJuego.CAMPO_Y_MIN + 96.0;
            y = (int) Math.round(yRef + (aleatorio.nextDouble() * 80.0 - 40.0));
        }

        int xMin = ConfiguracionJuego.CAMPO_X_MIN + margenX;
        int xMax = ConfiguracionJuego.CAMPO_X_MAX - 64;
        int yMin = ConfiguracionJuego.CAMPO_Y_MIN + margenY;
        int yMax = ConfiguracionJuego.CAMPO_Y_MAX - 64;
        x = Math.max(xMin, Math.min(xMax, x));
        y = Math.max(yMin, Math.min(yMax, y));
        return new int[] { x, y };
    }

    private boolean posicionValidaParaBonus(EntidadJuego entidad) {
        // Evita solapes con entidades y zonas de arco.
        if (entidad.getX() < 0 || entidad.getY() < 0) {
            return false;
        }
        if (entidad.getX() + entidad.getAncho() > ConfiguracionJuego.ANCHO_PANEL
            || entidad.getY() + entidad.getAlto() > ConfiguracionJuego.ALTO_PANEL) {
            return false;
        }
        if (entidad.getBounds().intersects(balon.getBounds())) {
            return false;
        }
        for (Jugador jugador : getTodosJugadores()) {
            if (entidad.getBounds().intersects(jugador.getBounds())) {
                return false;
            }
        }
        return !(arcoIzquierdo.intersects(entidad.getBounds()) || arcoDerecho.intersects(entidad.getBounds()));
    }

    private boolean estaEntrePostes(double centroY) {
        double margenPoste = 4.0;
        return centroY >= ConfiguracionJuego.Y_PORTERIA + margenPoste
            && centroY <= ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA - margenPoste;
    }

    private void limitarPorteroEnZona(Jugador portero, boolean local) {
        // Mantiene a cada portero dentro de su zona de accion.
        double factorSalida = factorSalidaPortero(local);
        int xMin = local ? 6 : ConfiguracionJuego.ANCHO_PANEL - 86 - (int) Math.round(36.0 * factorSalida);
        int xMax = local ? 58 + (int) Math.round(56.0 * factorSalida) : ConfiguracionJuego.ANCHO_PANEL - 20 + (int) Math.round(0.0 * factorSalida);
        int yMin = ConfiguracionJuego.Y_PORTERIA - 12;
        int yMax = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA - portero.getAlto() + 12;

        if (!local) {
            xMin = ConfiguracionJuego.ANCHO_PANEL - 86 - (int) Math.round(56.0 * factorSalida);
            xMax = ConfiguracionJuego.ANCHO_PANEL - 20;
        }

        if (portero.getX() < xMin) {
            portero.setX(xMin);
        }
        if (portero.getX() > xMax) {
            portero.setX(xMax);
        }
        if (portero.getY() < yMin) {
            portero.setY(yMin);
        }
        if (portero.getY() > yMax) {
            portero.setY(yMax);
        }
    }

    private int calcularXObjetivoPortero(Jugador portero, boolean local) {
        double baseX = local ? ConfiguracionJuego.CAMPO_X_MIN + 10.0 : ConfiguracionJuego.CAMPO_X_MAX - portero.getAncho() - 10.0;
        double salidaX = local ? ConfiguracionJuego.CAMPO_X_MIN + 38.0 : ConfiguracionJuego.CAMPO_X_MAX - portero.getAncho() - 38.0;
        double agresivaX = local ? ConfiguracionJuego.CAMPO_X_MIN + 58.0 : ConfiguracionJuego.CAMPO_X_MAX - portero.getAncho() - 58.0;
        if (convieneSalidaPortero(local)) {
            double avance = local
                ? ConfiguracionJuego.CAMPO_X_MIN + 84.0
                : ConfiguracionJuego.CAMPO_X_MAX - portero.getAncho() - 84.0;
            if (balonLibre) {
                double objetivoBalon = balon.getCentroX() - portero.getAncho() / 2.0;
                if (!local) {
                    objetivoBalon = balon.getCentroX() - portero.getAncho() / 2.0;
                }
                return (int) Math.round(lerp(avance, objetivoBalon, 0.22));
            }
            return (int) Math.round(avance);
        }
        if (amenazaUnoContraUno(local)) {
            double salidaUnoVsUno = local
                ? ConfiguracionJuego.CAMPO_X_MIN + 70.0
                : ConfiguracionJuego.CAMPO_X_MAX - portero.getAncho() - 70.0;
            return (int) Math.round(salidaUnoVsUno);
        }
        if (balonAmenazaArco(local)) {
            double distanciaHorizontal = local
                ? balon.getCentroX() - ConfiguracionJuego.CAMPO_X_MIN
                : ConfiguracionJuego.CAMPO_X_MAX - balon.getCentroX();
            double factorPeligro = 1.0 - Math.max(0.0, Math.min(1.0, distanciaHorizontal / 240.0));
            return (int) Math.round(salidaX + (agresivaX - salidaX) * factorPeligro);
        }

        if (!balonLibre && poseedorBalon != null && esJugadorLocal(poseedorBalon) != local && amenazaDeDisparoDelPoseedor(local)) {
            return (int) Math.round(salidaX);
        }

        return (int) Math.round(baseX);
    }

    private int calcularYObjetivoPortero(Jugador portero, boolean local) {
        double centroPorteria = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2.0;
        double yObjetivo = centroPorteria - portero.getAlto() / 2.0;

        if (convieneSalidaPortero(local)) {
            double yBalon = balon.getCentroY() - portero.getAlto() / 2.0;
            if (balonLibre) {
                double xPlano = local
                    ? Math.min(ConfiguracionJuego.CAMPO_X_MIN + 96.0, balon.getCentroX())
                    : Math.max(ConfiguracionJuego.CAMPO_X_MAX - 96.0, balon.getCentroX());
                yBalon = proyectarYIntercepcionBalon(xPlano) - portero.getAlto() / 2.0;
            }
            yObjetivo = lerp(yObjetivo, yBalon, 0.72);
        } else if (amenazaUnoContraUno(local) && poseedorBalon != null) {
            yObjetivo = poseedorBalon.getY() + poseedorBalon.getAlto() / 2.0 - portero.getAlto() / 2.0;
        } else if (balonAmenazaArco(local)) {
            double planoPortero = local
                ? portero.getX() + portero.getAncho() * 0.75
                : portero.getX() + portero.getAncho() * 0.25;
            yObjetivo = proyectarYIntercepcionBalon(planoPortero) - portero.getAlto() / 2.0;
        } else if (!balonLibre && poseedorBalon != null && esJugadorLocal(poseedorBalon) != local) {
            double planoCobertura = local
                ? ConfiguracionJuego.CAMPO_X_MIN + 28.0
                : ConfiguracionJuego.CAMPO_X_MAX - 28.0;
            yObjetivo = proyectarYLineaTiroConError(poseedorBalon, local, planoCobertura, portero) - portero.getAlto() / 2.0;
        } else {
            // Sin amenaza directa, mantiene alineacion con la pelota sin abandonar el centro del arco.
            yObjetivo = centroPorteria - portero.getAlto() / 2.0 + (balon.getCentroY() - centroPorteria) * 0.22;
        }

        double yMin = ConfiguracionJuego.Y_PORTERIA - 12.0;
        double yMax = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA - portero.getAlto() + 12.0;
        return (int) Math.round(Math.max(yMin, Math.min(yMax, yObjetivo)));
    }

    private double proyectarYLineaTiroConError(Jugador atacante, boolean arcoLocal, double xPlano, Jugador portero) {
        double base = proyectarYLineaTiro(atacante, arcoLocal, xPlano);
        int movimientoPortero = Math.abs(movimientoXDe(portero)) + Math.abs(movimientoYDe(portero));
        double incertidumbre = 16.0 + movimientoPortero * 2.6;
        if (arcoLocal) {
            if (cooldownLecturaPorteroLocalFrames <= 0) {
                errorLecturaPorteroLocal = (aleatorio.nextDouble() * 2.0 - 1.0) * incertidumbre;
                cooldownLecturaPorteroLocalFrames = 8 + aleatorio.nextInt(7);
            }
            return base + errorLecturaPorteroLocal + atacante.getDireccionY() * 9.0;
        }
        if (cooldownLecturaPorteroRivalFrames <= 0) {
            errorLecturaPorteroRival = (aleatorio.nextDouble() * 2.0 - 1.0) * incertidumbre;
            cooldownLecturaPorteroRivalFrames = 8 + aleatorio.nextInt(7);
        }
        return base + errorLecturaPorteroRival + atacante.getDireccionY() * 9.0;
    }

    private int calcularVelocidadPortero(Jugador portero, boolean local) {
        boolean sprintando = local ? sprintPorteroLocal : sprintPorteroRival;
        int velocidad = portero.getVelocidadMovimiento(sprintando);
        if (convieneSalidaPortero(local)) {
            return velocidad + 2;
        }
        if (amenazaUnoContraUno(local)) {
            return velocidad + 2;
        }
        if (balonAmenazaArco(local)) {
            return velocidad + 2;
        }
        if (!balonLibre && poseedorBalon != null && esJugadorLocal(poseedorBalon) != local && amenazaDeDisparoDelPoseedor(local)) {
            return velocidad + 1;
        }
        return velocidad;
    }

    private boolean balonAmenazaArco(boolean arcoLocal) {
        double xLineaGol = arcoLocal ? ConfiguracionJuego.CAMPO_X_MIN + 6.0 : ConfiguracionJuego.CAMPO_X_MAX - 6.0;
        double velocidadX = balon.getVelocidadX();
        if (arcoLocal && velocidadX >= -0.08 && balon.getCentroX() > ConfiguracionJuego.CAMPO_X_MIN + 110) {
            return false;
        }
        if (!arcoLocal && velocidadX <= 0.08 && balon.getCentroX() < ConfiguracionJuego.CAMPO_X_MAX - 110) {
            return false;
        }

        double tiempo = Math.abs(velocidadX) < 0.08 ? 0.0 : (xLineaGol - balon.getCentroX()) / velocidadX;
        if (tiempo < 0.0 || tiempo > LOOKAHEAD_ATAJADA_FRAMES + 10) {
            return false;
        }

        double yProyectada = balon.getCentroY() + balon.getVelocidadY() * tiempo;
        return yProyectada >= ConfiguracionJuego.Y_PORTERIA - 30
            && yProyectada <= ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA + 30
            && alturaBalonEnFrames(tiempo) <= ALTURA_MAXIMA_GOL + 8.0;
    }

    private boolean amenazaDeDisparoDelPoseedor(boolean arcoLocal) {
        if (poseedorBalon == null || esJugadorLocal(poseedorBalon) == arcoLocal) {
            return false;
        }
        double distanciaHorizontalAlArco = arcoLocal
            ? poseedorBalon.getX() - ConfiguracionJuego.CAMPO_X_MIN
            : ConfiguracionJuego.CAMPO_X_MAX - poseedorBalon.getX();
        return distanciaHorizontalAlArco < 280.0 && estaEnZonaDeTiro(poseedorBalon);
    }

    private boolean amenazaUnoContraUno(boolean arcoLocal) {
        if (poseedorBalon == null || esJugadorLocal(poseedorBalon) == arcoLocal || esPortero(poseedorBalon)) {
            return false;
        }
        double distanciaArco = distanciaHorizontalAlArco(poseedorBalon);
        if (distanciaArco > DISTANCIA_SALIDA_PORTERO) {
            return false;
        }
        Jugador defensor = rivalMasCercanoA(poseedorBalon, esJugadorLocal(poseedorBalon));
        double distanciaDefensor = defensor == null ? Double.MAX_VALUE : distanciaEntre(defensor, poseedorBalon);
        return distanciaDefensor > 92.0;
    }

    private boolean convieneSalidaPortero(boolean arcoLocal) {
        double factor = factorSalidaPortero(arcoLocal);
        return factor >= 0.46;
    }

    private double factorSalidaPortero(boolean arcoLocal) {
        double factor = 0.0;
        if (amenazaUnoContraUno(arcoLocal)) {
            factor = Math.max(factor, 1.0);
        }
        if (balonAmenazaArco(arcoLocal)) {
            factor = Math.max(factor, 0.88);
        }
        if (!balonLibre && poseedorBalon != null && esJugadorLocal(poseedorBalon) != arcoLocal && amenazaDeDisparoDelPoseedor(arcoLocal)) {
            factor = Math.max(factor, 0.58);
        }
        if (balonLibre) {
            double distanciaGol = arcoLocal
                ? balon.getCentroX() - ConfiguracionJuego.CAMPO_X_MIN
                : ConfiguracionJuego.CAMPO_X_MAX - balon.getCentroX();
            double cercania = 1.0 - Math.max(0.0, Math.min(1.0, distanciaGol / (DISTANCIA_SALIDA_PORTERO + 34.0)));
            double velocidadHaciaArco = arcoLocal ? Math.max(0.0, -balon.getVelocidadX()) : Math.max(0.0, balon.getVelocidadX());
            double altura = Math.max(0.0, 1.0 - balon.getAltura() / 18.0);
            boolean entrePostesAmplio = balon.getCentroY() >= ConfiguracionJuego.Y_PORTERIA - 62
                && balon.getCentroY() <= ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA + 62;
            if (distanciaGol < DISTANCIA_SALIDA_PORTERO + 26.0 && entrePostesAmplio) {
                factor = Math.max(factor, 0.28 + cercania * 0.40 + velocidadHaciaArco * 0.08 + altura * 0.16);
            }
            if (velocidadHaciaArco > 0.55 && distanciaGol < DISTANCIA_SALIDA_PORTERO + 12.0) {
                factor = Math.max(factor, 0.54 + cercania * 0.24);
            }
        }
        return Math.max(0.0, Math.min(1.0, factor));
    }

    private double proyectarYIntercepcionBalon(double xObjetivo) {
        double velocidadX = balon.getVelocidadX();
        if (Math.abs(velocidadX) < 0.08) {
            return balon.getCentroY();
        }
        double tiempo = (xObjetivo - balon.getCentroX()) / velocidadX;
        tiempo = Math.max(0.0, Math.min(LOOKAHEAD_ATAJADA_FRAMES, tiempo));
        return balon.getCentroY() + balon.getVelocidadY() * tiempo;
    }

    private double proyectarYLineaTiro(Jugador atacante, boolean arcoLocal, double xPlano) {
        double centroAtacanteX = atacante.getX() + atacante.getAncho() / 2.0;
        double centroAtacanteY = atacante.getY() + atacante.getAlto() / 2.0;
        double objetivoX = arcoLocal ? ConfiguracionJuego.CAMPO_X_MIN : ConfiguracionJuego.CAMPO_X_MAX;
        double objetivoY = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2.0;
        double dx = objetivoX - centroAtacanteX;
        if (Math.abs(dx) < 0.001) {
            return objetivoY;
        }
        double t = (xPlano - centroAtacanteX) / dx;
        t = Math.max(0.0, Math.min(1.0, t));
        return centroAtacanteY + (objetivoY - centroAtacanteY) * t;
    }

    private boolean intentarAtajadaPortero(Jugador portero, boolean local) {
        if (getRecuperacionPorteroFrames(portero) > 0) {
            return false;
        }
        if (!balonEnZonaPortero(local, balon.getCentroX(), balon.getCentroY())) {
            return false;
        }

        boolean amenazaReal = balonAmenazaArco(local);
        double distanciaActual = distanciaAlBalon(portero);
        if (!amenazaReal
            && amenazaUnoContraUno(local)
            && balon.getAltura() <= ALTURA_MAXIMA_CONTROL + 2.0
            && distanciaActual <= DISTANCIA_ATAJADA_PORTERO + 12.0) {
            tomarPosesion(portero, local);
            balonEnManos = false;
            cooldownAtajadaPorteroFrames = ConfiguracionJuego.FPS / 2;
            registrarSonido(TipoSonido.ROBO);
            mostrarTextoSaque("🧤 Sale y corta " + portero.getNombre());
            return true;
        }
        if (!amenazaReal && distanciaActual > DISTANCIA_ATAJADA_PORTERO * 0.9) {
            return false;
        }

        double velocidadBalon = balon.getRapidez();
        if (!amenazaReal && velocidadBalon < 1.15) {
            return false;
        }

        double centroPorteroX = portero.getX() + portero.getAncho() / 2.0;
        double centroPorteroY = portero.getY() + portero.getAlto() / 2.0;
        double velocidadX = Math.abs(balon.getVelocidadX());
        double velocidadY = Math.abs(balon.getVelocidadY());
        double factorAngulo = Math.min(1.0, velocidadY / (velocidadX + 0.2));
        int movimientoPortero = Math.abs(movimientoXDe(portero)) + Math.abs(movimientoYDe(portero));
        int framesReaccion = Math.max(0, RETARDO_REACCION_ATAJADA_FRAMES - framesDesdeUltimoDisparo);
        double factorAtajada = FACTOR_ATAJADA_MIN + aleatorio.nextDouble() * (FACTOR_ATAJADA_MAX - FACTOR_ATAJADA_MIN);
        double penalizacionVelocidad = Math.max(0.0, velocidadBalon - 3.0) * 6.2;
        double penalizacionAltura = Math.max(0.0, balon.getAltura() - 9.0) * 1.8;
        double penalizacionAngulo = factorAngulo * 8.0 + Math.max(0.0, velocidadBalon - 4.2) * 3.0;
        double penalizacionReaccion = framesReaccion * (3.2 + movimientoPortero * 0.55);
        double rangoAtajada = Math.max(20.0, DISTANCIA_ATAJADA_PORTERO * factorAtajada - penalizacionVelocidad - penalizacionAltura);
        double rangoDesvio = Math.max(28.0, DISTANCIA_DESVIO_PORTERO * (factorAtajada + 0.07) - penalizacionVelocidad * 0.75);
        rangoAtajada = Math.max(14.0, rangoAtajada - penalizacionAngulo - penalizacionReaccion);
        rangoDesvio = Math.max(20.0, rangoDesvio - penalizacionAngulo * 0.65 - penalizacionReaccion * 0.6);
        double probabilidadLanzada = amenazaReal ? 0.92 : 0.70;
        double mejorDistancia = Double.MAX_VALUE;
        double mejorAltura = Double.MAX_VALUE;
        double mejorX = balon.getCentroX();
        double mejorY = balon.getCentroY();

        for (int frame = 0; frame <= LOOKAHEAD_ATAJADA_FRAMES; frame++) {
            double futuraX = balon.getCentroX() + balon.getVelocidadX() * frame;
            double futuraY = balon.getCentroY() + balon.getVelocidadY() * frame;
            double futuraAltura = alturaBalonEnFrames(frame);
            if (!balonEnZonaPortero(local, futuraX, futuraY)) {
                continue;
            }
            if (futuraY < ConfiguracionJuego.Y_PORTERIA - 24 || futuraY > ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA + 24) {
                continue;
            }

            double dx = centroPorteroX - futuraX;
            double dy = centroPorteroY - (futuraY + (aleatorio.nextDouble() - 0.5) * 12.0);
            double distancia = Math.sqrt(dx * dx + dy * dy);
            if (distancia < mejorDistancia) {
                mejorDistancia = distancia;
                mejorAltura = futuraAltura;
                mejorX = futuraX;
                mejorY = futuraY;
            }
        }

        boolean puedeIntentar = mejorAltura <= ALTURA_MAXIMA_ATAJADA + 6.0
            && (amenazaReal || velocidadBalon > 1.45)
            && aleatorio.nextDouble() < probabilidadLanzada;
        if (!puedeIntentar) {
            return false;
        }

        setRecuperacionPorteroFrames(portero, RECUPERACION_LANZADA_PORTERO_FRAMES);
        double dirLanzadaX = mejorX - centroPorteroX;
        double dirLanzadaY = mejorY - centroPorteroY;
        double normaLanzada = Math.hypot(dirLanzadaX, dirLanzadaY);
        if (normaLanzada > 0.001) {
            portero.activarAnimacionLanzada(RECUPERACION_LANZADA_PORTERO_FRAMES + 8, dirLanzadaX / normaLanzada, dirLanzadaY / normaLanzada);
        }
        boolean atajadaSolida = mejorDistancia <= rangoAtajada && mejorAltura <= ALTURA_MAXIMA_ATAJADA;
        double probabilidadControl = Math.max(0.18, 0.52 - factorAngulo * 0.16 - Math.max(0.0, velocidadBalon - 3.2) * 0.08);
        if (mejorAltura <= 6.0 && velocidadBalon <= 3.0) {
            probabilidadControl += 0.14;
        }
        if (atajadaSolida && aleatorio.nextDouble() < probabilidadControl) {
            tomarPosesion(portero, local);
            balonEnManos = false;
            cooldownAtajadaPorteroFrames = ConfiguracionJuego.FPS / 2;
            registrarSonido(TipoSonido.ROBO);
            mostrarTextoSaque("🧤 Atajada de " + portero.getNombre());
            return true;
        }

        double probabilidadDesvio = Math.max(0.30, 0.80 - Math.max(0.0, velocidadBalon - 2.8) * 0.09 - factorAngulo * 0.12);
        if (mejorDistancia <= rangoDesvio
            && mejorAltura <= ALTURA_MAXIMA_ATAJADA + 6.0
            && amenazaReal
            && aleatorio.nextDouble() < probabilidadDesvio) {
            double despejeX = local ? 3.9 : -3.9;
            double despejeY = mejorY < centroPorteroY ? -1.8 : 1.8;
            balon.setPosicion(
                mejorX - balon.getAncho() / 2.0,
                mejorY - balon.getAlto() / 2.0
            );
            balon.fijarAltura(Math.min(mejorAltura, 8.0));
            balon.fijarVelocidades(despejeX, despejeY, 1.6);
            balonLibre = true;
            poseedorBalon = null;
            balonEnManos = false;
            ultimoToqueLocal = local;
            ultimoPateador = null;
            cooldownCapturaLibreFrames = 8;
            cooldownRoboFrames = ConfiguracionJuego.FPS / 4;
            cooldownAtajadaPorteroFrames = ConfiguracionJuego.FPS / 3;
            registrarSonido(TipoSonido.ROBO);
            mostrarTextoSaque("✋ Desvio de " + portero.getNombre());
            return true;
        }

        mostrarTextoSaque("🪽 " + portero.getNombre() + " se lanza");
        return false;
    }

    private boolean balonEnZonaPortero(boolean arcoLocal, double x, double y) {
        double referenciaX = arcoLocal ? ConfiguracionJuego.CAMPO_X_MIN : ConfiguracionJuego.CAMPO_X_MAX;
        double distanciaX = Math.abs(x - referenciaX);
        double centroPorteriaY = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2.0;
        double distanciaY = Math.abs(y - centroPorteriaY);
        return distanciaX <= ALCANCE_X_PORTERO && distanciaY <= ALCANCE_Y_PORTERO;
    }

    private double alturaBalonEnFrames(double frames) {
        double altura = balon.getAltura() + balon.getVelocidadZ() * frames - (GRAVEDAD_BALON * frames * (frames + 1.0)) / 2.0;
        return Math.max(0.0, altura);
    }

    private void actualizarEstadoJugadores() {
        // Actualiza estados temporales de todos los jugadores.
        porteroLocal.actualizarEstado(sprintPorteroLocal, Math.abs(movPorteroLocalX) + Math.abs(movPorteroLocalY));
        jugadorPrincipal.actualizarEstado(sprintPrincipal, Math.abs(movPrincipalX) + Math.abs(movPrincipalY));
        aliadoLocal.actualizarEstado(sprintAliado, Math.abs(movAliadoX) + Math.abs(movAliadoY));
        extremoLocal.actualizarEstado(sprintExtremoLocal, Math.abs(movExtremoLocalX) + Math.abs(movExtremoLocalY));
        mediaLocal.actualizarEstado(sprintMediaLocal, Math.abs(movMediaLocalX) + Math.abs(movMediaLocalY));
        porteroRival.actualizarEstado(sprintPorteroRival, Math.abs(movPorteroRivalX) + Math.abs(movPorteroRivalY));
        rivalUno.actualizarEstado(sprintRivalUno, Math.abs(movRivalUnoX) + Math.abs(movRivalUnoY));
        rivalDos.actualizarEstado(sprintRivalDos, Math.abs(movRivalDosX) + Math.abs(movRivalDosY));
        extremoRival.actualizarEstado(sprintExtremoRival, Math.abs(movExtremoRivalX) + Math.abs(movExtremoRivalY));
        mediaRival.actualizarEstado(sprintMediaRival, Math.abs(movMediaRivalX) + Math.abs(movMediaRivalY));
        arbitro.actualizarEstado(false, Math.abs(movArbitroX) + Math.abs(movArbitroY));

        // La animacion depende del ultimo desplazamiento de cada jugador.
        porteroLocal.actualizarAnimacion(movPorteroLocalX, movPorteroLocalY);
        jugadorPrincipal.actualizarAnimacion(movPrincipalX, movPrincipalY);
        aliadoLocal.actualizarAnimacion(movAliadoX, movAliadoY);
        extremoLocal.actualizarAnimacion(movExtremoLocalX, movExtremoLocalY);
        mediaLocal.actualizarAnimacion(movMediaLocalX, movMediaLocalY);
        porteroRival.actualizarAnimacion(movPorteroRivalX, movPorteroRivalY);
        rivalUno.actualizarAnimacion(movRivalUnoX, movRivalUnoY);
        rivalDos.actualizarAnimacion(movRivalDosX, movRivalDosY);
        extremoRival.actualizarAnimacion(movExtremoRivalX, movExtremoRivalY);
        mediaRival.actualizarAnimacion(movMediaRivalX, movMediaRivalY);
        arbitro.actualizarAnimacion(movArbitroX, movArbitroY);
    }

    private int velocidadMovimiento(Jugador jugador) {
        // Velocidad estimada del frame usada en robos y disputas.
        if (jugador == jugadorPrincipal) {
            return Math.abs(movPrincipalX) + Math.abs(movPrincipalY);
        }
        if (jugador == aliadoLocal) {
            return Math.abs(movAliadoX) + Math.abs(movAliadoY);
        }
        if (jugador == extremoLocal) {
            return Math.abs(movExtremoLocalX) + Math.abs(movExtremoLocalY);
        }
        if (jugador == mediaLocal) {
            return Math.abs(movMediaLocalX) + Math.abs(movMediaLocalY);
        }
        if (jugador == porteroLocal) {
            return Math.abs(movPorteroLocalX) + Math.abs(movPorteroLocalY);
        }
        if (jugador == rivalUno) {
            return Math.abs(movRivalUnoX) + Math.abs(movRivalUnoY);
        }
        if (jugador == rivalDos) {
            return Math.abs(movRivalDosX) + Math.abs(movRivalDosY);
        }
        if (jugador == extremoRival) {
            return Math.abs(movExtremoRivalX) + Math.abs(movExtremoRivalY);
        }
        if (jugador == mediaRival) {
            return Math.abs(movMediaRivalX) + Math.abs(movMediaRivalY);
        }
        if (jugador == porteroRival) {
            return Math.abs(movPorteroRivalX) + Math.abs(movPorteroRivalY);
        }
        return 0;
    }

    private int movimientoXDe(Jugador jugador) {
        if (jugador == jugadorPrincipal) {
            return movPrincipalX;
        }
        if (jugador == aliadoLocal) {
            return movAliadoX;
        }
        if (jugador == extremoLocal) {
            return movExtremoLocalX;
        }
        if (jugador == mediaLocal) {
            return movMediaLocalX;
        }
        if (jugador == porteroLocal) {
            return movPorteroLocalX;
        }
        if (jugador == rivalUno) {
            return movRivalUnoX;
        }
        if (jugador == rivalDos) {
            return movRivalDosX;
        }
        if (jugador == extremoRival) {
            return movExtremoRivalX;
        }
        if (jugador == mediaRival) {
            return movMediaRivalX;
        }
        if (jugador == porteroRival) {
            return movPorteroRivalX;
        }
        return 0;
    }

    private int movimientoYDe(Jugador jugador) {
        if (jugador == jugadorPrincipal) {
            return movPrincipalY;
        }
        if (jugador == aliadoLocal) {
            return movAliadoY;
        }
        if (jugador == extremoLocal) {
            return movExtremoLocalY;
        }
        if (jugador == mediaLocal) {
            return movMediaLocalY;
        }
        if (jugador == porteroLocal) {
            return movPorteroLocalY;
        }
        if (jugador == rivalUno) {
            return movRivalUnoY;
        }
        if (jugador == rivalDos) {
            return movRivalDosY;
        }
        if (jugador == extremoRival) {
            return movExtremoRivalY;
        }
        if (jugador == mediaRival) {
            return movMediaRivalY;
        }
        if (jugador == porteroRival) {
            return movPorteroRivalY;
        }
        return 0;
    }

    private Jugador jugadorMasCercanoAlBalon() {
        Jugador mejor = jugadorPrincipal;
        double mejorDist = distanciaCuadrada(jugadorPrincipal);
        for (Jugador jugador : getTodosJugadores()) {
            double d = distanciaCuadrada(jugador);
            if (d < mejorDist) {
                mejor = jugador;
                mejorDist = d;
            }
        }
        return mejor;
    }

    private double distanciaCuadrada(Jugador jugador) {
        double dx = jugador.getX() + jugador.getAncho() / 2.0 - balon.getCentroX();
        double dy = jugador.getY() + jugador.getAlto() / 2.0 - balon.getCentroY();
        return dx * dx + dy * dy;
    }

    private boolean esJugadorLocal(Jugador jugador) {
        // Distingue si el jugador pertenece al equipo local.
        return jugador == jugadorPrincipal
            || jugador == aliadoLocal
            || jugador == extremoLocal
            || jugador == mediaLocal
            || jugador == porteroLocal;
    }

    private boolean esPortero(Jugador jugador) {
        return jugador == porteroLocal || jugador == porteroRival;
    }

    private int getRecuperacionPorteroFrames(Jugador portero) {
        if (portero == porteroLocal) {
            return recuperacionPorteroLocalFrames;
        }
        if (portero == porteroRival) {
            return recuperacionPorteroRivalFrames;
        }
        return 0;
    }

    private void setRecuperacionPorteroFrames(Jugador portero, int frames) {
        if (portero == porteroLocal) {
            recuperacionPorteroLocalFrames = Math.max(0, frames);
        } else if (portero == porteroRival) {
            recuperacionPorteroRivalFrames = Math.max(0, frames);
        }
    }

    private int getCooldownDecisionNpc(Jugador jugador) {
        if (jugador == aliadoLocal) {
            return cooldownDecisionAliadoFrames;
        }
        if (jugador == rivalUno) {
            return cooldownDecisionRivalUnoFrames;
        }
        if (jugador == rivalDos) {
            return cooldownDecisionRivalDosFrames;
        }
        if (jugador == extremoLocal) {
            return cooldownDecisionExtremoLocalFrames;
        }
        if (jugador == extremoRival) {
            return cooldownDecisionExtremoRivalFrames;
        }
        if (jugador == mediaLocal) {
            return cooldownDecisionMediaLocalFrames;
        }
        if (jugador == mediaRival) {
            return cooldownDecisionMediaRivalFrames;
        }
        return cooldownDecisionNpcFrames;
    }

    private void setCooldownDecisionNpc(Jugador jugador, int frames) {
        if (jugador == null) {
            return;
        }
        int valor = Math.max(0, frames);
        if (jugador == aliadoLocal) {
            cooldownDecisionAliadoFrames = valor;
            return;
        }
        if (jugador == rivalUno) {
            cooldownDecisionRivalUnoFrames = valor;
            return;
        }
        if (jugador == rivalDos) {
            cooldownDecisionRivalDosFrames = valor;
            return;
        }
        if (jugador == extremoLocal) {
            cooldownDecisionExtremoLocalFrames = valor;
            return;
        }
        if (jugador == extremoRival) {
            cooldownDecisionExtremoRivalFrames = valor;
            return;
        }
        if (jugador == mediaLocal) {
            cooldownDecisionMediaLocalFrames = valor;
            return;
        }
        if (jugador == mediaRival) {
            cooldownDecisionMediaRivalFrames = valor;
            return;
        }
        cooldownDecisionNpcFrames = valor;
    }

    private int ajusteCooldownDecisionNpc(Jugador jugador) {
        int ajusteInteligencia = (int) Math.round((50 - jugador.getInteligencia()) * 0.12);
        if (jugador == aliadoLocal) {
            return -3 + ajusteInteligencia;
        }
        if (jugador == rivalUno) {
            return 1 + ajusteInteligencia;
        }
        if (jugador == rivalDos) {
            return 3 + ajusteInteligencia;
        }
        if (jugador == extremoLocal) {
            return ajusteInteligencia;
        }
        if (jugador == extremoRival) {
            return 2 + ajusteInteligencia;
        }
        if (jugador == mediaLocal) {
            return -1 + ajusteInteligencia;
        }
        if (jugador == mediaRival) {
            return 1 + ajusteInteligencia;
        }
        return ajusteInteligencia;
    }

    private int ajusteDecisionPorEnergia(Jugador jugador) {
        double energia = energiaRelativa(jugador);
        // Cansado: decide un poco mas lento. Entero: mas reactivo.
        if (energia < 0.22) {
            return 8;
        }
        if (energia < 0.36) {
            return 4;
        }
        if (energia > 0.78) {
            return -4;
        }
        return 0;
    }

    private double sesgoTiroNpc(Jugador jugador) {
        double ajusteInteligencia = (50 - jugador.getInteligencia()) * 0.22;
        if (jugador == jugadorPrincipal) {
            return 16.0 + ajusteInteligencia;
        }
        if (jugador == aliadoLocal) {
            return 8.0 + ajusteInteligencia;
        }
        if (jugador == rivalUno) {
            return 16.0 + ajusteInteligencia;
        }
        if (jugador == rivalDos) {
            return 7.0 + ajusteInteligencia;
        }
        if (jugador == extremoLocal) {
            return 6.0 + ajusteInteligencia;
        }
        if (jugador == extremoRival) {
            return 6.0 + ajusteInteligencia;
        }
        if (jugador == mediaLocal) {
            return 2.0 + ajusteInteligencia;
        }
        if (jugador == mediaRival) {
            return 2.0 + ajusteInteligencia;
        }
        return ajusteInteligencia * 0.5;
    }

    private double sesgoPaseNpc(Jugador jugador) {
        double ajusteInteligencia = (jugador.getInteligencia() - 50) * 0.22;
        if (jugador == jugadorPrincipal) {
            return -2.0 + ajusteInteligencia;
        }
        if (jugador == aliadoLocal) {
            return 9.0 + ajusteInteligencia;
        }
        if (jugador == rivalUno) {
            return -2.0 + ajusteInteligencia;
        }
        if (jugador == rivalDos) {
            return 9.0 + ajusteInteligencia;
        }
        if (jugador == extremoLocal) {
            return 12.0 + ajusteInteligencia;
        }
        if (jugador == extremoRival) {
            return 12.0 + ajusteInteligencia;
        }
        if (jugador == mediaLocal) {
            return 14.0 + ajusteInteligencia;
        }
        if (jugador == mediaRival) {
            return 14.0 + ajusteInteligencia;
        }
        return ajusteInteligencia * 0.5;
    }

    private boolean esDelanteroNpc(Jugador jugador) {
        return jugador == jugadorPrincipal || jugador == rivalUno;
    }

    private boolean esExtremoNpc(Jugador jugador) {
        return jugador == extremoLocal || jugador == extremoRival;
    }

    private boolean esMediocampistaNpc(Jugador jugador) {
        return jugador == mediaLocal || jugador == mediaRival;
    }

    private int[] ajustarObjetivoIndividualNpc(Jugador jugador, int objetivoX, int objetivoY) {
        int x = objetivoX;
        int y = objetivoY;
        if (jugador == aliadoLocal) {
            x -= 14;
            y += 10;
        } else if (jugador == extremoLocal) {
            x -= 10;
            y -= 16;
        } else if (jugador == rivalUno) {
            x += 10;
            y -= 12;
        } else if (jugador == rivalDos) {
            x += 4;
            y += 14;
        } else if (jugador == extremoRival) {
            x += 8;
            y -= 18;
        } else if (jugador == mediaLocal) {
            x -= 8;
            y += 4;
        } else if (jugador == mediaRival) {
            x += 8;
            y += 4;
        }
        int yMin = ConfiguracionJuego.CAMPO_Y_MIN + 18;
        int yMax = ConfiguracionJuego.CAMPO_Y_MAX - jugador.getAlto() - 18;
        int xMin = ConfiguracionJuego.CAMPO_X_MIN + 16;
        int xMax = ConfiguracionJuego.CAMPO_X_MAX - jugador.getAncho() - 16;
        x = ajustarXPorReglaOffside(jugador, x);
        x = Math.max(xMin, Math.min(xMax, x));
        y = Math.max(yMin, Math.min(yMax, y));
        return new int[] { x, y };
    }

    private int[] ajustarObjetivoActivoNpc(Jugador jugador, boolean equipoLocal, int objetivoX, int objetivoY) {
        // Reactividad global: el objetivo se acerca/aleja de la jugada segun energia
        // individual y cansancio colectivo.
        double energia = energiaRelativa(jugador);
        if (debeBuscarHidratacion(jugador, equipoLocal, energia)) {
            return calcularObjetivoHidratacion(jugador);
        }
        double cansancioEquipo = cansancioGeneralEquipo(equipoLocal);
        double momentum = factorMomentumEquipo(equipoLocal);
        double actividad = Math.max(0.16, Math.min(0.98, 0.42 + energia * 0.70 - cansancioEquipo * 0.36 + momentum * 0.24));
        double x = objetivoX;
        double y = objetivoY;
        boolean cerrarRoboCercano = false;

        if (!balonLibre && poseedorBalon != null) {
            boolean defendiendo = esJugadorLocal(poseedorBalon) != equipoLocal;
            boolean atacando = !defendiendo;
            if (defendiendo) {
                double presionX = poseedorBalon.getX() + (equipoLocal ? -16.0 : 16.0);
                double presionY = poseedorBalon.getY();
                double factor = 0.24 + actividad * 0.42;
                x = lerp(x, presionX, factor);
                y = lerp(y, presionY, factor);
                if (poseedorBalon != jugador && !esPortero(jugador)) {
                    double distanciaPoseedor = distanciaEntre(jugador, poseedorBalon);
                    if (distanciaPoseedor < 170.0) {
                        double proximidad = Math.max(0.0, (170.0 - distanciaPoseedor) / 170.0);
                        double cierreX = poseedorBalon.getX() + (equipoLocal ? -8.0 : 8.0);
                        double cierreY = poseedorBalon.getY();
                        double factorCierre = 0.30 + actividad * 0.36 + proximidad * 0.26;
                        x = lerp(x, cierreX, factorCierre);
                        y = lerp(y, cierreY, factorCierre);
                        cerrarRoboCercano = distanciaPoseedor < 136.0;
                    }
                }
            } else if (atacando && poseedorBalon != jugador) {
                double avance = (equipoLocal ? 1.0 : -1.0) * (12.0 + 24.0 * actividad);
                x += avance;
                double carrilAtaque = calcularCarrilAtaqueY(jugador, equipoLocal);
                y = lerp(y, carrilAtaque, 0.14 + actividad * 0.18);
            }
        } else {
            // Con balon libre solo un perseguidor va directo; el resto se abre.
            boolean esPerseguidor = perseguidorBalonLibre(equipoLocal) == jugador;
            if (esPerseguidor) {
                int[] intercepcion = calcularObjetivoIntercepcionBalonLibre(jugador, equipoLocal);
                double factor = 0.26 + actividad * 0.36;
                x = lerp(x, intercepcion[0], factor);
                y = lerp(y, intercepcion[1], factor);
            } else {
                double xSoporte = calcularXApoyoLibre(equipoLocal, esExtremo(jugador));
                double ySoporte = calcularYApoyoLibre(equipoLocal, esExtremo(jugador));
                x = lerp(x, xSoporte, 0.22 + actividad * 0.26);
                y = lerp(y, ySoporte, 0.22 + actividad * 0.30);
            }
        }

        if (!balonLibre && poseedorBalon != null) {
            boolean atacando = esJugadorLocal(poseedorBalon) == equipoLocal;
            int indiceRol = indiceRolCampo(jugador, equipoLocal);
            int totalCampo = totalJugadoresCampo(equipoLocal);
            double carrilRol = carrilCoordinadoPorRol(indiceRol, totalCampo) - jugador.getAlto() / 2.0;
            int direccionAtaque = equipoLocal ? 1 : -1;

            if (atacando && poseedorBalon != jugador) {
                PlanAtaque planAtaque = equipoLocal ? planAtaqueLocal : planAtaqueRival;
                double offsetX;
                if (planAtaque == PlanAtaque.VERTICAL) {
                    offsetX = 74.0 + indiceRol * 18.0;
                } else if (planAtaque == PlanAtaque.CAMBIO_BANDA) {
                    offsetX = 56.0 + indiceRol * 14.0;
                    carrilRol = (ConfiguracionJuego.CAMPO_Y_MIN + ConfiguracionJuego.CAMPO_Y_MAX) - carrilRol - jugador.getAlto();
                } else {
                    offsetX = 48.0 + indiceRol * 12.0;
                }
                double objetivoPlanX = poseedorBalon.getX() + direccionAtaque * offsetX;
                x = lerp(x, objetivoPlanX, 0.16 + actividad * 0.20);
                y = lerp(y, carrilRol, (0.18 + actividad * 0.24) * FACTOR_ORDEN_TACTICO);
            } else if (!atacando) {
                if (cerrarRoboCercano) {
                    double ajusteRolY = (indiceRol - Math.max(0, totalCampo - 1) / 2.0) * 12.0;
                    y = lerp(y, poseedorBalon.getY() + ajusteRolY, 0.18 + actividad * 0.22);
                }
                PlanDefensa planDefensa = equipoLocal ? planDefensaLocal : planDefensaRival;
                double offsetX;
                if (planDefensa == PlanDefensa.PRESION_ALTA) {
                    offsetX = 32.0 + indiceRol * 12.0;
                } else if (planDefensa == PlanDefensa.REPLIEGUE) {
                    offsetX = 126.0 + indiceRol * 12.0;
                } else {
                    offsetX = 76.0 + indiceRol * 10.0;
                }
                double objetivoPlanX = poseedorBalon.getX() - direccionAtaque * offsetX;
                if (indiceRol == 0 && planDefensa == PlanDefensa.PRESION_ALTA) {
                    objetivoPlanX = poseedorBalon.getX() - direccionAtaque * 18.0;
                }
                double factorPlan = cerrarRoboCercano ? (0.08 + actividad * 0.10) : (0.18 + actividad * 0.20);
                double factorCarril = cerrarRoboCercano
                    ? (0.06 + actividad * 0.08) * FACTOR_ORDEN_TACTICO
                    : (0.16 + actividad * 0.22) * FACTOR_ORDEN_TACTICO;
                x = lerp(x, objetivoPlanX, factorPlan);
                y = lerp(y, carrilRol, factorCarril);
            }
        }

        // Separacion entre companeros para ocupar mejor ancho/largo.
        double[] separacion = calcularEmpujeSeparacionEquipo(jugador, equipoLocal);
        x += separacion[0];
        y += separacion[1];
        double[] limitadoPorArea = limitarObjetivoPorAreaNpc(jugador, equipoLocal, x, y);
        x = limitadoPorArea[0];
        y = limitadoPorArea[1];

        int xMin = ConfiguracionJuego.CAMPO_X_MIN + 16;
        int xMax = ConfiguracionJuego.CAMPO_X_MAX - jugador.getAncho() - 16;
        int yMin = ConfiguracionJuego.CAMPO_Y_MIN + 18;
        int yMax = ConfiguracionJuego.CAMPO_Y_MAX - jugador.getAlto() - 18;
        int xFinal = (int) Math.round(Math.max(xMin, Math.min(xMax, x)));
        int yFinal = (int) Math.round(Math.max(yMin, Math.min(yMax, y)));
        return new int[] { xFinal, yFinal };
    }

    private int[] normalizarObjetivoNpc(Jugador jugador, boolean equipoLocal, int objetivoX, int objetivoY, boolean extremo) {
        int[] objetivoActivo = ajustarObjetivoActivoNpc(jugador, equipoLocal, objetivoX, objetivoY);
        int[] objetivoAjustado = ajustarObjetivoIndividualNpc(jugador, objetivoActivo[0], objetivoActivo[1]);
        int[] objetivoForzado = forzarObjetivoActivoNpc(jugador, equipoLocal, objetivoAjustado[0], objetivoAjustado[1], extremo);
        return desbloquearObjetivoNpcEstancado(jugador, equipoLocal, objetivoForzado[0], objetivoForzado[1], extremo);
    }

    private int[] forzarObjetivoActivoNpc(Jugador jugador, boolean equipoLocal, int objetivoX, int objetivoY, boolean extremo) {
        if (esEjecutorPendiente(jugador) || jugador == poseedorBalon) {
            return new int[] { objetivoX, objetivoY };
        }
        if (Math.abs(objetivoX - jugador.getX()) + Math.abs(objetivoY - jugador.getY()) > MARGEN_OBJETIVO_INACTIVO_NPC) {
            return new int[] { objetivoX, objetivoY };
        }

        int indiceRol = indiceRolCampo(jugador, equipoLocal);
        int fase = (framesAnimacion + indiceRol * 9) % 120;
        int onda = fase < 60 ? fase - 30 : 90 - fase;
        int direccionAtaque = equipoLocal ? 1 : -1;
        double x = objetivoX + direccionAtaque * (extremo ? 20.0 : 14.0);
        double y = objetivoY + onda * (extremo ? 0.75 : 0.58);

        if (balonLibre) {
            int[] intercepcion = calcularObjetivoIntercepcionBalonLibre(jugador, equipoLocal);
            x = lerp(x, intercepcion[0], 0.52);
            y = lerp(y, intercepcion[1], 0.48);
        } else if (poseedorBalon != null && poseedorEsLocal != equipoLocal) {
            int[] marca = calcularObjetivoMarcaPase(jugador, poseedorBalon);
            x = lerp(x, marca[0], 0.50);
            y = lerp(y, marca[1], 0.50);
        } else if (!balonLibre && poseedorBalon != null) {
            x = lerp(x, calcularXApoyoOfensivo(poseedorBalon, equipoLocal, extremo), 0.42);
            y = lerp(y, calcularCarrilApoyo(poseedorBalon, equipoLocal, extremo), 0.42);
        }

        int xMin = ConfiguracionJuego.CAMPO_X_MIN + 16;
        int xMax = ConfiguracionJuego.CAMPO_X_MAX - jugador.getAncho() - 16;
        int yMin = ConfiguracionJuego.CAMPO_Y_MIN + 18;
        int yMax = ConfiguracionJuego.CAMPO_Y_MAX - jugador.getAlto() - 18;
        int xFinal = ajustarXPorReglaOffside(jugador, (int) Math.round(x));
        int yFinal = (int) Math.round(y);
        xFinal = Math.max(xMin, Math.min(xMax, xFinal));
        yFinal = Math.max(yMin, Math.min(yMax, yFinal));
        return new int[] { xFinal, yFinal };
    }

    private int[] desbloquearObjetivoNpcEstancado(Jugador jugador, boolean equipoLocal, int objetivoX, int objetivoY, boolean extremo) {
        if (Math.abs(objetivoX - jugador.getX()) + Math.abs(objetivoY - jugador.getY()) > 2) {
            return new int[] { objetivoX, objetivoY };
        }
        if (esEjecutorPendiente(jugador) || jugador == poseedorBalon) {
            return new int[] { objetivoX, objetivoY };
        }

        double x = objetivoX;
        double y = objetivoY;
        if (balonLibre) {
            int[] intercepcion = calcularObjetivoIntercepcionBalonLibre(jugador, equipoLocal);
            x = lerp(x, intercepcion[0], 0.78);
            y = lerp(y, intercepcion[1], 0.78);
        } else if (poseedorBalon != null && poseedorEsLocal != equipoLocal) {
            int[] marca = calcularObjetivoMarcaPase(jugador, poseedorBalon);
            x = lerp(x, marca[0], 0.74);
            y = lerp(y, marca[1], 0.74);
        } else if (poseedorBalon != null) {
            x = lerp(x, calcularXApoyoOfensivo(poseedorBalon, equipoLocal, extremo), 0.68);
            y = lerp(y, calcularCarrilApoyo(poseedorBalon, equipoLocal, extremo), 0.68);
        }

        int direccionAtaque = equipoLocal ? 1 : -1;
        x += direccionAtaque * (extremo ? 24.0 : 16.0);
        y += (((framesAnimacion / 6) % 2 == 0) ? 1.0 : -1.0) * (extremo ? 14.0 : 10.0);

        int xMin = ConfiguracionJuego.CAMPO_X_MIN + 16;
        int xMax = ConfiguracionJuego.CAMPO_X_MAX - jugador.getAncho() - 16;
        int yMin = ConfiguracionJuego.CAMPO_Y_MIN + 18;
        int yMax = ConfiguracionJuego.CAMPO_Y_MAX - jugador.getAlto() - 18;
        int xFinal = ajustarXPorReglaOffside(jugador, (int) Math.round(x));
        int yFinal = (int) Math.round(y);
        xFinal = Math.max(xMin, Math.min(xMax, xFinal));
        yFinal = Math.max(yMin, Math.min(yMax, yFinal));
        return new int[] { xFinal, yFinal };
    }

    private boolean debeBuscarHidratacion(Jugador jugador, boolean equipoLocal, double energia) {
        if (esPortero(jugador) || !hidratacionBanca.estaDisponible()) {
            return false;
        }
        boolean muyCansado = jugador.estaAgotado() || energia <= 0.12;
        if (!muyCansado) {
            return false;
        }
        if (!balonLibre && poseedorBalon == jugador) {
            return false;
        }
        if (!balonLibre && poseedorBalon != null && esJugadorLocal(poseedorBalon) == equipoLocal) {
            // Si su equipo ataca, solo se hidrata en estado extremo.
            return energia <= 0.07;
        }
        if (!balonLibre && poseedorBalon != null && esJugadorLocal(poseedorBalon) != equipoLocal && energia > 0.10) {
            // Defendiendo, todavia debe intentar competir salvo agotamiento severo.
            return false;
        }
        return true;
    }

    private int[] calcularObjetivoHidratacion(Jugador jugador) {
        int xMin = ConfiguracionJuego.CAMPO_X_MIN + 16;
        int xMax = ConfiguracionJuego.CAMPO_X_MAX - jugador.getAncho() - 16;
        int yMin = ConfiguracionJuego.CAMPO_Y_MIN + 8;
        int yMax = ConfiguracionJuego.CAMPO_Y_MAX - jugador.getAlto() - 18;
        int x = hidratacionBanca.getX() + hidratacionBanca.getAncho() / 2 - jugador.getAncho() / 2;
        int y = hidratacionBanca.getY() + hidratacionBanca.getAlto() / 2 - jugador.getAlto() / 2;
        x = ajustarXPorReglaOffside(jugador, x);
        x = Math.max(xMin, Math.min(xMax, x));
        y = Math.max(yMin, Math.min(yMax, y));
        return new int[] { x, y };
    }

    private double[] limitarObjetivoPorAreaNpc(Jugador jugador, boolean equipoLocal, double objetivoX, double objetivoY) {
        if (esPortero(jugador)) {
            return new double[] { objetivoX, objetivoY };
        }

        int[] base = posicionBaseJugador(jugador);
        double baseX = base[0];
        double baseY = base[1];
        double anclaX = baseX;
        double anclaY = baseY;

        double radioX = radioAreaXPorRol(jugador);
        double radioY = radioAreaYPorRol(jugador);
        double libertad = 0.92;

        if (poseedorBalon == jugador) {
            // Quien conduce necesita mas libertad para romper lineas.
            libertad = 1.28;
        } else if (balonLibre) {
            Jugador perseguidor = perseguidorBalonLibre(equipoLocal);
            double balonX = balon.getCentroX() - jugador.getAncho() / 2.0;
            double balonY = balon.getCentroY() - jugador.getAlto() / 2.0;
            if (perseguidor == jugador) {
                // El perseguidor no debe quedar amarrado a su zona base.
                anclaX = lerp(baseX, balonX, 0.92);
                anclaY = lerp(baseY, balonY, 0.92);
                libertad = 1.86;
            } else {
                // El resto acompaña la jugada en bloque sin amontonarse.
                anclaX = lerp(baseX, balonX, 0.46);
                anclaY = lerp(baseY, balonY, 0.42);
                libertad = 1.34;
            }
        } else if (!balonLibre && poseedorBalon != null) {
            boolean mismoEquipoPoseedor = esJugadorLocal(poseedorBalon) == equipoLocal;
            if (mismoEquipoPoseedor) {
                anclaX = lerp(baseX, poseedorBalon.getX(), 0.34);
                anclaY = lerp(baseY, poseedorBalon.getY(), 0.28);
                libertad = 1.12;
            } else if (seleccionarPresionadorEquipo(equipoLocal) == jugador) {
                anclaX = lerp(baseX, poseedorBalon.getX(), 0.78);
                anclaY = lerp(baseY, poseedorBalon.getY(), 0.66);
                libertad = 1.52;
            } else {
                anclaX = lerp(baseX, poseedorBalon.getX(), 0.50);
                anclaY = lerp(baseY, poseedorBalon.getY(), 0.44);
                libertad = 1.28;
            }
        }

        if (framesRitmoAlto > 0) {
            libertad = Math.max(libertad, 1.52);
        } else if (framesSinDinamismo > FRAMES_UMBRAL_BAJA_DINAMICA / 2) {
            libertad = Math.max(libertad, 1.44);
        }

        double minX = anclaX - radioX * libertad;
        double maxX = anclaX + radioX * libertad;
        double minY = anclaY - radioY * libertad;
        double maxY = anclaY + radioY * libertad;

        minX = Math.max(ConfiguracionJuego.CAMPO_X_MIN + 14.0, minX);
        maxX = Math.min(ConfiguracionJuego.CAMPO_X_MAX - jugador.getAncho() - 14.0, maxX);
        minY = Math.max(ConfiguracionJuego.CAMPO_Y_MIN + 16.0, minY);
        maxY = Math.min(ConfiguracionJuego.CAMPO_Y_MAX - jugador.getAlto() - 16.0, maxY);

        double x = Math.max(minX, Math.min(maxX, objetivoX));
        double y = Math.max(minY, Math.min(maxY, objetivoY));
        return new double[] { x, y };
    }

    private int[] posicionBaseJugador(Jugador jugador) {
        if (jugador == jugadorPrincipal) {
            return new int[] { ConfiguracionJuego.POS_X_BASE_LOCAL, ConfiguracionJuego.POS_Y_CAMPO_ARRIBA };
        }
        if (jugador == aliadoLocal) {
            return new int[] { ConfiguracionJuego.POS_X_BASE_LOCAL + 45, ConfiguracionJuego.POS_Y_CAMPO_ABAJO };
        }
        if (jugador == extremoLocal) {
            return new int[] { ConfiguracionJuego.POS_X_BASE_LOCAL + 78, ConfiguracionJuego.POS_Y_CAMPO_ARRIBA + 84 };
        }
        if (jugador == mediaLocal) {
            return new int[] { ConfiguracionJuego.POS_X_BASE_LOCAL + 24, ConfiguracionJuego.POS_Y_CAMPO_ABAJO + 72 };
        }
        if (jugador == rivalUno) {
            return new int[] { ConfiguracionJuego.POS_X_BASE_RIVAL - 30, ConfiguracionJuego.POS_Y_CAMPO_ARRIBA };
        }
        if (jugador == rivalDos) {
            return new int[] { ConfiguracionJuego.POS_X_BASE_RIVAL, ConfiguracionJuego.POS_Y_CAMPO_ABAJO };
        }
        if (jugador == extremoRival) {
            return new int[] { ConfiguracionJuego.POS_X_BASE_RIVAL - 44, ConfiguracionJuego.POS_Y_CAMPO_ARRIBA + 94 };
        }
        if (jugador == mediaRival) {
            return new int[] { ConfiguracionJuego.POS_X_BASE_RIVAL - 10, ConfiguracionJuego.POS_Y_CAMPO_ABAJO + 68 };
        }
        return new int[] { jugador.getX(), jugador.getY() };
    }

    private double radioAreaXPorRol(Jugador jugador) {
        if (jugador == jugadorPrincipal || jugador == rivalUno) {
            return 176.0;
        }
        if (jugador == aliadoLocal || jugador == rivalDos) {
            return 160.0;
        }
        if (jugador == extremoLocal || jugador == extremoRival) {
            return 194.0;
        }
        if (jugador == mediaLocal || jugador == mediaRival) {
            return 168.0;
        }
        return 172.0;
    }

    private double radioAreaYPorRol(Jugador jugador) {
        if (jugador == jugadorPrincipal || jugador == rivalUno) {
            return 132.0;
        }
        if (jugador == aliadoLocal || jugador == rivalDos) {
            return 138.0;
        }
        if (jugador == extremoLocal || jugador == extremoRival) {
            return 146.0;
        }
        if (jugador == mediaLocal || jugador == mediaRival) {
            return 142.0;
        }
        return 136.0;
    }

    private double[] calcularEmpujeSeparacionEquipo(Jugador jugador, boolean equipoLocal) {
        double empujeX = 0.0;
        double empujeY = 0.0;
        for (Jugador otro : equipoLocal ? getLocales() : getRivales()) {
            if (otro == jugador || esPortero(otro) || otro.estaExpulsado()) {
                continue;
            }
            double dx = (jugador.getX() + jugador.getAncho() / 2.0) - (otro.getX() + otro.getAncho() / 2.0);
            double dy = (jugador.getY() + jugador.getAlto() / 2.0) - (otro.getY() + otro.getAlto() / 2.0);
            double distancia = Math.hypot(dx, dy);
            if (distancia < 0.001 || distancia > 152.0) {
                continue;
            }
            double factor = (152.0 - distancia) / 152.0;
            empujeX += (dx / distancia) * factor * 32.0;
            empujeY += (dy / distancia) * factor * 28.0;
        }
        return new double[] { empujeX, empujeY };
    }

    private double lerp(double actual, double objetivo, double factor) {
        return actual + (objetivo - actual) * Math.max(0.0, Math.min(1.0, factor));
    }

    private double energiaRelativa(Jugador jugador) {
        double max = Math.max(1.0, jugador.getStaminaMax());
        return Math.max(0.0, Math.min(1.0, jugador.getStamina() / max));
    }

    private double cansancioGeneralEquipo(boolean equipoLocal) {
        Jugador[] equipo = equipoLocal ? getLocales() : getRivales();
        double suma = 0.0;
        int total = 0;
        for (Jugador jugador : equipo) {
            if (esPortero(jugador) || jugador.estaExpulsado()) {
                continue;
            }
            suma += 1.0 - energiaRelativa(jugador);
            total++;
        }
        if (total == 0) {
            return 0.0;
        }
        return suma / total;
    }

    private double factorMomentumEquipo(boolean equipoLocal) {
        int frames = equipoLocal ? framesMomentumLocal : framesMomentumRival;
        int bonus = equipoLocal ? puntosBonus : puntosBonusRival;
        double porFrames = Math.max(0.0, Math.min(1.0, frames / (double) (ConfiguracionJuego.FPS * 8)));
        double porBonus = Math.max(0.0, Math.min(1.0, bonus / 10.0));
        return Math.max(0.0, Math.min(0.95, porFrames * 0.72 + porBonus * 0.28));
    }

    private boolean enTransicionOfensiva(boolean equipoLocal) {
        return (equipoLocal ? framesTransicionLocal : framesTransicionRival) > 0;
    }

    private void activarTransicionOfensiva(boolean equipoLocal, int intensidadFrames) {
        int frames = Math.max(8, intensidadFrames);
        if (equipoLocal) {
            framesTransicionLocal = Math.max(framesTransicionLocal, frames);
            framesTransicionRival = Math.max(framesTransicionRival, frames / 2);
        } else {
            framesTransicionRival = Math.max(framesTransicionRival, frames);
            framesTransicionLocal = Math.max(framesTransicionLocal, frames / 2);
        }
    }

    private void actualizarPlanesNpc() {
        if (framesPlanLocal > 0) {
            framesPlanLocal--;
        }
        if (framesPlanRival > 0) {
            framesPlanRival--;
        }
        if (framesPlanLocal == 0) {
            planAtaqueLocal = seleccionarPlanAtaque(true);
            planDefensaLocal = seleccionarPlanDefensa(true);
            framesPlanLocal = 16 + aleatorio.nextInt(14);
        }
        if (framesPlanRival == 0) {
            planAtaqueRival = seleccionarPlanAtaque(false);
            planDefensaRival = seleccionarPlanDefensa(false);
            framesPlanRival = 16 + aleatorio.nextInt(14);
        }
    }

    private PlanAtaque seleccionarPlanAtaque(boolean equipoLocal) {
        double energia = 1.0 - cansancioGeneralEquipo(equipoLocal);
        if (energia < 0.18) {
            return PlanAtaque.POSICIONAL;
        }
        return aleatorio.nextDouble() < 0.66 ? PlanAtaque.VERTICAL : PlanAtaque.CAMBIO_BANDA;
    }

    private PlanDefensa seleccionarPlanDefensa(boolean equipoLocal) {
        double energia = 1.0 - cansancioGeneralEquipo(equipoLocal);
        if (energia > 0.14) {
            return PlanDefensa.PRESION_ALTA;
        }
        return PlanDefensa.BLOQUE_MEDIO;
    }

    private double calcularAmbicionGanadora(boolean equipoLocal) {
        int diferencia = equipoLocal ? (golesRival - golesLocal) : (golesLocal - golesRival);
        double urgenciaMarcador = diferencia > 0 ? Math.min(0.8, diferencia * 0.34) : 0.08;
        double progreso = Math.max(0.0, Math.min(1.0, framesPartidoJugados / (double) DURACION_PARTIDO_FRAMES));
        double urgenciaTiempo = progreso > 0.60 ? (progreso - 0.60) * 1.05 : 0.0;
        return Math.max(0.12, Math.min(0.95, urgenciaMarcador + urgenciaTiempo));
    }

    private int indiceRolCampo(Jugador jugador, boolean equipoLocal) {
        Jugador[] equipo = equipoLocal ? getLocales() : getRivales();
        int indice = 0;
        for (Jugador candidato : equipo) {
            if (esPortero(candidato) || candidato.estaExpulsado()) {
                continue;
            }
            if (candidato == jugador) {
                return indice;
            }
            indice++;
        }
        return 0;
    }

    private int totalJugadoresCampo(boolean equipoLocal) {
        int total = 0;
        for (Jugador jugador : equipoLocal ? getLocales() : getRivales()) {
            if (!esPortero(jugador) && !jugador.estaExpulsado()) {
                total++;
            }
        }
        return Math.max(1, total);
    }

    private double carrilCoordinadoPorRol(int indice, int total) {
        double fraccion = total <= 1 ? 0.5 : (indice + 1.0) / (total + 1.0);
        double yMin = ConfiguracionJuego.CAMPO_Y_MIN + 48.0;
        double yMax = ConfiguracionJuego.CAMPO_Y_MAX - 52.0;
        return yMin + (yMax - yMin) * fraccion;
    }

    private int ajustarXPorReglaOffside(Jugador jugador, int objetivoX) {
        if (REGLAS_CALLEJERAS) {
            return objetivoX;
        }
        if (balonLibre || poseedorBalon == null || poseedorBalon == jugador || esPortero(jugador)) {
            return objetivoX;
        }
        boolean jugadorLocal = esJugadorLocal(jugador);
        if (jugadorLocal != poseedorEsLocal) {
            return objetivoX;
        }

        double centroObjetivo = objetivoX + jugador.getAncho() / 2.0;
        double medio = ConfiguracionJuego.ANCHO_PANEL / 2.0;
        if (jugadorLocal) {
            // Ataca hacia la derecha.
            if (centroObjetivo <= medio) {
                return objetivoX;
            }
            double lineaValida = Math.max(balon.getCentroX(), calcularLineaSegundoDefensor(false)) - MARGEN_OFFSIDE_NPC;
            int xMaxOnside = (int) Math.round(lineaValida - jugador.getAncho() / 2.0);
            return Math.min(objetivoX, xMaxOnside);
        }
        // Rival ataca hacia la izquierda.
        if (centroObjetivo >= medio) {
            return objetivoX;
        }
        double lineaValida = Math.min(balon.getCentroX(), calcularLineaSegundoDefensor(true)) + MARGEN_OFFSIDE_NPC;
        int xMinOnside = (int) Math.round(lineaValida - jugador.getAncho() / 2.0);
        return Math.max(objetivoX, xMinOnside);
    }

    private boolean intersecanRectangulos(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        return x1 < x2 + w2
            && x1 + w1 > x2
            && y1 < y2 + h2
            && y1 + h1 > y2;
    }

    private int calcularPaso(int actual, int objetivo, int velocidadMaxima) {
        // Avanza hacia un objetivo respetando la velocidad maxima.
        if (actual < objetivo) {
            return Math.min(velocidadMaxima, objetivo - actual);
        }
        if (actual > objetivo) {
            return -Math.min(velocidadMaxima, actual - objetivo);
        }
        return 0;
    }

    private boolean debeSprintarNpc(Jugador jugador, int objetivoX, int objetivoY, boolean portero) {
        if (portero || !jugador.puedeSprintar()) {
            return false;
        }
        boolean equipoLocal = esJugadorLocal(jugador);
        int distanciaManhattan = Math.abs(objetivoX - jugador.getX()) + Math.abs(objetivoY - jugador.getY());
        double energia = energiaRelativa(jugador);
        double cansancioEquipo = cansancioGeneralEquipo(equipoLocal);
        boolean disputa = poseedorBalon == jugador || distanciaAlBalon(jugador) < 120.0 || balonLibre;
        boolean oportunidadAtaque = poseedorBalon == jugador && distanciaHorizontalAlArco(jugador) < 360.0;
        boolean retornoDefensivo = poseedorBalon != null && esJugadorLocal(poseedorBalon) != equipoLocal;
        boolean transicion = enTransicionOfensiva(equipoLocal) || enTransicionOfensiva(!equipoLocal);
        if (jugador.getStamina() < 14.0) {
            return false;
        }
        int umbralDistancia = (int) Math.round(118.0 - energia * 48.0 + cansancioEquipo * 32.0);
        if (disputa || retornoDefensivo) {
            umbralDistancia -= 18;
        }
        if (transicion) {
            umbralDistancia -= 14;
            disputa = true;
        }
        if (framesRitmoAlto > 0) {
            umbralDistancia -= 24;
            disputa = true;
        }
        umbralDistancia = Math.max(56, Math.min(130, umbralDistancia));
        return distanciaManhattan > umbralDistancia && (disputa || oportunidadAtaque || retornoDefensivo);
    }

    private boolean debeSprintarPortero(Jugador portero, boolean local, int objetivoX, int objetivoY) {
        if (!portero.puedeSprintar()) {
            return false;
        }
        int distanciaManhattan = Math.abs(objetivoX - portero.getX()) + Math.abs(objetivoY - portero.getY());
        boolean amenaza = convieneSalidaPortero(local)
            || balonAmenazaArco(local)
            || (!balonLibre && poseedorBalon != null && esJugadorLocal(poseedorBalon) != local && amenazaDeDisparoDelPoseedor(local));
        if (portero.getStamina() < 14.0) {
            return false;
        }
        if (framesRitmoAlto > 0 && distanciaManhattan > 26) {
            return true;
        }
        return amenaza && distanciaManhattan > 24;
    }

    private int contarAmarillasEquipo(boolean equipoLocal) {
        int total = 0;
        for (Jugador jugador : equipoLocal ? getLocales() : getRivales()) {
            total += jugador.getTarjetasAmarillas();
        }
        return total;
    }

    private int contarRojasEquipo(boolean equipoLocal) {
        int total = 0;
        for (Jugador jugador : equipoLocal ? getLocales() : getRivales()) {
            if (jugador.tieneTarjetaRoja()) {
                total++;
            }
        }
        return total;
    }

    private void limitarEntidadAlPanel(EntidadJuego entidad) {
        // Limite duro dentro del panel visible.
        if (entidad.getX() < 0) {
            entidad.setX(0);
        }
        if (entidad.getY() < 0) {
            entidad.setY(0);
        }
        if (entidad.getX() + entidad.getAncho() > ConfiguracionJuego.ANCHO_PANEL) {
            entidad.setX(ConfiguracionJuego.ANCHO_PANEL - entidad.getAncho());
        }
        if (entidad.getY() + entidad.getAlto() > ConfiguracionJuego.ALTO_PANEL) {
            entidad.setY(ConfiguracionJuego.ALTO_PANEL - entidad.getAlto());
        }
    }

    public Jugador[] getLocales() {
        // Orden estable para render y HUD.
        return jugadoresLocales;
    }

    public Jugador[] getRivales() {
        return jugadoresRivales;
    }

    public Jugador[] getTodosJugadores() {
        return todosJugadores;
    }

    public Jugador getJugadorPrincipal() {
        return jugadorPrincipal;
    }

    public Jugador getArbitro() {
        return arbitro;
    }

    public Balon getBalon() {
        return balon;
    }

    public HidratacionBanca getHidratacionBanca() {
        return hidratacionBanca;
    }

    public Turbo getTurbo() {
        return turbo;
    }

    public GeometriaCancha getCancha() {
        return cancha;
    }

    public int getGolesLocal() {
        return golesLocal;
    }

    public int getGolesRival() {
        return golesRival;
    }

    public int getPuntosBonus() {
        return puntosBonus;
    }

    public int getPuntosBonusRival() {
        return puntosBonusRival;
    }

    public int getUsosHidratacionBancaRestantes() {
        return hidratacionBanca.getUsosRestantes();
    }

    public int getStaminaPrincipalPorcentaje() {
        return (int) Math.round((jugadorPrincipal.getStamina() / jugadorPrincipal.getStaminaMax()) * 100.0);
    }

    public String getPoseedorTexto() {
        if (balonLibre || poseedorBalon == null) {
            return "Libre";
        }
        return poseedorEsLocal ? "Local" : "Rival";
    }

    public String getTiempoPartidoTexto() {
        int frames = Math.min(DURACION_PARTIDO_FRAMES, framesPartidoJugados);
        int segundosReales = frames / ConfiguracionJuego.FPS;
        int total = Math.max(1, ConfiguracionJuego.DURACION_PARTIDO_SEGUNDOS);
        int minutosEscalados = (int) Math.round((segundosReales / (double) total) * ConfiguracionJuego.MINUTOS_REGLAMENTARIOS);
        minutosEscalados = Math.max(0, Math.min(ConfiguracionJuego.MINUTOS_REGLAMENTARIOS, minutosEscalados));
        int segReloj = (int) Math.round(((segundosReales / (double) total) * ConfiguracionJuego.MINUTOS_REGLAMENTARIOS - minutosEscalados) * 60.0);
        if (minutosEscalados >= ConfiguracionJuego.MINUTOS_REGLAMENTARIOS) {
            return "90:00";
        }
        return String.format("%02d:%02d", minutosEscalados, Math.max(0, Math.min(59, segReloj)));
    }

    public int getFramesAnimacion() {
        return framesAnimacion;
    }

    public boolean isModoEspectador() {
        return modoEspectador;
    }

    public void setModoEspectador(boolean modoEspectador) {
        this.modoEspectador = modoEspectador;
        narrar(modoEspectador ? "👀 Modo espectador activado" : "🎮 Modo jugador activado", true);
    }

    public void alternarModoEspectador() {
        setModoEspectador(!modoEspectador);
    }

    public String getTextoSaque() {
        return textoSaque;
    }

    public String getNarracionActual() {
        return narracionActual;
    }

    public int getFramesNarracion() {
        return framesNarracion;
    }

    public int getFramesTextoSaque() {
        return framesTextoSaque;
    }

    public String getEstadoArbitrajeTexto() {
        if (estadoArbitrajeActual == null) {
            return "";
        }
        if (estadoArbitrajeActual == EstadoArbitraje.COBRA_SAQUE) {
            return "SAQUE";
        }
        if (estadoArbitrajeActual == EstadoArbitraje.MARCA_FALTA) {
            return "FALTA";
        }
        if (estadoArbitrajeActual == EstadoArbitraje.TARJETA_AMARILLA) {
            return "AMARILLA";
        }
        if (estadoArbitrajeActual == EstadoArbitraje.TARJETA_ROJA) {
            return "ROJA";
        }
        if (estadoArbitrajeActual == EstadoArbitraje.APLICA_VENTAJA) {
            return "SIGA";
        }
        if (estadoArbitrajeActual == EstadoArbitraje.VALIDA_GOL) {
            return "GOL";
        }
        return "";
    }

    public int getFramesAccionArbitro() {
        return framesAccionArbitro;
    }

    public String getResumenTarjetas() {
        return "Tarjetas L " + contarAmarillasEquipo(true) + "A/" + contarRojasEquipo(true) + "R"
            + " | R " + contarAmarillasEquipo(false) + "A/" + contarRojasEquipo(false) + "R";
    }

    public String getDisciplinaPrincipalTexto() {
        String base = "Leo " + jugadorPrincipal.getTarjetasAmarillas() + "A/" + (jugadorPrincipal.tieneTarjetaRoja() ? "1R" : "0R")
            + " Faltas " + jugadorPrincipal.getFaltasCometidas();
        return jugadorPrincipal.estaExpulsado() ? base + " EXP" : base;
    }

    public boolean isSorteoMonedaActivo() {
        return sorteoMonedaActivo;
    }

    public int getFramesAnimacionMoneda() {
        return framesAnimacionMoneda;
    }

    public int getFramesSorteoMoneda() {
        return framesSorteoMoneda;
    }

    public boolean isPrimerSaqueLocal() {
        return primerSaqueLocal;
    }

    public String getResultadoMoneda() {
        return resultadoMoneda;
    }

    public String getUltimoGoleador() {
        return ultimoGoleador;
    }

    public String getResumenUltimoGol() {
        if (ultimoGoleador == null || ultimoGoleador.isEmpty()) {
            return "";
        }
        return ultimoEquipoGoleador + ": " + ultimoGoleador;
    }
}
