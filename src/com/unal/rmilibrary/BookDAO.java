// BookDAO.java
package com.unal.rmilibrary;

import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

public class BookDAO {
    private final String url; // jdbc:sqlite:library.db

    public BookDAO(String jdbcUrl) throws SQLException {
        this.url = jdbcUrl;
        try (Connection conn = getConnection()) {
            // Inicializa tablas si no existen
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS books (isbn TEXT PRIMARY KEY, title TEXT NOT NULL, author TEXT, total_copies INTEGER NOT NULL, available_copies INTEGER NOT NULL)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS loans (id INTEGER PRIMARY KEY AUTOINCREMENT, isbn TEXT NOT NULL, user_id TEXT NOT NULL, loan_date TEXT NOT NULL, due_date TEXT NOT NULL, returned INTEGER NOT NULL DEFAULT 0)");
            }
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    public synchronized Optional<QueryResponse> queryByISBN(String isbn) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT title, total_copies, available_copies FROM books WHERE isbn = ?")) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new QueryResponse(true, rs.getString("title"), rs.getInt("total_copies"), rs.getInt("available_copies")));
                } else {
                    return Optional.empty();
                }
            }
        }
    }

    public synchronized LoanResponse loanByISBN(String isbn, String userId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            // comprobar disponibilidad
            try (PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE isbn = ?")) {
                ps.setString(1, isbn);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return new LoanResponse(false, "Libro no encontrado (ISBN).", null);
                    }
                    int available = rs.getInt("available_copies");
                    if (available <= 0) {
                        conn.rollback();
                        return new LoanResponse(false, "No hay ejemplares disponibles.", null);
                    }
                }
            }
            // decrementar y crear loan
            try (PreparedStatement ps1 = conn.prepareStatement("UPDATE books SET available_copies = available_copies - 1 WHERE isbn = ?")) {
                ps1.setString(1, isbn);
                ps1.executeUpdate();
            }
            LocalDate due = LocalDate.now().plusDays(7);
            try (PreparedStatement ps2 = conn.prepareStatement("INSERT INTO loans(isbn, user_id, loan_date, due_date, returned) VALUES (?, ?, ?, ?, 0)")) {
                ps2.setString(1, isbn);
                ps2.setString(2, userId);
                ps2.setString(3, LocalDate.now().toString());
                ps2.setString(4, due.toString());
                ps2.executeUpdate();
            }
            conn.commit();
            return new LoanResponse(true, "Préstamo confirmado.", due);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return new LoanResponse(false, "Error interno: " + ex.getMessage(), null);
        }
    }

    public synchronized LoanResponse loanByTitle(String title, String userId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            String isbnFound = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT isbn, available_copies FROM books WHERE title = ? ORDER BY available_copies DESC")) {
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
            // actualizar y crear loan
            try (PreparedStatement ps1 = conn.prepareStatement("UPDATE books SET available_copies = available_copies - 1 WHERE isbn = ?")) {
                ps1.setString(1, isbnFound);
                ps1.executeUpdate();
            }
            LocalDate due = LocalDate.now().plusDays(7);
            try (PreparedStatement ps2 = conn.prepareStatement("INSERT INTO loans(isbn, user_id, loan_date, due_date, returned) VALUES (?, ?, ?, ?, 0)")) {
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

    public synchronized ReturnResponse returnBook(String isbn, String userId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            // encontrar préstamo no devuelto del user
            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM loans WHERE isbn = ? AND user_id = ? AND returned = 0 LIMIT 1")) {
                ps.setString(1, isbn);
                ps.setString(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return new ReturnResponse(false, "No se encontró préstamo activo para ese usuario y ISBN.");
                    }
                    int loanId = rs.getInt("id");
                    try (PreparedStatement ps2 = conn.prepareStatement("UPDATE loans SET returned = 1 WHERE id = ?")) {
                        ps2.setInt(1, loanId);
                        ps2.executeUpdate();
                    }
                    try (PreparedStatement ps3 = conn.prepareStatement("UPDATE books SET available_copies = available_copies + 1 WHERE isbn = ?")) {
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
