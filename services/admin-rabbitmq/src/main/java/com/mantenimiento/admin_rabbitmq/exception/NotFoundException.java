package com.mantenimiento.admin_rabbitmq.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}