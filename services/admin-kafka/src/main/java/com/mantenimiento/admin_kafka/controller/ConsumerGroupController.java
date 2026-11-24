package com.mantenimiento.admin_kafka.controller;

import com.mantenimiento.admin_kafka.dto.GroupDetail;
import com.mantenimiento.admin_kafka.dto.GroupInfo;
import com.mantenimiento.admin_kafka.service.ConsumerGroupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta de consumer groups. Solo lectura: la SecurityConfig ya permite
 * GET a Usuario y Admin sobre /api/kafka/**.
 */
@RestController
@RequestMapping("/api/kafka/groups")
public class ConsumerGroupController {

    private final ConsumerGroupService service;

    public ConsumerGroupController(ConsumerGroupService service) {
        this.service = service;
    }

    @GetMapping
    public List<GroupInfo> listar() {
        return service.listar();
    }

    @GetMapping("/{groupId}")
    public GroupDetail obtener(@PathVariable String groupId) {
        return service.obtener(groupId);
    }
}