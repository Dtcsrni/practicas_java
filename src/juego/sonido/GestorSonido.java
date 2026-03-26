package juego.sonido;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

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
                case INICIO -> {
                    reproducirSilbatazo(1420.0, 1620.0, 140, 0.30);
                    reproducirSecuencia(new Nota[] {
                        new Nota(392.0, 70, 0.34),
                        new Nota(493.88, 80, 0.34),
                        new Nota(523.25, 90, 0.35),
                        new Nota(659.25, 110, 0.33)
                    });
                }
                case PASE -> {
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        reproducirSecuencia(new Nota[] {
                            new Nota(440.0, 48, 0.25)
                        });
                    } else {
                        reproducirSecuencia(new Nota[] {
                            new Nota(470.0, 44, 0.24),
                            new Nota(390.0, 36, 0.20)
                        });
                    }
                }
                case TIRO -> {
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        reproducirSecuencia(new Nota[] {
                            new Nota(250.0, 40, 0.45),
                            new Nota(160.0, 70, 0.35)
                        });
                    } else {
                        reproducirSecuencia(new Nota[] {
                            new Nota(280.0, 38, 0.42),
                            new Nota(210.0, 42, 0.36),
                            new Nota(145.0, 55, 0.32)
                        });
                    }
                }
                case ROBO -> {
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        reproducirSecuencia(new Nota[] {
                            new Nota(530.0, 26, 0.24),
                            new Nota(360.0, 42, 0.22)
                        });
                    } else {
                        reproducirSecuencia(new Nota[] {
                            new Nota(490.0, 28, 0.24),
                            new Nota(340.0, 30, 0.22),
                            new Nota(280.0, 34, 0.20)
                        });
                    }
                }
                case SAQUE -> reproducirSilbatazo(1650.0, 1820.0, 125, 0.28);
                case GOL -> {
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        reproducirSecuencia(new Nota[] {
                            new Nota(392.0, 80, 0.35),
                            new Nota(523.25, 90, 0.35),
                            new Nota(659.25, 150, 0.38)
                        });
                    } else {
                        reproducirSecuencia(new Nota[] {
                            new Nota(329.63, 70, 0.34),
                            new Nota(493.88, 90, 0.35),
                            new Nota(659.25, 120, 0.37),
                            new Nota(783.99, 140, 0.34)
                        });
                    }
                }
                case VICTORIA -> reproducirSecuencia(new Nota[] {
                    new Nota(523.25, 70, 0.35),
                    new Nota(659.25, 70, 0.35),
                    new Nota(783.99, 90, 0.35),
                    new Nota(1046.50, 160, 0.34)
                });
                case DERROTA -> reproducirSecuencia(new Nota[] {
                    new Nota(392.00, 70, 0.30),
                    new Nota(329.63, 80, 0.32),
                    new Nota(261.63, 100, 0.33),
                    new Nota(196.00, 170, 0.30)
                });
                default -> {
                }
            }
        } catch (LineUnavailableException ignored) {
            // El audio es opcional: un fallo aqui no debe romper el juego.
        }
    }

    private void reproducirSilbatazo(double frecuenciaInicio, double frecuenciaFin, int duracionMs, double volumen)
        throws LineUnavailableException {
        AudioFormat formato = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (SourceDataLine linea = AudioSystem.getSourceDataLine(formato)) {
            linea.open(formato, 4096);
            linea.start();
            escribirSilbatazo(linea, frecuenciaInicio, frecuenciaFin, duracionMs, volumen);
            linea.drain();
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

    private void escribirSilbatazo(
        SourceDataLine linea,
        double frecuenciaInicio,
        double frecuenciaFin,
        int duracionMs,
        double volumen
    ) {
        int totalMuestras = (int) ((duracionMs / 1000.0) * SAMPLE_RATE);
        byte[] buffer = new byte[totalMuestras * 2];
        double amplitud = 32767.0 * Math.max(0.0, Math.min(1.0, volumen));

        for (int i = 0; i < totalMuestras; i++) {
            double progreso = totalMuestras <= 0 ? 1.0 : i / (double) totalMuestras;
            double tiempo = i / SAMPLE_RATE;
            double frecuencia = frecuenciaInicio + (frecuenciaFin - frecuenciaInicio) * progreso;
            double vibrato = Math.sin(2.0 * Math.PI * 5.8 * tiempo) * 10.0;
            double fase = 2.0 * Math.PI * (frecuencia + vibrato) * tiempo;
            // Mezcla armonica ligera para acercarse a timbre de silbato.
            double senoBase = Math.sin(fase) * 0.84;
            double armonico = Math.sin(fase * 2.0) * 0.16;
            double envolvente = calcularEnvolvente(i, totalMuestras);
            short muestra = (short) ((senoBase + armonico) * amplitud * envolvente);
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
