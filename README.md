# Biblioteca RMI

README para compilar, ejecutar y probar la aplicación **Biblioteca RMI** desarrollada en Java usando SQLite y Java RMI.
Video: https://drive.google.com/drive/folders/1mMDJyABePbFGx8BXwhj1gK92wt-xvwcG?usp=sharing
---

## 1. Descripción corta

Aplicación cliente/servidor que expone un servicio RMI (`LibraryService`) para consultar libros, realizar préstamos y registrar devoluciones. La persistencia se realiza con SQLite.

El servidor publica el servicio en un RMI Registry (puerto 1099 por defecto) y el cliente se conecta por `rmi://<host>:1099/LibraryService`.

---

## 2. Requisitos previos

* **Java JDK 11** o superior instalado.

  * Verificar:

    ```bash
    java -version
    javac -version
    ```

* **SQLite** (consola o DB Browser) instalado.

  * Verificar:

    ```bash
    sqlite3 --version
    ```

* El proyecto incluye la librería JDBC para SQLite en `lib/sqlite-jdbc-3.36.0.3.jar`.

---

## 3. Estructura de carpetas (esperada)

```
proyecto-rmi/
│
├─ lib/
│   └─ sqlite-jdbc-3.36.0.3.jar
│
├─ src/
│   └─ com/
│       └─ unal/
│           └─ rmilibrary/
│               ├─ LibraryService.java
│               ├─ LibraryServiceImpl.java
│               ├─ LibraryServer.java
│               ├─ LibraryClient.java
│               ├─ BookDAO.java
│               ├─ LoanResponse.java
│               ├─ QueryResponse.java
│               └─ ReturnResponse.java
│
└─ db/
    ├─ schema.sql
    └─ seed.sql
```

> Nota: el README asume que usarás `lib/sqlite-jdbc-3.36.0.3.jar`. Si usas otra versión, reemplaza el nombre en los comandos.

---

## 4. Compilar el código (Windows - CMD)

Abre **CMD** en la raíz del proyecto (`proyecto-rmi`) y ejecuta:

```cmd
javac -cp ".;lib/sqlite-jdbc-3.36.0.3.jar" src/com/unal/rmilibrary/*.java -d out
```

> Resultado: los `.class` se generarán en la carpeta `out/` con la estructura de paquetes.

---

## 5. Crear la base de datos (SQLite)

Desde la raíz del proyecto (o la carpeta `db/`) crea `library.db` y carga esquema y datos iniciales:

```bash
sqlite3 library.db
.read db/schema.sql
.read db/seed.sql
.exit
```

Verificar contenido:

```bash
sqlite3 library.db "SELECT * FROM books;"
```

> Importante: `library.db` debe estar en el mismo directorio desde donde ejecutarás el servidor (o pasar una ruta absoluta al servidor).

---

## 6. Levantar el servidor (Windows - CMD)

Ejecuta en una terminal (manténla abierta):

```cmd
java -cp "out;lib/sqlite-jdbc-3.36.0.3.jar" com.unal.rmilibrary.LibraryServer
```

### Qué hace el `LibraryServer` al arrancar

* Detecta la IP local mediante `InetAddress.getLocalHost()` y asigna `java.rmi.server.hostname` con esa IP.
* Intenta crear un RMI Registry en el puerto `1099` (si ya existe, lo reutiliza).
* Instancia `LibraryServiceImpl` (la implementación del servicio que usa `BookDAO` y SQLite).
* Publica el servicio en el registry con el nombre `LibraryService`.
* Imprime en consola la URL de acceso: `rmi://<tu-ip-local>:1099/LibraryService`.
* Se bloquea (espera) para mantener el servidor en ejecución.

> Observación de consistencia: si tu `LibraryServer.java` todavía hace `Naming.rebind("rmi://localhost:1099/LibraryService", servicio)`, cambia esa línea por:
>
> ```java
> Naming.rebind("rmi://" + hostAddress + ":1099/" + serviceName, servicio);
> ```
>
> Esto garantiza que el objeto quede registrado con la IP accesible desde otros equipos.

---

## 7. Ejecutar el cliente

Abre otra terminal (no cierres el servidor) y ejecuta:

```cmd
java -cp "out;lib/sqlite-jdbc-3.36.0.3.jar" com.unal.rmilibrary.LibraryClient localhost
```

* Si el cliente corre en otra PC, reemplaza `localhost` por la IP del servidor (ej: `192.168.1.10`):

```cmd
java -cp "out;lib/sqlite-jdbc-3.36.0.3.jar" com.unal.rmilibrary.LibraryClient 192.168.1.10
```

### Flujo en el cliente

1. Pide la IP del servidor (si ya pasaste `localhost` como argumento, lo toma).
2. Se conecta mediante `Naming.lookup("rmi://<host>:1099/LibraryService")`.
3. Pide `userId` (si no lo das se genera uno por timestamp).
4. Muestra menú con opciones: consultar (ISBN), prestar (ISBN o título), devolver (ISBN) y salir.
5. Cada acción ejecuta una llamada remota al servicio y muestra la respuesta.

---

## 8. Probar operaciones (ejemplo)

1. En el cliente, ingresa un `userId` (ej: `juan`).

2. Menú:

   * `1` → Consulta por ISBN (`978-0140449136` ejemplo).
   * `2` → Prestar por ISBN.
   * `3` → Prestar por título (`The Hobbit` ejemplo).
   * `4` → Devolver por ISBN.
   * `0` → Salir.

3. Verificar cambios en la BD (en el servidor o donde esté `library.db`):

```bash
sqlite3 library.db "SELECT * FROM books;"
sqlite3 library.db "SELECT * FROM loans;"
```

---

## 9. Checklist de pruebas rápidas

* [ ] Consultar un libro existente (ver datos correctos).
* [ ] Hacer un préstamo → `available_copies` baja.
* [ ] Consultar de nuevo → cantidad disponible menor.
* [ ] Devolver el libro → `available_copies` vuelve a subir.
* [ ] Ejecutar 2 clientes simultáneos (dos terminales) y probar concurrencia básica.

---

## 10. Troubleshooting / Problemas comunes

### 10.1 `No suitable driver found for jdbc:sqlite:library.db`

* Solución: incluir el JAR de sqlite en el classpath al ejecutar el **servidor**. Ejemplo:

```cmd
java -cp "out;lib/sqlite-jdbc-3.36.0.3.jar" com.unal.rmilibrary.LibraryServer
```

### 10.2 `Connection refused` / `Connection timed out` en `Naming.lookup`

* Verifica que el servidor esté corriendo.
* Asegúrate de que el RMI Registry esté en `1099` o que uses el puerto correcto.
* Comprueba que `java.rmi.server.hostname` en el servidor sea una IP accesible por los clientes.
* Abre el puerto `1099` en el firewall (CMD como administrador):

```cmd
netsh advfirewall firewall add rule name="RMI 1099" dir=in action=allow protocol=TCP localport=1099
```

* Verificar que el servidor escucha en el puerto:

```cmd
netstat -ano | find "1099"
```

* Probar conectividad desde el cliente (PowerShell):

```powershell
Test-NetConnection -ComputerName 192.168.1.10 -Port 1099
```

### 10.3 `NotBoundException`

* El nombre `LibraryService` no está registrado. Asegúrate de que `Naming.rebind(...)` en el servidor se ejecutó correctamente.

### 10.4 Errores de serialización (`ClassNotFoundException` / `InvalidClassException`)

* Asegúrate de que las clases `QueryResponse`, `LoanResponse`, `ReturnResponse` existan en el classpath del **cliente** con el mismo paquete y versión (implementen `Serializable`).

---

## 11. Uso en múltiples PCs / pruebas locales (simular varias IPs)

### A. Probar en la misma máquina (simular distintas IPs con loopback)

* Abrir CMD como administrador y ejecutar (ejemplo para crear direcciones 127.0.0.2 y 127.0.0.3):

```cmd
netsh interface ipv4 add address "Loopback Pseudo-Interface 1" 127.0.0.2 255.0.0.0
netsh interface ipv4 add address "Loopback Pseudo-Interface 1" 127.0.0.3 255.0.0.0
```

* Levantar servidores/clients apuntando a `127.0.0.2`, `127.0.0.3`, etc.

### B. Probar en la LAN (otras PCs)

* Asegúrate de que el servidor se ejecute con `hostAddress` accesible (ej: `192.168.1.10`).
* En otras PCs: `java -cp "out;lib/sqlite-jdbc-3.36.0.3.jar" com.unal.rmilibrary.LibraryClient 192.168.1.10`
* Si el servidor está detrás de NAT o router, configura port forwarding en el router al puerto 1099.

---

## 12. Scripts útiles (opcional)

### 12.1 `add-loopback.bat` (ejecutar como administrador)

```bat
@echo off
REM Agrega loopback adicionales
netsh interface ipv4 add address "Loopback Pseudo-Interface 1" 127.0.0.2 255.0.0.0
netsh interface ipv4 add address "Loopback Pseudo-Interface 1" 127.0.0.3 255.0.0.0
echo Loopbacks added.
```

### 12.2 `run-server.bat` (ejecutar desde la raíz del proyecto)

```bat
@echo off
REM Ejecutar servidor (asegúrate de compilar primero)
java -cp "out;lib/sqlite-jdbc-3.36.0.3.jar" com.unal.rmilibrary.LibraryServer
pause
```

### 12.3 `run-client-local.bat`

```bat
@echo off
REM Ejecutar cliente apuntando a localhost
java -cp "out;lib/sqlite-jdbc-3.36.0.3.jar" com.unal.rmilibrary.LibraryClient localhost
pause
```

---

## 13. Recomendaciones finales y mejoras

* **No usar SQLite para producción** con muchos clientes concurrentes; usar una BD servidor (Postgres/MySQL) para mayor concurrencia y robustez.
* Considerar agregar manejo de timeouts o reintentos en el `LibraryClient` para mejorar UX cuando hay latencia o fallos de red.
* Proteger el acceso a RMI (VPN / red privada / RMI sobre SSL) si vas a exponer el servidor fuera de una red de confianza.
* Empaquetar en un `fat-jar` si no deseas manejar classpath manualmente.

---

Si quieres, puedo:

* Generar los `.bat` listos para que los descargues.
* Empaquetar un `jar` ejecutable (fat-jar) con la dependencia de sqlite incluida.
* Modificar `LibraryServer.java` para usar explícitamente `Naming.rebind("rmi://" + hostAddress + ":1099/" + serviceName, servicio);` y darte el código listo.

Dime cuál de estas tareas prefieres y lo hago.
