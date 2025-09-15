// LibraryService.java
package com.unal.rmilibrary;

import java.rmi.Remote;
import java.rmi.RemoteException;

//Interfaz con cada uno de los métodos a implementar que extiende a Remote.
public interface LibraryService extends Remote {

    LoanResponse loanByISBN(String isbn, String userId) throws RemoteException;

    LoanResponse loanByTitle(String title, String userId) throws RemoteException;

    QueryResponse queryByISBN(String isbn) throws RemoteException;

    ReturnResponse returnBook(String isbn, String userId) throws RemoteException;
}
