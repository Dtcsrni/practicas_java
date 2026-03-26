package juego.core;

import java.awt.event.KeyEvent;

// Traduce teclado crudo a intenciones de movimiento, pase y tiro.
public class EntradaJuego {
    private static final long PERIODO_BARRA_NS = 1_100_000_000L;
    private static final double FACTOR_MIN_CARGA = 0.35;
    private static final double FACTOR_MAX_CARGA = 1.00;

    private boolean arriba;
    private boolean abajo;
    private boolean izquierda;
    private boolean derecha;
    private boolean correr;

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
        // Actualiza banderas de movimiento segun la tecla presionada.
        actualizarEstadoMovimiento(codigoTecla, true);
        if (codigoTecla == KeyEvent.VK_SHIFT) {
            correr = true;
        }
        actualizarUltimaDireccion();

        // Empieza a cargar la accion al mantener la tecla.
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
        // Libera banderas de movimiento al soltar la tecla.
        actualizarEstadoMovimiento(codigoTecla, false);
        if (codigoTecla == KeyEvent.VK_SHIFT) {
            correr = false;
        }
        actualizarUltimaDireccion();

        // Dispara la accion usando el tiempo de carga acumulado.
        if (codigoTecla == KeyEvent.VK_SPACE && pasePresionado) {
            pasePresionado = false;
            pasePendiente = true;
            factorPasePendiente = calcularFactorBarra(inicioCargaPaseNs);
        }
        if (codigoTecla == KeyEvent.VK_X && tiroPresionado) {
            tiroPresionado = false;
            tiroPendiente = true;
            factorTiroPendiente = calcularFactorBarra(inicioCargaTiroNs);
        }
    }

    public int calcularDeltaX(int velocidad) {
        // Convierte el input horizontal en delta por frame.
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
        // Convierte el input vertical en delta por frame.
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
        // Evita arrastres de input al pausar, anotar o reiniciar.
        arriba = false;
        abajo = false;
        izquierda = false;
        derecha = false;
        correr = false;
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

    public boolean estaCorriendo() {
        return correr;
    }

    public boolean estaCargandoPase() {
        return pasePresionado;
    }

    public boolean estaCargandoTiro() {
        return tiroPresionado;
    }

    public double getFactorCargaPaseActual() {
        return pasePresionado ? calcularFactorBarra(inicioCargaPaseNs) : 0.0;
    }

    public double getFactorCargaTiroActual() {
        return tiroPresionado ? calcularFactorBarra(inicioCargaTiroNs) : 0.0;
    }

    public String getEtiquetaCargaActiva() {
        if (tiroPresionado) {
            return "TIRO";
        }
        if (pasePresionado) {
            return "PASE";
        }
        return "";
    }

    public double getFactorCargaActiva() {
        if (tiroPresionado) {
            return getFactorCargaTiroActual();
        }
        if (pasePresionado) {
            return getFactorCargaPaseActual();
        }
        return 0.0;
    }

    private int direccionActualX() {
        // Direccion horizontal normalizada: -1 izquierda, 0 quieto, 1 derecha.
        return (izquierda ? -1 : 0) + (derecha ? 1 : 0);
    }

    private int direccionActualY() {
        // Direccion vertical normalizada: -1 arriba, 0 quieto, 1 abajo.
        return (arriba ? -1 : 0) + (abajo ? 1 : 0);
    }

    private void actualizarEstadoMovimiento(int codigoTecla, boolean presionada) {
        // Unifica el mapeo de teclas a estados de movimiento.
        if (esTeclaArriba(codigoTecla)) {
            arriba = presionada;
        }
        if (esTeclaAbajo(codigoTecla)) {
            abajo = presionada;
        }
        if (esTeclaIzquierda(codigoTecla)) {
            izquierda = presionada;
        }
        if (esTeclaDerecha(codigoTecla)) {
            derecha = presionada;
        }
    }

    private boolean esTeclaArriba(int codigoTecla) {
        // Acepta tanto WASD como flechas.
        return codigoTecla == KeyEvent.VK_W || codigoTecla == KeyEvent.VK_UP;
    }

    private boolean esTeclaAbajo(int codigoTecla) {
        // Acepta tanto WASD como flechas.
        return codigoTecla == KeyEvent.VK_S || codigoTecla == KeyEvent.VK_DOWN;
    }

    private boolean esTeclaIzquierda(int codigoTecla) {
        // Acepta tanto WASD como flechas.
        return codigoTecla == KeyEvent.VK_A || codigoTecla == KeyEvent.VK_LEFT;
    }

    private boolean esTeclaDerecha(int codigoTecla) {
        // Acepta tanto WASD como flechas.
        return codigoTecla == KeyEvent.VK_D || codigoTecla == KeyEvent.VK_RIGHT;
    }

    private void actualizarUltimaDireccion() {
        // Guarda la ultima direccion no nula para orientar pases/tiros sin input.
        int dx = direccionActualX();
        int dy = direccionActualY();
        if (dx != 0 || dy != 0) {
            ultimaDireccionX = dx;
            ultimaDireccionY = dy;
        }
    }

    private double calcularFactorBarra(long inicioCargaNs) {
        if (inicioCargaNs <= 0L) {
            return FACTOR_MIN_CARGA;
        }
        long duracionNs = Math.max(0L, System.nanoTime() - inicioCargaNs);
        long ciclo = PERIODO_BARRA_NS <= 0 ? 1 : PERIODO_BARRA_NS;
        long posicion = duracionNs % ciclo;
        double fase = posicion / (double) ciclo;
        // Onda triangular: 0->1->0 para que la barra suba y baje.
        double triangular = fase < 0.5 ? fase * 2.0 : (1.0 - fase) * 2.0;
        return FACTOR_MIN_CARGA + (FACTOR_MAX_CARGA - FACTOR_MIN_CARGA) * triangular;
    }
}
