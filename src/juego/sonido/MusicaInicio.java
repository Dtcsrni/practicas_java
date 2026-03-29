package juego.sonido;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.prefs.Preferences;

// Reproductor simple de loop musical para la pantalla de inicio.
public class MusicaInicio {
    private static MusicaInicio instancia;
    private static final float SAMPLE_RATE = 22050.0f;
    private volatile boolean running = false;
    private volatile boolean muted = false;
    private volatile double volumen = 0.64;
    private Thread hilo;
    private final Preferences prefs;

    private MusicaInicio() {
        prefs = Preferences.userNodeForPackage(MusicaInicio.class);
        // Cargar preferencias persistentes
        volumen = prefs.getDouble("musica.volumen", volumen);
        muted = prefs.getBoolean("musica.muted", muted);
    }

    public static synchronized MusicaInicio getInstancia() {
        if (instancia == null) {
            instancia = new MusicaInicio();
        }
        return instancia;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        hilo = new Thread(this::runLoop, "musica-inicio");
        hilo.setDaemon(true);
        hilo.start();
    }

    public synchronized void stop() {
        running = false;
        if (hilo != null) {
            hilo.interrupt();
            hilo = null;
        }
    }

    public boolean isPlaying() {
        return running;
    }

    public synchronized void toggleMuted() {
        muted = !muted;
        try { prefs.putBoolean("musica.muted", muted); } catch (Exception ignored) {}
    }

    public boolean isMuted() {
        return muted;
    }

    public synchronized void setVolumen(double v) {
        volumen = Math.max(0.0, Math.min(1.0, v));
        try { prefs.putDouble("musica.volumen", volumen); } catch (Exception ignored) {}
    }

    public double getVolumen() {
        return volumen;
    }

    public synchronized void setMuted(boolean m) {
        muted = m;
        try { prefs.putBoolean("musica.muted", muted); } catch (Exception ignored) {}
    }

    private void runLoop() {
        try {
            AudioFormat formato = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            try (SourceDataLine linea = AudioSystem.getSourceDataLine(formato)) {
                linea.open(formato, 4096);
                linea.start();
                byte[] loop = generarLoop();
                while (running) {
                    if (muted) {
                        try { Thread.sleep(120); } catch (InterruptedException e) { break; }
                        continue;
                    }
                    linea.write(loop, 0, loop.length);
                }
                linea.drain();
            }
        } catch (LineUnavailableException ignored) {
        } finally {
            running = false;
        }
    }

    // Genera un loop musical sencillo (mono, 16-bit little endian).
    private byte[] generarLoop() {
        int duracionSeg = 8;
        int samples = (int) (SAMPLE_RATE * duracionSeg);
        double[] mezcla = new double[samples];

        // Base de acordes (C - G - Am - F) en registro medio
        double[] roots = {130.8128, 97.9989, 110.0, 87.3071};
        int bars = roots.length;
        int barSamples = samples / bars;

        for (int b = 0; b < bars; b++) {
            double root = roots[b];
            int start = b * barSamples;
            int end = (b == bars - 1) ? samples : start + barSamples;
            for (int i = start; i < end; i++) {
                double t = i / SAMPLE_RATE;
                double posInBar = (i - start) / (double) Math.max(1, (end - start));
                // Tres voces: bajo (octava baja), pad (octava media), brillo (quinta/octava)
                double bajo = Math.sin(2.0 * Math.PI * (root * 0.5) * t) * 0.14;
                double pad = Math.sin(2.0 * Math.PI * (root) * t) * 0.12;
                double brillo = Math.sin(2.0 * Math.PI * (root * 1.5) * t) * 0.07;
                // Arpegio sutil
                double arp = Math.sin(2.0 * Math.PI * (root * (1.0 + 0.25 * Math.sin(t * 3.0))) * t) * 0.03 * (Math.sin(posInBar * Math.PI));
                double env = 0.85;
                // Suavizado entre barras
                double cross = 1.0;
                double edge = 0.06;
                if (posInBar < edge) cross = posInBar / edge; else if (posInBar > 1.0 - edge) cross = (1.0 - posInBar) / edge;
                mezcla[i] += (bajo + pad + brillo + arp) * env * Math.max(0.0, cross);
            }
        }

        // Normalizar
        double max = 0.0;
        for (double v : mezcla) max = Math.max(max, Math.abs(v));
        double escala = max > 0.0 ? (0.95 / max) : 0.0;

        byte[] audio = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double val = mezcla[i] * escala * volumen;
            val = Math.max(-1.0, Math.min(1.0, val));
            short muestra = (short) Math.round(val * 32767.0);
            audio[i * 2] = (byte) (muestra & 0xff);
            audio[i * 2 + 1] = (byte) ((muestra >> 8) & 0xff);
        }
        return audio;
    }
}
