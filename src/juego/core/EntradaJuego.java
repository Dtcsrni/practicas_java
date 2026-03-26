package juego.core;

import java.awt.event.KeyEvent;

// Encapsula el estado del teclado para desacoplar input y logica.
public class EntradaJuego {
    private static final long CARGA_MAX_NS = 1_200_000_000L;
    private static final double FACTOR_MIN_CARGA = 0.35;
    private static final double FACTOR_MAX_CARGA = 1.00;

    private boolean arriba;
    private boolean abajo;
    private boolean izquierda;
    private boolean derecha;

    private boolean pasePendiente;
    private boolean tiroPendiente;
    private boolean pasePresionado;
    private boolean tiroPresionado;
    private long inicioCargaPaseNs;
    private long inicioCargaTiroNs;
    private double factorPasePendiente;
    private double factorTiroPendiente;

    private int ultimaDireccionX = 1;
    private int ultimaDireccionY = 0;

    public void procesarPresion(int codigoTecla) {
        // Activa banderas de direccion cuando una tecla se presiona.
        if (codigoTecla == KeyEvent.VK_W || codigoTecla == KeyEvent.VK_UP) {
            arriba = true;
        }
        if (codigoTecla == KeyEvent.VK_S || codigoTecla == KeyEvent.VK_DOWN) {
            abajo = true;
        }
        if (codigoTecla == KeyEvent.VK_A || codigoTecla == KeyEvent.VK_LEFT) {
            izquierda = true;
        }
        if (codigoTecla == KeyEvent.VK_D || codigoTecla == KeyEvent.VK_RIGHT) {
            derecha = true;
        }
        actualizarUltimaDireccion();

        // Inicia carga de pase/tiro al presionar.
        if (codigoTecla == KeyEvent.VK_SPACE && !pasePresionado) {
            pasePresionado = true;
            inicioCargaPaseNs = System.nanoTime();
        }
        if (codigoTecla == KeyEvent.VK_X && !tiroPresionado) {
            tiroPresionado = true;
            inicioCargaTiroNs = System.nanoTime();
        }
    }

    public void procesarLiberacion(int codigoTecla) {
        // Desactiva banderas cuando se suelta la tecla.
        if (codigoTecla == KeyEvent.VK_W || codigoTecla == KeyEvent.VK_UP) {
            arriba = false;
        }
        if (codigoTecla == KeyEvent.VK_S || codigoTecla == KeyEvent.VK_DOWN) {
            abajo = false;
        }
        if (codigoTecla == KeyEvent.VK_A || codigoTecla == KeyEvent.VK_LEFT) {
            izquierda = false;
        }
        if (codigoTecla == KeyEvent.VK_D || codigoTecla == KeyEvent.VK_RIGHT) {
            derecha = false;
        }
        actualizarUltimaDireccion();

        // Ejecuta accion al soltar, con fuerza proporcional al tiempo de carga.
        if (codigoTecla == KeyEvent.VK_SPACE && pasePresionado) {
            pasePresionado = false;
            pasePendiente = true;
            factorPasePendiente = calcularFactorCarga(inicioCargaPaseNs);
        }
        if (codigoTecla == KeyEvent.VK_X && tiroPresionado) {
            tiroPresionado = false;
            tiroPendiente = true;
            factorTiroPendiente = calcularFactorCarga(inicioCargaTiroNs);
        }
    }

    public int calcularDeltaX(int velocidad) {
        // Convierte estado de teclas en desplazamiento horizontal.
        int dx = 0;
        if (izquierda) {
            dx -= velocidad;
        }
        if (derecha) {
            dx += velocidad;
        }
        return dx;
    }

    public int calcularDeltaY(int velocidad) {
        // Convierte estado de teclas en desplazamiento vertical.
        int dy = 0;
        if (arriba) {
            dy -= velocidad;
        }
        if (abajo) {
            dy += velocidad;
        }
        return dy;
    }

    public void limpiarMovimiento() {
        // Util cuando hay cambio de estado (pausa, gol, reinicio).
        arriba = false;
        abajo = false;
        izquierda = false;
        derecha = false;
        pasePendiente = false;
        tiroPendiente = false;
        pasePresionado = false;
        tiroPresionado = false;
        factorPasePendiente = 0.0;
        factorTiroPendiente = 0.0;
    }

    public int getDireccionAccionX() {
        int dx = direccionActualX();
        if (dx != 0 || direccionActualY() != 0) {
            return dx;
        }
        return ultimaDireccionX;
    }

    public int getDireccionAccionY() {
        int dy = direccionActualY();
        if (direccionActualX() != 0 || dy != 0) {
            return dy;
        }
        return ultimaDireccionY;
    }

    public void solicitarPase() {
        pasePendiente = true;
        factorPasePendiente = 1.0;
    }

    public void solicitarTiro() {
        tiroPendiente = true;
        factorTiroPendiente = 1.0;
    }

    public boolean consumirPase() {
        boolean valor = pasePendiente;
        pasePendiente = false;
        return valor;
    }

    public boolean consumirTiro() {
        boolean valor = tiroPendiente;
        tiroPendiente = false;
        return valor;
    }

    public double consumirFactorPase() {
        double factor = factorPasePendiente <= 0.0 ? FACTOR_MIN_CARGA : factorPasePendiente;
        factorPasePendiente = 0.0;
        return factor;
    }

    public double consumirFactorTiro() {
        double factor = factorTiroPendiente <= 0.0 ? FACTOR_MIN_CARGA : factorTiroPendiente;
        factorTiroPendiente = 0.0;
        return factor;
    }

    private int direccionActualX() {
        int dx = 0;
        if (izquierda) {
            dx -= 1;
        }
        if (derecha) {
            dx += 1;
        }
        return dx;
    }

    private int direccionActualY() {
        int dy = 0;
        if (arriba) {
            dy -= 1;
        }
        if (abajo) {
            dy += 1;
        }
        return dy;
    }

    private void actualizarUltimaDireccion() {
        int dx = direccionActualX();
        int dy = direccionActualY();
        if (dx != 0 || dy != 0) {
            ultimaDireccionX = dx;
            ultimaDireccionY = dy;
        }
    }

    private double calcularFactorCarga(long inicioCargaNs) {
        if (inicioCargaNs <= 0L) {
            return FACTOR_MIN_CARGA;
        }
        long duracionNs = Math.max(0L, System.nanoTime() - inicioCargaNs);
        double progreso = Math.min(1.0, duracionNs / (double) CARGA_MAX_NS);
        return FACTOR_MIN_CARGA + (FACTOR_MAX_CARGA - FACTOR_MIN_CARGA) * progreso;
    }
}
