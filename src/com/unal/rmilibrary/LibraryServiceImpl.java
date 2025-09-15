// LibraryServiceImpl.java
package com.unal.rmilibrary;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.Optional;

// Implementación del servicio de biblioteca extiende a UnicastRemoteObject que permite el uso de RMI
public class LibraryServiceImpl extends UnicastRemoteObject implements LibraryService {

    private final BookDAO dao;

    protected LibraryServiceImpl(String jdbcUrl) throws RemoteException, SQLException {
        super();
        this.dao = new BookDAO(jdbcUrl);
    }

    @Override
    public LoanResponse loanByISBN(String isbn, String userId) throws RemoteException {
        return dao.loanByISBN(isbn, userId);
    }

    @Override
    public LoanResponse loanByTitle(String title, String userId) throws RemoteException {
        return dao.loanByTitle(title, userId);
    }

    @Override
    public QueryResponse queryByISBN(String isbn) throws RemoteException {
        try {
            Optional<QueryResponse> r = dao.queryByISBN(isbn);
            return r.orElse(new QueryResponse(false, "", 0, 0));
        } catch (SQLException ex) {
            throw new RemoteException("Error DB", ex);
        }
    }

    @Override
    public ReturnResponse returnBook(String isbn, String userId) throws RemoteException {
        return dao.returnBook(isbn, userId);
    }
}
