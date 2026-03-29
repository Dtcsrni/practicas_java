package juego.entidades;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.RenderingHints;

// Balon con fisica 2D sobre el piso y una componente vertical simple.
public class Balon extends EntidadJuego {
    // La fisica del balon separa posicion horizontal y altura para simular
    // piso, rebote y "globos" sin usar un motor fisico completo.
    private double posX;
    private double posY;
    private double velocidadX;
    private double velocidadY;
    private double altura;
    private double velocidadZ;

    private static final double FRICCION_SUELO = 0.978;
    private static final double FRICCION_AIRE = 0.994;
    private static final double VELOCIDAD_MINIMA = 0.025;
    private static final double REBOTE = 0.74;
    private static final double VELOCIDAD_MAXIMA = 9.5;
    private static final double GRAVEDAD = 0.36;
    private static final double REBOTE_VERTICAL = 0.52;
    private static final double VELOCIDAD_VERTICAL_MINIMA = 0.18;
    private static final double VELOCIDAD_VERTICAL_MAXIMA = 11.5;
    private static final double RESISTENCIA_RODADO = 0.010;
    private static final double ACOPLE_REBOTE_HORIZONTAL = 0.05;
    private double anguloAnimacion;

    public Balon(int x, int y, int tamano) {
        super(x, y, tamano, tamano);
        // La posicion real vive en double para no perder suavidad.
        posX = x;
        posY = y;
        velocidadX = 0.0;
        velocidadY = 0.0;
        altura = 0.0;
        velocidadZ = 0.0;
        anguloAnimacion = 0.0;
    }

    public void reiniciarEnCentro(int anchoPanel, int altoPanel) {
        // Vuelve al centro y elimina cualquier movimiento residual.
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
        // Aplica impulso horizontal y, opcionalmente, elevacion.
        velocidadX += impulsoX;
        velocidadY += impulsoY;
        velocidadZ += impulsoZ;
        anguloAnimacion += (impulsoX - impulsoY) * 0.08;
        limitarVelocidad();
    }

    public void actualizarFisica(int anchoPanel, int altoPanel) {
        // Integra movimiento horizontal y altura del balon.
        velocidadZ -= GRAVEDAD;
        posX += velocidadX;
        posY += velocidadY;
        altura += velocidadZ;

        boolean enAire = altura > 0.0 || Math.abs(velocidadZ) > VELOCIDAD_VERTICAL_MINIMA;
        double factorFriccion = enAire ? FRICCION_AIRE : FRICCION_SUELO;
        velocidadX *= factorFriccion;
        velocidadY *= factorFriccion;

        // La pelota pierde impulso extra al rodar sobre el cesped.
        if (!enAire) {
            aplicarResistenciaRodado();
        }

        if (altura < 0.0) {
            altura = 0.0;
            // Cuando toca piso, parte de la energia vertical se conserva como rebote.
            if (Math.abs(velocidadZ) > VELOCIDAD_VERTICAL_MINIMA) {
                double impacto = Math.abs(velocidadZ);
                velocidadZ = impacto * REBOTE_VERTICAL;
                // Pequeño acople para que los rebotes cambien levemente la trayectoria.
                velocidadX += (velocidadX >= 0 ? 1 : -1) * impacto * ACOPLE_REBOTE_HORIZONTAL;
                velocidadY += (velocidadY >= 0 ? 1 : -1) * impacto * ACOPLE_REBOTE_HORIZONTAL;
            } else {
                velocidadZ = 0.0;
            }
        }

        // Rebota contra los bordes externos del panel con perdida adicional por choque.
        if (posX < 0) {
            posX = 0;
            velocidadX = -velocidadX * REBOTE;
            velocidadY *= 0.94;
        }
        if (posY < 0) {
            posY = 0;
            velocidadY = -velocidadY * REBOTE;
            velocidadX *= 0.94;
        }
        if (posX + ancho > anchoPanel) {
            posX = anchoPanel - ancho;
            velocidadX = -velocidadX * REBOTE;
            velocidadY *= 0.94;
        }
        if (posY + alto > altoPanel) {
            posY = altoPanel - alto;
            velocidadY = -velocidadY * REBOTE;
            velocidadX *= 0.94;
        }

        if (Math.abs(velocidadX) < VELOCIDAD_MINIMA) {
            velocidadX = 0.0;
        }
        if (Math.abs(velocidadY) < VELOCIDAD_MINIMA) {
            velocidadY = 0.0;
        }
        if (altura <= 0.0 && Math.abs(velocidadZ) < VELOCIDAD_VERTICAL_MINIMA) {
            velocidadZ = 0.0;
        }

        anguloAnimacion += (Math.abs(velocidadX) + Math.abs(velocidadY)) * (enAire ? 0.11 : 0.18);
        limitarVelocidad();

        sincronizarPosicionEntera();
    }

    private void aplicarResistenciaRodado() {
        if (velocidadX > 0) {
            velocidadX = Math.max(0.0, velocidadX - RESISTENCIA_RODADO);
        } else if (velocidadX < 0) {
            velocidadX = Math.min(0.0, velocidadX + RESISTENCIA_RODADO);
        }
        if (velocidadY > 0) {
            velocidadY = Math.max(0.0, velocidadY - RESISTENCIA_RODADO);
        } else if (velocidadY < 0) {
            velocidadY = Math.min(0.0, velocidadY + RESISTENCIA_RODADO);
        }
    }

    public void setPosicion(double nuevaX, double nuevaY) {
        posX = nuevaX;
        posY = nuevaY;
        sincronizarPosicionEntera();
    }

    public void detener() {
        // Deja el balon completamente quieto y en el piso.
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

    public int getIndiceSpriteFrame() {
        if (altura > 10.0) {
            return 7;
        }
        if (altura > 3.0) {
            return 6;
        }
        return Math.floorMod((int) Math.round(anguloAnimacion * 2.6), 6);
    }

    public boolean isSpriteEnAire() {
        return altura > 2.0 || Math.abs(velocidadZ) > VELOCIDAD_VERTICAL_MINIMA;
    }

    public boolean estaEnAire() {
        return altura > 0.1 || Math.abs(velocidadZ) > VELOCIDAD_VERTICAL_MINIMA;
    }

    public boolean estaControlableEnPiso() {
        // Este umbral se usa en MotorJuego para decidir si un jugador puede "domar" la pelota.
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

    public void animarConduccion(double deltaX, double deltaY, boolean enManos) {
        double rapidezVisual = Math.hypot(deltaX, deltaY);
        if (enManos) {
            altura = 0.0;
            velocidadX = 0.0;
            velocidadY = 0.0;
            velocidadZ = 0.0;
            anguloAnimacion += 0.08;
            return;
        }

        velocidadX = deltaX * 0.22;
        velocidadY = deltaY * 0.22;
        velocidadZ = 0.0;
        anguloAnimacion += Math.max(0.14, rapidezVisual * 0.24);
        if (rapidezVisual > 0.30) {
            altura = Math.max(0.0, 0.35 + Math.abs(Math.sin(anguloAnimacion * 0.85)) * Math.min(1.4, rapidezVisual * 0.22));
        } else {
            altura = 0.0;
        }
    }

    public int getRadio() {
        return ancho / 2;
    }

    @Override
    public Rectangle getBounds() {
        // Caja mas ajustada al balon para reducir contactos "fantasma" en bordes.
        int margen = Math.max(2, (int) Math.round(ancho * 0.22));
        return boundsConMargen(margen, margen);
    }

    private void sincronizarPosicionEntera() {
        // La simulacion usa doubles; render y colisiones usan enteros.
        x = (int) Math.round(posX);
        y = (int) Math.round(posY);
    }

    private void limitarVelocidad() {
        // Limita velocidades a un rango estable para el gameplay.
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
        // Sombra en piso + balon con volumen, brillo y paneles mas legibles.
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        int yRender = getYRender();
        double rapidez = Math.hypot(velocidadX, velocidadY);
        int sombraAncho = Math.max(6, ancho + 2 - (int) Math.round(altura * 0.10));
        int sombraAlto = Math.max(4, alto / 3 - (int) Math.round(altura * 0.03));
        int sombraX = x + (ancho - sombraAncho) / 2;
        int sombraY = y + alto - sombraAlto / 2;
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(sombraX, sombraY, sombraAncho, sombraAlto);
        if (altura > 3.0) {
            g2.setColor(new Color(0, 0, 0, 28));
            g2.fillOval(sombraX + 2, sombraY + 1, Math.max(4, sombraAncho - 4), Math.max(3, sombraAlto - 1));
        }
        if (rapidez > 2.4) {
            int trail = Math.min(18, (int) Math.round(rapidez * 2.2));
            int trailX = (int) Math.round(x - velocidadX * 2.4);
            int trailY = (int) Math.round(yRender - velocidadY * 2.4);
            g2.setColor(new Color(255, 248, 226, 72));
            g2.fillOval(trailX, trailY, Math.max(8, ancho - trail), Math.max(8, alto - trail));
        }

        // Suave degradado radial para dar volumen, con foco ligeramente desplazado.
        float[] stops = { 0.0f, 0.6f, 1.0f };
        Color[] grads = new Color[] {
            new Color(250, 250, 250, 255),
            new Color(235, 235, 238, 255),
            new Color(170, 174, 180, 255)
        };
        g2.setPaint(new RadialGradientPaint(
            (float) (x + ancho * 0.35),
            (float) (yRender + alto * 0.30),
            Math.max(8.0f, ancho * 0.60f),
            stops,
            grads
        ));
        g2.fillOval(x, yRender, ancho, alto);

        // Contornos más legibles: borde exterior oscuro y sutil borde interior claro.
        java.awt.Stroke previo = g2.getStroke();
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(28, 30, 36, 220));
        g2.drawOval(x, yRender, ancho, alto);
        g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 255, 255, 90));
        g2.drawOval(x + 1, yRender + 1, ancho - 2, alto - 2);
        g2.setStroke(previo);

        int centroX = x + ancho / 2;
        int centroY = yRender + alto / 2;
        int panel = Math.max(6, ancho / 4);
        int oscilacionX = (int) Math.round(Math.cos(anguloAnimacion) * 2.2);
        int oscilacionY = (int) Math.round(Math.sin(anguloAnimacion) * 2.2);
        // Paneles: centro y ejes mejor definidos, con borde para contraste.
        Color panelFill = new Color(34, 36, 42, 230);
        Color panelEdge = new Color(12, 14, 18, 200);
        g2.setColor(panelFill);
        g2.fillOval(centroX - panel / 2 + oscilacionX, centroY - panel / 2 + oscilacionY, panel, panel);
        g2.setColor(panelEdge);
        g2.drawOval(centroX - panel / 2 + oscilacionX, centroY - panel / 2 + oscilacionY, panel, panel);

        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(18, 20, 24, 180));
        g2.drawLine(centroX, yRender + 4, centroX + oscilacionX, centroY - panel / 2);
        g2.drawLine(centroX, centroY + panel / 2, centroX - oscilacionX, yRender + alto - 4);
        g2.drawLine(x + 4, centroY, centroX - panel / 2 + oscilacionX, centroY + oscilacionY);
        g2.drawLine(centroX + panel / 2, centroY, x + ancho - 4, centroY - oscilacionY);
        g2.setStroke(previo);

        int panelExtra = Math.max(3, panel - 1);
        g2.setColor(new Color(40, 42, 48, 220));
        g2.fillOval(x + 3 + faseSigno(anguloAnimacion), yRender + alto / 2 - panelExtra / 2, panelExtra, panelExtra);
        g2.fillOval(x + ancho - panelExtra - 3 - faseSigno(anguloAnimacion), yRender + alto / 2 - panelExtra / 2, panelExtra, panelExtra);
        g2.fillOval(centroX - panelExtra / 2, yRender + 2 + faseSigno(anguloAnimacion + Math.PI / 2.0), panelExtra, panelExtra);
        g2.fillOval(centroX - panelExtra / 2, yRender + alto - panelExtra - 2 - faseSigno(anguloAnimacion + Math.PI / 2.0), panelExtra, panelExtra);

        // Arcos decorativos, menos intrusivos.
        g2.setColor(new Color(24, 24, 28, 120));
        int fase = (int) Math.round((anguloAnimacion * 10.0) % 360.0);
        g2.drawArc(x + 4, yRender + 5, ancho - 8, alto - 10, fase, 140);
        g2.drawArc(x + 6, yRender + 7, ancho - 12, alto - 14, fase + 170, 120);

        // Brillo y reflejos: pequeño highlight con degradado para no cubrir paneles.
        g2.setPaint(new GradientPaint(x + ancho * 0.12f, yRender + alto * 0.12f, new Color(255, 255, 255, 220), x + ancho * 0.32f, yRender + alto * 0.32f, new Color(255, 255, 255, 0)));
        g2.fillOval(x + ancho / 6, yRender + alto / 8, Math.max(6, ancho / 4), Math.max(4, alto / 6));

        if (altura > 1.5) {
            g2.setColor(new Color(200, 220, 255, 48));
            g2.fillOval(x - 1, yRender - 1, ancho + 2, alto + 2);
        }
        if (rapidez > 3.2) {
            g2.setColor(new Color(255, 244, 190, 64));
            g2.drawOval(x - 1, yRender - 1, ancho + 2, alto + 2);
        }

        // Pequeño punto especular cerca del borde superior
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillOval(x + 3, yRender + 3, Math.max(4, ancho / 6), Math.max(3, alto / 8));
        g2.dispose();
    }

    private int faseSigno(double valor) {
        return Math.sin(valor) >= 0 ? 1 : -1;
    }
}
