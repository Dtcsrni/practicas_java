Playtest: guía y pasos

Objetivo
- Ejecutar una sesión rápida de validación de cambios visuales y control UI (música, slider, animaciones de tarjeta, stamina/hidratación).

Requisitos
- Java instalado (mismo usado para desarrollo).
- La aplicación debe estar compilada y en `out_run` (o ejecutarse desde el IDE).

Comandos sugeridos (desde workspace):

```powershell
# Compilar
javac -d out_run -sourcepath src src/juego/app/Main.java src/juego/tools/PlaytestRobot.java
# Ejecutar el juego en una terminal
java -cp out_run juego.app.Main
# En otra terminal, ejecutar el helper (asegúrate que la ventana del juego esté enfocada)
java -cp out_run juego.tools.PlaytestRobot
```

Checklist de playtest (pasos manuales)
1. Abrir el juego, observar la pantalla de inicio.
2. Verificar que el control de música aparece en la esquina y el knob responde al hover/drag.
3. Presionar `ENTER` para iniciar partido.
4. Realizar movimientos (flechas) y acciones (`SPACE` para tiro). Observar animaciones y partículas.
5. Provocar falta/amarilla/roja mediante juego o forzando evento (si procede) y observar animación de tarjeta.
6. Confirmar que la hidratación en banca restaura a jugadores y que la barra de energía está central y legible.
7. Revisar `playtest_log.txt` para confirmar la secuencia automatizada (si se ejecutó `PlaytestRobot`).

Criterios de aceptación rápidos
- Slider de volumen se persiste entre ejecuciones (chequear ajuste y reiniciar).
- Iconos de tarjeta y hidratación aparecen coherentes con la paleta HUD.
- Contrastes en menús y overlays son legibles en pantallas típicas.

Registro de ajustes
- `playtest_log.txt` se generará en el directorio raíz si se ejecuta `PlaytestRobot`.
- Si detectas problemas, anota: pantalla, paso, paso reproducible, captura (si procede).

Notas técnicas
- `PlaytestRobot` usa `java.awt.Robot` y requiere que la ventana del juego esté enfocada para que los eventos de teclado sean recibidos.
- Si la posición del slider debe probarse con mouse, es mejor hacerlo manualmente (el helper actual solo envía teclas para mayor fiabilidad). 
