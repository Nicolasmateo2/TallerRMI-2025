package com.unal.rmilibrary;

import java.net.InetAddress;             // Para registrar el servicio remoto usando una URL RMI
import java.rmi.Naming; // Para crear o ubicar un registro RMI en un puerto específico
import java.rmi.registry.LocateRegistry;       // Para detectar la IP local de la máquina

public class LibraryServer {

    public static void main(String[] args) {
        try {
            // URL de conexión a la base de datos SQLite (por defecto "library.db" en el directorio actual)
            String jdbcUrl = "jdbc:sqlite:library.db";
            if (args.length >= 1) {
                jdbcUrl = args[0]; // Permitir pasar otra BD como argumento
            }
            // Detectar automáticamente la IP local de la máquina (ej. 192.168.1.10)
            String hostAddress = InetAddress.getLocalHost().getHostAddress();

            // Configurar propiedad obligatoria para RMI:
            // indica qué dirección IP deben usar los clientes al conectarse a este servidor
            System.setProperty("java.rmi.server.hostname", hostAddress);

            // Iniciar registro RMI en puerto 1099 (estándar)
            try {
                LocateRegistry.createRegistry(1099); // crea un nuevo registry si no existe
                System.out.println("Registro RMI creado en puerto 1099");
            } catch (Exception e) {
                // Si ya había un registro levantado, simplemente usar ese
                System.out.println("Usando registro RMI existente en puerto 1099");
            }

            // Crear la implementación del servicio de biblioteca con acceso a la BD
            LibraryServiceImpl servicio = new LibraryServiceImpl(jdbcUrl);

            // Nombre con el que se publicará el servicio en el registro
            String serviceName = "LibraryService";

            // Asociar el objeto remoto con un nombre RMI dentro del registro
            // IMPORTANTE: aquí usa "localhost", pero los clientes remotos deberían usar la IP detectada
            Naming.rebind("rmi://localhost:1099/" + serviceName, servicio);

            // Mostrar información útil al usuario/admin
            System.out.println("=== SERVIDOR DE BIBLIOTECA ===");
            System.out.println("Servicio registrado como: " + serviceName);
            System.out.println("Servidor en IP: " + hostAddress);
            System.out.println("URL de acceso: rmi://" + hostAddress + ":1099/" + serviceName);
            System.out.println("Esperando conexiones de clientes...");

            // Bloquear el hilo principal para mantener el servidor activo
            synchronized (LibraryServer.class) {
                LibraryServer.class.wait();
            }

        } catch (Exception e) {
            // Captura de errores en cualquier parte del arranque
            System.err.println("Error iniciando el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
