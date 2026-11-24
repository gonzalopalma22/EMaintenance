package com.mantenimiento.admin_kafka.dto;

import java.util.List;

/**
 * Detalle de un consumer group: miembros, particiones asignadas y lag.
 */
public record GroupDetail(
        String groupId,
        String estado,
        String coordinador,
        String asignador,
        long lagTotal,
        List<Miembro> miembros,
        List<Offset> offsets) {

    /**
     * Consumidor conectado al grupo.
     *
     * @param consumerId  id interno asignado por el broker
     * @param clientId    client.id configurado (ej. analitica-consumer-0)
     * @param host        host desde el que se conecta
     * @param particiones particiones asignadas, formato topic-partición
     */
    public record Miembro(String consumerId, String clientId, String host, List<String> particiones) {
    }

    /**
     * Posición del grupo en una partición.
     *
     * @param topic      nombre del topic
     * @param particion  número de partición
     * @param offset     último offset confirmado (commit) por el grupo
     * @param finLog     offset final de la partición (log end offset)
     * @param lag        finLog - offset
     */
    public record Offset(String topic, int particion, long offset, long finLog, long lag) {
    }
}