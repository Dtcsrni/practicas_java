package juego.core;

import java.awt.Point;
import java.awt.Rectangle;

// Fuente unica de verdad para la geometria del campo y sus puntos reglamentarios.
public final class GeometriaCancha {
    public enum LadoSalida {
        IZQUIERDA,
        DERECHA,
        SUPERIOR,
        INFERIOR,
        DENTRO
    }

    public enum Cuadrante {
        SUPERIOR_IZQUIERDO,
        SUPERIOR_DERECHO,
        INFERIOR_IZQUIERDO,
        INFERIOR_DERECHO
    }

    private final Rectangle campo;
    private final Rectangle areaGrandeIzquierda;
    private final Rectangle areaGrandeDerecha;
    private final Rectangle areaChicaIzquierda;
    private final Rectangle areaChicaDerecha;
    private final Rectangle porteriaIzquierda;
    private final Rectangle porteriaDerecha;
    private final int centroX;
    private final int centroY;
    private final int radioCirculoCentral;
    private final int radioArcoArea;
    private final int distanciaPuntoPenal;
    private final int profundidadPorteria;
    private final int radioArcoEsquina;

    private GeometriaCancha(
        Rectangle campo,
        Rectangle areaGrandeIzquierda,
        Rectangle areaGrandeDerecha,
        Rectangle areaChicaIzquierda,
        Rectangle areaChicaDerecha,
        Rectangle porteriaIzquierda,
        Rectangle porteriaDerecha,
        int centroX,
        int centroY,
        int radioCirculoCentral,
        int radioArcoArea,
        int distanciaPuntoPenal,
        int profundidadPorteria,
        int radioArcoEsquina
    ) {
        this.campo = campo;
        this.areaGrandeIzquierda = areaGrandeIzquierda;
        this.areaGrandeDerecha = areaGrandeDerecha;
        this.areaChicaIzquierda = areaChicaIzquierda;
        this.areaChicaDerecha = areaChicaDerecha;
        this.porteriaIzquierda = porteriaIzquierda;
        this.porteriaDerecha = porteriaDerecha;
        this.centroX = centroX;
        this.centroY = centroY;
        this.radioCirculoCentral = radioCirculoCentral;
        this.radioArcoArea = radioArcoArea;
        this.distanciaPuntoPenal = distanciaPuntoPenal;
        this.profundidadPorteria = profundidadPorteria;
        this.radioArcoEsquina = radioArcoEsquina;
    }

    public static GeometriaCancha crearReglamentariaEscalada(int anchoPanel, int altoPanel) {
        // Conserva el mapa unificado, pero con una interpretacion mas arcade:
        // cancha algo mas ancha visualmente y porterias/areas adaptadas a ritmo alto.
        int margenVertical = 42;
        int altoCampo = altoPanel - margenVertical * 2;
        int anchoCampo = (int) Math.round(altoCampo * 112.0 / 68.0);
        anchoCampo = Math.min(anchoPanel - 72, anchoCampo);
        int margenHorizontal = (anchoPanel - anchoCampo) / 2;

        Rectangle campo = new Rectangle(margenHorizontal, margenVertical, anchoCampo, altoCampo);
        int centroX = campo.x + campo.width / 2;
        int centroY = campo.y + campo.height / 2;

        int profundidadAreaGrande = (int) Math.round(campo.width * 0.142);
        int altoAreaGrande = (int) Math.round(campo.height * 0.52);
        int profundidadAreaChica = (int) Math.round(campo.width * 0.064);
        int altoAreaChica = (int) Math.round(campo.height * 0.27);
        int altoPorteria = (int) Math.round(campo.height * 0.135);
        int profundidadPorteria = Math.max(20, (int) Math.round(campo.width * 0.032));
        int radioCirculoCentral = (int) Math.round(campo.height * 0.122);
        int radioArcoArea = Math.max(36, (int) Math.round(radioCirculoCentral * 0.86));
        int distanciaPuntoPenal = (int) Math.round(campo.width * 0.115);
        int radioArcoEsquina = Math.max(10, (int) Math.round(campo.height * 0.02));

        Rectangle areaGrandeIzquierda = new Rectangle(
            campo.x,
            centroY - altoAreaGrande / 2,
            profundidadAreaGrande,
            altoAreaGrande
        );
        Rectangle areaGrandeDerecha = new Rectangle(
            campo.x + campo.width - profundidadAreaGrande,
            centroY - altoAreaGrande / 2,
            profundidadAreaGrande,
            altoAreaGrande
        );
        Rectangle areaChicaIzquierda = new Rectangle(
            campo.x,
            centroY - altoAreaChica / 2,
            profundidadAreaChica,
            altoAreaChica
        );
        Rectangle areaChicaDerecha = new Rectangle(
            campo.x + campo.width - profundidadAreaChica,
            centroY - altoAreaChica / 2,
            profundidadAreaChica,
            altoAreaChica
        );
        Rectangle porteriaIzquierda = new Rectangle(
            campo.x - profundidadPorteria,
            centroY - altoPorteria / 2,
            profundidadPorteria,
            altoPorteria
        );
        Rectangle porteriaDerecha = new Rectangle(
            campo.x + campo.width,
            centroY - altoPorteria / 2,
            profundidadPorteria,
            altoPorteria
        );

        return new GeometriaCancha(
            campo,
            areaGrandeIzquierda,
            areaGrandeDerecha,
            areaChicaIzquierda,
            areaChicaDerecha,
            porteriaIzquierda,
            porteriaDerecha,
            centroX,
            centroY,
            radioCirculoCentral,
            radioArcoArea,
            distanciaPuntoPenal,
            profundidadPorteria,
            radioArcoEsquina
        );
    }

    public Rectangle getCampo() {
        return new Rectangle(campo);
    }

    public Rectangle getAreaGrande(boolean local) {
        return new Rectangle(local ? areaGrandeIzquierda : areaGrandeDerecha);
    }

    public Rectangle getAreaChica(boolean local) {
        return new Rectangle(local ? areaChicaIzquierda : areaChicaDerecha);
    }

    public Rectangle getPorteria(boolean local) {
        return new Rectangle(local ? porteriaIzquierda : porteriaDerecha);
    }

    public int getCampoXMin() {
        return campo.x;
    }

    public int getCampoYMin() {
        return campo.y;
    }

    public int getCampoXMax() {
        return campo.x + campo.width;
    }

    public int getCampoYMax() {
        return campo.y + campo.height;
    }

    public int getCentroX() {
        return centroX;
    }

    public int getCentroY() {
        return centroY;
    }

    public int getRadioCirculoCentral() {
        return radioCirculoCentral;
    }

    public int getRadioArcoArea() {
        return radioArcoArea;
    }

    public int getProfundidadPorteria() {
        return profundidadPorteria;
    }

    public int getAlturaPorteria() {
        return porteriaIzquierda.height;
    }

    public int getPorteriaY() {
        return porteriaIzquierda.y;
    }

    public int getPuntoPenalX(boolean local) {
        return local ? campo.x + distanciaPuntoPenal : campo.x + campo.width - distanciaPuntoPenal;
    }

    public int getPuntoPenalY() {
        return centroY;
    }

    public Point getPuntoCentral() {
        return new Point(centroX, centroY);
    }

    public Point getPuntoSaqueInicial() {
        return getPuntoCentral();
    }

    public Point getPuntoSaqueMeta(boolean local) {
        Rectangle areaChica = local ? areaChicaIzquierda : areaChicaDerecha;
        int x = local ? areaChica.x + areaChica.width - 4 : areaChica.x + 4;
        return new Point(x, centroY);
    }

    public Point getCornerSuperior(boolean local) {
        return new Point(local ? getCampoXMax() : getCampoXMin(), getCampoYMin());
    }

    public Point getCornerInferior(boolean local) {
        return new Point(local ? getCampoXMax() : getCampoXMin(), getCampoYMax());
    }

    public int getRadioArcoEsquina() {
        return radioArcoEsquina;
    }

    public int clampXEnLineaLateral(double x) {
        return (int) Math.max(getCampoXMin(), Math.min(getCampoXMax(), Math.round(x)));
    }

    public int clampYEnCampo(double y) {
        return (int) Math.max(getCampoYMin(), Math.min(getCampoYMax(), Math.round(y)));
    }

    public Point clampPuntoDentroCampo(double x, double y, int margen) {
        int puntoX = (int) Math.max(getCampoXMin() + margen, Math.min(getCampoXMax() - margen, Math.round(x)));
        int puntoY = (int) Math.max(getCampoYMin() + margen, Math.min(getCampoYMax() - margen, Math.round(y)));
        return new Point(puntoX, puntoY);
    }

    public Point normalizarPuntoLibreIndirecto(double x, double y) {
        int margenX = Math.max(20, campo.width / 36);
        int margenY = Math.max(20, campo.height / 34);
        return clampPuntoDentroCampo(x, y, Math.min(margenX, margenY));
    }

    public boolean estaDentroCampo(double x, double y) {
        return x >= getCampoXMin() && x <= getCampoXMax()
            && y >= getCampoYMin() && y <= getCampoYMax();
    }

    public boolean estaEntrePostes(double y) {
        return y >= getPorteriaY() && y <= getPorteriaY() + getAlturaPorteria();
    }

    public LadoSalida resolverLadoSalida(double x, double y) {
        if (y < getCampoYMin()) {
            return LadoSalida.SUPERIOR;
        }
        if (y > getCampoYMax()) {
            return LadoSalida.INFERIOR;
        }
        if (x < getCampoXMin()) {
            return LadoSalida.IZQUIERDA;
        }
        if (x > getCampoXMax()) {
            return LadoSalida.DERECHA;
        }
        return LadoSalida.DENTRO;
    }

    public Cuadrante resolverCuadrante(double x, double y) {
        boolean derecha = x >= centroX;
        boolean inferior = y >= centroY;
        if (derecha && inferior) {
            return Cuadrante.INFERIOR_DERECHO;
        }
        if (derecha) {
            return Cuadrante.SUPERIOR_DERECHO;
        }
        if (inferior) {
            return Cuadrante.INFERIOR_IZQUIERDO;
        }
        return Cuadrante.SUPERIOR_IZQUIERDO;
    }
}
