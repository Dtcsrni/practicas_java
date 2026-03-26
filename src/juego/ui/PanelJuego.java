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

// Panel principal: recibe input, avanza la simulacion y delega el render.
public class PanelJuego extends JPanel implements KeyListener, ActionListener {
    private final Timer timer;
    private final EntradaJuego entrada;
    private final MaquinaEstadosJuego maquinaEstados;
    private final MotorJuego motor;
    private final RenderJuego renderizador;
    private final GestorSonido gestorSonido;

    public PanelJuego() {
        // Configuracion base del panel de juego.
        setPreferredSize(new Dimension(ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL));
        setFocusable(true);
        addKeyListener(this);

        // Dependencias del loop principal.
        entrada = new EntradaJuego();
        maquinaEstados = new MaquinaEstadosJuego();
        motor = new MotorJuego();
        renderizador = new RenderJuego();
        gestorSonido = new GestorSonido();

        // Swing Timer usado como game loop.
        timer = new Timer(1000 / ConfiguracionJuego.FPS, this);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Render completo del frame actual.
        renderizador.dibujarEscena(g, motor, maquinaEstados, motor.getFramesAnimacion());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // 1) Actualiza estados temporales de la UI.
        maquinaEstados.actualizar();

        // 2) Avanza la simulacion solo si el estado lo permite.
        if (maquinaEstados.permiteActualizarMundo()) {
            EventoJuego evento = motor.actualizar(entrada);
            maquinaEstados.procesarEventoJuego(evento);
            // Limpia arrastres de input tras eventos fuertes.
            if (evento != EventoJuego.NINGUNO) {
                entrada.limpiarMovimiento();
            }
        }
        reproducirSonidosPendientes();

        // 3) Solicita el repintado del frame.
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int codigo = e.getKeyCode();

        // ENTER inicia la partida desde el menu.
        if (codigo == KeyEvent.VK_ENTER) {
            maquinaEstados.iniciarDesdeInicio();
            gestorSonido.reproducir(TipoSonido.INICIO);
            return;
        }

        // R reinicia partido y estado visual.
        if (codigo == KeyEvent.VK_R) {
            motor.reiniciarPartido();
            maquinaEstados.reiniciar();
            entrada.limpiarMovimiento();
            gestorSonido.reproducir(TipoSonido.SAQUE);
            return;
        }

        // P pausa o reanuda.
        if (codigo == KeyEvent.VK_P) {
            maquinaEstados.alternarPausa();
            if (!maquinaEstados.permiteActualizarMundo()) {
                entrada.limpiarMovimiento();
            }
            return;
        }

        // El movimiento solo se procesa durante el partido.
        if (maquinaEstados.permiteActualizarMundo()) {
            entrada.procesarPresion(codigo);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Soltar tecla siempre limpia su bandera asociada.
        entrada.procesarLiberacion(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Swing exige este metodo, pero no se necesita.
    }

    private void reproducirSonidosPendientes() {
        TipoSonido sonido;
        while ((sonido = motor.consumirSonidoPendiente()) != null) {
            gestorSonido.reproducir(sonido);
        }
    }
}
