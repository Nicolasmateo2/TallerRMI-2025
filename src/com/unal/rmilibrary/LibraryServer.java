// LibraryServer.java
package com.unal.rmilibrary;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.net.InetAddress;

public class LibraryServer {
    public static void main(String[] args) {
        try {
            String jdbcUrl = "jdbc:sqlite:library.db";
            if (args.length >= 1) jdbcUrl = args[0];

            // Detectar IP local
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            System.setProperty("java.rmi.server.hostname", hostAddress);

            // Iniciar registro
            try {
                LocateRegistry.createRegistry(1099);
                System.out.println("Registro RMI creado en puerto 1099");
            } catch (Exception e) {
                System.out.println("Usando registro RMI existente en puerto 1099");
            }

            // Publicar servicio
            LibraryServiceImpl servicio = new LibraryServiceImpl(jdbcUrl);
            String serviceName = "LibraryService";
            Naming.rebind("rmi://localhost:1099/" + serviceName, servicio);

            // Info de conexión
            System.out.println("=== SERVIDOR DE BIBLIOTECA ===");
            System.out.println("Servicio registrado como: " + serviceName);
            System.out.println("Servidor en IP: " + hostAddress);
            System.out.println("URL de acceso: rmi://" + hostAddress + ":1099/" + serviceName);
            System.out.println("Esperando conexiones de clientes...");

            // Mantener servidor activo
            synchronized (LibraryServer.class) {
                LibraryServer.class.wait();
            }

        } catch (Exception e) {
            System.err.println("Error iniciando el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}