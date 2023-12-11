package org.service.accountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
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


@Schema(name = "Response DTO", description = "Response DTO")
@Data
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false, of = {"status", "message"})
@ToString(callSuper = false, of = {"status", "message"})
public class ResponseDto implements Serializable{
    @Schema(name = "status", description = "description", example = "OK")
    String status;
    @Schema(name = "message", description = "description", example = "status message")
    String message;

}
