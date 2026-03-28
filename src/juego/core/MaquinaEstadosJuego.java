package juego.core;

// Centraliza las transiciones de UI entre menu, partido y mensajes.
public class MaquinaEstadosJuego {
    private EstadoJuego estadoActual;
    private int framesEstado;
    private String mensajeTemporal;
    private boolean inicioModoEspectador;

    public MaquinaEstadosJuego() {
        // El juego siempre arranca en el menu inicial.
        estadoActual = EstadoJuego.INICIO;
        framesEstado = 0;
        mensajeTemporal = "";
        inicioModoEspectador = true;
    }

    public EstadoJuego getEstadoActual() {
        return estadoActual;
    }

    public String getMensajeTemporal() {
        return mensajeTemporal;
    }

    public boolean isInicioModoEspectador() {
        return inicioModoEspectador;
    }

    public boolean permiteActualizarMundo() {
        // La simulacion corre durante el partido y tambien en el cierre final
        // para sostener animaciones de victoria/derrota en cancha.
        return estadoActual == EstadoJuego.JUGANDO
            || estadoActual == EstadoJuego.VICTORIA
            || estadoActual == EstadoJuego.DERROTA
            || estadoActual == EstadoJuego.EMPATE;
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
        inicioModoEspectador = true;
    }

    public void alternarModoInicio() {
        if (estadoActual == EstadoJuego.INICIO) {
            inicioModoEspectador = !inicioModoEspectador;
        }
    }

    public void procesarEventoJuego(EventoJuego evento) {
        // Convierte eventos del motor en estados visibles para el jugador.
        switch (evento) {
            case GOL_LOCAL -> mostrarOverlayTemporal(EstadoJuego.GOL, ConfiguracionJuego.FRAMES_MENSAJE_GOL, "Gol local");
            case GOL_RIVAL -> mostrarOverlayTemporal(EstadoJuego.GOL, ConfiguracionJuego.FRAMES_MENSAJE_GOL, "Gol rival");
            case FALTA_A_FAVOR -> mostrarOverlayTemporal(EstadoJuego.FALTA, ConfiguracionJuego.FRAMES_MENSAJE_FALTA, "Falta a favor");
            case FALTA_EN_CONTRA -> mostrarOverlayTemporal(EstadoJuego.FALTA, ConfiguracionJuego.FRAMES_MENSAJE_FALTA, "Falta en contra");
            case VICTORIA -> mostrarEstadoFinal(EstadoJuego.VICTORIA);
            case EMPATE -> mostrarEstadoFinal(EstadoJuego.EMPATE);
            case DERROTA -> mostrarEstadoFinal(EstadoJuego.DERROTA);
            default -> {
            }
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

    private void mostrarOverlayTemporal(EstadoJuego estado, int duracionFrames, String mensaje) {
        // Muestra un estado intermedio (gol/falta) por un tiempo limitado.
        cambiarEstado(estado);
        framesEstado = duracionFrames;
        mensajeTemporal = mensaje;
    }

    private void mostrarEstadoFinal(EstadoJuego estado) {
        // Estados finales (victoria/empate/derrota) sin contador de expiracion.
        cambiarEstado(estado);
        mensajeTemporal = "";
    }
}
