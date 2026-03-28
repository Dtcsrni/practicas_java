package juego.sonido;

// Evento sonoro enriquecido con intensidad normalizada.
public record SonidoEvento(TipoSonido tipo, double intensidad) {
    public SonidoEvento {
        if (tipo == null) {
            throw new IllegalArgumentException("tipo no puede ser null");
        }
        intensidad = Math.max(0.0, Math.min(1.0, intensidad));
    }

    public static SonidoEvento normal(TipoSonido tipo) {
        return new SonidoEvento(tipo, 0.5);
    }
}
