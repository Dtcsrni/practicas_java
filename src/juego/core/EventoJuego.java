package juego.core;

// Eventos de gameplay que disparan cambios en la maquina de estados.
public enum EventoJuego {
    NINGUNO,
    GOL_LOCAL,
    GOL_RIVAL,
    FALTA_A_FAVOR,
    FALTA_EN_CONTRA,
    VICTORIA,
    DERROTA
}
