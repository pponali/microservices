package org.service.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@Data
@AllArgsConstructor
public class ErrorResponseDto {
    String apiPath;
    LocalDateTime timeStamp;
    String message;
    String errorCode;

    public ErrorResponseDto(String description, HttpStatus httpStatus, String message, LocalDateTime now) {
    }
}
