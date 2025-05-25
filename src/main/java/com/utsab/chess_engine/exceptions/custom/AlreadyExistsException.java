package com.utsab.chess_engine.exceptions.custom;

import lombok.Getter;

@Getter
public class AlreadyExistsException extends RuntimeException {
    private final String details;

    public AlreadyExistsException(String message, String details) {
        super(message);
        this.details = details;
    }

}