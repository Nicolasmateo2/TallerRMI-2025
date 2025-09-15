// ReturnResponse.java
package com.unal.rmilibrary;

import java.io.Serializable;

// Respuesta de la devolución de un libro que es serializable para la BD
public class ReturnResponse implements Serializable {

    public final boolean success;
    public final String message;

    public ReturnResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
