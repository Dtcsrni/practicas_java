package juego.core;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.Random;

import juego.entidades.Balon;
import juego.entidades.EntidadJuego;
import juego.entidades.Jugador;
import juego.entidades.MonedaEspecial;
import juego.entidades.Turbo;
import juego.sonido.TipoSonido;

// Nucleo de la simulacion: mueve jugadores, resuelve posesion y actualiza el partido.
public class MotorJuego {
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
    private static final double ALCANCE_X_PORTERO = 178.0;
    private static final double ALCANCE_Y_PORTERO = 132.0;
    private static final int LOOKAHEAD_ATAJADA_FRAMES = 14;
    private static final double FACTOR_ATAJADA_MIN = 0.52;
    private static final double FACTOR_ATAJADA_MAX = 0.74;
    private static final double GRAVEDAD_BALON = 0.36;
    private static final int MARGEN_REANUDACION = 72;
    private static final double ARRASTRE_BALON = 0.34;
    private static final double APORTE_MOVIMIENTO_POSEEDOR = 0.20;
    private static final double IMPULSO_BASE_MOVIMIENTO = 0.34;
    private static final int COOLDOWN_DECISION_NPC = 18;
    private static final int RETRASO_SAQUE_FRAMES = 24;
    private static final double DISTANCIA_PRESION_ALTA = 86.0;
    private static final double DISTANCIA_PASE_SEGURA = 250.0;
    private static final double DISTANCIA_PASE_NPC = 170.0;
    private static final double DISTANCIA_TIRO_CLARA = 210.0;
    private static final double MAX_ADELANTO_GOLPEO = 12.0;
    private static final int RETARDO_REACCION_ATAJADA_FRAMES = 4;
    private static final int Y_APERTURA_SUPERIOR = ConfiguracionJuego.CAMPO_Y_MIN + 130;
    private static final int Y_APERTURA_INFERIOR = ConfiguracionJuego.CAMPO_Y_MAX - 130;

    private final Random aleatorio;
    private final Rectangle arcoIzquierdo;
    private final Rectangle arcoDerecho;

    private final Jugador porteroLocal;
    private final Jugador jugadorPrincipal;
    private final Jugador aliadoLocal;

    private final Jugador porteroRival;
    private final Jugador rivalUno;
    private final Jugador rivalDos;

    private final Balon balon;
    private final MonedaEspecial monedaEspecial;
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
    private int movPorteroLocalX;
    private int movPorteroLocalY;
    private int movPorteroRivalX;
    private int movPorteroRivalY;
    private boolean sprintPrincipal;
    private boolean sprintAliado;
    private boolean sprintRivalUno;
    private boolean sprintRivalDos;
    private boolean sprintPorteroLocal;
    private boolean sprintPorteroRival;

    private int golesLocal;
    private int golesRival;
    private int puntosBonus;
    private int contadorAparicionMonedaEspecial;
    private int contadorAparicionTurbo;
    private int framesTurboRestantesEnEscenario;
    private int cooldownRoboFrames;
    private int cooldownCapturaLibreFrames;
    private int cooldownDecisionNpcFrames;
    private int cooldownAtajadaPorteroFrames;
    private int framesDesdeUltimoDisparo;
    private double errorLecturaPorteroLocal;
    private double errorLecturaPorteroRival;
    private int cooldownLecturaPorteroLocalFrames;
    private int cooldownLecturaPorteroRivalFrames;
    private Jugador ultimoPateador;
    private int bloqueoRecapturaUltimoPateadorFrames;
    private int framesAnimacion;
    private boolean ultimoToqueLocal;
    private String textoSaque;
    private int framesTextoSaque;
    private TipoReanudacion tipoReanudacionPendiente;
    private Jugador ejecutorReanudacion;
    private boolean saquePendienteLocal;
    private int saquePendienteX;
    private int saquePendienteY;
    private int framesRetrasoSaque;
    private boolean balonEnManos;
    private String ultimoGoleador;
    private String ultimoEquipoGoleador;

    public MotorJuego() {
        // Geometria de gol usada para goles, travesano y saques.
        aleatorio = new Random();
        // Los arcos coinciden con la linea final jugable.
        arcoIzquierdo = new Rectangle(
            ConfiguracionJuego.CAMPO_X_MIN - 2,
            ConfiguracionJuego.Y_PORTERIA,
            4,
            ConfiguracionJuego.ALTO_PORTERIA
        );
        arcoDerecho = new Rectangle(
            ConfiguracionJuego.CAMPO_X_MAX - 2,
            ConfiguracionJuego.Y_PORTERIA,
            4,
            ConfiguracionJuego.ALTO_PORTERIA
        );

        // Plantillas fijas del 3 vs 3.
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

        balon = new Balon(ConfiguracionJuego.ANCHO_PANEL / 2 - 10, ConfiguracionJuego.ALTO_PANEL / 2 - 10, 20);
        monedaEspecial = new MonedaEspecial(24);
        turbo = new Turbo();
        sonidosPendientes = new ArrayDeque<>();
        reiniciarPartido();
    }

    public void reiniciarPartido() {
        // Reinicio completo del partido y sus contadores.
        golesLocal = 0;
        golesRival = 0;
        puntosBonus = 0;
        contadorAparicionMonedaEspecial = 0;
        contadorAparicionTurbo = 0;
        framesTurboRestantesEnEscenario = 0;
        cooldownRoboFrames = 0;
        cooldownCapturaLibreFrames = 0;
        cooldownDecisionNpcFrames = 0;
        cooldownAtajadaPorteroFrames = 0;
        framesDesdeUltimoDisparo = RETARDO_REACCION_ATAJADA_FRAMES + 12;
        errorLecturaPorteroLocal = 0.0;
        errorLecturaPorteroRival = 0.0;
        cooldownLecturaPorteroLocalFrames = 0;
        cooldownLecturaPorteroRivalFrames = 0;
        tipoReanudacionPendiente = TipoReanudacion.NINGUNA;
        ejecutorReanudacion = null;
        framesRetrasoSaque = 0;
        balonEnManos = false;
        ultimoPateador = null;
        bloqueoRecapturaUltimoPateadorFrames = 0;
        framesAnimacion = 0;
        ultimoToqueLocal = true;
        textoSaque = "";
        framesTextoSaque = 0;
        ultimoGoleador = "";
        ultimoEquipoGoleador = "";
        monedaEspecial.desactivar();
        turbo.desactivar();
        reiniciarJugada(true);
    }

    public EventoJuego actualizar(EntradaJuego entrada) {
        framesAnimacion++;

        if (framesRetrasoSaque > 0) {
            framesRetrasoSaque--;
            moverPrincipal(entrada);
            moverAliadoLocal();
            moverRivales();
            moverPorteros();
            actualizarEstadoJugadores();
            arrastrarBalonConPoseedor();
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
        moverPrincipal(entrada);
        moverAliadoLocal();
        moverRivales();
        moverPorteros();

        // 2) Estados temporales y animaciones.
        actualizarEstadoJugadores();

        // 3) Posesion, disparo y fisica del balon.
        actualizarPosesionYBalon(entrada);

        // 4) Goles y salidas por lineas.
        EventoJuego eventoGol = verificarGol();
        if (eventoGol != EventoJuego.NINGUNO) {
            return eventoGol;
        }

        // 5) Bonus y temporizadores auxiliares.
        actualizarMonedaEspecial();
        actualizarTurbo();
        actualizarTemporizadoresGlobales();
        return EventoJuego.NINGUNO;
    }

    private void moverPrincipal(EntradaJuego entrada) {
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

        // El jugador humano es el unico cuya direccion sale directo del teclado.
        sprintPrincipal = entrada.estaCorriendo() && jugadorPrincipal.puedeSprintar();
        int velocidad = jugadorPrincipal.getVelocidadMovimiento(sprintPrincipal);
        movPrincipalX = entrada.calcularDeltaX(velocidad);
        movPrincipalY = entrada.calcularDeltaY(velocidad);
        jugadorPrincipal.mover(movPrincipalX, movPrincipalY);
        limitarEntidadAlPanel(jugadorPrincipal);
    }

    private void moverAliadoLocal() {
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
            // En defensa intenta cerrar linea de pase y proteger el carril central.
            objetivoX = (poseedorBalon.getX() + jugadorPrincipal.getX()) / 2 - 28;
            objetivoY = calcularYBloqueDefensivo(poseedorBalon, true, false);
        } else {
            boolean persigue = perseguidorBalonLibre(true) == aliadoLocal;
            if (persigue) {
                objetivoX = (int) balon.getCentroX() - aliadoLocal.getAncho() / 2;
                objetivoY = (int) balon.getCentroY() - aliadoLocal.getAlto() / 2;
            } else {
                objetivoX = calcularXApoyoLibre(true, false);
                objetivoY = calcularYApoyoLibre(true, false);
            }
        }

        sprintAliado = debeSprintarNpc(aliadoLocal, objetivoX, objetivoY, false);
        int velocidadAliado = aliadoLocal.getVelocidadMovimiento(sprintAliado);
        movAliadoX = calcularPaso(aliadoLocal.getX(), objetivoX, velocidadAliado);
        movAliadoY = calcularPaso(aliadoLocal.getY(), objetivoY, velocidadAliado);
        aliadoLocal.mover(movAliadoX, movAliadoY);
        limitarEntidadAlPanel(aliadoLocal);
    }

    private void moverRivales() {
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
            objetivoRivalUnoX = poseedorBalon.getX() - 14;
            objetivoRivalUnoY = poseedorBalon.getY();
        } else if (balonLibre) {
            Jugador perseguidor = perseguidorBalonLibre(false);
            if (perseguidor == rivalUno) {
                objetivoRivalUnoX = balon.getX() - 8;
                objetivoRivalUnoY = balon.getY();
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
            objetivoRivalDosX = calcularXCobertura(poseedorBalon, false, true);
            objetivoRivalDosY = calcularYBloqueDefensivo(poseedorBalon, false, true);
        } else if (balonLibre) {
            Jugador perseguidor = perseguidorBalonLibre(false);
            if (perseguidor == rivalUno) {
                objetivoRivalDosX = calcularXApoyoLibre(false, true);
                objetivoRivalDosY = calcularYApoyoLibre(false, true);
            } else {
                objetivoRivalDosX = balon.getX() - 8;
                objetivoRivalDosY = balon.getY();
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

        sprintRivalUno = debeSprintarNpc(rivalUno, objetivoRivalUnoX, objetivoRivalUnoY, false);
        int velocidadRivalUno = rivalUno.getVelocidadMovimiento(sprintRivalUno);
        movRivalUnoX = calcularPaso(rivalUno.getX(), objetivoRivalUnoX, velocidadRivalUno);
        movRivalUnoY = calcularPaso(rivalUno.getY(), objetivoRivalUnoY, velocidadRivalUno);
        rivalUno.mover(movRivalUnoX, movRivalUnoY);
        limitarEntidadAlPanel(rivalUno);

        sprintRivalDos = debeSprintarNpc(rivalDos, objetivoRivalDosX, objetivoRivalDosY, false);
        int velocidadRivalDos = rivalDos.getVelocidadMovimiento(sprintRivalDos);
        movRivalDosX = calcularPaso(rivalDos.getX(), objetivoRivalDosX, velocidadRivalDos);
        movRivalDosY = calcularPaso(rivalDos.getY(), objetivoRivalDosY, velocidadRivalDos);
        rivalDos.mover(movRivalDosX, movRivalDosY);
        limitarEntidadAlPanel(rivalDos);
    }

    private void moverPorteros() {
        // El portero ya no sigue solo la Y de la jugada:
        // intenta cerrar el angulo al arco y se adelanta si detecta una amenaza real.
        int xObjetivoLocal;
        int yObjetivoLocal;
        if (esEjecutorPendiente(porteroLocal)) {
            xObjetivoLocal = calcularXEjecutorPendiente(porteroLocal, true);
            yObjetivoLocal = calcularYEjecutorPendiente(porteroLocal);
        } else {
            xObjetivoLocal = calcularXObjetivoPortero(porteroLocal, true);
            yObjetivoLocal = calcularYObjetivoPortero(porteroLocal, true);
        }
        sprintPorteroLocal = debeSprintarPortero(porteroLocal, true, xObjetivoLocal, yObjetivoLocal);
        int velocidadLocal = calcularVelocidadPortero(porteroLocal, true);
        movPorteroLocalX = calcularPaso(porteroLocal.getX(), xObjetivoLocal, velocidadLocal);
        movPorteroLocalY = calcularPaso(porteroLocal.getY(), yObjetivoLocal, velocidadLocal);
        porteroLocal.mover(movPorteroLocalX, movPorteroLocalY);

        int xObjetivoRival;
        int yObjetivoRival;
        if (esEjecutorPendiente(porteroRival)) {
            xObjetivoRival = calcularXEjecutorPendiente(porteroRival, false);
            yObjetivoRival = calcularYEjecutorPendiente(porteroRival);
        } else {
            xObjetivoRival = calcularXObjetivoPortero(porteroRival, false);
            yObjetivoRival = calcularYObjetivoPortero(porteroRival, false);
        }
        sprintPorteroRival = debeSprintarPortero(porteroRival, false, xObjetivoRival, yObjetivoRival);
        int velocidadRival = calcularVelocidadPortero(porteroRival, false);
        movPorteroRivalX = calcularPaso(porteroRival.getX(), xObjetivoRival, velocidadRival);
        movPorteroRivalY = calcularPaso(porteroRival.getY(), yObjetivoRival, velocidadRival);
        porteroRival.mover(movPorteroRivalX, movPorteroRivalY);

        limitarPorteroEnZona(porteroLocal, true);
        limitarPorteroEnZona(porteroRival, false);
    }

    private void actualizarPosesionYBalon(EntradaJuego entrada) {
        // Evita bloqueos en esquinas cuando la pelota se queda muerta.
        resolverAtascoEnEsquina();

        if (balonLibre) {
            // Sin poseedor, el balon usa fisica pura y cualquiera puede capturarlo.
            balon.actualizarFisica(ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL);
            if (intentarAtajadaPortero()) {
                return;
            }
            intentarCapturaBalonLibre();
            return;
        }

        // Con poseedor, primero hay disputa por robo y luego decision de accion.
        intentarRobo();
        boolean accion = ejecutarAccionBalonSiAplica(entrada) || ejecutarAccionNpcSiAplica();
        if (!accion) {
            // Si nadie pasa ni tira, el balon sigue "pegado" al conductor con un arrastre suave.
            arrastrarBalonConPoseedor();
        }
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
        }
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
        if (balonEnManos && esPortero(poseedorBalon)) {
            return;
        }

        // El robo no es instantaneo: exige cercania, compara inercia/velocidad
        // y suma una pequena variacion aleatoria para que las disputas no sean identicas.
        Jugador[] candidatos = poseedorEsLocal ? getRivales() : getLocales();
        for (Jugador candidato : candidatos) {
            if (!estanEnRangoDeRobo(candidato, poseedorBalon)) {
                continue;
            }

            int fuerzaAtacante = velocidadMovimiento(candidato) + candidato.getVelocidad();
            int fuerzaPoseedor = velocidadMovimiento(poseedorBalon) + poseedorBalon.getVelocidad() + 2;
            int tirada = aleatorio.nextInt(3);

            if (fuerzaAtacante + tirada >= fuerzaPoseedor + 3) {
                poseedorBalon = candidato;
                poseedorEsLocal = esJugadorLocal(candidato);
                ultimoToqueLocal = poseedorEsLocal;
                cooldownRoboFrames = (int) (ConfiguracionJuego.FPS * 0.65);
                registrarSonido(TipoSonido.ROBO);
                break;
            }
        }
    }

    private void intentarCapturaBalonLibre() {
        // Evita recaptura instantanea justo despues de un pase o tiro.
        if (cooldownCapturaLibreFrames > 0) {
            return;
        }

        // Solo se puede controlar una pelota baja y suficientemente lenta.
        if (balon.getAltura() > ALTURA_MAXIMA_CONTROL || !balon.estaControlableEnPiso()) {
            return;
        }

        if (balon.getRapidez() > VELOCIDAD_MAXIMA_PARA_CONTROL) {
            return;
        }

        Jugador mejorCandidato = null;
        double mejorPuntaje = Double.MAX_VALUE;
        for (Jugador jugador : getTodosJugadores()) {
            if (jugador == ultimoPateador && bloqueoRecapturaUltimoPateadorFrames > 0) {
                continue;
            }
            if (estaEnZonaDeControl(jugador) && jugador.getBounds().intersects(balon.getBounds())) {
                double distancia = distanciaAlBalon(jugador);
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

    private boolean ejecutarAccionBalonSiAplica(EntradaJuego entrada) {
        boolean pase = entrada.consumirPase();
        boolean tiro = entrada.consumirTiro();
        double factorPase = pase ? entrada.consumirFactorPase() : 0.0;
        double factorTiro = tiro ? entrada.consumirFactorTiro() : 0.0;
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
            ejecutarPase(entrada, factorPase);
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
        if (poseedorBalon == null || poseedorBalon == jugadorPrincipal || cooldownDecisionNpcFrames > 0) {
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
        double scoreTiro = calcularScoreTiroNpc(poseedor, distanciaPresion, enZonaDeTiro);
        double scorePase = calcularScorePaseParaTiro(poseedor, receptor, distanciaPresion);
        boolean paseParaTiroMejor = receptor != null && scorePase > scoreTiro + 18.0;

        double[] direccion;
        double fuerza;
        boolean esTiro;

        if (esPortero) {
            Jugador receptorLargo = seleccionarReceptorLargoPortero(poseedor);
            direccion = receptorLargo != null ? direccionHaciaJugador(poseedor, receptorLargo) : direccionAlArcoContrario(poseedor);
            fuerza = FUERZA_PASE_MAX;
            esTiro = false;
            registrarSonido(TipoSonido.SAQUE);
        } else if (paseParaTiroMejor && !bajoPresion) {
            direccion = direccionHaciaJugador(poseedor, receptor);
            fuerza = FUERZA_PASE_MAX * 0.9;
            esTiro = false;
            registrarSonido(TipoSonido.PASE);
        } else if (enZonaDeTiro || bajoPresion || distanciaArco < DISTANCIA_TIRO_CLARA) {
            direccion = direccionTiroNpc(poseedor);
            fuerza = FUERZA_TIRO_MAX * 0.98;
            esTiro = true;
            registrarSonido(TipoSonido.TIRO);
        } else if (receptor != null) {
            direccion = receptor != null ? direccionHaciaJugador(poseedor, receptor) : direccionAlArcoContrario(poseedor);
            fuerza = (FUERZA_PASE_MIN + FUERZA_PASE_MAX) / 2.0;
            esTiro = false;
            registrarSonido(TipoSonido.PASE);
        } else {
            return false;
        }

        double elevacion = esTiro ? 4.2 : (esPortero ? 3.1 : 1.4);
        balonEnManos = false;
        lanzarBalonDesdePoseedor(direccion, fuerza, elevacion);
        balonLibre = true;
        ultimoToqueLocal = esJugadorLocal(poseedor);
        poseedorBalon = null;
        cooldownRoboFrames = ConfiguracionJuego.FPS / 3;
        cooldownCapturaLibreFrames = esPortero ? 12 : 12;
        cooldownDecisionNpcFrames = COOLDOWN_DECISION_NPC;
        ultimoPateador = poseedor;
        bloqueoRecapturaUltimoPateadorFrames = esPortero ? 18 : 20;
        return true;
    }

    private double calcularScoreTiroNpc(Jugador tirador, double distanciaPresion, boolean enZonaDeTiro) {
        double distanciaArco = distanciaHorizontalAlArco(tirador);
        double score = enZonaDeTiro ? 34.0 : -16.0;
        score += Math.max(0.0, 290.0 - distanciaArco) * 0.24;
        score += Math.min(40.0, distanciaPresion) * 0.16;
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
        double score = 0.0;
        score += Math.max(0.0, distanciaArcoPasador - distanciaArcoReceptor) * 0.32;
        score += Math.min(80.0, separacionReceptor) * 0.22;
        score += Math.max(0.0, distanciaPresionPasador - 12.0) * 0.08;
        score -= Math.max(0.0, distanciaPase - DISTANCIA_PASE_NPC) * 0.22;
        if (estaEnZonaDeTiro(receptor)) {
            score += 20.0;
        }
        return score;
    }

    private boolean estanEnRangoDeRobo(Jugador atacante, Jugador poseedor) {
        Rectangle zonaRobo = new Rectangle(
            poseedor.getX(),
            poseedor.getY(),
            poseedor.getAncho(),
            poseedor.getAlto()
        );
        return atacante.getBounds().intersects(zonaRobo);
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
        double fuerza = interpolarFuerza(FUERZA_PASE_MIN, FUERZA_PASE_MAX, factorCarga);
        // El pase tiende a viajar mas raso para favorecer la recepcion.
        double elevacion = 1.0 + factorCarga * 1.6;
        lanzarBalonDesdePoseedor(direccion, fuerza, elevacion);
    }

    private double interpolarFuerza(double min, double max, double factorCarga) {
        double factor = Math.max(0.0, Math.min(1.0, factorCarga));
        return min + (max - min) * factor;
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
        double offsetVertical = jugador.getY() + jugador.getAlto() / 2.0 < centroPorteriaY ? 58.0 : -58.0;
        double objetivoY = centroPorteriaY + offsetVertical;
        if (aleatorio.nextDouble() < 0.22) {
            objetivoY = centroPorteriaY;
        }

        double dx = arcoX - (jugador.getX() + jugador.getAncho() / 2.0);
        double dy = objetivoY - (jugador.getY() + jugador.getAlto() / 2.0);
        double magnitud = Math.sqrt(dx * dx + dy * dy);
        if (magnitud < 0.0001) {
            return direccionAlArcoContrario(jugador);
        }
        return new double[] { dx / magnitud, dy / magnitud };
    }

    private Jugador seleccionarReceptorLocal(Jugador pasador) {
        Jugador[] opciones = new Jugador[] { jugadorPrincipal, aliadoLocal, porteroLocal };
        Jugador mejor = null;
        double mejorPuntaje = Double.MAX_VALUE;
        for (Jugador candidato : opciones) {
            if (candidato == pasador) {
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
        for (Jugador candidato : opciones) {
            if (candidato == pasador) {
                continue;
            }

            double dx = candidato.getX() - pasador.getX();
            double dy = candidato.getY() - pasador.getY();
            double avance = esJugadorLocal(pasador) ? -dx : dx;
            double penalizacionAtras = avance > 12 ? 220.0 : 0.0;
            double bonificacionProgresion = avance < -26 ? -90.0 : 0.0;
            double distancia = Math.sqrt(dx * dx + dy * dy);
            if (distancia > DISTANCIA_PASE_SEGURA) {
                continue;
            }

            Jugador marcador = rivalMasCercanoA(candidato, esJugadorLocal(candidato));
            double separacion = marcador == null ? DISTANCIA_PASE_SEGURA : distanciaEntre(candidato, marcador);
            double penalizacionMarca = separacion < DISTANCIA_PRESION_ALTA ? 240.0 : 0.0;
            double bonificacionZonaTiro = estaEnZonaDeTiro(candidato) ? -70.0 : 0.0;
            double puntaje = dx * dx + dy * dy + penalizacionAtras + penalizacionMarca + bonificacionProgresion + bonificacionZonaTiro;
            if (puntaje < mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejor = candidato;
            }
        }
        return mejor;
    }

    private Jugador seleccionarReceptorLargoPortero(Jugador portero) {
        Jugador[] opciones = esJugadorLocal(portero) ? getLocales() : getRivales();
        Jugador mejor = null;
        double mejorDistancia = -1.0;
        for (Jugador candidato : opciones) {
            if (candidato == portero || esPortero(candidato)) {
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

    private void pegarBalonAlPoseedor() {
        if (poseedorBalon == null) {
            return;
        }
        // Al recuperar la posesion, el balon se recoloca al costado del cuerpo
        // segun el equipo para que el ataque tenga una orientacion visual clara.
        int desplazamientoX = balonEnManos
            ? poseedorBalon.getAncho() / 2 - balon.getAncho() / 2
            : (poseedorEsLocal ? poseedorBalon.getAncho() - 6 : -balon.getAncho() + 6);
        int nuevaX = poseedorBalon.getX() + desplazamientoX;
        int nuevaY = balonEnManos
            ? poseedorBalon.getY() + Math.max(2, poseedorBalon.getAlto() / 5) - balon.getAlto() / 2
            : poseedorBalon.getY() + poseedorBalon.getAlto() / 2 - balon.getAlto() / 2;
        balon.setPosicion(nuevaX, nuevaY);
        balon.detener();
        limitarEntidadAlPanel(balon);
    }

    private void arrastrarBalonConPoseedor() {
        if (poseedorBalon == null) {
            return;
        }

        // La posesion no teletransporta el balon:
        // lo interpola hacia una posicion objetivo y le suma parte del movimiento del jugador.
        int desplazamientoX = balonEnManos
            ? poseedorBalon.getAncho() / 2 - balon.getAncho() / 2
            : (poseedorEsLocal ? poseedorBalon.getAncho() - 8 : -balon.getAncho() + 8);
        double objetivoX = poseedorBalon.getX() + desplazamientoX;
        double objetivoY = balonEnManos
            ? poseedorBalon.getY() + Math.max(2, poseedorBalon.getAlto() / 5) - balon.getAlto() / 2.0
            : poseedorBalon.getY() + poseedorBalon.getAlto() / 2.0 - balon.getAlto() / 2.0;

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
        int offset = extremo ? 176 : 110;
        int x = poseedor.getX() + (equipoLocal ? -offset : offset);
        int minimo = ConfiguracionJuego.CAMPO_X_MIN + 32;
        int maximo = ConfiguracionJuego.CAMPO_X_MAX - 80;
        return Math.max(minimo, Math.min(maximo, x));
    }

    private int calcularXApoyoLibre(boolean equipoLocal, boolean extremo) {
        int offset = extremo ? 124 : 72;
        int x = (int) balon.getCentroX() + (equipoLocal ? -offset : offset);
        int minimo = ConfiguracionJuego.CAMPO_X_MIN + 28;
        int maximo = ConfiguracionJuego.CAMPO_X_MAX - 76;
        return Math.max(minimo, Math.min(maximo, x));
    }

    private int calcularYApoyoLibre(boolean equipoLocal, boolean extremo) {
        int centro = (int) balon.getCentroY();
        int offset = extremo ? 118 : 74;
        int direccion = centro < ConfiguracionJuego.ALTO_PANEL / 2 ? 1 : -1;
        int y = centro + direccion * offset;
        if (!equipoLocal) {
            y = centro - direccion * (extremo ? 92 : 58);
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

    private Jugador perseguidorBalonLibre(boolean equipoLocal) {
        Jugador[] opciones = equipoLocal ? getLocales() : getRivales();
        Jugador mejor = null;
        double mejorPuntaje = Double.MAX_VALUE;
        for (Jugador jugador : opciones) {
            if (jugador == porteroLocal || jugador == porteroRival) {
                continue;
            }
            double puntaje = distanciaAlBalon(jugador) + penalizacionAgrupamiento(jugador) * 0.65;
            if (puntaje < mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejor = jugador;
            }
        }
        return mejor == null ? (equipoLocal ? jugadorPrincipal : rivalUno) : mejor;
    }

    private double penalizacionAgrupamiento(Jugador jugador) {
        double penalizacion = 0.0;
        for (Jugador otro : getTodosJugadores()) {
            if (otro == jugador) {
                continue;
            }
            double distancia = distanciaEntre(jugador, otro);
            if (distancia < 42.0) {
                penalizacion += 18.0;
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
        double centroX = balon.getCentroX();
        double centroY = balon.getCentroY();
        boolean enRangoArco = estaEntrePostes(centroY);

        // Un gol exige cruzar la linea entre postes y por debajo de cierta altura.
        // Si va demasiado alto, se trata como salida por encima del arco.
        if (centroX >= ConfiguracionJuego.CAMPO_X_MAX && enRangoArco) {
            if (intentarReboteTravesano(false)) {
                return EventoJuego.NINGUNO;
            }
            if (balon.getAltura() > ALTURA_MAXIMA_GOL) {
                resolverSalidaSobreArco(false, centroY);
                return EventoJuego.NINGUNO;
            }
            golesLocal++;
            registrarGol(ultimoPateador != null ? ultimoPateador : jugadorPrincipal, true);
            registrarSonido(TipoSonido.GOL);
            if (golesLocal >= ConfiguracionJuego.META_GOLES) {
                registrarSonido(TipoSonido.VICTORIA);
                return EventoJuego.VICTORIA;
            }
            reiniciarJugada(false);
            return EventoJuego.GOL_LOCAL;
        }

        if (centroX <= ConfiguracionJuego.CAMPO_X_MIN && enRangoArco) {
            if (intentarReboteTravesano(true)) {
                return EventoJuego.NINGUNO;
            }
            if (balon.getAltura() > ALTURA_MAXIMA_GOL) {
                resolverSalidaSobreArco(true, centroY);
                return EventoJuego.NINGUNO;
            }
            golesRival++;
            registrarGol(ultimoPateador != null ? ultimoPateador : rivalUno, false);
            registrarSonido(TipoSonido.GOL);
            if (golesRival >= ConfiguracionJuego.META_GOLES) {
                registrarSonido(TipoSonido.DERROTA);
                return EventoJuego.DERROTA;
            }
            reiniciarJugada(true);
            return EventoJuego.GOL_RIVAL;
        }

        if (balonLibre) {
            manejarBalonFueraDeCancha(centroX, centroY);
        }

        return EventoJuego.NINGUNO;
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
        mostrarTextoSaque("Travesaño");
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
        if (!ladoDerecho) {
            if (ultimoToqueLocal) {
                asignarSaqueEsquina(
                    false,
                    ConfiguracionJuego.CAMPO_X_MIN + 8,
                    centroY < ConfiguracionJuego.ALTO_PANEL / 2 ? ConfiguracionJuego.CAMPO_Y_MIN + 8 : ConfiguracionJuego.CAMPO_Y_MAX - 8,
                    "Tiro de esquina rival"
                );
            } else {
                asignarSaqueMeta(true, "Saque de meta local");
            }
            return;
        }

        if (!ultimoToqueLocal) {
            asignarSaqueEsquina(
                true,
                ConfiguracionJuego.CAMPO_X_MAX - 8,
                centroY < ConfiguracionJuego.ALTO_PANEL / 2 ? ConfiguracionJuego.CAMPO_Y_MIN + 8 : ConfiguracionJuego.CAMPO_Y_MAX - 8,
                "Tiro de esquina local"
            );
        } else {
            asignarSaqueMeta(false, "Saque de meta rival");
        }
    }

    private void manejarBalonFueraDeCancha(double centroX, double centroY) {
        boolean fueraSuperior = centroY < ConfiguracionJuego.CAMPO_Y_MIN;
        boolean fueraInferior = centroY > ConfiguracionJuego.CAMPO_Y_MAX;
        boolean fueraIzquierda = centroX < ConfiguracionJuego.CAMPO_X_MIN;
        boolean fueraDerecha = centroX > ConfiguracionJuego.CAMPO_X_MAX;
        boolean enRangoArco = estaEntrePostes(centroY);

        // Salida por banda.
        if (fueraSuperior || fueraInferior) {
            boolean saqueLocal = !ultimoToqueLocal;
            int ySaque = fueraSuperior ? ConfiguracionJuego.CAMPO_Y_MIN + 8 : ConfiguracionJuego.CAMPO_Y_MAX - 8;
            int xSaque = (int) Math.max(
                ConfiguracionJuego.CAMPO_X_MIN + 10,
                Math.min(ConfiguracionJuego.CAMPO_X_MAX - 10, centroX)
            );
            asignarSaqueBanda(saqueLocal, xSaque, ySaque, "Saque de banda " + (saqueLocal ? "local" : "rival"));
            return;
        }

        // Salida por fondo sin gol: meta o esquina.
        if (fueraIzquierda && !enRangoArco) {
            // Fondo izquierdo defendido por el local.
            if (ultimoToqueLocal) {
                // Ultimo toque local: esquina rival.
                asignarSaqueEsquina(
                    false,
                    ConfiguracionJuego.CAMPO_X_MIN + 8,
                    centroY < ConfiguracionJuego.ALTO_PANEL / 2 ? ConfiguracionJuego.CAMPO_Y_MIN + 8 : ConfiguracionJuego.CAMPO_Y_MAX - 8,
                    "Tiro de esquina rival"
                );
            } else {
                // Ultimo toque rival: saque de meta local.
                asignarSaqueMeta(true, "Saque de meta local");
            }
            return;
        }

        if (fueraDerecha && !enRangoArco) {
            // Fondo derecho defendido por el rival.
            if (!ultimoToqueLocal) {
                // Ultimo toque rival: esquina local.
                asignarSaqueEsquina(
                    true,
                    ConfiguracionJuego.CAMPO_X_MAX - 8,
                    centroY < ConfiguracionJuego.ALTO_PANEL / 2 ? ConfiguracionJuego.CAMPO_Y_MIN + 8 : ConfiguracionJuego.CAMPO_Y_MAX - 8,
                    "Tiro de esquina local"
                );
            } else {
                // Ultimo toque local: saque de meta rival.
                asignarSaqueMeta(false, "Saque de meta rival");
            }
        }
    }

    private void asignarSaqueBanda(boolean saqueLocal, int x, int y, String mensaje) {
        Jugador ejecutor = seleccionarCobradorCampo(saqueLocal, y);
        programarReanudacion(TipoReanudacion.BANDA, ejecutor, saqueLocal, x, y, mensaje);
    }

    private void asignarSaqueMeta(boolean saqueLocal, String mensaje) {
        Jugador ejecutor = saqueLocal ? porteroLocal : porteroRival;
        int xBalon = saqueLocal ? ConfiguracionJuego.CAMPO_X_MIN + 42 : ConfiguracionJuego.CAMPO_X_MAX - 42;
        int yBalon = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2;
        programarReanudacion(TipoReanudacion.META, ejecutor, saqueLocal, xBalon, yBalon, mensaje);
    }

    private void asignarSaqueEsquina(boolean saqueLocal, int x, double y, String mensaje) {
        Jugador ejecutor = seleccionarCobradorCampo(saqueLocal, y);
        programarReanudacion(TipoReanudacion.ESQUINA, ejecutor, saqueLocal, x, (int) y, mensaje);
    }

    private void mostrarTextoSaque(String texto) {
        textoSaque = texto;
        framesTextoSaque = ConfiguracionJuego.FPS * 2;
    }

    private void registrarGol(Jugador goleador, boolean equipoLocal) {
        ultimoGoleador = goleador == null ? "Sin identificar" : goleador.getNombre();
        ultimoEquipoGoleador = equipoLocal ? "Local" : "Rival";
    }

    private Jugador seleccionarCobradorCampo(boolean equipoLocal, double y) {
        if (equipoLocal) {
            return y < ConfiguracionJuego.ALTO_PANEL / 2 ? jugadorPrincipal : aliadoLocal;
        }
        return y < ConfiguracionJuego.ALTO_PANEL / 2 ? rivalUno : rivalDos;
    }

    private void tomarPosesion(Jugador jugador, boolean equipoLocal) {
        // Tomar posesion congela la fisica libre, asigna equipo con ultimo toque
        // y da un breve margen para evitar robos/decisiones en el mismo frame.
        poseedorBalon = jugador;
        poseedorEsLocal = equipoLocal;
        ultimoToqueLocal = equipoLocal;
        balonLibre = false;
        cooldownRoboFrames = (int) (ConfiguracionJuego.FPS * 0.7);
        cooldownCapturaLibreFrames = 0;
        cooldownDecisionNpcFrames = COOLDOWN_DECISION_NPC / 2;
        ultimoPateador = null;
        bloqueoRecapturaUltimoPateadorFrames = 0;
        balonEnManos = false;
        pegarBalonAlPoseedor();
    }

    private void tomarPosesionEnPunto(Jugador jugador, boolean equipoLocal, int balonX, int balonY) {
        ubicarEjecutorDetrasDelBalonInmediato(jugador, equipoLocal, balonX, balonY);
        tomarPosesion(jugador, equipoLocal);
        balon.setPosicion(balonX - balon.getAncho() / 2.0, balonY - balon.getAlto() / 2.0);
        balon.detener();
        cooldownRoboFrames = ConfiguracionJuego.FPS;
        cooldownCapturaLibreFrames = 10;
        cooldownDecisionNpcFrames = COOLDOWN_DECISION_NPC;
    }

    private void reiniciarJugada(boolean saqueLocal) {
        // Reinicio sin teletransportar: el cobrador va hacia el centro con el balon.
        Jugador cobrador = saqueLocal ? jugadorPrincipal : rivalUno;
        int centroX = ConfiguracionJuego.ANCHO_PANEL / 2;
        int centroY = ConfiguracionJuego.ALTO_PANEL / 2;
        programarReanudacion(
            TipoReanudacion.INICIAL,
            cobrador,
            saqueLocal,
            centroX,
            centroY,
            saqueLocal ? "Saque inicial local" : "Saque inicial rival"
        );
    }

    private void programarReanudacion(TipoReanudacion tipo, Jugador ejecutor, boolean saqueLocal, int x, int y, String mensaje) {
        tipoReanudacionPendiente = tipo;
        ejecutorReanudacion = ejecutor;
        saquePendienteLocal = saqueLocal;
        saquePendienteX = x;
        saquePendienteY = y;
        framesRetrasoSaque = RETRASO_SAQUE_FRAMES;
        if (ejecutor != null) {
            tomarPosesion(ejecutor, saqueLocal);
            balonEnManos = true;
            cooldownRoboFrames = ConfiguracionJuego.FPS;
            cooldownDecisionNpcFrames = COOLDOWN_DECISION_NPC;
        } else {
            balon.setPosicion(x - balon.getAncho() / 2.0, y - balon.getAlto() / 2.0);
            balon.detener();
            balonLibre = true;
            poseedorBalon = null;
            balonEnManos = false;
        }
        mostrarTextoSaque(mensaje);
    }

    private void ejecutarReanudacionPendiente() {
        if (tipoReanudacionPendiente == TipoReanudacion.NINGUNA) {
            return;
        }

        if (poseedorBalon == null && ejecutorReanudacion != null) {
            tomarPosesion(ejecutorReanudacion, saquePendienteLocal);
        }
        balonEnManos = poseedorBalon != null && esPortero(poseedorBalon);
        cooldownDecisionNpcFrames = COOLDOWN_DECISION_NPC / 2;
        registrarSonido(TipoSonido.SAQUE);
        tipoReanudacionPendiente = TipoReanudacion.NINGUNA;
        ejecutorReanudacion = null;
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
        if (cooldownAtajadaPorteroFrames > 0) {
            cooldownAtajadaPorteroFrames--;
        }
        if (cooldownLecturaPorteroLocalFrames > 0) {
            cooldownLecturaPorteroLocalFrames--;
        }
        if (cooldownLecturaPorteroRivalFrames > 0) {
            cooldownLecturaPorteroRivalFrames--;
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
    }

    private enum TipoReanudacion {
        NINGUNA,
        BANDA,
        META,
        ESQUINA,
        INICIAL
    }

    private void prepararFormacionSaqueBanda(boolean saqueLocal, Jugador ejecutor, int x, int y) {
        Jugador[] atacantes = saqueLocal ? getLocales() : getRivales();
        Jugador[] defensores = saqueLocal ? getRivales() : getLocales();
        int interiorY = y <= ConfiguracionJuego.ALTO_PANEL / 2 ? y + 92 : y - 92;
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
        int salidaX = saqueLocal ? ConfiguracionJuego.CAMPO_X_MIN + 186 : ConfiguracionJuego.CAMPO_X_MAX - 186;
        int salidaXLejana = saqueLocal ? ConfiguracionJuego.CAMPO_X_MIN + 304 : ConfiguracionJuego.CAMPO_X_MAX - 304;

        for (Jugador atacante : atacantes) {
            if (atacante == (saqueLocal ? porteroLocal : porteroRival)) {
                continue;
            }
            int objetivoX = atacante == atacantes[1] ? salidaX : salidaXLejana;
            int objetivoY = atacante == atacantes[1]
                ? ConfiguracionJuego.Y_PORTERIA + 28
                : ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA - 28;
            colocarJugador(atacante, objetivoX, objetivoY);
        }

        int presionX = saqueLocal ? ConfiguracionJuego.CAMPO_X_MIN + 340 : ConfiguracionJuego.CAMPO_X_MAX - 340;
        int centroY = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2;
        colocarDefensasReanudacion(defensores, presionX, centroY);
    }

    private void prepararFormacionSaqueEsquina(boolean saqueLocal, Jugador ejecutor, int x, int y) {
        Jugador[] atacantes = saqueLocal ? getLocales() : getRivales();
        Jugador[] defensores = saqueLocal ? getRivales() : getLocales();
        int arcoCentroY = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2;
        int primerPaloX = saqueLocal ? ConfiguracionJuego.CAMPO_X_MAX - 58 : ConfiguracionJuego.CAMPO_X_MIN + 58;
        int segundoPaloX = saqueLocal ? ConfiguracionJuego.CAMPO_X_MAX - 132 : ConfiguracionJuego.CAMPO_X_MIN + 132;

        for (Jugador atacante : atacantes) {
            if (atacante == ejecutor) {
                continue;
            }
            int objetivoX = atacante == atacantes[0] ? segundoPaloX : primerPaloX;
            int objetivoY = atacante == atacantes[0] ? arcoCentroY + 42 : arcoCentroY - 22;
            colocarJugador(atacante, objetivoX, objetivoY);
        }

        int marcaX = saqueLocal ? ConfiguracionJuego.CAMPO_X_MAX - 96 : ConfiguracionJuego.CAMPO_X_MIN + 96;
        colocarDefensasReanudacion(defensores, marcaX, arcoCentroY);
        int yAjustada = y <= ConfiguracionJuego.ALTO_PANEL / 2 ? ConfiguracionJuego.CAMPO_Y_MIN + 8 : ConfiguracionJuego.CAMPO_Y_MAX - 8;
        ubicarEjecutorDetrasDelBalon(ejecutor, saqueLocal, x, yAjustada);
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
        jugador.setX((int) Math.round(balonX - offsetX));
        jugador.setY(balonY - jugador.getAlto() / 2);
        limitarEntidadAlPanel(jugador);
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
        if (jugador == aliadoLocal || jugador == rivalDos) {
            objetivo += jugadorLocal ? -36 : 36;
        } else {
            objetivo += jugadorLocal ? 36 : -36;
        }
        return Math.max(ConfiguracionJuego.CAMPO_X_MIN + 24, Math.min(ConfiguracionJuego.CAMPO_X_MAX - 24, objetivo));
    }

    private int calcularYDesmarqueNpc(Jugador jugador) {
        if (jugador == aliadoLocal || jugador == rivalDos) {
            return Y_APERTURA_INFERIOR - jugador.getAlto() / 2;
        }
        return Y_APERTURA_SUPERIOR - jugador.getAlto() / 2;
    }

    private void colocarJugador(Jugador jugador, int centroX, int centroY) {
        jugador.setX(centroX - jugador.getAncho() / 2);
        jugador.setY(centroY - jugador.getAlto() / 2);
        limitarEntidadAlPanel(jugador);
    }

    private void registrarSonido(TipoSonido tipoSonido) {
        sonidosPendientes.offer(tipoSonido);
    }

    public TipoSonido consumirSonidoPendiente() {
        return sonidosPendientes.poll();
    }

    private void actualizarMonedaEspecial() {
        // Gestion de la moneda especial.
        contadorAparicionMonedaEspecial++;

        if (!monedaEspecial.estaActiva()) {
            if (contadorAparicionMonedaEspecial >= ConfiguracionJuego.INTERVALO_MONEDA_ESPECIAL) {
                recolocarEntidadBonus(monedaEspecial);
                monedaEspecial.activar(monedaEspecial.getX(), monedaEspecial.getY(), ConfiguracionJuego.DURACION_MONEDA_ESPECIAL);
                contadorAparicionMonedaEspecial = 0;
            }
            return;
        }

        if (jugadorPrincipal.getBounds().intersects(monedaEspecial.getBounds())) {
            puntosBonus += monedaEspecial.getPuntos();
            monedaEspecial.desactivar();
            contadorAparicionMonedaEspecial = 0;
        } else {
            monedaEspecial.actualizar();
        }
    }

    private void actualizarTurbo() {
        // Gestion del turbo del jugador principal.
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

        if (jugadorPrincipal.getBounds().intersects(turbo.getBounds())) {
            turbo.aplicarEfecto(jugadorPrincipal);
            turbo.desactivar();
            contadorAparicionTurbo = 0;
            framesTurboRestantesEnEscenario = 0;
        }
    }

    private void recolocarEntidadBonus(EntidadJuego entidad) {
        // Busca una posicion valida para bonus con intentos limitados.
        for (int intentos = 0; intentos < 100; intentos++) {
            int maxX = ConfiguracionJuego.ANCHO_PANEL - entidad.getAncho();
            int maxY = ConfiguracionJuego.ALTO_PANEL - entidad.getAlto();
            entidad.setX(aleatorio.nextInt(Math.max(1, maxX + 1)));
            entidad.setY(aleatorio.nextInt(Math.max(1, maxY + 1)));

            if (posicionValidaParaBonus(entidad)) {
                return;
            }
        }
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
        int xMin = local ? 6 : ConfiguracionJuego.ANCHO_PANEL - 86;
        int xMax = local ? 58 : ConfiguracionJuego.ANCHO_PANEL - 20;
        int yMin = ConfiguracionJuego.Y_PORTERIA - 12;
        int yMax = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA - portero.getAlto() + 12;

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

        if (balonAmenazaArco(local)) {
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
        if (!balonEnZonaPortero(local, balon.getCentroX(), balon.getCentroY())) {
            return false;
        }

        boolean amenazaReal = balonAmenazaArco(local);
        double distanciaActual = distanciaAlBalon(portero);
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

        if (mejorDistancia <= rangoAtajada
            && mejorAltura <= ALTURA_MAXIMA_ATAJADA
            && (amenazaReal || velocidadBalon > 1.45)) {
            tomarPosesion(portero, local);
            balonEnManos = true;
            cooldownAtajadaPorteroFrames = ConfiguracionJuego.FPS / 2;
            registrarSonido(TipoSonido.ROBO);
            mostrarTextoSaque("Atajada de " + portero.getNombre());
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
            mostrarTextoSaque("Desvio de " + portero.getNombre());
            return true;
        }

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
        porteroRival.actualizarEstado(sprintPorteroRival, Math.abs(movPorteroRivalX) + Math.abs(movPorteroRivalY));
        rivalUno.actualizarEstado(sprintRivalUno, Math.abs(movRivalUnoX) + Math.abs(movRivalUnoY));
        rivalDos.actualizarEstado(sprintRivalDos, Math.abs(movRivalDosX) + Math.abs(movRivalDosY));

        // La animacion depende del ultimo desplazamiento de cada jugador.
        porteroLocal.actualizarAnimacion(movPorteroLocalX, movPorteroLocalY);
        jugadorPrincipal.actualizarAnimacion(movPrincipalX, movPrincipalY);
        aliadoLocal.actualizarAnimacion(movAliadoX, movAliadoY);
        porteroRival.actualizarAnimacion(movPorteroRivalX, movPorteroRivalY);
        rivalUno.actualizarAnimacion(movRivalUnoX, movRivalUnoY);
        rivalDos.actualizarAnimacion(movRivalDosX, movRivalDosY);
    }

    private int velocidadMovimiento(Jugador jugador) {
        // Velocidad estimada del frame usada en robos y disputas.
        if (jugador == jugadorPrincipal) {
            return Math.abs(movPrincipalX) + Math.abs(movPrincipalY);
        }
        if (jugador == aliadoLocal) {
            return Math.abs(movAliadoX) + Math.abs(movAliadoY);
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
        if (jugador == porteroLocal) {
            return movPorteroLocalX;
        }
        if (jugador == rivalUno) {
            return movRivalUnoX;
        }
        if (jugador == rivalDos) {
            return movRivalDosX;
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
        if (jugador == porteroLocal) {
            return movPorteroLocalY;
        }
        if (jugador == rivalUno) {
            return movRivalUnoY;
        }
        if (jugador == rivalDos) {
            return movRivalDosY;
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
        return jugador == jugadorPrincipal || jugador == aliadoLocal || jugador == porteroLocal;
    }

    private boolean esPortero(Jugador jugador) {
        return jugador == porteroLocal || jugador == porteroRival;
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
        int distanciaManhattan = Math.abs(objetivoX - jugador.getX()) + Math.abs(objetivoY - jugador.getY());
        boolean disputa = poseedorBalon == jugador || distanciaAlBalon(jugador) < 120.0 || balonLibre;
        boolean oportunidadAtaque = poseedorBalon == jugador && distanciaHorizontalAlArco(jugador) < 360.0;
        boolean retornoDefensivo = poseedorBalon != null && esJugadorLocal(poseedorBalon) != esJugadorLocal(jugador);
        if (jugador.getStamina() < 18.0) {
            return false;
        }
        return distanciaManhattan > 110 && (disputa || oportunidadAtaque || retornoDefensivo);
    }

    private boolean debeSprintarPortero(Jugador portero, boolean local, int objetivoX, int objetivoY) {
        if (!portero.puedeSprintar()) {
            return false;
        }
        int distanciaManhattan = Math.abs(objetivoX - portero.getX()) + Math.abs(objetivoY - portero.getY());
        boolean amenaza = balonAmenazaArco(local)
            || (!balonLibre && poseedorBalon != null && esJugadorLocal(poseedorBalon) != local && amenazaDeDisparoDelPoseedor(local));
        if (portero.getStamina() < 14.0) {
            return false;
        }
        return amenaza && distanciaManhattan > 36;
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
        return new Jugador[] { porteroLocal, jugadorPrincipal, aliadoLocal };
    }

    public Jugador[] getRivales() {
        return new Jugador[] { porteroRival, rivalUno, rivalDos };
    }

    public Jugador[] getTodosJugadores() {
        return new Jugador[] {
            porteroLocal, jugadorPrincipal, aliadoLocal,
            porteroRival, rivalUno, rivalDos
        };
    }

    public Jugador getJugadorPrincipal() {
        return jugadorPrincipal;
    }

    public Balon getBalon() {
        return balon;
    }

    public MonedaEspecial getMonedaEspecial() {
        return monedaEspecial;
    }

    public Turbo getTurbo() {
        return turbo;
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

    public int getStaminaPrincipalPorcentaje() {
        return (int) Math.round((jugadorPrincipal.getStamina() / jugadorPrincipal.getStaminaMax()) * 100.0);
    }

    public String getPoseedorTexto() {
        if (balonLibre || poseedorBalon == null) {
            return "Libre";
        }
        return poseedorEsLocal ? "Local" : "Rival";
    }

    public int getFramesAnimacion() {
        return framesAnimacion;
    }

    public String getTextoSaque() {
        return textoSaque;
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
