package com.bca.rentora.rentora.exceptions;

import com.bca.rentora.rentora.dtos.errors.Errorresponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
//    making a class to handle resource not found exception
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Errorresponse>handleResourceNotFoundException(ResourceNotFoundException ex){
        Errorresponse resp = new Errorresponse(ex.getMessage(), HttpStatus.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Errorresponse>handleIllegalArgumentException(IllegalArgumentException ex){
        Errorresponse resp = new Errorresponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
    }
    @ExceptionHandler({
            BadCredentialsException.class,
            InternalAuthenticationServiceException.class
    })
    public ResponseEntity<Errorresponse> handleBadCredentialsException(Exception e){
        Errorresponse resp = new Errorresponse(e.getMessage(),HttpStatus.BAD_REQUEST);
        return ResponseEntity.badRequest().body(resp);
    }

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<String> handleRuntime(RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
            return ResponseEntity.badRequest().body(ex.getBindingResult().getFieldError().getDefaultMessage());
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<String> handleBadJson(HttpMessageNotReadableException ex) {
            return ResponseEntity.badRequest().body("Malformed request body");
        }

     @ExceptionHandler(Exception.class)
     public ResponseEntity<Errorresponse> handleGeneric(Exception ex) {
        ex.printStackTrace(); // TEMP: so you see it in console
        Errorresponse resp = new Errorresponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
     }
}

