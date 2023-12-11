package com.services.integrations.controller;

import com.services.integrations.dto.KafkaRequest;
import com.services.integrations.service.KafkaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @Author prakashponali
 * @Date 14/11/23
 * @Description
 */

@RestController
@RequestMapping(value = "/api/v1")
public class KafkaController {

    @Autowired
    private KafkaService kafkaService;

    @PostMapping(value = "/publish")
    ResponseEntity<?> publishMessage(@RequestBody KafkaRequest kafkaRequest) {
        try {
            kafkaService.publish(kafkaRequest);
            return ResponseEntity.ok("Message is published !!");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
