package com.mantenimiento.admin_rabbitmq.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}