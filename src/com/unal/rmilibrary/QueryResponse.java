// QueryResponse.java
package com.unal.rmilibrary;

import java.io.Serializable;

public class QueryResponse implements Serializable {
    public final boolean found;
    public final String title;
    public final int totalCopies;
    public final int availableCopies;

    public QueryResponse(boolean found, String title, int totalCopies, int availableCopies) {
        this.found = found;
        this.title = title;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }
}