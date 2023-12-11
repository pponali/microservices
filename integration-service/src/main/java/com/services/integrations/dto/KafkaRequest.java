package com.services.integrations.dto;

import lombok.*;

/**
 * @Author prakashponali
 * @Date 14/11/23
 * @Description
 */


@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class KafkaRequest {

    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private String message;
}
