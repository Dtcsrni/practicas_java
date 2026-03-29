package juego.tools;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Helper sencillo para enviar una secuencia de teclas al juego y registrar eventos.
// NOTA: requiere que la ventana del juego esté enfocada antes de ejecutar.
public class PlaytestRobot {
    public static void main(String[] args) throws Exception {
        System.out.println("PlaytestRobot: asegure que la ventana del juego esté enfocada (tiene 4s)...");
        Thread.sleep(4000);
        Robot r;
        try {
            r = new Robot();
        } catch (AWTException ex) {
            System.err.println("No se pudo crear Robot: " + ex.getMessage());
            return;
        }
        try (PrintWriter log = new PrintWriter(new FileWriter("playtest_log.txt", true))) {
            log.printf("[%s] Playtest START\n", now());

            // Start (ENTER)
            press(r, KeyEvent.VK_ENTER, log, "ENTER (start)");
            Thread.sleep(1400);

            // Mover derecha un par de segundos
            for (int i = 0; i < 8; i++) {
                press(r, KeyEvent.VK_RIGHT, log, "RIGHT");
                Thread.sleep(140);
            }

            // Tirar / acción (SPACE)
            press(r, KeyEvent.VK_SPACE, log, "SPACE (shoot)");
            Thread.sleep(900);

            // Toggle música
            press(r, KeyEvent.VK_M, log, "M (toggle music)");
            Thread.sleep(600);

            // Ajustar volumen: subir con F10, bajar con F9
            press(r, KeyEvent.VK_F10, log, "F10 (vol+)");
            Thread.sleep(200);
            press(r, KeyEvent.VK_F10, log, "F10 (vol+)");
            Thread.sleep(200);
            press(r, KeyEvent.VK_F9, log, "F9 (vol-)");
            Thread.sleep(300);

            // Pausa y resume
            press(r, KeyEvent.VK_P, log, "P (pause)");
            Thread.sleep(900);
            press(r, KeyEvent.VK_P, log, "P (resume)");

            log.printf("[%s] Playtest END\n", now());
        }
        System.out.println("PlaytestRobot: secuencia enviada, revisar playtest_log.txt");
    }

    private static void press(Robot r, int key, PrintWriter log, String label) {
        r.keyPress(key);
        r.keyRelease(key);
        log.printf("[%s] %s\n", now(), label);
        System.out.println(label);
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
    }
}
