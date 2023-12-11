package org.service.accountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@Schema(name = "ErrorResponseDto", description = "Error Response Dto")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDto {
    @Schema(name = "Api Path", example = "/api/v1/accounts/create")
    @Size(min = 5, max = 30, message = "Api Path should be min of 5 and max of 30 characters")
    @NotEmpty(message = "Api Path should not be empty")
    String apiPath;

    @Schema(name = "error code" ,description =  "error code")
    @NotEmpty(message = "error code should not be empty")
    @Size(min = 5, max = 30, message = "error code should be min of 5 and max of 30 characters")
    HttpStatus errorCode;

    @Schema(name = "message" ,description =  "error message")
    String message;

    @Schema(name = "time stamp", description = "time stamp")
    LocalDateTime timeStamp;


}
