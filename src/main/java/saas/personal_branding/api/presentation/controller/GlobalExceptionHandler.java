package saas.personal_branding.api.presentation.controller;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import saas.personal_branding.api.application.exception.BusinessException;
import saas.personal_branding.api.application.exception.PlatformException;
import saas.personal_branding.api.application.exception.TokenException;
import saas.personal_branding.api.application.exception.UserException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problemDetail.setTitle("Constraint Violation");
        problemDetail.setProperty("timestamp", Instant.now());

        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .collect(Collectors.toList());
        problemDetail.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Constraint violation");
        problemDetail.setTitle("Constraint Violation");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errors", ex.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "property", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()
                ))
                .collect(Collectors.toList()));

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex) {
        HttpStatus status = HttpStatus.CONFLICT;
        HttpHeaders headers = new HttpHeaders();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("code", ex.getErrorCode());
        problemDetail.setProperty("errorCode", ex.getErrorCode());
        
        if (ex instanceof UserException.InvalidCredentialsException) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof UserException.InactiveAccountException) {
            status = HttpStatus.FORBIDDEN;
        } else if (ex instanceof UserException.EmailNotVerifiedException) {
            status = HttpStatus.FORBIDDEN;
        } else if (ex instanceof UserException.EmailAlreadyVerifiedException) {
            status = HttpStatus.CONFLICT;
        } else if (ex instanceof UserException.TooManyLoginAttemptsException tooManyLoginAttemptsException) {
            status = HttpStatus.TOO_MANY_REQUESTS;
            headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(tooManyLoginAttemptsException.getRetryAfterSeconds()));
        } else if (ex instanceof TokenException.RefreshTokenRateLimitedException refreshRateLimitedException) {
            status = HttpStatus.TOO_MANY_REQUESTS;
            headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(refreshRateLimitedException.getRetryAfterSeconds()));
        } else if (ex instanceof UserException.EmailVerificationResendRateLimitedException resendRateLimitedException) {
            status = HttpStatus.TOO_MANY_REQUESTS;
            headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(resendRateLimitedException.getRetryAfterSeconds()));
        } else if (ex instanceof TokenException.PasswordResetTokenNotFoundException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof TokenException.PasswordResetTokenExpiredException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof TokenException.EmailVerificationTokenNotFoundException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof TokenException.EmailVerificationTokenExpiredException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof PlatformException.SelectionRequiredException selectionRequiredException) {
            status = HttpStatus.CONFLICT;
            problemDetail.setProperty("candidates", selectionRequiredException.getCandidates());
        } else if (ex instanceof PlatformException.NoEligibleAccountException) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
        } else if (ex instanceof PlatformException.InvalidAccountSelectionException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof PlatformException.ProviderCommunicationException) {
            status = HttpStatus.BAD_GATEWAY;
        }

        

        if (ex instanceof UserException.TooManyLoginAttemptsException tooManyLoginAttemptsException) {
            problemDetail.setProperty("retryAfterSeconds", tooManyLoginAttemptsException.getRetryAfterSeconds());
        } else if (ex instanceof TokenException.RefreshTokenRateLimitedException refreshRateLimitedException) {
            problemDetail.setProperty("retryAfterSeconds", refreshRateLimitedException.getRetryAfterSeconds());
        } else if (ex instanceof UserException.EmailVerificationResendRateLimitedException resendRateLimitedException) {
            problemDetail.setProperty("retryAfterSeconds", resendRateLimitedException.getRetryAfterSeconds());
        }

        return new ResponseEntity<>(problemDetail, headers, status);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Invalid Argument");
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleOtherExceptions(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        problemDetail.setTitle("Unexpected Error");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("details", ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    private Map<String, Object> mapFieldError(FieldError error) {
        return Map.of(
                "field", error.getField(),
                "message", error.getDefaultMessage()
        );
    }
}
