// BookDAO.java
package com.unal.rmilibrary;

import java.sql.*;              // Librerías JDBC para trabajar con SQLite
import java.time.LocalDate;     // Manejo de fechas (préstamos, devoluciones)
import java.util.Optional;      // Para devolver valores opcionales (puede o no haber resultado)

public class BookDAO {

    private final String url; // Cadena de conexión JDBC (ej: "jdbc:sqlite:library.db")

    // Constructor: recibe la URL de conexión JDBC
    // y asegura que las tablas 'books' y 'loans' existan (si no, las crea)
    public BookDAO(String jdbcUrl) throws SQLException {
        this.url = jdbcUrl;
        try (Connection conn = getConnection()) {
            // Inicializa tablas si no existen
            try (Statement st = conn.createStatement()) {
                // Tabla de libros: cada libro identificado por ISBN
                st.executeUpdate("CREATE TABLE IF NOT EXISTS books ("
                        + "isbn TEXT PRIMARY KEY, "
                        + "title TEXT NOT NULL, "
                        + "author TEXT, "
                        + "total_copies INTEGER NOT NULL, "
                        + "available_copies INTEGER NOT NULL)");

                // Tabla de préstamos: cada préstamo asociado a un libro y un usuario
                st.executeUpdate("CREATE TABLE IF NOT EXISTS loans ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "isbn TEXT NOT NULL, "
                        + "user_id TEXT NOT NULL, "
                        + "loan_date TEXT NOT NULL, "
                        + "due_date TEXT NOT NULL, "
                        + "returned INTEGER NOT NULL DEFAULT 0)");
            }
        }
    }

    // Obtiene una conexión a la base de datos SQLite
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    // Consulta un libro por su ISBN
    // Retorna un QueryResponse dentro de Optional si existe, o vacío si no
    public synchronized Optional<QueryResponse> queryByISBN(String isbn) throws SQLException {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT title, total_copies, available_copies FROM books WHERE isbn = ?")) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Se encontró el libro → devolver detalles
                    return Optional.of(new QueryResponse(
                            true,
                            rs.getString("title"),
                            rs.getInt("total_copies"),
                            rs.getInt("available_copies")
                    ));
                } else {
                    // No se encontró el libro
                    return Optional.empty();
                }
            }
        }
    }

    // Registrar un préstamo de libro por ISBN
    public synchronized LoanResponse loanByISBN(String isbn, String userId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Transacción manual para consistencia

            // Verificar disponibilidad
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT available_copies FROM books WHERE isbn = ?")) {
                ps.setString(1, isbn);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // No existe el libro
                        conn.rollback();
                        return new LoanResponse(false, "Libro no encontrado (ISBN).", null);
                    }
                    int available = rs.getInt("available_copies");
                    if (available <= 0) {
                        // No hay ejemplares disponibles
                        conn.rollback();
                        return new LoanResponse(false, "No hay ejemplares disponibles.", null);
                    }
                }
            }

            // Reducir en 1 la cantidad de ejemplares disponibles
            try (PreparedStatement ps1 = conn.prepareStatement(
                    "UPDATE books SET available_copies = available_copies - 1 WHERE isbn = ?")) {
                ps1.setString(1, isbn);
                ps1.executeUpdate();
            }

            // Calcular fecha de devolución (7 días después de hoy)
            LocalDate due = LocalDate.now().plusDays(7);

            // Registrar el préstamo en la tabla 'loans'
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO loans(isbn, user_id, loan_date, due_date, returned) VALUES (?, ?, ?, ?, 0)")) {
                ps2.setString(1, isbn);
                ps2.setString(2, userId);
                ps2.setString(3, LocalDate.now().toString());
                ps2.setString(4, due.toString());
                ps2.executeUpdate();
            }

            conn.commit(); // Confirmar transacción
            return new LoanResponse(true, "Préstamo confirmado.", due);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return new LoanResponse(false, "Error interno: " + ex.getMessage(), null);
        }
    }

    // Registrar un préstamo de libro por título
    public synchronized LoanResponse loanByTitle(String title, String userId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            String isbnFound = null;

            // Buscar libro por título (el que tenga más disponibles primero)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT isbn, available_copies FROM books WHERE title = ? ORDER BY available_copies DESC")) {
                ps.setString(1, title);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return new LoanResponse(false, "Libro no encontrado por título.", null);
                    }
                    isbnFound = rs.getString("isbn");
                    int available = rs.getInt("available_copies");
                    if (available <= 0) {
                        conn.rollback();
                        return new LoanResponse(false, "No hay ejemplares disponibles para ese título.", null);
                    }
                }
            }

            // Reducir en 1 la cantidad de ejemplares disponibles
            try (PreparedStatement ps1 = conn.prepareStatement(
                    "UPDATE books SET available_copies = available_copies - 1 WHERE isbn = ?")) {
                ps1.setString(1, isbnFound);
                ps1.executeUpdate();
            }

            // Calcular fecha de devolución
            LocalDate due = LocalDate.now().plusDays(7);

            // Insertar préstamo en tabla 'loans'
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO loans(isbn, user_id, loan_date, due_date, returned) VALUES (?, ?, ?, ?, 0)")) {
                ps2.setString(1, isbnFound);
                ps2.setString(2, userId);
                ps2.setString(3, LocalDate.now().toString());
                ps2.setString(4, due.toString());
                ps2.executeUpdate();
            }

            conn.commit();
            return new LoanResponse(true, "Préstamo confirmado (por título).", due);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return new LoanResponse(false, "Error interno: " + ex.getMessage(), null);
        }
    }

    // Registrar la devolución de un libro
    public synchronized ReturnResponse returnBook(String isbn, String userId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            // Buscar préstamo activo (no devuelto) para ese usuario e ISBN
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM loans WHERE isbn = ? AND user_id = ? AND returned = 0 LIMIT 1")) {
                ps.setString(1, isbn);
                ps.setString(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return new ReturnResponse(false, "No se encontró préstamo activo para ese usuario y ISBN.");
                    }
                    int loanId = rs.getInt("id");

                    // Marcar préstamo como devuelto
                    try (PreparedStatement ps2 = conn.prepareStatement(
                            "UPDATE loans SET returned = 1 WHERE id = ?")) {
                        ps2.setInt(1, loanId);
                        ps2.executeUpdate();
                    }

                    // Incrementar en 1 la cantidad de copias disponibles
                    try (PreparedStatement ps3 = conn.prepareStatement(
                            "UPDATE books SET available_copies = available_copies + 1 WHERE isbn = ?")) {
                        ps3.setString(1, isbn);
                        ps3.executeUpdate();
                    }
                }
            }

            conn.commit();
            return new ReturnResponse(true, "Devolución registrada correctamente.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            return new ReturnResponse(false, "Error interno: " + ex.getMessage());
        }
    }
}
