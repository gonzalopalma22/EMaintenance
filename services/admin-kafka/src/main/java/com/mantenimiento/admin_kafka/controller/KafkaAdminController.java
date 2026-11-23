package com.mantenimiento.admin_kafka.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mantenimiento.admin_kafka.dto.ClusterInfo;
import com.mantenimiento.admin_kafka.dto.ConfigRequest;
import com.mantenimiento.admin_kafka.dto.PartitionsRequest;
import com.mantenimiento.admin_kafka.dto.TopicDetail;
import com.mantenimiento.admin_kafka.dto.TopicInfo;
import com.mantenimiento.admin_kafka.dto.TopicRequest;
import com.mantenimiento.admin_kafka.service.KafkaAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping("/cluster")
    public ClusterInfo cluster() {
        return service.cluster();
    }

    @GetMapping("/topics")
    public List<TopicInfo> listar() {
        return service.listarTopics();
    }

    @GetMapping("/topics/{nombre}")
    public TopicDetail obtener(@PathVariable String nombre,
                               @RequestParam(defaultValue = "false") boolean todas) {
        return service.obtenerTopic(nombre, todas);
    }

    @PostMapping("/topics")
    public ResponseEntity<TopicInfo> crear(@Valid @RequestBody TopicRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearTopic(req));
    }

    @PatchMapping("/topics/{nombre}/partitions")
    public Map<String, Object> aumentarParticiones(@PathVariable String nombre,
                                                   @Valid @RequestBody PartitionsRequest req) {
        service.aumentarParticiones(nombre, req.partitions());
        return Map.of("topic", nombre, "partitions", req.partitions());
    }

    @PutMapping("/topics/{nombre}/config")
    public TopicDetail modificarConfig(@PathVariable String nombre,
                                       @Valid @RequestBody ConfigRequest req) {
        service.modificarConfig(nombre, req.configs());
        return service.obtenerTopic(nombre, false);
    }

    @DeleteMapping("/topics/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre) {
        service.eliminarTopic(nombre);
        return ResponseEntity.noContent().build();
    }
}