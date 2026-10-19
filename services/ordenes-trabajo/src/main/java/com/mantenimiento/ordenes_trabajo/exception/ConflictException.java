package com.mantenimiento.ordenes_trabajo.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}