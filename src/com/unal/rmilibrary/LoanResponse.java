// LoanResponse.java
package com.unal.rmilibrary;

import java.io.Serializable;
import java.time.LocalDate;

// Respuesta de un préstamo
public class LoanResponse implements Serializable {

    public final boolean success;
    public final String message;
    public final LocalDate dueDate; // null si success==false

    public LoanResponse(boolean success, String message, LocalDate dueDate) {
        this.success = success;
        this.message = message;
        this.dueDate = dueDate;
    }
}
