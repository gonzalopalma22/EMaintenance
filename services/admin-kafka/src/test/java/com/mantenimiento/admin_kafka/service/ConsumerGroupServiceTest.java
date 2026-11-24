package com.mantenimiento.admin_kafka.service;

import com.mantenimiento.admin_kafka.dto.GroupDetail;
import com.mantenimiento.admin_kafka.dto.GroupInfo;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsumerGroupServiceTest {

    private static final String GRUPO = "analitica";
    private static final TopicPartition P0 = new TopicPartition("telemetria.lecturas", 0);
    private static final TopicPartition P1 = new TopicPartition("telemetria.lecturas", 1);

    private Admin admin;
    private ConsumerGroupService service;

    @BeforeEach
    void preparar() {
        admin = mock(Admin.class);
        service = new ConsumerGroupService(admin);

        // listado: existe un solo grupo
        ConsumerGroupListing listing = mock(ConsumerGroupListing.class);
        when(listing.groupId()).thenReturn(GRUPO);
        ListConsumerGroupsResult listado = mock(ListConsumerGroupsResult.class);
        when(listado.valid()).thenReturn(KafkaFuture.completedFuture(List.of(listing)));
        when(admin.listConsumerGroups()).thenReturn(listado);

        // descripción: un miembro con las particiones 0 y 1
        MemberAssignment asignacion = mock(MemberAssignment.class);
        when(asignacion.topicPartitions()).thenReturn(Set.of(P0, P1));
        MemberDescription miembro = mock(MemberDescription.class);
        when(miembro.consumerId()).thenReturn("analitica-consumer-0-abc");
        when(miembro.clientId()).thenReturn("analitica-consumer-0");
        when(miembro.host()).thenReturn("/172.18.0.9");
        when(miembro.assignment()).thenReturn(asignacion);

        ConsumerGroupDescription descripcion = mock(ConsumerGroupDescription.class);
        when(descripcion.members()).thenReturn(List.of(miembro));
        when(descripcion.coordinator()).thenReturn(new Node(1, "kafka-1", 29092));
        when(descripcion.partitionAssignor()).thenReturn("cooperative-sticky");

        DescribeConsumerGroupsResult describe = mock(DescribeConsumerGroupsResult.class);
        when(describe.all()).thenReturn(KafkaFuture.completedFuture(Map.of(GRUPO, descripcion)));
        when(admin.describeConsumerGroups(anyCollection())).thenReturn(describe);
    }

    @Test
    void listaLosGruposConSusMiembros() {
        List<GroupInfo> grupos = service.listar();

        assertThat(grupos).hasSize(1);
        assertThat(grupos.get(0).groupId()).isEqualTo(GRUPO);
        assertThat(grupos.get(0).miembros()).isEqualTo(1);
        assertThat(grupos.get(0).estado()).isEqualTo("ACTIVO");
    }

    @Test
    void calculaElLagPorParticionYElTotal() {
        // el grupo confirmó 10 en P0 (fin 25) y 5 en P1 (fin 5)
        ListConsumerGroupOffsetsResult confirmados = mock(ListConsumerGroupOffsetsResult.class);
        when(confirmados.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(
                Map.of(P0, new OffsetAndMetadata(10), P1, new OffsetAndMetadata(5))));
        when(admin.listConsumerGroupOffsets(GRUPO)).thenReturn(confirmados);

        ListOffsetsResultInfo fin0 = mock(ListOffsetsResultInfo.class);
        when(fin0.offset()).thenReturn(25L);
        ListOffsetsResultInfo fin1 = mock(ListOffsetsResultInfo.class);
        when(fin1.offset()).thenReturn(5L);
        ListOffsetsResult finales = mock(ListOffsetsResult.class);
        when(finales.all()).thenReturn(KafkaFuture.completedFuture(Map.of(P0, fin0, P1, fin1)));
        when(admin.listOffsets(anyMap())).thenReturn(finales);

        GroupDetail detalle = service.obtener(GRUPO);

        assertThat(detalle.lagTotal()).isEqualTo(15);
        assertThat(detalle.asignador()).isEqualTo("cooperative-sticky");
        assertThat(detalle.coordinador()).isEqualTo("kafka-1:29092");
        assertThat(detalle.miembros()).hasSize(1);
        assertThat(detalle.miembros().get(0).particiones())
                .containsExactly("telemetria.lecturas-0", "telemetria.lecturas-1");
        assertThat(detalle.offsets()).hasSize(2);
        assertThat(detalle.offsets().get(0).lag()).isEqualTo(15);
        assertThat(detalle.offsets().get(1).lag()).isZero();
    }

    @Test
    void grupoInexistenteLanzaNoEncontrado() {
        assertThatThrownBy(() -> service.obtener("no-existe"))
                .isInstanceOf(GroupIdNotFoundException.class);
    }
}