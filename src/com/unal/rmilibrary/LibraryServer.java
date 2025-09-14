// LibraryServer.java
package com.unal.rmilibrary;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class LibraryServer {
    public static void main(String[] args) {
        int port = 1099;
        String jdbcUrl = "jdbc:sqlite:library.db";
        if (args.length >= 1) jdbcUrl = args[0];
        try {
            System.out.println("Creando RMI registry en puerto " + port);
            Registry registry = LocateRegistry.createRegistry(port);

            LibraryServiceImpl impl = new LibraryServiceImpl(jdbcUrl);
            registry.rebind("LibraryService", impl);

            System.out.println("LibraryService binded. Servidor listo.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
