# La Canchita - Futbol Callejero (Java)

Juego de futbol 2D hecho en Java/Swing con enfoque arcade: stamina, saques, atajadas, rebotes, bonus y audio procedural.

## Nombre de repo recomendado

`la-canchita-java`

Alternativas:
- `futbol-callejero-java`
- `la-canchita-arcade`

## Caracteristicas

- Partido 3v3 (portero + 2 jugadores de campo por equipo).
- IA con decisiones de pase/tiro y desmarque.
- Sistema de stamina y sprint.
- Saques de banda, meta, esquina y central con delay.
- Porteros con atajada, desvio y saque largo.
- Fisica de balon con altura y gravedad.
- Sonidos dinamicos (pase, tiro, robo, saque, silbato y melodias de cierre).

## Controles

- `W A S D`: mover jugador principal.
- `SHIFT`: correr (consume stamina).
- `SPACE`: pase (con carga segun implementacion actual).
- `X`: tiro (con carga segun implementacion actual).

## Estructura del proyecto

- `src/juego/app`: punto de entrada.
- `src/juego/core`: logica del partido y estados.
- `src/juego/entidades`: jugador, balon y bonus.
- `src/juego/ui`: render y ventana.
- `src/juego/sonido`: gestor y tipos de sonido.

## Requisitos

- Java JDK 17+ (recomendado).
- Windows, Linux o macOS con soporte para Java desktop (Swing).

## Compilar

PowerShell:

```powershell
New-Item -ItemType Directory -Force -Path out_run | Out-Null
$files = Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out_run $files
```

## Ejecutar

```powershell
java -cp out_run juego.app.Main
```

## Estado actual del gameplay

- Ritmo mas dinamico en reanudaciones.
- Menos teletransporte en saques: el ejecutor se desplaza hacia el punto.
- Portero puede tomar el balon con manos tras atajada antes del saque largo.

## Creditos

Proyecto de practica en Java orientado a mecanicas de juego y simulacion 2D.
