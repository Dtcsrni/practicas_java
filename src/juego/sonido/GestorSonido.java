package juego.sonido;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

// Sintesis PCM ligera para evitar depender del beep del sistema operativo.
public class GestorSonido {
    private static final float SAMPLE_RATE = 22050.0f;
    private static final double NIVEL_SALIDA = 0.92;

    private final ExecutorService executor;

    public GestorSonido() {
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread hilo = new Thread(runnable, "gestor-sonido-juego");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    public void reproducir(TipoSonido sonido) {
        reproducir(SonidoEvento.normal(sonido));
    }

    public void reproducir(SonidoEvento evento) {
        if (evento == null) {
            return;
        }
        executor.submit(() -> reproducirInterno(evento));
    }

    private void reproducirInterno(SonidoEvento evento) {
        try {
            byte[] audio = sintetizar(evento);
            if (audio.length == 0) {
                return;
            }
            AudioFormat formato = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            try (SourceDataLine linea = AudioSystem.getSourceDataLine(formato)) {
                linea.open(formato, 4096);
                linea.start();
                linea.write(audio, 0, audio.length);
                linea.drain();
            }
        } catch (LineUnavailableException ignored) {
            // El audio es opcional: si el equipo no tiene salida disponible, el juego sigue.
        }
    }

    private byte[] sintetizar(SonidoEvento evento) {
        double intensidad = evento.intensidad();
        Capa[] capas = switch (evento.tipo()) {
            case INICIO -> new Capa[] {
                silbato(980.0, 1220.0, 180, 0.20, 0.0),
                tono(392.0, 70, 0.10, 165.0, Forma.TRIANGULAR),
                tono(523.25, 80, 0.11, 245.0, Forma.TRIANGULAR),
                tono(659.25, 130, 0.12, 335.0, Forma.TRIANGULAR)
            };
            case PASE -> crearCapasPase(intensidad);
            case TIRO -> crearCapasTiro(intensidad);
            case BARRIDA -> crearCapasBarrida(intensidad);
            case ROBO -> crearCapasRobo(intensidad);
            case SAQUE -> new Capa[] {
                silbato(1120.0, 1320.0, 125, 0.19, 0.0)
            };
            case GOL -> new Capa[] {
                golpeBalon(128.0, 90, 0.17, 0.0),
                tono(392.0, 75, 0.10, 70.0, Forma.TRIANGULAR),
                tono(523.25, 85, 0.11, 150.0, Forma.TRIANGULAR),
                tono(698.46, 170, 0.13, 235.0, Forma.SIERRA_SUAVE)
            };
            case FALTA -> new Capa[] {
                silbato(920.0, 1100.0, 120, 0.18, 0.0),
                ruido(120, 0.06, 0.0, 0.24)
            };
            case QUEJIDO -> new Capa[] {
                tonoDeslizante(660.0, 320.0, 180, 0.14, 0.0, Forma.SENO),
                ruido(80, 0.04, 0.0, 0.34)
            };
            case TARJETA -> new Capa[] {
                silbato(1500.0, 1620.0, 90, 0.20, 0.0),
                tono(880.0, 60, 0.12, 0.0, Forma.TRIANGULAR)
            };
            case PENAL -> new Capa[] {
                tonoDeslizante(240.0, 880.0, 260, 0.09, 0.0, Forma.SIERRA_SUAVE),
                golpeBalon(140.0 + variar(-8.0, 8.0), 110, 0.22, 160.0),
                aire(760.0, 360.0, 120, 0.06, 14.0)
            };
            case VICTORIA -> new Capa[] {
                tono(523.25, 85, 0.10, 0.0, Forma.TRIANGULAR),
                tono(659.25, 85, 0.11, 90.0, Forma.TRIANGULAR),
                tono(783.99, 100, 0.12, 180.0, Forma.TRIANGULAR),
                tonoDeslizante(1046.50, 1174.66, 190, 0.12, 285.0, Forma.SIERRA_SUAVE)
            };
            case DERROTA -> new Capa[] {
                tonoDeslizante(392.0, 340.0, 95, 0.09, 0.0, Forma.TRIANGULAR),
                tonoDeslizante(329.63, 272.0, 110, 0.10, 95.0, Forma.TRIANGULAR),
                tonoDeslizante(261.63, 196.0, 150, 0.10, 205.0, Forma.SENO)
            };
        };
        return renderizar(capas);
    }

    private Capa[] crearCapasPase(double intensidad) {
        double cuerpo = 0.15 + intensidad * 0.07;
        if (intensidad < 0.34) {
            return new Capa[] {
                golpeBalon(172.0 + variar(-10.0, 8.0), 70, cuerpo, 0.0),
                aire(620.0, 420.0, 52, 0.03, 6.0)
            };
        }
        if (intensidad < 0.70) {
            return new Capa[] {
                golpeBalon(154.0 + variar(-10.0, 10.0), 90, cuerpo + 0.03, 0.0),
                tonoDeslizante(230.0, 180.0, 70, 0.05, 8.0, Forma.SENO),
                aire(720.0, 360.0, 82, 0.05, 10.0)
            };
        }
        return new Capa[] {
            golpeBalon(132.0 + variar(-8.0, 8.0), 108, cuerpo + 0.05, 0.0),
            tonoDeslizante(215.0, 126.0, 95, 0.06, 10.0, Forma.SENO),
            aire(940.0, 240.0, 128, 0.08, 14.0)
        };
    }

    private Capa[] crearCapasTiro(double intensidad) {
        double golpe = 0.20 + intensidad * 0.12;
        double arrastre = 0.06 + intensidad * 0.05;
        return new Capa[] {
            golpeBalon(132.0 - intensidad * 18.0 + variar(-6.0, 6.0), 108 + (int) Math.round(intensidad * 30.0), golpe, 0.0),
            tonoDeslizante(240.0 - intensidad * 24.0, 96.0 - intensidad * 18.0, 120 + (int) Math.round(intensidad * 35.0),
                0.06 + intensidad * 0.04, 0.0, Forma.SENO),
            aire(860.0 + intensidad * 220.0, 180.0, 120 + (int) Math.round(intensidad * 65.0), arrastre, 14.0)
        };
    }

    private Capa[] crearCapasBarrida(double intensidad) {
        return new Capa[] {
            ruido(80 + (int) Math.round(intensidad * 80.0), 0.06 + intensidad * 0.10, 0.0, 0.34 + intensidad * 0.18),
            tonoDeslizante(300.0 + intensidad * 40.0, 118.0, 90 + (int) Math.round(intensidad * 55.0),
                0.05 + intensidad * 0.05, 4.0, Forma.SIERRA_SUAVE),
            golpeBalon(150.0 - intensidad * 26.0, 58 + (int) Math.round(intensidad * 24.0), 0.08 + intensidad * 0.07, 24.0)
        };
    }

    private Capa[] crearCapasRobo(double intensidad) {
        return new Capa[] {
            ruido(70 + (int) Math.round(intensidad * 40.0), 0.05 + intensidad * 0.04, 0.0, 0.18 + intensidad * 0.14),
            tonoDeslizante(260.0 + intensidad * 40.0, 170.0, 68 + (int) Math.round(intensidad * 28.0),
                0.04 + intensidad * 0.04, 4.0, Forma.SIERRA_SUAVE),
            golpeBalon(138.0 - intensidad * 12.0, 60 + (int) Math.round(intensidad * 18.0), 0.09 + intensidad * 0.05, 16.0)
        };
    }

    private byte[] renderizar(Capa[] capas) {
        int totalMuestras = 0;
        for (Capa capa : capas) {
            totalMuestras = Math.max(totalMuestras, msAMuestras(capa.inicioMs + capa.duracionMs));
        }
        if (totalMuestras <= 0) {
            return new byte[0];
        }

        double[] mezcla = new double[totalMuestras];
        for (Capa capa : capas) {
            escribirCapa(mezcla, capa);
        }

        normalizar(mezcla);
        byte[] audio = new byte[mezcla.length * 2];
        for (int i = 0; i < mezcla.length; i++) {
            short muestra = (short) Math.round(mezcla[i] * 32767.0 * NIVEL_SALIDA);
            audio[i * 2] = (byte) (muestra & 0xff);
            audio[i * 2 + 1] = (byte) ((muestra >> 8) & 0xff);
        }
        return audio;
    }

    private void escribirCapa(double[] mezcla, Capa capa) {
        int inicio = msAMuestras(capa.inicioMs);
        int duracion = msAMuestras(capa.duracionMs);
        double fase = 0.0;
        double ruidoAnterior = 0.0;

        for (int i = 0; i < duracion && inicio + i < mezcla.length; i++) {
            double progreso = duracion <= 1 ? 1.0 : i / (double) (duracion - 1);
            double frecuencia = interpolar(capa.frecuenciaInicio, capa.frecuenciaFin, progreso);
            if (capa.forma != Forma.RUIDO) {
                fase += (2.0 * Math.PI * Math.max(0.0, frecuencia)) / SAMPLE_RATE;
            }

            double muestra = switch (capa.forma) {
                case SENO -> Math.sin(fase);
                case TRIANGULAR -> (2.0 / Math.PI) * Math.asin(Math.sin(fase));
                case SIERRA_SUAVE -> {
                    double base = (fase / (2.0 * Math.PI)) % 1.0;
                    double sierra = (2.0 * base) - 1.0;
                    double seno = Math.sin(fase);
                    yield (sierra * 0.45) + (seno * 0.55);
                }
                case RUIDO -> {
                    double blanco = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
                    ruidoAnterior += (blanco - ruidoAnterior) * (0.08 + (capa.texturaRuido * 0.32));
                    yield ruidoAnterior;
                }
            };

            mezcla[inicio + i] += muestra * capa.volumen * envolvente(progreso);
        }
    }

    private void normalizar(double[] mezcla) {
        double maximo = 0.0;
        for (double muestra : mezcla) {
            maximo = Math.max(maximo, Math.abs(muestra));
        }
        if (maximo <= 0.0001) {
            return;
        }
        double escala = maximo > 1.0 ? 1.0 / maximo : 1.0;
        for (int i = 0; i < mezcla.length; i++) {
            double valor = mezcla[i] * escala;
            mezcla[i] = Math.max(-1.0, Math.min(1.0, valor));
        }
    }

    private double envolvente(double progreso) {
        if (progreso < 0.08) {
            return progreso / 0.08;
        }
        if (progreso > 0.74) {
            return Math.max(0.0, (1.0 - progreso) / 0.26);
        }
        return 1.0;
    }

    private Capa silbato(double inicio, double fin, int duracionMs, double volumen, double inicioMs) {
        return new Capa(Forma.SIERRA_SUAVE, inicio, fin, duracionMs, volumen, inicioMs, 0.0);
    }

    private Capa golpeBalon(double frecuencia, int duracionMs, double volumen, double inicioMs) {
        return new Capa(Forma.SENO, frecuencia, frecuencia * 0.52, duracionMs, volumen, inicioMs, 0.0);
    }

    private Capa aire(double inicio, double fin, int duracionMs, double volumen, double inicioMs) {
        return new Capa(Forma.RUIDO, inicio, fin, duracionMs, volumen, inicioMs, 0.65);
    }

    private Capa ruido(int duracionMs, double volumen, double inicioMs, double textura) {
        return new Capa(Forma.RUIDO, 0.0, 0.0, duracionMs, volumen, inicioMs, textura);
    }

    private Capa tono(double frecuencia, int duracionMs, double volumen, double inicioMs, Forma forma) {
        return new Capa(forma, frecuencia, frecuencia, duracionMs, volumen, inicioMs, 0.0);
    }

    private Capa tonoDeslizante(
        double frecuenciaInicio,
        double frecuenciaFin,
        int duracionMs,
        double volumen,
        double inicioMs,
        Forma forma
    ) {
        return new Capa(forma, frecuenciaInicio, frecuenciaFin, duracionMs, volumen, inicioMs, 0.0);
    }

    private int msAMuestras(double ms) {
        return Math.max(0, (int) Math.round((ms / 1000.0) * SAMPLE_RATE));
    }

    private double interpolar(double inicio, double fin, double progreso) {
        return inicio + ((fin - inicio) * progreso);
    }

    private double variar(double minimo, double maximo) {
        return ThreadLocalRandom.current().nextDouble(minimo, maximo);
    }

    private enum Forma {
        SENO,
        TRIANGULAR,
        SIERRA_SUAVE,
        RUIDO
    }

    private record Capa(
        Forma forma,
        double frecuenciaInicio,
        double frecuenciaFin,
        int duracionMs,
        double volumen,
        double inicioMs,
        double texturaRuido
    ) {
    }
}
