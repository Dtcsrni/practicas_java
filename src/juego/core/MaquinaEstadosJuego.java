package juego.core;

// Centraliza las transiciones de UI entre menu, partido y mensajes.
public class MaquinaEstadosJuego {
    private EstadoJuego estadoActual;
    private int framesEstado;
    private String mensajeTemporal;

    public MaquinaEstadosJuego() {
        // El juego siempre arranca en el menu inicial.
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
        // La simulacion solo corre durante el partido activo.
        return estadoActual == EstadoJuego.JUGANDO;
    }

    public void iniciarDesdeInicio() {
        // Entra al partido solo desde la pantalla inicial.
        if (estadoActual == EstadoJuego.INICIO) {
            cambiarEstado(EstadoJuego.JUGANDO);
            mensajeTemporal = "";
        }
    }

    public void alternarPausa() {
        // Alterna entre pausa y partido sin reiniciar la simulacion.
        if (estadoActual == EstadoJuego.JUGANDO) {
            cambiarEstado(EstadoJuego.PAUSADO);
        } else if (estadoActual == EstadoJuego.PAUSADO) {
            cambiarEstado(EstadoJuego.JUGANDO);
        }
    }

    public void reiniciar() {
        // Reinicia la interfaz al estado inicial.
        cambiarEstado(EstadoJuego.INICIO);
        framesEstado = 0;
        mensajeTemporal = "";
    }

    public void procesarEventoJuego(EventoJuego evento) {
        // Convierte eventos del motor en estados visibles para el jugador.
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
        // Los overlays temporales expiran y devuelven el control al partido.
        if (estadoActual == EstadoJuego.GOL || estadoActual == EstadoJuego.FALTA) {
            framesEstado--;
            if (framesEstado <= 0) {
                cambiarEstado(EstadoJuego.JUGANDO);
                mensajeTemporal = "";
            }
        }
    }

    private void cambiarEstado(EstadoJuego nuevoEstado) {
        // Unico punto de mutacion del estado visual.
        estadoActual = nuevoEstado;
    }
}
