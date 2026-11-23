package com.example.meetingrooms.api;
import com.example.meetingrooms.api.ApiDtos.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<ErrorResponse> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getStatusCode().toString(),e.getReason()==null?"Request failed":e.getReason()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e){String msg=e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).findFirst().orElse("Invalid request");return ResponseEntity.badRequest().body(new ErrorResponse("ValidationError",msg));}
 @ExceptionHandler(HttpMessageNotReadableException.class) public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e){return ResponseEntity.badRequest().body(new ErrorResponse("ValidationError","Request body or date-time format is invalid"));}
 @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<ErrorResponse> conflict(DataIntegrityViolationException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Conflict","A unique value or database constraint was violated"));}
 @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<ErrorResponse> illegal(IllegalArgumentException e){return ResponseEntity.badRequest().body(new ErrorResponse("ValidationError",e.getMessage()));}
}
