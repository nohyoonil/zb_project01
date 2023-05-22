package com.example.account.exception;

import com.example.account.dto.ErrorResponse;
import com.example.account.type.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.example.account.type.ErrorCode.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountException.class)
    public ErrorResponse handleAccountException(AccountException e) {
        log.error("{} is occurred. ", e.getErrorCode());

        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleAccountException(Exception e) {
        log.error("Exception is occurred. ", e);

        return new ErrorResponse(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getDescription());
    }
}
