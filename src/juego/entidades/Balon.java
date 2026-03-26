package juego.entidades;

import java.awt.Color;
import java.awt.Graphics;

// Entidad del balon con fisica ligera (inercia, friccion y rebote).
public class Balon extends EntidadJuego {
    private double posX;
    private double posY;
    private double velocidadX;
    private double velocidadY;
    private double altura;
    private double velocidadZ;

    private static final double FRICCION = 0.988;
    private static final double VELOCIDAD_MINIMA = 0.03;
    private static final double REBOTE = 0.78;
    private static final double VELOCIDAD_MAXIMA = 9.5;
    private static final double GRAVEDAD = 0.36;
    private static final double REBOTE_VERTICAL = 0.54;
    private static final double VELOCIDAD_VERTICAL_MINIMA = 0.18;
    private static final double VELOCIDAD_VERTICAL_MAXIMA = 11.5;
    private double anguloAnimacion;

    public Balon(int x, int y, int tamano) {
        super(x, y, tamano, tamano);
        // Se guarda tambien en double para evitar perdida por redondeo.
        posX = x;
        posY = y;
        velocidadX = 0.0;
        velocidadY = 0.0;
        altura = 0.0;
        velocidadZ = 0.0;
        anguloAnimacion = 0.0;
    }

    public void reiniciarEnCentro(int anchoPanel, int altoPanel) {
        // Centra balon y limpia velocidad.
        posX = (anchoPanel - ancho) / 2.0;
        posY = (altoPanel - alto) / 2.0;
        velocidadX = 0.0;
        velocidadY = 0.0;
        altura = 0.0;
        velocidadZ = 0.0;
        anguloAnimacion = 0.0;
        sincronizarPosicionEntera();
    }

    public void impulsar(double impulsoX, double impulsoY) {
        impulsar(impulsoX, impulsoY, 0.0);
    }

    public void impulsar(double impulsoX, double impulsoY, double impulsoZ) {
        // Suma impulso y recorta a velocidad maxima estable.
        velocidadX += impulsoX;
        velocidadY += impulsoY;
        velocidadZ += impulsoZ;
        limitarVelocidad();
    }

    public void actualizarFisica(int anchoPanel, int altoPanel) {
        // Integracion simple: posicion += velocidad.
        posX += velocidadX;
        posY += velocidadY;
        altura += velocidadZ;
        velocidadZ -= GRAVEDAD;

        velocidadX *= FRICCION;
        velocidadY *= FRICCION;
        if (Math.abs(velocidadX) < VELOCIDAD_MINIMA) {
            velocidadX = 0.0;
        }
        if (Math.abs(velocidadY) < VELOCIDAD_MINIMA) {
            velocidadY = 0.0;
        }
        if (altura <= 0.0 && Math.abs(velocidadZ) < VELOCIDAD_VERTICAL_MINIMA) {
            velocidadZ = 0.0;
        }
        anguloAnimacion += (Math.abs(velocidadX) + Math.abs(velocidadY)) * 0.18;

        if (altura < 0.0) {
            altura = 0.0;
            if (Math.abs(velocidadZ) > VELOCIDAD_VERTICAL_MINIMA) {
                velocidadZ = -velocidadZ * REBOTE_VERTICAL;
            } else {
                velocidadZ = 0.0;
            }
        }

        // Rebote contra límites del panel.
        if (posX < 0) {
            posX = 0;
            velocidadX = -velocidadX * REBOTE;
        }
        if (posY < 0) {
            posY = 0;
            velocidadY = -velocidadY * REBOTE;
        }
        if (posX + ancho > anchoPanel) {
            posX = anchoPanel - ancho;
            velocidadX = -velocidadX * REBOTE;
        }
        if (posY + alto > altoPanel) {
            posY = altoPanel - alto;
            velocidadY = -velocidadY * REBOTE;
        }

        sincronizarPosicionEntera();
    }

    public void setPosicion(double nuevaX, double nuevaY) {
        posX = nuevaX;
        posY = nuevaY;
        sincronizarPosicionEntera();
    }

    public void detener() {
        // Util para reinicios/control de posesion.
        velocidadX = 0.0;
        velocidadY = 0.0;
        velocidadZ = 0.0;
        altura = 0.0;
    }

    public double getCentroX() {
        return posX + ancho / 2.0;
    }

    public double getCentroY() {
        return posY + alto / 2.0;
    }

    public double getVelocidadX() {
        return velocidadX;
    }

    public double getVelocidadY() {
        return velocidadY;
    }

    public double getVelocidadZ() {
        return velocidadZ;
    }

    public double getRapidez() {
        return Math.sqrt(velocidadX * velocidadX + velocidadY * velocidadY);
    }

    public double getAltura() {
        return altura;
    }

    public boolean estaEnAire() {
        return altura > 0.1 || Math.abs(velocidadZ) > VELOCIDAD_VERTICAL_MINIMA;
    }

    public boolean estaControlableEnPiso() {
        return altura <= 8.0 && Math.abs(velocidadZ) <= 1.0;
    }

    public int getYRender() {
        return (int) Math.round(y - altura);
    }

    public void fijarAltura(double nuevaAltura) {
        altura = Math.max(0.0, nuevaAltura);
    }

    public void fijarVelocidades(double nuevaVelocidadX, double nuevaVelocidadY, double nuevaVelocidadZ) {
        velocidadX = nuevaVelocidadX;
        velocidadY = nuevaVelocidadY;
        velocidadZ = nuevaVelocidadZ;
        limitarVelocidad();
    }

    public int getRadio() {
        return ancho / 2;
    }

    private void sincronizarPosicionEntera() {
        // La logica usa doubles; el render/collision usa enteros.
        x = (int) Math.round(posX);
        y = (int) Math.round(posY);
    }

    private void limitarVelocidad() {
        // Evita valores extremos no jugables.
        if (velocidadX > VELOCIDAD_MAXIMA) {
            velocidadX = VELOCIDAD_MAXIMA;
        } else if (velocidadX < -VELOCIDAD_MAXIMA) {
            velocidadX = -VELOCIDAD_MAXIMA;
        }

        if (velocidadY > VELOCIDAD_MAXIMA) {
            velocidadY = VELOCIDAD_MAXIMA;
        } else if (velocidadY < -VELOCIDAD_MAXIMA) {
            velocidadY = -VELOCIDAD_MAXIMA;
        }

        if (velocidadZ > VELOCIDAD_VERTICAL_MAXIMA) {
            velocidadZ = VELOCIDAD_VERTICAL_MAXIMA;
        } else if (velocidadZ < -VELOCIDAD_VERTICAL_MAXIMA) {
            velocidadZ = -VELOCIDAD_VERTICAL_MAXIMA;
        }
    }

    @Override
    public void dibujar(Graphics g) {
        // Balon estilo clasico: sombra en piso y altura visible.
        int yRender = getYRender();
        int sombraAncho = Math.max(6, ancho - (int) Math.round(altura * 0.10));
        int sombraAlto = Math.max(4, alto / 3 - (int) Math.round(altura * 0.03));
        int sombraX = x + (ancho - sombraAncho) / 2;
        int sombraY = y + alto - sombraAlto / 2;
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(sombraX, sombraY, sombraAncho, sombraAlto);

        g.setColor(new Color(238, 238, 238));
        g.fillOval(x, yRender, ancho, alto);
        g.setColor(new Color(25, 25, 25));
        g.drawOval(x, yRender, ancho, alto);

        int centroX = x + ancho / 2;
        int centroY = yRender + alto / 2;
        int panel = Math.max(3, ancho / 5);
        int oscilacionX = (int) Math.round(Math.cos(anguloAnimacion) * 2.0);
        int oscilacionY = (int) Math.round(Math.sin(anguloAnimacion) * 2.0);

        g.fillOval(centroX - panel / 2 + oscilacionX, centroY - panel / 2 + oscilacionY, panel, panel);
        g.drawLine(centroX, yRender + 2, centroX + oscilacionX, centroY - panel / 2);
        g.drawLine(centroX, centroY + panel / 2, centroX - oscilacionX, yRender + alto - 2);
        g.drawLine(x + 2, centroY, centroX - panel / 2 + oscilacionX, centroY + oscilacionY);
        g.drawLine(centroX + panel / 2, centroY, x + ancho - 2, centroY - oscilacionY);
    }
}
