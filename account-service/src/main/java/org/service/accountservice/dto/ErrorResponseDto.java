package org.service.accountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@Schema(name = "ErrorResponseDto", description = "Error Response Dto")
@Data
@AllArgsConstructor
public class ErrorResponseDto {
    @Schema(name = "Api Path", example = "/api/v1/accounts/create")
    @Size(min = 5, max = 30, message = "Api Path should be min of 5 and max of 30 characters")
    @NotEmpty(message = "Api Path should not be empty")
    String apiPath;

    @Schema(name = "time stamp", description = "time stamp")
    LocalDateTime timeStamp;
    @Schema(name = "message" ,description =  "error message")
    String message;

    @Schema(name = "error code" ,description =  "error code")
    @NotEmpty(message = "error code should not be empty")
    @Size(min = 5, max = 30, message = "error code should be min of 5 and max of 30 characters")
    String errorCode;

    public ErrorResponseDto(String description, HttpStatus httpStatus, String message, LocalDateTime now) {
    }
}
