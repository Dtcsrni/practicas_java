import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Action;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.security.Key;

import javax.swing.Timer;

public class PanelJuego extends JPanel implements KeyListener, ActionListener {
    // Constantes del panel
    public static final int ANCHO = 640;
    public static final int ALTO = 320;
    // El panel tiene un jugador. Objeto jugador
    private Jugador jugador;
    private Moneda moneda; // Para mostrar como se pueden agregar mas objetos al juego
    // Timer de swing para actualizar el juego periódicamente
    private Timer timer;
    // variables booleanas para saber qué teclas estan presionadas
    private boolean arriba;
    private boolean abajo;
    private boolean izquierda;
    private boolean derecha;

    // Variables de progreso del juego
    private int puntos;
    private int meta;
    private boolean juegoGanado;

    // Constructor que define las caracteristicas del panel
    public PanelJuego() {
        // Tamaño del panel
        setPreferredSize(new Dimension(ANCHO, ALTO));
        // COlor del fondo inicial
        setBackground(Color.BLACK);
        // Permite que el panel reciba eventos de teclado
        setFocusable(true);
        // Registrar el escuchador del teclado en este mismo panel
        addKeyListener(this);
        // Creamos al jugador con una posicion inicial
        jugador = new Jugador(100, 100, 30, 30, 5);

        // Crear la moneda
        moneda = new Moneda(ANCHO, ALTO, 20);

        // Inicializamos variables de puntaje
        puntos = 0;
        meta = 5;
        juegoGanado = false;

        // Creamos un timer que se ejecuta cada 16 ms aprox (60FPS)
        timer = new Timer(16, this);
        timer.start();
    }

    // Método que actualiza el estado del juego
    public void actualizar() {
        // Movimiento vertical
        if (arriba) {
            jugador.mover(0, -jugador.getVelocidad());
        }
        if (abajo) {
            jugador.mover(0, jugador.getVelocidad());
        }
        if (izquierda) {
            jugador.mover(-jugador.getVelocidad(), 0);
        }
        if (derecha) {
            jugador.mover(jugador.getVelocidad(), 0);
        }

        // Evitar que el jugador salga de los límites del panel
        if (jugador.getX() < 0) {
            jugador.setX(0);
        }
        if (jugador.getY() < 0) {
            jugador.setY(0);
        }
        if (jugador.getX() + jugador.getAncho() > ANCHO) {
            jugador.setX(ANCHO - jugador.getAncho());
        }
        if (jugador.getY() + jugador.getAlto() > ALTO) {
            jugador.setY(ALTO - jugador.getAlto());
        }

        // Detectar colisión con la moneda
        if(jugador.getBounds().intersects(moneda.getBounds())) {
            puntos++;
            moneda.reposicionar(ANCHO, ALTO);
        }

        // Detectar condición de victoria
        if(puntos >= meta) {
            juegoGanado = true;
            timer.stop(); // Detener el juego
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        //Dibujamos un fondo sencillo tipo cancha
        g.setColor(new Color(0, 255, 0));
        g.fillRect(0,0, ANCHO, ALTO);
        //Dibujamos líneas de referencia para que el movimiento se note mejor
        g.setColor(new Color(50, 150, 50));
        for(int i = 0; i < ANCHO; i+=40){
            g.drawLine(i, 0, i, ALTO);
        }
        for(int i = 0; i < ALTO; i+=40){
            g.drawLine(0, i, ANCHO, i);
        }
        //Dibujamos al jugador mediante su propio método
        jugador.dibujar(g);
        moneda.dibujar(g);

        if(juegoGanado)
        {
            g.setColor(Color.WHITE);
            g.drawString("¡Has ganado!", ANCHO/2 - 30, ALTO/2);
        }
        //Texto de apoyo
        g.setColor(Color.WHITE);
        g.drawString("Mover W A S D o flechas", 20, 20);
        g.drawString("Sesion mínima usable con POO", 20, 40);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int codigo = e.getKeyCode();

        if (codigo == KeyEvent.VK_W || codigo == KeyEvent.VK_UP) {
            arriba = true;
        }
        if (codigo == KeyEvent.VK_S || codigo == KeyEvent.VK_DOWN) {
            abajo = true;
        }
        if (codigo == KeyEvent.VK_A || codigo == KeyEvent.VK_LEFT) {
            izquierda = true;
        }
        if (codigo == KeyEvent.VK_D || codigo == KeyEvent.VK_RIGHT) {
            derecha = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int codigo = e.getKeyCode();
        if (codigo == KeyEvent.VK_W || codigo == KeyEvent.VK_UP) {
            arriba = false;
        }
        if (codigo == KeyEvent.VK_S || codigo == KeyEvent.VK_DOWN) {
            abajo = false;
        }
        if (codigo == KeyEvent.VK_A || codigo == KeyEvent.VK_LEFT) {
            izquierda = false;
        }
        if (codigo == KeyEvent.VK_D || codigo == KeyEvent.VK_RIGHT) {
            derecha = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No se utiliza en este ejemplo
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Cada vez que el timer se dispara:
        // 1. Actualizamos estado
        // 2. redibujamos
        actualizar();
        repaint();
    }
}
