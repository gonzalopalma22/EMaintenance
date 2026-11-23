package com.mantenimiento.admin_kafka.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mantenimiento.admin_kafka.dto.ClusterInfo;
import com.mantenimiento.admin_kafka.dto.TopicDetail;
import com.mantenimiento.admin_kafka.dto.TopicInfo;
import com.mantenimiento.admin_kafka.dto.TopicRequest;

@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);
    private static final long TIMEOUT_SEGUNDOS = 15;

    private final Admin admin;

    public KafkaAdminService(Admin admin) {
        this.admin = admin;
    }

    // ---------- Consultas ----------

    public ClusterInfo cluster() {
        DescribeClusterResult r = admin.describeCluster();
        String id = esperar(r.clusterId());
        Collection<Node> nodos = esperar(r.nodes());
        Node controlador = esperar(r.controller());
        List<ClusterInfo.Broker> brokers = nodos.stream()
                .map(n -> new ClusterInfo.Broker(n.id(), n.host(), n.port()))
                .sorted(Comparator.comparingInt(ClusterInfo.Broker::id))
                .toList();
        return new ClusterInfo(id, controlador == null ? -1 : controlador.id(), brokers);
    }

    public List<TopicInfo> listarTopics() {
        Set<String> nombres = esperar(admin.listTopics(new ListTopicsOptions().listInternal(false)).names());
        if (nombres.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames());
        return descripciones.values().stream()
                .map(d -> new TopicInfo(d.name(), d.partitions().size(), factorReplicacion(d), d.isInternal()))
                .sorted(Comparator.comparing(TopicInfo::name))
                .toList();
    }

    public TopicDetail obtenerTopic(String nombre, boolean todasLasConfigs) {
        TopicDescription d = esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = esperar(admin.describeConfigs(List.of(recurso)).all()).get(recurso);

        Map<String, String> configs = new TreeMap<>();
        for (ConfigEntry e : config.entries()) {
            if (todasLasConfigs || e.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG) {
                configs.put(e.name(), e.value());
            }
        }

        List<TopicDetail.Particion> particiones = d.partitions().stream()
                .map(p -> new TopicDetail.Particion(
                        p.partition(),
                        p.leader() == null ? -1 : p.leader().id(),
                        ids(p.replicas()),
                        ids(p.isr())))
                .toList();

        return new TopicDetail(d.name(), d.partitions().size(), factorReplicacion(d), particiones, configs);
    }

    // ---------- Operaciones que modifican ----------

    public TopicInfo crearTopic(TopicRequest req) {
        validarNombre(req.name());
        NewTopic nuevo = new NewTopic(req.name(), req.partitions(), req.replicationFactor().shortValue());
        if (req.configs() != null && !req.configs().isEmpty()) {
            nuevo.configs(req.configs());
        }
        esperar(admin.createTopics(List.of(nuevo)).all());
        log.info("Tópico creado. nombre={} particiones={} replicas={}",
                req.name(), req.partitions(), req.replicationFactor());
        return new TopicInfo(req.name(), req.partitions(), req.replicationFactor(), false);
    }

    public void aumentarParticiones(String nombre, int nuevas) {
        validarNombre(nombre);
        TopicDescription actual = esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
        int actuales = actual.partitions().size();
        if (nuevas <= actuales) {
            throw new IllegalArgumentException(
                    "Solo se pueden aumentar las particiones (actuales: " + actuales + ", pedidas: " + nuevas + ")");
        }
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(nuevas))).all());
        log.info("Particiones aumentadas. nombre={} de={} a={}", nombre, actuales, nuevas);
    }

    public void modificarConfig(String nombre, Map<String, String> configs) {
        validarNombre(nombre);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        List<AlterConfigOp> operaciones = configs.entrySet().stream()
                .map(e -> e.getValue() == null
                        ? new AlterConfigOp(new ConfigEntry(e.getKey(), null), AlterConfigOp.OpType.DELETE)
                        : new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
                .toList();
        esperar(admin.incrementalAlterConfigs(Map.of(recurso, operaciones)).all());
        log.info("Configuración modificada. nombre={} claves={}", nombre, configs.keySet());
    }

    public void eliminarTopic(String nombre) {
        validarNombre(nombre);
        esperar(admin.deleteTopics(List.of(nombre)).all());
        log.warn("Tópico eliminado. nombre={}", nombre);
    }

    // ---------- Utilidades ----------

    private void validarNombre(String nombre) {
        if (nombre.equals(".") || nombre.equals("..")) {
            throw new IllegalArgumentException("Nombre de tópico no permitido");
        }
        if (nombre.startsWith("__")) {
            throw new IllegalArgumentException("Los tópicos que empiezan con '__' están reservados para Kafka");
        }
    }

    private int factorReplicacion(TopicDescription d) {
        return d.partitions().isEmpty() ? 0 : d.partitions().get(0).replicas().size();
    }

    private List<Integer> ids(List<Node> nodos) {
        return nodos.stream().map(Node::id).toList();
    }

    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminException("Operación interrumpida", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new KafkaAdminException("Error al comunicarse con Kafka", e.getCause());
        } catch (TimeoutException e) {
            throw new KafkaAdminException("Kafka no respondió a tiempo", e);
        }
    }
}