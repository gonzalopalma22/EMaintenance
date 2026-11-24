package com.mantenimiento.admin_kafka.dto;

/**
 * Resumen de un consumer group para el listado.
 *
 * @param groupId  identificador del grupo (group.id del consumidor)
 * @param miembros cantidad de consumidores conectados ahora mismo
 * @param estado   ACTIVO si tiene miembros, SIN_MIEMBROS si no
 */
public record GroupInfo(String groupId, int miembros, String estado) {
}