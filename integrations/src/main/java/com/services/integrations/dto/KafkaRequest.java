package com.services.integrations.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @Author prakashponali
 * @Date 14/11/23
 * @Description
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class KafkaRequest {

    private String key;
    private String message;
}
