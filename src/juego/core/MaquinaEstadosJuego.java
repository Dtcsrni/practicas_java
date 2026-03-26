package juego.core;

// Gestiona transiciones de estado de forma centralizada.
public class MaquinaEstadosJuego {
    private EstadoJuego estadoActual;
    private int framesEstado;
    private String mensajeTemporal;

    public MaquinaEstadosJuego() {
        // El juego inicia en pantalla de inicio.
        estadoActual = EstadoJuego.INICIO;
        framesEstado = 0;
        mensajeTemporal = "";
    }

    public EstadoJuego getEstadoActual() {
        return estadoActual;
    }

    public String getMensajeTemporal() {
        return mensajeTemporal;
    }

    public boolean permiteActualizarMundo() {
        // Solo en estado JUGANDO se actualiza la simulacion.
        return estadoActual == EstadoJuego.JUGANDO;
    }

    public void iniciarDesdeInicio() {
        // Entrada controlada al juego desde menu inicial.
        if (estadoActual == EstadoJuego.INICIO) {
            cambiarEstado(EstadoJuego.JUGANDO);
            mensajeTemporal = "";
        }
    }

    public void alternarPausa() {
        // Toggle de pausa sin perder el estado previo de partido.
        if (estadoActual == EstadoJuego.JUGANDO) {
            cambiarEstado(EstadoJuego.PAUSADO);
        } else if (estadoActual == EstadoJuego.PAUSADO) {
            cambiarEstado(EstadoJuego.JUGANDO);
        }
    }

    public void reiniciar() {
        // Reinicio de flujo completo hacia menu inicial.
        cambiarEstado(EstadoJuego.INICIO);
        framesEstado = 0;
        mensajeTemporal = "";
    }

    public void procesarEventoJuego(EventoJuego evento) {
        // Traduce eventos de gameplay a transiciones visuales.
        if (evento == EventoJuego.GOL_LOCAL) {
            cambiarEstado(EstadoJuego.GOL);
            framesEstado = ConfiguracionJuego.FRAMES_MENSAJE_GOL;
            mensajeTemporal = "Gol local";
            return;
        }
        if (evento == EventoJuego.GOL_RIVAL) {
            cambiarEstado(EstadoJuego.GOL);
            framesEstado = ConfiguracionJuego.FRAMES_MENSAJE_GOL;
            mensajeTemporal = "Gol rival";
            return;
        }
        if (evento == EventoJuego.FALTA_A_FAVOR) {
            cambiarEstado(EstadoJuego.FALTA);
            framesEstado = ConfiguracionJuego.FRAMES_MENSAJE_FALTA;
            mensajeTemporal = "Falta a favor";
            return;
        }
        if (evento == EventoJuego.FALTA_EN_CONTRA) {
            cambiarEstado(EstadoJuego.FALTA);
            framesEstado = ConfiguracionJuego.FRAMES_MENSAJE_FALTA;
            mensajeTemporal = "Falta en contra";
            return;
        }
        if (evento == EventoJuego.VICTORIA) {
            cambiarEstado(EstadoJuego.VICTORIA);
            mensajeTemporal = "";
            return;
        }
        if (evento == EventoJuego.DERROTA) {
            cambiarEstado(EstadoJuego.DERROTA);
            mensajeTemporal = "";
        }
    }

    public void actualizar() {
        // Estados temporales caducan por frames y regresan a JUGANDO.
        if (estadoActual == EstadoJuego.GOL || estadoActual == EstadoJuego.FALTA) {
            framesEstado--;
            if (framesEstado <= 0) {
                cambiarEstado(EstadoJuego.JUGANDO);
                mensajeTemporal = "";
            }
        }
    }

    private void cambiarEstado(EstadoJuego nuevoEstado) {
        // Punto unico de cambio de estado.
        estadoActual = nuevoEstado;
    }
}
