package com.services.authorization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 *
 * <a href="https://martinfowler.com/eaaCatalog/dataTransferObject.html">DTO Pattern</a>
 */

@Data @AllArgsConstructor
public class ResponseDto implements Serializable {
    String status;
    String message;
}
