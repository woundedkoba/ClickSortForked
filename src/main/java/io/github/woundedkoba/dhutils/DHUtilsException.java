package io.github.woundedkoba.dhutils;

import java.io.Serial;

public class DHUtilsException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DHUtilsException(String message) {
        super(message);
    }
}
