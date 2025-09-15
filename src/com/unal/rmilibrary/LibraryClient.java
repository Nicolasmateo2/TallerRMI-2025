package com.unal.rmilibrary;

import java.rmi.Naming;        // API RMI: Naming.lookup() para obtener el "stub" remoto
import java.util.Scanner;     // Para leer entrada por consola

public class LibraryClient {

    public static void main(String[] args) {
        try {
            Scanner sc = new Scanner(System.in); // Scanner para leer del STDIN

            // Pide la IP del servidor; si el usuario presiona ENTER se usa "localhost"
            System.out.print("Ingrese la IP del servidor (enter para localhost): ");
            String servidor = sc.nextLine().trim();
            if (servidor.isEmpty()) {
                servidor = "localhost";
            }

            // Construye la URL RMI usando puerto fijo 1099 y nombre "LibraryService"
            String url = "rmi://" + servidor + ":1099/LibraryService";

            // Busca/obtiene el stub remoto que implementa LibraryService en el registry
            LibraryService service = (LibraryService) Naming.lookup(url);
            System.out.println("Conectado a " + url);

            // Solicita al usuario su id (se usa para asociar préstamos/devoluciones)
            System.out.print("Ingrese su ID de usuario: ");
            String userId = sc.nextLine().trim();
            // Si no ingresa id, se crea uno por defecto usando timestamp
            if (userId.isEmpty()) {
                userId = "usuario_" + System.currentTimeMillis();
            }

            // Bucle principal del cliente: muestra menú y realiza llamadas remotas
            while (true) {
                System.out.println("\n=== MENÚ BIBLIOTECA ===");
                System.out.println("Usuario: " + userId);
                System.out.println("1. Consultar por ISBN");
                System.out.println("2. Préstamo por ISBN");
                System.out.println("3. Préstamo por Título");
                System.out.println("4. Devolver libro");
                System.out.println("0. Salir");
                System.out.print("Seleccione opción: ");

                String opt = sc.nextLine().trim(); // lectura de la opción
                switch (opt) {
                    case "1":
                        // Opción 1: consulta remota por ISBN
                        System.out.print("ISBN: ");
                        String isbn = sc.nextLine().trim();
                        QueryResponse qr = service.queryByISBN(isbn); // llamada RMI
                        if (!qr.found) {
                            System.out.println("No encontrado."); 
                        }else {
                            System.out.printf("Título: %s | total: %d | disponibles: %d%n",
                                    qr.title, qr.totalCopies, qr.availableCopies);
                        }
                        break;
                    case "2":
                        // Opción 2: préstamo remoto por ISBN
                        System.out.print("ISBN: ");
                        isbn = sc.nextLine().trim();
                        LoanResponse lr1 = service.loanByISBN(isbn, userId); // llamada RMI
                        System.out.println(lr1.message);
                        break;
                    case "3":
                        // Opción 3: préstamo remoto por título
                        System.out.print("Título: ");
                        String title = sc.nextLine().trim();
                        LoanResponse lr2 = service.loanByTitle(title, userId); // llamada RMI
                        System.out.println(lr2.message);
                        break;
                    case "4":
                        // Opción 4: devolver libro
                        System.out.print("ISBN: ");
                        isbn = sc.nextLine().trim();
                        ReturnResponse rr = service.returnBook(isbn, userId); // llamada RMI
                        System.out.println(rr.message);
                        break;
                    case "0":
                        // Salir del programa
                        System.out.println("Saliendo...");
                        return; // termina la ejecución del main (no cierra explicitamente el Scanner)
                    default:
                        System.out.println("Opción inválida.");
                }
            }

        } catch (Exception e) {
            // Si ocurre cualquier excepción (conexión RMI, remote exception, etc.)
            System.err.println("Error en el cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
