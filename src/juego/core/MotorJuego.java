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
    private static final double FUERZA_TIRO_MIN = 4.3;
    private static final double FUERZA_TIRO_MAX = 6.4;
    private static final double VELOCIDAD_MAXIMA_PARA_CONTROL = 2.25;
    private static final double DISTANCIA_MAXIMA_CONTROL = 36.0;
    private static final double DISTANCIA_MAXIMA_POSESION = 68.0;
    private static final double ALTURA_MAXIMA_CONTROL = 8.0;
    private static final double ALTURA_MAXIMA_GOL = 26.0;
    private static final double ALTURA_MINIMA_TRAVESANO = 26.0;
    private static final double ALTURA_MAXIMA_TRAVESANO = 38.0;
    private static final double ALTURA_MAXIMA_ATAJADA = 32.0;
    private static final double DISTANCIA_ATAJADA_PORTERO = 60.0;
    private static final double DISTANCIA_DESVIO_PORTERO = 86.0;
    private static final int LOOKAHEAD_ATAJADA_FRAMES = 14;
    private static final double GRAVEDAD_BALON = 0.36;
    private static final int MARGEN_REANUDACION = 72;
    private static final double ARRASTRE_BALON = 0.22;
    private static final double APORTE_MOVIMIENTO_POSEEDOR = 0.14;
    private static final double IMPULSO_BASE_MOVIMIENTO = 0.34;
    private static final int COOLDOWN_DECISION_NPC = 14;
    private static final double DISTANCIA_PRESION_ALTA = 86.0;
    private static final double DISTANCIA_PASE_SEGURA = 250.0;
    private static final double DISTANCIA_PASE_NPC = 170.0;
    private static final double DISTANCIA_TIRO_CLARA = 150.0;

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
    private Jugador ultimoPateador;
    private int bloqueoRecapturaUltimoPateadorFrames;
    private int framesAnimacion;
    private boolean ultimoToqueLocal;
    private String textoSaque;
    private int framesTextoSaque;
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
        return EventoJuego.NINGUNO;
    }

    private void moverPrincipal(EntradaJuego entrada) {
        // El jugador humano es el unico cuya direccion sale directo del teclado.
        movPrincipalX = entrada.calcularDeltaX(jugadorPrincipal.getVelocidad());
        movPrincipalY = entrada.calcularDeltaY(jugadorPrincipal.getVelocidad());
        jugadorPrincipal.mover(movPrincipalX, movPrincipalY);
        limitarEntidadAlPanel(jugadorPrincipal);
    }

    private void moverAliadoLocal() {
        // El aliado alterna entre apoyo interior, desmarque y repliegue.
        int objetivoX;
        int objetivoY;
        if (!balonLibre && poseedorBalon == aliadoLocal) {
            objetivoX = ConfiguracionJuego.CAMPO_X_MAX - 170;
            objetivoY = calcularCarrilAtaqueY(aliadoLocal, true);
        } else if (!balonLibre && poseedorEsLocal) {
            objetivoX = calcularXApoyoOfensivo(poseedorBalon, true, false);
            objetivoY = calcularCarrilApoyo(poseedorBalon, true, false);
        } else if (!balonLibre) {
            // En defensa intenta cerrar linea de pase y proteger el carril central.
            objetivoX = (poseedorBalon.getX() + jugadorPrincipal.getX()) / 2 - 28;
            objetivoY = calcularYBloqueDefensivo(poseedorBalon, true, false);
        } else {
            objetivoX = (int) balon.getCentroX() - aliadoLocal.getAncho() / 2;
            objetivoY = (int) balon.getCentroY() - aliadoLocal.getAlto() / 2;
        }

        movAliadoX = calcularPaso(aliadoLocal.getX(), objetivoX, aliadoLocal.getVelocidad());
        movAliadoY = calcularPaso(aliadoLocal.getY(), objetivoY, aliadoLocal.getVelocidad());
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

        if (!balonLibre && poseedorEsLocal) {
            objetivoRivalUnoX = poseedorBalon.getX() - 14;
            objetivoRivalUnoY = poseedorBalon.getY();
            objetivoRivalDosX = calcularXCobertura(poseedorBalon, false, true);
            objetivoRivalDosY = calcularYBloqueDefensivo(poseedorBalon, false, true);
        } else if (balonLibre) {
            objetivoRivalUnoX = balon.getX() - 8;
            objetivoRivalUnoY = balon.getY();
            objetivoRivalDosX = (int) balon.getCentroX() + 52;
            objetivoRivalDosY = (int) balon.getCentroY() + (balon.getCentroY() < ConfiguracionJuego.ALTO_PANEL / 2 ? 56 : -56);
        } else if (poseedorBalon == rivalUno) {
            objetivoRivalUnoX = ConfiguracionJuego.CAMPO_X_MIN + 132;
            objetivoRivalUnoY = calcularCarrilAtaqueY(rivalUno, false);
            objetivoRivalDosX = calcularXApoyoOfensivo(rivalUno, false, true);
            objetivoRivalDosY = calcularCarrilApoyo(rivalUno, false, true);
        } else if (poseedorBalon == rivalDos) {
            objetivoRivalDosX = ConfiguracionJuego.CAMPO_X_MIN + 132;
            objetivoRivalDosY = calcularCarrilAtaqueY(rivalDos, false);
            objetivoRivalUnoX = calcularXApoyoOfensivo(rivalDos, false, false);
            objetivoRivalUnoY = calcularCarrilApoyo(rivalDos, false, false);
        } else {
            objetivoRivalUnoX = calcularXApoyoOfensivo(poseedorBalon, false, false);
            objetivoRivalUnoY = calcularCarrilApoyo(poseedorBalon, false, false);
            objetivoRivalDosX = calcularXApoyoOfensivo(poseedorBalon, false, true);
            objetivoRivalDosY = calcularCarrilApoyo(poseedorBalon, false, true);
        }

        movRivalUnoX = calcularPaso(rivalUno.getX(), objetivoRivalUnoX, rivalUno.getVelocidad());
        movRivalUnoY = calcularPaso(rivalUno.getY(), objetivoRivalUnoY, rivalUno.getVelocidad());
        rivalUno.mover(movRivalUnoX, movRivalUnoY);
        limitarEntidadAlPanel(rivalUno);

        movRivalDosX = calcularPaso(rivalDos.getX(), objetivoRivalDosX, rivalDos.getVelocidad());
        movRivalDosY = calcularPaso(rivalDos.getY(), objetivoRivalDosY, rivalDos.getVelocidad());
        rivalDos.mover(movRivalDosX, movRivalDosY);
        limitarEntidadAlPanel(rivalDos);
    }

    private void moverPorteros() {
        // El portero ya no sigue solo la Y de la jugada:
        // intenta cerrar el angulo al arco y se adelanta si detecta una amenaza real.
        int xObjetivoLocal = calcularXObjetivoPortero(porteroLocal, true);
        int yObjetivoLocal = calcularYObjetivoPortero(porteroLocal, true);
        int velocidadLocal = calcularVelocidadPortero(porteroLocal, true);
        movPorteroLocalX = calcularPaso(porteroLocal.getX(), xObjetivoLocal, velocidadLocal);
        movPorteroLocalY = calcularPaso(porteroLocal.getY(), yObjetivoLocal, velocidadLocal);
        porteroLocal.mover(movPorteroLocalX, movPorteroLocalY);

        int xObjetivoRival = calcularXObjetivoPortero(porteroRival, false);
        int yObjetivoRival = calcularYObjetivoPortero(porteroRival, false);
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

        // El robo no es instantaneo: exige cercania, compara inercia/velocidad
        // y suma una pequena variacion aleatoria para que las disputas no sean identicas.
        Jugador[] candidatos = poseedorEsLocal ? getRivales() : getLocales();
        for (Jugador candidato : candidatos) {
            if (!estanEnRangoDeRobo(candidato, poseedorBalon)) {
                continue;
            }

            int fuerzaAtacante = velocidadMovimiento(candidato) + candidato.getVelocidad();
            int fuerzaPoseedor = velocidadMovimiento(poseedorBalon) + poseedorBalon.getVelocidad();
            int tirada = aleatorio.nextInt(4);

            if (fuerzaAtacante + tirada >= fuerzaPoseedor + 1) {
                poseedorBalon = candidato;
                poseedorEsLocal = esJugadorLocal(candidato);
                ultimoToqueLocal = poseedorEsLocal;
                cooldownRoboFrames = ConfiguracionJuego.FPS / 2;
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
        double mejorDistancia = Double.MAX_VALUE;
        for (Jugador jugador : getTodosJugadores()) {
            if (jugador == ultimoPateador && bloqueoRecapturaUltimoPateadorFrames > 0) {
                continue;
            }
            if (estaEnZonaDeControl(jugador) && jugador.getBounds().intersects(balon.getBounds())) {
                double distancia = distanciaAlBalon(jugador);
                if (distancia < mejorDistancia) {
                    mejorDistancia = distancia;
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
        cooldownRoboFrames = ConfiguracionJuego.FPS / 5;
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

        // IA simple basada en contexto:
        // si el NPC esta en zona de tiro remata,
        // si es portero o esta presionado busca pase/despeje,
        // si no, sigue conduciendo.
        Jugador poseedor = poseedorBalon;
        Jugador presionCercana = rivalMasCercanoA(poseedor, esJugadorLocal(poseedor));
        Jugador receptor = seleccionarReceptorNpc(poseedor);
        double distanciaPresion = presionCercana == null ? Double.MAX_VALUE : distanciaEntre(poseedor, presionCercana);
        double distanciaArco = distanciaHorizontalAlArco(poseedor);
        boolean enZonaDeTiro = estaEnZonaDeTiro(poseedor);
        boolean bajoPresion = distanciaPresion < DISTANCIA_PRESION_ALTA;
        boolean esPortero = poseedor == porteroLocal || poseedor == porteroRival;
        boolean paseVentajoso = receptor != null && distanciaEntre(poseedor, receptor) < DISTANCIA_PASE_NPC;
        boolean receptorMasLibre = receptor != null
            && distanciaHorizontalAlArco(receptor) + 30.0 < distanciaArco
            && distanciaRivalMasCercano(receptor, esJugadorLocal(receptor)) > distanciaPresion + 14.0;

        double[] direccion;
        double fuerza;

        if (enZonaDeTiro && (!paseVentajoso || distanciaArco < DISTANCIA_TIRO_CLARA || bajoPresion)) {
            direccion = direccionAlArcoContrario(poseedor);
            fuerza = FUERZA_TIRO_MAX * 0.94;
            registrarSonido(TipoSonido.TIRO);
        } else if (esPortero || bajoPresion || receptorMasLibre || paseVentajoso) {
            direccion = receptor != null ? direccionHaciaJugador(poseedor, receptor) : direccionAlArcoContrario(poseedor);
            fuerza = esPortero
                ? FUERZA_PASE_MAX
                : (receptorMasLibre ? FUERZA_PASE_MAX * 0.88 : (FUERZA_PASE_MIN + FUERZA_PASE_MAX) / 2.0);
            registrarSonido(esPortero ? TipoSonido.SAQUE : TipoSonido.PASE);
        } else {
            return false;
        }

        double elevacion = enZonaDeTiro ? 4.8 : (esPortero ? 2.2 : 1.6);
        lanzarBalonDesdePoseedor(direccion, fuerza, elevacion);
        balonLibre = true;
        ultimoToqueLocal = esJugadorLocal(poseedor);
        poseedorBalon = null;
        cooldownRoboFrames = ConfiguracionJuego.FPS / 4;
        cooldownCapturaLibreFrames = esPortero ? 12 : 9;
        cooldownDecisionNpcFrames = COOLDOWN_DECISION_NPC;
        ultimoPateador = poseedor;
        bloqueoRecapturaUltimoPateadorFrames = esPortero ? 18 : 12;
        return true;
    }

    private boolean estanEnRangoDeRobo(Jugador atacante, Jugador poseedor) {
        Rectangle zonaRobo = new Rectangle(
            poseedor.getX() - 4,
            poseedor.getY() - 4,
            poseedor.getAncho() + 8,
            poseedor.getAlto() + 8
        );
        return atacante.getBounds().intersects(zonaRobo);
    }

    private void ejecutarTiro(EntradaJuego entrada, double factorCarga) {
        // El tiro siempre prioriza el arco rival y usa el input como ajuste fino.
        double[] direccion = obtenerDireccionTiro(entrada, poseedorBalon);
        double fuerza = interpolarFuerza(FUERZA_TIRO_MIN, FUERZA_TIRO_MAX, factorCarga);
        // La carga tambien levanta el balon: un tiro fuerte sale mas alto.
        double elevacion = 2.8 + factorCarga * 3.8;
        lanzarBalonDesdePoseedor(direccion, fuerza, elevacion);
    }

    private void ejecutarPase(EntradaJuego entrada, double factorCarga) {
        // El pase usa la misma mecanica de carga, con elevacion mas baja.
        double[] direccion = obtenerDireccionNormalizada(entrada);
        double fuerza = interpolarFuerza(FUERZA_PASE_MIN, FUERZA_PASE_MAX, factorCarga);
        // El pase tiende a viajar mas raso para favorecer la recepcion.
        double elevacion = 1.0 + factorCarga * 1.6;
        lanzarBalonDesdePoseedor(direccion, fuerza, elevacion);
    }

    private double interpolarFuerza(double min, double max, double factorCarga) {
        double factor = Math.max(0.0, Math.min(1.0, factorCarga));
        return min + (max - min) * factor;
    }

    private double[] obtenerDireccionNormalizada(EntradaJuego entrada) {
        double dx = entrada.getDireccionAccionX();
        double dy = entrada.getDireccionAccionY();
        double magnitud = Math.sqrt(dx * dx + dy * dy);
        if (magnitud < 0.0001) {
            // Sin direccion valida, el juego asume avance frontal.
            return new double[] { 1.0, 0.0 };
        }
        return new double[] { dx / magnitud, dy / magnitud };
    }

    private double[] obtenerDireccionTiro(EntradaJuego entrada, Jugador tirador) {
        if (tirador == null) {
            return obtenerDireccionNormalizada(entrada);
        }

        // El tiro no apunta al cursor: construye un objetivo en la porteria rival
        // y permite un pequeno sesgo horizontal/vertical con el input actual.
        double arcoX = esJugadorLocal(tirador) ? ConfiguracionJuego.CAMPO_X_MAX + 8.0 : ConfiguracionJuego.CAMPO_X_MIN - 8.0;
        double centroPorteriaY = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2.0;
        double sesgoVertical = entrada.getDireccionAccionY() * 72.0;
        double sesgoHorizontal = entrada.getDireccionAccionX() * 18.0;
        double objetivoX = arcoX + sesgoHorizontal;
        double objetivoY = centroPorteriaY + sesgoVertical;

        double origenX = tirador.getX() + tirador.getAncho() / 2.0;
        double origenY = tirador.getY() + tirador.getAlto() / 2.0;
        double dx = objetivoX - origenX;
        double dy = objetivoY - origenY;
        double magnitud = Math.sqrt(dx * dx + dy * dy);
        if (magnitud < 0.0001) {
            return new double[] { esJugadorLocal(tirador) ? 1.0 : -1.0, 0.0 };
        }
        return new double[] { dx / magnitud, dy / magnitud };
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

    private void pegarBalonAlPoseedor() {
        if (poseedorBalon == null) {
            return;
        }
        // Al recuperar la posesion, el balon se recoloca al costado del cuerpo
        // segun el equipo para que el ataque tenga una orientacion visual clara.
        int desplazamientoX = poseedorEsLocal ? poseedorBalon.getAncho() - 6 : -balon.getAncho() + 6;
        int nuevaX = poseedorBalon.getX() + desplazamientoX;
        int nuevaY = poseedorBalon.getY() + poseedorBalon.getAlto() / 2 - balon.getAlto() / 2;
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
        int desplazamientoX = poseedorEsLocal ? poseedorBalon.getAncho() - 10 : -balon.getAncho() + 10;
        double objetivoX = poseedorBalon.getX() + desplazamientoX;
        double objetivoY = poseedorBalon.getY() + poseedorBalon.getAlto() / 2.0 - balon.getAlto() / 2.0;

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
            cooldownCapturaLibreFrames = 4;
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
        double nuevaX = centroPoseedorX + direccion[0] * separacion - balon.getAncho() / 2.0;
        double nuevaY = centroPoseedorY + direccion[1] * separacion - balon.getAlto() / 2.0;

        balon.setPosicion(nuevaX, nuevaY);
        double impulsoX = direccion[0] * fuerza + movimientoXDe(poseedorBalon) * IMPULSO_BASE_MOVIMIENTO;
        double impulsoY = direccion[1] * fuerza + movimientoYDe(poseedorBalon) * IMPULSO_BASE_MOVIMIENTO;
        balon.detener();
        balon.impulsar(impulsoX, impulsoY, elevacion);
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
        return distanciaHorizontalAlArco < 250.0 && Math.abs(centroY - centroPorteriaY) < 132.0;
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
        int offset = extremo ? 96 : 58;
        int carril = poseedor.getY() + signo * offset;
        int minimo = ConfiguracionJuego.CAMPO_Y_MIN + 26;
        int maximo = ConfiguracionJuego.CAMPO_Y_MAX - 56;
        return Math.max(minimo, Math.min(maximo, carril));
    }

    private int calcularYBloqueDefensivo(Jugador poseedor, boolean equipoLocal, boolean extremo) {
        int centroPorteria = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2;
        int referencia = (poseedor.getY() + centroPorteria) / 2;
        int ajuste = extremo ? 52 : -36;
        int y = referencia + (poseedor.getY() < centroPorteria ? ajuste : -ajuste);
        int minimo = ConfiguracionJuego.CAMPO_Y_MIN + 24;
        int maximo = ConfiguracionJuego.CAMPO_Y_MAX - 54;
        return Math.max(minimo, Math.min(maximo, y));
    }

    private int calcularXApoyoOfensivo(Jugador poseedor, boolean equipoLocal, boolean extremo) {
        int offset = extremo ? 116 : 74;
        int x = poseedor.getX() + (equipoLocal ? -offset : offset);
        int minimo = ConfiguracionJuego.CAMPO_X_MIN + 32;
        int maximo = ConfiguracionJuego.CAMPO_X_MAX - 80;
        return Math.max(minimo, Math.min(maximo, x));
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
        boolean enRangoArco = centroY >= ConfiguracionJuego.Y_PORTERIA
            && centroY <= ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA;

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
            mostrarTextoSaque("Saque inicial rival");
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
            mostrarTextoSaque("Saque inicial local");
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
                    centroY < ConfiguracionJuego.ALTO_PANEL / 2 ? ConfiguracionJuego.CAMPO_Y_MIN + 8 : ConfiguracionJuego.CAMPO_Y_MAX - 8
                );
                mostrarTextoSaque("Tiro de esquina rival");
            } else {
                asignarSaqueMeta(true);
                mostrarTextoSaque("Saque de meta local");
            }
            return;
        }

        if (!ultimoToqueLocal) {
            asignarSaqueEsquina(
                true,
                ConfiguracionJuego.CAMPO_X_MAX - 8,
                centroY < ConfiguracionJuego.ALTO_PANEL / 2 ? ConfiguracionJuego.CAMPO_Y_MIN + 8 : ConfiguracionJuego.CAMPO_Y_MAX - 8
            );
            mostrarTextoSaque("Tiro de esquina local");
        } else {
            asignarSaqueMeta(false);
            mostrarTextoSaque("Saque de meta rival");
        }
    }

    private void manejarBalonFueraDeCancha(double centroX, double centroY) {
        boolean fueraSuperior = centroY < ConfiguracionJuego.CAMPO_Y_MIN;
        boolean fueraInferior = centroY > ConfiguracionJuego.CAMPO_Y_MAX;
        boolean fueraIzquierda = centroX < ConfiguracionJuego.CAMPO_X_MIN;
        boolean fueraDerecha = centroX > ConfiguracionJuego.CAMPO_X_MAX;
        boolean enRangoArco = centroY >= ConfiguracionJuego.Y_PORTERIA
            && centroY <= ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA;

        // Salida por banda.
        if (fueraSuperior || fueraInferior) {
            boolean saqueLocal = !ultimoToqueLocal;
            int ySaque = fueraSuperior ? ConfiguracionJuego.CAMPO_Y_MIN + 8 : ConfiguracionJuego.CAMPO_Y_MAX - 8;
            int xSaque = (int) Math.max(
                ConfiguracionJuego.CAMPO_X_MIN + 10,
                Math.min(ConfiguracionJuego.CAMPO_X_MAX - 10, centroX)
            );
            asignarSaqueBanda(saqueLocal, xSaque, ySaque);
            mostrarTextoSaque("Saque de banda " + (saqueLocal ? "local" : "rival"));
            return;
        }

        // Salida por fondo sin gol: meta o esquina.
        if (fueraIzquierda && !enRangoArco) {
            // Fondo izquierdo defendido por el local.
            if (ultimoToqueLocal) {
                // Ultimo toque local: esquina rival.
                asignarSaqueEsquina(false, ConfiguracionJuego.CAMPO_X_MIN + 8, centroY < ConfiguracionJuego.ALTO_PANEL / 2 ? ConfiguracionJuego.CAMPO_Y_MIN + 8 : ConfiguracionJuego.CAMPO_Y_MAX - 8);
                mostrarTextoSaque("Tiro de esquina rival");
            } else {
                // Ultimo toque rival: saque de meta local.
                asignarSaqueMeta(true);
                mostrarTextoSaque("Saque de meta local");
            }
            return;
        }

        if (fueraDerecha && !enRangoArco) {
            // Fondo derecho defendido por el rival.
            if (!ultimoToqueLocal) {
                // Ultimo toque rival: esquina local.
                asignarSaqueEsquina(true, ConfiguracionJuego.CAMPO_X_MAX - 8, centroY < ConfiguracionJuego.ALTO_PANEL / 2 ? ConfiguracionJuego.CAMPO_Y_MIN + 8 : ConfiguracionJuego.CAMPO_Y_MAX - 8);
                mostrarTextoSaque("Tiro de esquina local");
            } else {
                // Ultimo toque local: saque de meta rival.
                asignarSaqueMeta(false);
                mostrarTextoSaque("Saque de meta rival");
            }
        }
    }

    private void asignarSaqueBanda(boolean saqueLocal, int x, int y) {
        Jugador ejecutor = seleccionarCobradorCampo(saqueLocal, y);
        prepararFormacionSaqueBanda(saqueLocal, ejecutor, x, y);
        tomarPosesionEnPunto(ejecutor, saqueLocal, x, y);
        registrarSonido(TipoSonido.SAQUE);
    }

    private void asignarSaqueMeta(boolean saqueLocal) {
        Jugador portero = saqueLocal ? porteroLocal : porteroRival;
        prepararFormacionSaqueMeta(saqueLocal);
        int xBalon = saqueLocal ? ConfiguracionJuego.CAMPO_X_MIN + 42 : ConfiguracionJuego.CAMPO_X_MAX - 42;
        int yBalon = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA / 2;
        tomarPosesionEnPunto(portero, saqueLocal, xBalon, yBalon);
        registrarSonido(TipoSonido.SAQUE);
    }

    private void asignarSaqueEsquina(boolean saqueLocal, int x, double y) {
        Jugador ejecutor = seleccionarCobradorCampo(saqueLocal, y);
        prepararFormacionSaqueEsquina(saqueLocal, ejecutor, x, (int) y);
        tomarPosesionEnPunto(ejecutor, saqueLocal, x, (int) y);
        registrarSonido(TipoSonido.SAQUE);
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
        cooldownRoboFrames = ConfiguracionJuego.FPS / 2;
        cooldownCapturaLibreFrames = 0;
        cooldownDecisionNpcFrames = COOLDOWN_DECISION_NPC / 2;
        ultimoPateador = null;
        bloqueoRecapturaUltimoPateadorFrames = 0;
        pegarBalonAlPoseedor();
    }

    private void tomarPosesionEnPunto(Jugador jugador, boolean equipoLocal, int balonX, int balonY) {
        ubicarEjecutorDetrasDelBalon(jugador, equipoLocal, balonX, balonY);
        tomarPosesion(jugador, equipoLocal);
        balon.setPosicion(balonX - balon.getAncho() / 2.0, balonY - balon.getAlto() / 2.0);
        balon.detener();
        cooldownRoboFrames = ConfiguracionJuego.FPS;
        cooldownCapturaLibreFrames = 10;
        cooldownDecisionNpcFrames = COOLDOWN_DECISION_NPC;
    }

    private void reiniciarJugada(boolean saqueLocal) {
        // Recoloca la formacion e inicia la siguiente jugada.
        jugadorPrincipal.setX(ConfiguracionJuego.POS_X_BASE_LOCAL - 30);
        jugadorPrincipal.setY(ConfiguracionJuego.ALTO_PANEL / 2 - 58);
        aliadoLocal.setX(ConfiguracionJuego.POS_X_BASE_LOCAL - 72);
        aliadoLocal.setY(ConfiguracionJuego.ALTO_PANEL / 2 + 34);
        porteroLocal.setX(20);
        porteroLocal.setY(ConfiguracionJuego.POS_Y_PORTERO);

        rivalUno.setX(ConfiguracionJuego.POS_X_BASE_RIVAL + 22);
        rivalUno.setY(ConfiguracionJuego.ALTO_PANEL / 2 - 58);
        rivalDos.setX(ConfiguracionJuego.POS_X_BASE_RIVAL + 58);
        rivalDos.setY(ConfiguracionJuego.ALTO_PANEL / 2 + 34);
        porteroRival.setX(ConfiguracionJuego.ANCHO_PANEL - 50);
        porteroRival.setY(ConfiguracionJuego.POS_Y_PORTERO);

        Jugador cobrador = saqueLocal ? jugadorPrincipal : rivalUno;
        Jugador apoyo = saqueLocal ? aliadoLocal : rivalDos;
        int centroX = ConfiguracionJuego.ANCHO_PANEL / 2;
        int centroY = ConfiguracionJuego.ALTO_PANEL / 2;
        apoyo.setX(saqueLocal ? centroX - 96 : centroX + 60);
        apoyo.setY(centroY - apoyo.getAlto() / 2);
        tomarPosesionEnPunto(cobrador, saqueLocal, centroX, centroY);
        registrarSonido(TipoSonido.SAQUE);
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

    private void ubicarEjecutorDetrasDelBalon(Jugador jugador, boolean equipoLocal, int balonX, int balonY) {
        int offsetX = equipoLocal ? jugador.getAncho() - 6 : -balon.getAncho() + 6;
        jugador.setX((int) Math.round(balonX - offsetX));
        jugador.setY(balonY - jugador.getAlto() / 2);
        limitarEntidadAlPanel(jugador);
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
            yObjetivo = proyectarYLineaTiro(poseedorBalon, local, planoCobertura) - portero.getAlto() / 2.0;
        } else {
            // Sin amenaza directa, mantiene alineacion con la pelota sin abandonar el centro del arco.
            yObjetivo = centroPorteria - portero.getAlto() / 2.0 + (balon.getCentroY() - centroPorteria) * 0.22;
        }

        double yMin = ConfiguracionJuego.Y_PORTERIA - 12.0;
        double yMax = ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA - portero.getAlto() + 12.0;
        return (int) Math.round(Math.max(yMin, Math.min(yMax, yObjetivo)));
    }

    private int calcularVelocidadPortero(Jugador portero, boolean local) {
        int velocidad = portero.getVelocidad();
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
        return yProyectada >= ConfiguracionJuego.Y_PORTERIA - 42
            && yProyectada <= ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA + 42
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
        double mejorDistancia = Double.MAX_VALUE;
        double mejorAltura = Double.MAX_VALUE;
        double mejorX = balon.getCentroX();
        double mejorY = balon.getCentroY();

        for (int frame = 0; frame <= LOOKAHEAD_ATAJADA_FRAMES; frame++) {
            double futuraX = balon.getCentroX() + balon.getVelocidadX() * frame;
            double futuraY = balon.getCentroY() + balon.getVelocidadY() * frame;
            double futuraAltura = alturaBalonEnFrames(frame);
            if (futuraY < ConfiguracionJuego.Y_PORTERIA - 36 || futuraY > ConfiguracionJuego.Y_PORTERIA + ConfiguracionJuego.ALTO_PORTERIA + 36) {
                continue;
            }

            double dx = centroPorteroX - futuraX;
            double dy = centroPorteroY - futuraY;
            double distancia = Math.sqrt(dx * dx + dy * dy);
            if (distancia < mejorDistancia) {
                mejorDistancia = distancia;
                mejorAltura = futuraAltura;
                mejorX = futuraX;
                mejorY = futuraY;
            }
        }

        if (mejorDistancia <= DISTANCIA_ATAJADA_PORTERO && mejorAltura <= ALTURA_MAXIMA_ATAJADA && (amenazaReal || velocidadBalon > 1.45)) {
            tomarPosesion(portero, local);
            cooldownAtajadaPorteroFrames = ConfiguracionJuego.FPS / 2;
            registrarSonido(TipoSonido.ROBO);
            mostrarTextoSaque("Atajada de " + portero.getNombre());
            return true;
        }

        if (mejorDistancia <= DISTANCIA_DESVIO_PORTERO && mejorAltura <= ALTURA_MAXIMA_ATAJADA + 6.0 && amenazaReal) {
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

    private double alturaBalonEnFrames(double frames) {
        double altura = balon.getAltura() + balon.getVelocidadZ() * frames - (GRAVEDAD_BALON * frames * (frames + 1.0)) / 2.0;
        return Math.max(0.0, altura);
    }

    private void actualizarEstadoJugadores() {
        // Actualiza estados temporales de todos los jugadores.
        porteroLocal.actualizarEstado();
        jugadorPrincipal.actualizarEstado();
        aliadoLocal.actualizarEstado();
        porteroRival.actualizarEstado();
        rivalUno.actualizarEstado();
        rivalDos.actualizarEstado();

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
