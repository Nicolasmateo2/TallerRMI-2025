// LibraryClient.java
package com.unal.rmilibrary;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class LibraryClient {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 1099;
        if (args.length >= 1) host = args[0];
        Registry registry = LocateRegistry.getRegistry(host, port);
        LibraryService service = (LibraryService) registry.lookup("LibraryService");

        Scanner sc = new Scanner(System.in);
        System.out.print("Tu userId: ");
        String userId = sc.nextLine().trim();
        while (true) {
            System.out.println("\nOpciones: 1) Consultar por ISBN 2) Prestar por ISBN 3) Prestar por Título 4) Devolver por ISBN 5) Salir");
            String opt = sc.nextLine().trim();
            if (opt.equals("1")) {
                System.out.print("ISBN: ");
                String isbn = sc.nextLine().trim();
                QueryResponse qr = service.queryByISBN(isbn);
                if (!qr.found) System.out.println("No encontrado.");
                else System.out.printf("Título: %s | total: %d | disponibles: %d%n", qr.title, qr.totalCopies, qr.availableCopies);
            } else if (opt.equals("2")) {
                System.out.print("ISBN: ");
                String isbn = sc.nextLine().trim();
                LoanResponse lr = service.loanByISBN(isbn, userId);
                System.out.println(lr.message + (lr.dueDate != null ? " — Devuelve: " + lr.dueDate.format(DateTimeFormatter.ISO_DATE) : ""));
            } else if (opt.equals("3")) {
                System.out.print("Título: ");
                String title = sc.nextLine().trim();
                LoanResponse lr = service.loanByTitle(title, userId);
                System.out.println(lr.message + (lr.dueDate != null ? " — Devuelve: " + lr.dueDate.format(DateTimeFormatter.ISO_DATE) : ""));
            } else if (opt.equals("4")) {
                System.out.print("ISBN: ");
                String isbn = sc.nextLine().trim();
                ReturnResponse rr = service.returnBook(isbn, userId);
                System.out.println(rr.message);
            } else {
                break;
            }
        }
        sc.close();
        System.out.println("Cliente terminado.");
    }
}
