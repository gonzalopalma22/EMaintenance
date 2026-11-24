package com.mantenimiento.admin_kafka.controller;

import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mantenimiento.admin_kafka.service.KafkaAdminException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(UnknownTopicOrPartitionException.class)
    public ProblemDetail noExiste(UnknownTopicOrPartitionException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "El tópico no existe");
    }

    @ExceptionHandler(GroupIdNotFoundException.class)
    public ProblemDetail grupoNoExiste(GroupIdNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TopicExistsException.class)
    public ProblemDetail yaExiste(TopicExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "El tópico ya existe");
    }

    @ExceptionHandler({
            InvalidTopicException.class,
            InvalidPartitionsException.class,
            InvalidReplicationFactorException.class,
            InvalidConfigurationException.class,
            InvalidRequestException.class,
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class})
    public ProblemDetail solicitudInvalida(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Solicitud inválida");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler(KafkaAdminException.class)
    public ProblemDetail tiempoAgotado(KafkaAdminException ex) {
        log.error("Fallo administrando Kafka: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage());
    }

    @ExceptionHandler(KafkaException.class)
    public ProblemDetail errorKafka(KafkaException ex) {
        log.error("Error de Kafka", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Error al comunicarse con Kafka");
    }
}