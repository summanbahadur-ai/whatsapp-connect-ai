package com.whatsappconnect.auth;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/** Never echo rejected values, SQL text, credential material or exception messages. */
@RestControllerAdvice(assignableTypes=AuthController.class)
@Profile("database")
public class AuthErrors {
    @ExceptionHandler(SessionService.InvalidCredentials.class)
    ResponseEntity<Map<String,String>> credentials(){return error(401,"INVALID_CREDENTIALS","Invalid email or password.");}
    @ExceptionHandler(SignupService.DuplicateEmail.class)
    ResponseEntity<Map<String,String>> duplicate() {
        return error(409,"EMAIL_ALREADY_EXISTS","An account with this email already exists.");
    }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class})
    ResponseEntity<Map<String,String>> invalid() {
        return error(400,"INVALID_SIGNUP","Enter a valid name, email and password (8 to 72 characters).");
    }
    @ExceptionHandler(SignupService.InvalidPassword.class)
    ResponseEntity<Map<String,String>> password() {
        return error(400,"INVALID_PASSWORD","Password must contain at least 8 characters and at most 72 UTF-8 bytes.");
    }
    @ExceptionHandler({DataAccessException.class,TransactionException.class})
    ResponseEntity<Map<String,String>> storage() {
        return error(503,"AUTH_UNAVAILABLE","Authentication is temporarily unavailable. Try again shortly.");
    }
    private ResponseEntity<Map<String,String>> error(int status,String code,String message) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("error",message,"code",code));
    }
}
