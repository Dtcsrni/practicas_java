# Guía para instalar y configurar Java OpenJDK en Windows

## 1. Introducción
Esta guía te ayudará a instalar Java OpenJDK, configurar el entorno y ejecutar tu primer programa "Hola Mundo" en Windows.

---

## 2. Descarga e instalación de OpenJDK

### Paso 1: Descargar OpenJDK
- Ve a la página oficial de OpenJDK: [https://jdk.java.net/](https://jdk.java.net/)
- Elige la versión recomendada (por ejemplo, OpenJDK 17 o 21).
- Descarga el archivo ZIP para Windows.

### Paso 2: Instalar OpenJDK
- Extrae el archivo ZIP en una carpeta, por ejemplo: `C:\Program Files\OpenJDK`.

---

## 3. Configuración de variables de entorno

### Paso 3: Configurar JAVA_HOME
1. Abre el Panel de Control > Sistema > Configuración avanzada del sistema.
2. Haz clic en "Variables de entorno".
3. En "Variables del sistema", haz clic en "Nueva".
4. Nombre de la variable: `JAVA_HOME`
5. Valor de la variable: la ruta donde extrajiste OpenJDK (ejemplo: `C:\Program Files\OpenJDK`).

### Paso 4: Agregar Java al PATH
1. En "Variables del sistema", busca la variable `Path` y haz clic en "Editar".
2. Agrega al final: `%JAVA_HOME%\bin`
3. Haz clic en "Aceptar" para guardar los cambios.

---

## 4. Verificar instalación

### Paso 5: Comprobar Java
- Abre una terminal (PowerShell o CMD).
- Escribe:

```
java -version
```

- Deberías ver la versión instalada de OpenJDK.

---

## 5. Instalar un editor de código (opcional)

### Paso 6: Instalar Visual Studio Code
- Ve a [https://code.visualstudio.com/](https://code.visualstudio.com/)
- Descarga e instala VS Code.

---

## 6. Crear y ejecutar el programa "Hola Mundo"

### Paso 7: Crear el archivo Java
1. Abre VS Code o tu editor favorito.
2. Crea una carpeta llamada `src`.
3. Dentro de `src`, crea un archivo llamado `practica1.java`.
4. Escribe el siguiente código:

```java
public class practica1 {
    public static void main(String[] args) {
        System.out.println("Hola Mundo");
    }
}
```

### Paso 8: Compilar el programa
- Abre una terminal en la carpeta raíz del proyecto.
- Escribe:

```
javac src/practica1.java
```

### Paso 9: Ejecutar el programa
- En la terminal, escribe:

```
java -cp src practica1
```

- Deberías ver:

```
Hola Mundo
```

---

## 7. Solución de problemas comunes
- Si ves un error de "no se reconoce como un comando", revisa el PATH.
- Si el archivo no compila, revisa el nombre de la clase y el archivo.

---

## 8. Recursos adicionales
- [Documentación oficial de Java](https://docs.oracle.com/en/java/)
- [OpenJDK](https://openjdk.org/)
- [VS Code Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)

---

¡Listo! Ahora tienes Java configurado y ejecutaste tu primer programa.
