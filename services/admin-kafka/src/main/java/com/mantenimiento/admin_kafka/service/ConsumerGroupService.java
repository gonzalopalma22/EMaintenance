package com.mantenimiento.admin_kafka.service;

import com.mantenimiento.admin_kafka.dto.GroupDetail;
import com.mantenimiento.admin_kafka.dto.GroupInfo;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Administración de consumer groups: listado, detalle de miembros y lag.
 * Usa solo el AdminClient; no consume mensajes ni altera offsets.
 */
@Service
public class ConsumerGroupService {

    private static final long TIMEOUT_SEGUNDOS = 15;

    private final Admin admin;

    public ConsumerGroupService(Admin admin) {
        this.admin = admin;
    }

    public List<GroupInfo> listar() {
        List<String> ids = idsDeGrupos();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, ConsumerGroupDescription> descripciones =
                esperar(admin.describeConsumerGroups(ids).all());

        return ids.stream()
                .map(id -> {
                    int miembros = descripciones.get(id).members().size();
                    return new GroupInfo(id, miembros, estado(miembros));
                })
                .toList();
    }

    public GroupDetail obtener(String groupId) {
        if (!idsDeGrupos().contains(groupId)) {
            throw new GroupIdNotFoundException("El consumer group no existe: " + groupId);
        }

        ConsumerGroupDescription descripcion =
                esperar(admin.describeConsumerGroups(List.of(groupId)).all()).get(groupId);

        List<GroupDetail.Miembro> miembros = descripcion.members().stream()
                .map(this::aMiembro)
                .sorted(Comparator.comparing(GroupDetail.Miembro::clientId))
                .toList();

        List<GroupDetail.Offset> offsets = calcularOffsets(groupId);
        long lagTotal = offsets.stream().mapToLong(GroupDetail.Offset::lag).sum();

        return new GroupDetail(
                groupId,
                estado(miembros.size()),
                nodo(descripcion.coordinator()),
                descripcion.partitionAssignor(),
                lagTotal,
                miembros,
                offsets);
    }

    // ---- internos ----

    private List<String> idsDeGrupos() {
        Collection<ConsumerGroupListing> grupos = esperar(admin.listConsumerGroups().valid());
        return grupos.stream().map(ConsumerGroupListing::groupId).sorted().toList();
    }

    private GroupDetail.Miembro aMiembro(MemberDescription m) {
        List<String> particiones = m.assignment().topicPartitions().stream()
                .sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
                .map(tp -> tp.topic() + "-" + tp.partition())
                .toList();
        return new GroupDetail.Miembro(m.consumerId(), m.clientId(), m.host(), particiones);
    }

    private List<GroupDetail.Offset> calcularOffsets(String groupId) {
        Map<TopicPartition, OffsetAndMetadata> confirmados =
                esperar(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());
        if (confirmados.isEmpty()) {
            return List.of();
        }

        Map<TopicPartition, OffsetSpec> pedido = new HashMap<>();
        confirmados.keySet().forEach(tp -> pedido.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResultInfo> finales = esperar(admin.listOffsets(pedido).all());

        return confirmados.entrySet().stream()
                .filter(e -> e.getValue() != null && finales.containsKey(e.getKey()))
                .map(e -> {
                    TopicPartition tp = e.getKey();
                    long offset = e.getValue().offset();
                    long fin = finales.get(tp).offset();
                    return new GroupDetail.Offset(tp.topic(), tp.partition(), offset, fin, Math.max(0, fin - offset));
                })
                .sorted(Comparator.comparing(GroupDetail.Offset::topic).thenComparingInt(GroupDetail.Offset::particion))
                .toList();
    }

    private static String estado(int miembros) {
        return miembros > 0 ? "ACTIVO" : "SIN_MIEMBROS";
    }

    private static String nodo(Node n) {
        return n == null ? "desconocido" : n.host() + ":" + n.port();
    }

    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminException("Operación interrumpida", e);
        } catch (TimeoutException e) {
            throw new KafkaAdminException("Tiempo de espera agotado consultando Kafka", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new KafkaAdminException("Error consultando Kafka: " + e.getCause().getMessage(), e.getCause());
        }
    }
}