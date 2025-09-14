// ReturnResponse.java
package com.unal.rmilibrary;

import java.io.Serializable;

public class ReturnResponse implements Serializable {
    public final boolean success;
    public final String message;

    public ReturnResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}