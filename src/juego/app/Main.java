package juego.app;

import juego.ui.VentanaJuego;

// Punto de entrada: crea la ventana principal y arranca el juego.
public class Main {
    public static void main(String[] args){
        // El resto del ciclo de juego vive dentro de Swing (Panel + Timer).
        new VentanaJuego();
    }
}
