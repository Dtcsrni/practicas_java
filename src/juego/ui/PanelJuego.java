package juego.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JPanel;
import javax.swing.Timer;

import juego.core.ConfiguracionJuego;
import juego.core.EntradaJuego;
import juego.core.EventoJuego;
import juego.core.MaquinaEstadosJuego;
import juego.core.MotorJuego;
import juego.sonido.GestorSonido;
import juego.sonido.TipoSonido;

// Orquestador de la capa UI:
// - recibe teclado
// - avanza logica por timer
// - delega render
public class PanelJuego extends JPanel implements KeyListener, ActionListener {
    private final Timer timer;
    private final EntradaJuego entrada;
    private final MaquinaEstadosJuego maquinaEstados;
    private final MotorJuego motor;
    private final RenderJuego renderizador;
    private final GestorSonido gestorSonido;

    public PanelJuego() {
        // Configuracion visual del panel.
        setPreferredSize(new Dimension(ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL));
        setFocusable(true);
        addKeyListener(this);

        // Dependencias principales del loop.
        entrada = new EntradaJuego();
        maquinaEstados = new MaquinaEstadosJuego();
        motor = new MotorJuego();
        renderizador = new RenderJuego();
        gestorSonido = new GestorSonido();

        // Timer de Swing que simula el "game loop".
        timer = new Timer(1000 / ConfiguracionJuego.FPS, this);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Dibuja toda la escena según estado actual.
        renderizador.dibujarEscena(g, motor, maquinaEstados, motor.getFramesAnimacion());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // 1) Actualiza transiciones de estado.
        maquinaEstados.actualizar();

        // 2) Si el estado lo permite, avanza simulacion.
        if (maquinaEstados.permiteActualizarMundo()) {
            EventoJuego evento = motor.actualizar(entrada);
            maquinaEstados.procesarEventoJuego(evento);
            // Limpia input en eventos para evitar arrastre no deseado.
            if (evento != EventoJuego.NINGUNO) {
                entrada.limpiarMovimiento();
            }
        }
        reproducirSonidosPendientes();

        // 3) Solicita redibujado.
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int codigo = e.getKeyCode();

        // Enter inicia la partida desde menu.
        if (codigo == KeyEvent.VK_ENTER) {
            maquinaEstados.iniciarDesdeInicio();
            gestorSonido.reproducir(TipoSonido.INICIO);
            return;
        }

        // R reinicia logica y estado visual.
        if (codigo == KeyEvent.VK_R) {
            motor.reiniciarPartido();
            maquinaEstados.reiniciar();
            entrada.limpiarMovimiento();
            gestorSonido.reproducir(TipoSonido.SAQUE);
            return;
        }

        // P alterna pausa.
        if (codigo == KeyEvent.VK_P) {
            maquinaEstados.alternarPausa();
            if (!maquinaEstados.permiteActualizarMundo()) {
                entrada.limpiarMovimiento();
            }
            return;
        }

        // Solo captura movimiento si estamos en estado jugable.
        if (maquinaEstados.permiteActualizarMundo()) {
            entrada.procesarPresion(codigo);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Sin condiciones de estado: siempre soltar tecla limpia bandera.
        entrada.procesarLiberacion(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No se utiliza.
    }

    private void reproducirSonidosPendientes() {
        TipoSonido sonido;
        while ((sonido = motor.consumirSonidoPendiente()) != null) {
            gestorSonido.reproducir(sonido);
        }
    }
}
