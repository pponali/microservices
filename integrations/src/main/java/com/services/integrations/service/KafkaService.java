package com.services.integrations.service;

import com.services.integrations.dto.KafkaRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * @Author prakashponali
 * @Date 14/11/23
 * @Description
 */

@Service
public class KafkaService {

    @Value("${kafka.topic}")
    private String topicName;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(KafkaRequest kafkaRequest) {
        CompletableFuture<SendResult<String, Object>> returnvalue = kafkaTemplate.send(topicName, kafkaRequest.getKey(), kafkaRequest.getMessage());
        returnvalue.whenComplete((result, ex) -> {
            if (ex == null) {
                System.out.println("Message Offset " + result.getRecordMetadata().offset() + " message parting " + result.getRecordMetadata().partition());
            } else {
                System.out.println(ex.getMessage());
            }
        });
    }


}
