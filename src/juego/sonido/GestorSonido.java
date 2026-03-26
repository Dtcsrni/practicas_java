package juego.sonido;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

// Genera tonos sinteticos para no depender de archivos de audio externos.
public class GestorSonido {
    private static final float SAMPLE_RATE = 22050.0f;

    private final ExecutorService executor;

    public GestorSonido() {
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread hilo = new Thread(runnable, "gestor-sonido-juego");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    public void reproducir(TipoSonido tipoSonido) {
        executor.submit(() -> reproducirInterno(tipoSonido));
    }

    private void reproducirInterno(TipoSonido tipoSonido) {
        try {
            switch (tipoSonido) {
                case INICIO -> reproducirSecuencia(new Nota[] {
                    new Nota(392.0, 70, 0.35),
                    new Nota(523.25, 90, 0.35)
                });
                case PASE -> reproducirSecuencia(new Nota[] {
                    new Nota(420.0, 55, 0.30)
                });
                case TIRO -> reproducirSecuencia(new Nota[] {
                    new Nota(250.0, 40, 0.45),
                    new Nota(160.0, 70, 0.35)
                });
                case ROBO -> reproducirSecuencia(new Nota[] {
                    new Nota(510.0, 35, 0.28),
                    new Nota(320.0, 45, 0.24)
                });
                case SAQUE -> reproducirSecuencia(new Nota[] {
                    new Nota(610.0, 55, 0.26)
                });
                case GOL -> reproducirSecuencia(new Nota[] {
                    new Nota(392.0, 80, 0.35),
                    new Nota(523.25, 90, 0.35),
                    new Nota(659.25, 150, 0.38)
                });
                case VICTORIA -> reproducirSecuencia(new Nota[] {
                    new Nota(523.25, 70, 0.35),
                    new Nota(659.25, 70, 0.35),
                    new Nota(783.99, 140, 0.35)
                });
                case DERROTA -> reproducirSecuencia(new Nota[] {
                    new Nota(349.23, 80, 0.35),
                    new Nota(293.66, 100, 0.35),
                    new Nota(220.00, 140, 0.30)
                });
                default -> {
                }
            }
        } catch (LineUnavailableException ignored) {
            // El audio es opcional: un fallo aqui no debe romper el juego.
        }
    }

    private void reproducirSecuencia(Nota[] notas) throws LineUnavailableException {
        AudioFormat formato = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (SourceDataLine linea = AudioSystem.getSourceDataLine(formato)) {
            linea.open(formato, 4096);
            linea.start();
            for (Nota nota : notas) {
                escribirNota(linea, nota);
            }
            linea.drain();
        }
    }

    private void escribirNota(SourceDataLine linea, Nota nota) {
        int totalMuestras = (int) ((nota.duracionMs / 1000.0) * SAMPLE_RATE);
        byte[] buffer = new byte[totalMuestras * 2];
        double amplitud = 32767.0 * Math.max(0.0, Math.min(1.0, nota.volumen));

        for (int i = 0; i < totalMuestras; i++) {
            double tiempo = i / SAMPLE_RATE;
            double envolvente = calcularEnvolvente(i, totalMuestras);
            short muestra = (short) (Math.sin(2.0 * Math.PI * nota.frecuencia * tiempo) * amplitud * envolvente);
            buffer[i * 2] = (byte) (muestra & 0xff);
            buffer[i * 2 + 1] = (byte) ((muestra >> 8) & 0xff);
        }
        linea.write(buffer, 0, buffer.length);
    }

    private double calcularEnvolvente(int indice, int totalMuestras) {
        double progreso = totalMuestras <= 0 ? 1.0 : indice / (double) totalMuestras;
        if (progreso < 0.08) {
            return progreso / 0.08;
        }
        if (progreso > 0.84) {
            return Math.max(0.0, (1.0 - progreso) / 0.16);
        }
        return 1.0;
    }

    private record Nota(double frecuencia, int duracionMs, double volumen) {
    }
}
