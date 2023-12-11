package org.service.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

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
