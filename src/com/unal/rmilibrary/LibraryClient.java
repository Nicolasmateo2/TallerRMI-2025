package com.unal.rmilibrary;

import java.rmi.Naming;
import java.util.Scanner;

public class LibraryClient {
    public static void main(String[] args) {
        try {
            Scanner sc = new Scanner(System.in);

            // Pedir IP o usar localhost
            System.out.print("Ingrese la IP del servidor (enter para localhost): ");
            String servidor = sc.nextLine().trim();
            if (servidor.isEmpty()) servidor = "localhost";

            String url = "rmi://" + servidor + ":1099/LibraryService";
            LibraryService service = (LibraryService) Naming.lookup(url);
            System.out.println("Conectado a " + url);

            System.out.print("Ingrese su ID de usuario: ");
            String userId = sc.nextLine().trim();
            if (userId.isEmpty()) userId = "usuario_" + System.currentTimeMillis();

            while (true) {
                System.out.println("\n=== MENÚ BIBLIOTECA ===");
                System.out.println("Usuario: " + userId);
                System.out.println("1. Consultar por ISBN");
                System.out.println("2. Préstamo por ISBN");
                System.out.println("3. Préstamo por Título");
                System.out.println("4. Devolver libro");
                System.out.println("0. Salir");
                System.out.print("Seleccione opción: ");

                String opt = sc.nextLine().trim();
                switch (opt) {
                    case "1":
                        System.out.print("ISBN: ");
                        String isbn = sc.nextLine().trim();
                        QueryResponse qr = service.queryByISBN(isbn);
                        if (!qr.found) System.out.println("No encontrado.");
                        else System.out.printf("Título: %s | total: %d | disponibles: %d%n",
                                qr.title, qr.totalCopies, qr.availableCopies);
                        break;
                    case "2":
                        System.out.print("ISBN: ");
                        isbn = sc.nextLine().trim();
                        LoanResponse lr1 = service.loanByISBN(isbn, userId);
                        System.out.println(lr1.message);
                        break;
                    case "3":
                        System.out.print("Título: ");
                        String title = sc.nextLine().trim();
                        LoanResponse lr2 = service.loanByTitle(title, userId);
                        System.out.println(lr2.message);
                        break;
                    case "4":
                        System.out.print("ISBN: ");
                        isbn = sc.nextLine().trim();
                        ReturnResponse rr = service.returnBook(isbn, userId);
                        System.out.println(rr.message);
                        break;
                    case "0":
                        System.out.println("Saliendo...");
                        return;
                    default:
                        System.out.println("Opción inválida.");
                }
            }

        } catch (Exception e) {
            System.err.println("Error en el cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
