package com.example.exception;

import com.example.dto.ProxyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author：baimuyunxi
 * @Package：com.example.exception
 * @Project：xunFeiProxy
 * @name：GlobalExceptionHandler
 * @Date：2025/5/16 09:43
 * @Filename：GlobalExceptionHandler
 */
@ControllerAdvice
@RestController
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProxyResponse> handleException(Exception e) {
        log.error("Unexpected error occurred: ", e);

        ProxyResponse response = new ProxyResponse(false,
                "Internal server error: " + e.getMessage());

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProxyResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Invalid request parameter: ", e);

        ProxyResponse response = new ProxyResponse(false,
                "Invalid parameter: " + e.getMessage());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
