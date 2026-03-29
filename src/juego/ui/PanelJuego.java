package juego.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import juego.core.ConfiguracionJuego;
import juego.core.EntradaJuego;
import juego.core.EventoJuego;
import juego.core.MaquinaEstadosJuego;
import juego.core.MotorJuego;
import juego.core.EstadoJuego;
import juego.sonido.GestorSonido;
import juego.sonido.SonidoEvento;
import juego.sonido.TipoSonido;
import juego.sonido.MusicaInicio;

import java.awt.Color;

// Panel principal: recibe input, avanza la simulacion y delega el render.
public class PanelJuego extends JPanel implements KeyListener, java.awt.event.MouseListener, java.awt.event.MouseMotionListener {
    private static final long FRAME_NANOS = 1_000_000_000L / ConfiguracionJuego.FPS;
    private final Object frameLock;
    private final ScheduledExecutorService loopExecutor;
    private final EntradaJuego entrada;
    private final MaquinaEstadosJuego maquinaEstados;
    private final MotorJuego motor;
    private final RenderJuego renderizador;
    private final GestorSonido gestorSonido;
    private final MusicaInicio musicaInicio;
    private volatile long ultimoTickNanos;
    // Estado de interacción con el slider de volumen
    private boolean draggingVol = false;

    public PanelJuego() {
        // Configuracion base del panel de juego.
        setPreferredSize(new Dimension(ConfiguracionJuego.ANCHO_PANEL, ConfiguracionJuego.ALTO_PANEL));
        setBackground(new Color(6, 12, 20));
        setDoubleBuffered(true);
        setFocusable(true);
        addKeyListener(this);
        frameLock = new Object();

        // Dependencias del loop principal.
        entrada = new EntradaJuego();
        maquinaEstados = new MaquinaEstadosJuego();
        motor = new MotorJuego();
        renderizador = new RenderJuego();
        gestorSonido = new GestorSonido();
        musicaInicio = MusicaInicio.getInstancia();
        ultimoTickNanos = System.nanoTime();

        // Registrar listeners de mouse para controles de UI (p. ej. musica)
        addMouseListener(this);
        addMouseMotionListener(this);

        ThreadFactory fabrica = runnable -> {
            Thread hilo = new Thread(runnable, "loop-juego");
            hilo.setDaemon(true);
            return hilo;
        };
        loopExecutor = Executors.newSingleThreadScheduledExecutor(fabrica);
        loopExecutor.scheduleWithFixedDelay(this::tickJuego, 0, Math.max(1, 1000 / ConfiguracionJuego.FPS), TimeUnit.MILLISECONDS);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        synchronized (frameLock) {
            // Ajustar escala uniforme para que la escena se adapte al tamaño
            int panelW = getWidth();
            int panelH = getHeight();
            double scaleX = panelW / (double) ConfiguracionJuego.ANCHO_PANEL;
            double scaleY = panelH / (double) ConfiguracionJuego.ALTO_PANEL;
            double scale = Math.min(scaleX, scaleY);

            Graphics g2 = g.create();
            try {
                // Centrar la escena escalada dentro del panel
                int tx = (int) Math.round((panelW - ConfiguracionJuego.ANCHO_PANEL * scale) / 2.0);
                int ty = (int) Math.round((panelH - ConfiguracionJuego.ALTO_PANEL * scale) / 2.0);
                java.awt.Graphics2D g2d = (java.awt.Graphics2D) g2;
                g2d.translate(tx, ty);
                g2d.scale(scale, scale);
                renderizador.dibujarEscena(g2d, motor, maquinaEstados, entrada, motor.getFramesAnimacion());
            } finally {
                g2.dispose();
            }
        }
    }

    private void tickJuego() {
        long ahora = System.nanoTime();
        if (ahora - ultimoTickNanos < FRAME_NANOS) {
            return;
        }
        ultimoTickNanos = ahora;
        synchronized (frameLock) {
            maquinaEstados.actualizar();
            // Gestion simple de musica de pantalla de inicio
            EstadoJuego estado = maquinaEstados.getEstadoActual();
            if (estado == EstadoJuego.INICIO) {
                if (!musicaInicio.isPlaying()) {
                    musicaInicio.start();
                }
            } else {
                if (musicaInicio.isPlaying()) {
                    musicaInicio.stop();
                }
            }
            if (maquinaEstados.permiteActualizarMundo()) {
                EventoJuego evento = motor.actualizar(entrada);
                maquinaEstados.procesarEventoJuego(evento);
                if (evento != EventoJuego.NINGUNO) {
                    entrada.limpiarMovimiento();
                }
            }
            reproducirSonidosPendientes();
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int codigo = e.getKeyCode();

        if (manejarTeclaGlobal(codigo)) {
            return;
        }

        // El movimiento solo se procesa durante el partido.
        synchronized (frameLock) {
            if (maquinaEstados.permiteActualizarMundo() && !motor.isModoEspectador()) {
                entrada.procesarPresion(codigo);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Soltar tecla siempre limpia su bandera asociada.
        synchronized (frameLock) {
            entrada.procesarLiberacion(e.getKeyCode());
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Swing exige este metodo, pero no se necesita.
    }

    private void reproducirSonidosPendientes() {
        SonidoEvento sonido;
        while ((sonido = motor.consumirSonidoPendiente()) != null) {
            gestorSonido.reproducir(sonido);
        }
    }

    private java.awt.Point pantallaAGame(int mx, int my) {
        int panelW = getWidth();
        int panelH = getHeight();
        double scaleX = panelW / (double) ConfiguracionJuego.ANCHO_PANEL;
        double scaleY = panelH / (double) ConfiguracionJuego.ALTO_PANEL;
        double scale = Math.min(scaleX, scaleY);
        int tx = (int) Math.round((panelW - ConfiguracionJuego.ANCHO_PANEL * scale) / 2.0);
        int ty = (int) Math.round((panelH - ConfiguracionJuego.ALTO_PANEL * scale) / 2.0);
        int gx = (int) Math.round((mx - tx) / scale);
        int gy = (int) Math.round((my - ty) / scale);
        return new java.awt.Point(gx, gy);
    }

    private boolean manejarTeclaGlobal(int codigo) {
        // Atajos globales disponibles sin importar direccion de movimiento.
        // ENTER inicia la partida desde el menu.
        synchronized (frameLock) {
            if (codigo == KeyEvent.VK_ENTER) {
                motor.setModoEspectador(maquinaEstados.isInicioModoEspectador());
                maquinaEstados.iniciarDesdeInicio();
                gestorSonido.reproducir(TipoSonido.INICIO);
                return true;
            }
            if (codigo == KeyEvent.VK_LEFT || codigo == KeyEvent.VK_RIGHT || codigo == KeyEvent.VK_TAB) {
                maquinaEstados.alternarModoInicio();
                return true;
            }
            // R reinicia partido y estado visual.
            if (codigo == KeyEvent.VK_R) {
                motor.reiniciarPartido();
                maquinaEstados.reiniciar();
                entrada.limpiarMovimiento();
                gestorSonido.reproducir(TipoSonido.SAQUE);
                return true;
            }
            // P pausa o reanuda.
            if (codigo == KeyEvent.VK_P) {
                maquinaEstados.alternarPausa();
                if (!maquinaEstados.permiteActualizarMundo()) {
                    entrada.limpiarMovimiento();
                }
                return true;
            }
            // F2 alterna modo espectador/jugador.
            if (codigo == KeyEvent.VK_F2) {
                motor.alternarModoEspectador();
                entrada.limpiarMovimiento();
                return true;
            }
            if (codigo == KeyEvent.VK_M) {
                musicaInicio.toggleMuted();
                gestorSonido.reproducir(TipoSonido.SAQUE);
                return true;
            }
            // F10/F9: ajustar volumen de musica
            if (codigo == KeyEvent.VK_F10) {
                double v = MusicaInicio.getInstancia().getVolumen();
                MusicaInicio.getInstancia().setVolumen(Math.min(1.0, v + 0.05));
                gestorSonido.reproducir(TipoSonido.SAQUE);
                return true;
            }
            if (codigo == KeyEvent.VK_F9) {
                double v = MusicaInicio.getInstancia().getVolumen();
                MusicaInicio.getInstancia().setVolumen(Math.max(0.0, v - 0.05));
                gestorSonido.reproducir(TipoSonido.SAQUE);
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {
        synchronized (frameLock) {
            if (maquinaEstados.getEstadoActual() != EstadoJuego.INICIO) {
                return;
            }
            java.awt.Point p = pantallaAGame(e.getX(), e.getY());
            java.awt.Rectangle rect = RenderJuego.getMusicControlRect();
            if (rect.contains(p)) {
                MusicaInicio mi = MusicaInicio.getInstancia();
                if (mi.isPlaying()) {
                    mi.toggleMuted();
                } else {
                    mi.start();
                    mi.setVolumen(0.64);
                }
                gestorSonido.reproducir(TipoSonido.SAQUE);
            }
        }
    }

    @Override
    public void mouseMoved(java.awt.event.MouseEvent e) {
        java.awt.Point p = pantallaAGame(e.getX(), e.getY());
        boolean overMusic = maquinaEstados.getEstadoActual() == EstadoJuego.INICIO && (RenderJuego.getMusicControlRect().contains(p) || RenderJuego.getMusicSliderRect().contains(p));
        if (overMusic) {
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        } else {
            setCursor(java.awt.Cursor.getDefaultCursor());
        }
        // Notificar al renderizador si el slider está en hover para efectos visuales
        renderizador.setSliderHover(overMusic);
    }

    @Override
    public void mousePressed(java.awt.event.MouseEvent e) {
        synchronized (frameLock) {
            if (maquinaEstados.getEstadoActual() != EstadoJuego.INICIO) return;
            java.awt.Point p = pantallaAGame(e.getX(), e.getY());
            java.awt.Rectangle slider = RenderJuego.getMusicSliderRect();
            if (slider.contains(p)) {
                draggingVol = true;
                MusicaInicio mi = MusicaInicio.getInstancia();
                if (!mi.isPlaying()) mi.start();
                // notificar visual activo
                renderizador.setSliderActive(true);
                double v = (p.x - slider.x) / (double) slider.width;
                mi.setVolumen(v);
                gestorSonido.reproducir(TipoSonido.SAQUE);
            }
        }
    }

    @Override
    public void mouseReleased(java.awt.event.MouseEvent e) {
        synchronized (frameLock) {
            if (draggingVol) {
                draggingVol = false;
                renderizador.setSliderActive(false);
            }
        }
    }

    @Override
    public void mouseDragged(java.awt.event.MouseEvent e) {
        synchronized (frameLock) {
            if (!draggingVol) return;
            java.awt.Point p = pantallaAGame(e.getX(), e.getY());
            java.awt.Rectangle slider = RenderJuego.getMusicSliderRect();
            double v = (p.x - slider.x) / (double) slider.width;
            MusicaInicio.getInstancia().setVolumen(v);
            // asegurar que render muestre estado activo mientras se arrastra
            renderizador.setSliderActive(true);
        }
    }

    // Unused mouse events
    @Override public void mouseEntered(java.awt.event.MouseEvent e) {}
    @Override public void mouseExited(java.awt.event.MouseEvent e) {}

    @Override
    public void removeNotify() {
        loopExecutor.shutdownNow();
        super.removeNotify();
    }
}
