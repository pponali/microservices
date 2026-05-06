package com.services.authorization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

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
}
