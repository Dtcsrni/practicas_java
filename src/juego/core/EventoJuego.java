package juego.core;

// Eventos puntuales que la simulacion emite hacia la maquina de estados.
public enum EventoJuego {
    NINGUNO,
    GOL_LOCAL,
    GOL_RIVAL,
    FALTA_A_FAVOR,
    FALTA_EN_CONTRA,
    EMPATE,
    VICTORIA,
    DERROTA
}
