package juego.entidades;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;

// Punto fijo de hidratacion junto a la banca.
public class HidratacionBanca extends EntidadJuego {
    private int cooldownUsoFrames;
    private int usosRestantes;

    public HidratacionBanca(int x, int y, int ancho, int alto) {
        super(x, y, ancho, alto);
        this.cooldownUsoFrames = 0;
        this.usosRestantes = 0;
    }

    public void actualizar() {
        if (cooldownUsoFrames > 0) {
            cooldownUsoFrames--;
        }
    }

    public boolean estaDisponible() {
        return cooldownUsoFrames <= 0 && usosRestantes > 0;
    }

    public void activarCooldown(int frames) {
        cooldownUsoFrames = Math.max(0, frames);
    }

    public void reiniciar() {
        cooldownUsoFrames = 0;
        usosRestantes = juego.core.ConfiguracionJuego.USOS_HIDRATACION_BANCA;
    }

    public boolean consumirUso() {
        if (!estaDisponible()) {
            return false;
        }
        usosRestantes = Math.max(0, usosRestantes - 1);
        return true;
    }

    public int getUsosRestantes() {
        return usosRestantes;
    }

    @Override
    public void dibujar(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int pulso = (int) ((System.nanoTime() / 11_000_000L) % 6);

        g2.setColor(new Color(18, 46, 72, 120));
        g2.fillRoundRect(x - 6, y - 6, ancho + 12, alto + 12, 12, 12);

        g2.setPaint(new GradientPaint(x, y, new Color(86, 194, 255), x, y + alto, new Color(36, 106, 178)));
        g2.fillRoundRect(x, y, ancho, alto, 10, 10);
        g2.setColor(new Color(18, 52, 88));
        g2.drawRoundRect(x, y, ancho, alto, 10, 10);

        int botellaW = Math.max(8, ancho / 5);
        int botellaH = Math.max(15, alto - 8);
        int baseY = y + 4;
        for (int i = 0; i < 3; i++) {
            int bx = x + 6 + i * (botellaW + 4);
            int offset = (i == 1 ? pulso / 2 : 0);
            g2.setColor(new Color(220, 245, 255, 215));
            g2.fillRoundRect(bx, baseY - offset, botellaW, botellaH, 4, 4);
            g2.setColor(new Color(48, 132, 198));
            g2.fillRect(bx + 2, baseY + botellaH / 2 - offset, botellaW - 4, botellaH / 2 - 1);
            g2.setColor(new Color(18, 78, 126));
            g2.drawRoundRect(bx, baseY - offset, botellaW, botellaH, 4, 4);
            g2.setColor(new Color(244, 255, 255));
            g2.fillRect(bx + 2, baseY - 3 - offset, botellaW - 4, 3);
        }

        g2.setColor(new Color(240, 252, 255, 210));
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("H2O", x + ancho - 26, y + alto - 6);
        g2.setColor(new Color(250, 245, 190, 220));
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.drawString(String.valueOf(usosRestantes), x + 4, y + alto - 6);
        g2.dispose();
    }
}
