package com.rush.ticket.catalog.exception

import jakarta.validation.ConstraintViolationException
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.net.URI
import java.time.Instant
import java.util.*

@RestControllerAdvice
class GlobalExceptionHandler {

    companion object {
        private val BASE_URI = URI.create("https://rush-ticket.com/errors")
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException, request: WebRequest): ResponseEntity<Any> {
        val errorCode = AppErrorCode.CONSTRAINT_VIOLATION
        val status = HttpStatus.BAD_REQUEST

        val invalidParameters = ex.constraintViolations.associate { violation ->
            val propertyName = violation.propertyPath.toString().substringAfterLast('.')
            propertyName to (violation.message ?: "Invalid value")
        }

        val problemDetail = createProblemDetail(errorCode, status, errorCode.defaultTitle).apply {
            setProperty("invalid_parameters", invalidParameters)
        }

        return buildResponseEntity(problemDetail, status)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<Any> {
        val errorCode = AppErrorCode.INVALID_INPUT_BODY
        val status = HttpStatus.BAD_REQUEST

        val fieldErrors = ex.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "Invalid value")
        }

        val problemDetail = createProblemDetail(errorCode, status, errorCode.defaultTitle).apply {
            setProperty("field_errors", fieldErrors)
        }

        return buildResponseEntity(problemDetail, status)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException, request: WebRequest): ResponseEntity<Any> {
        val errorCode = AppErrorCode.INVALID_PARAMETER_TYPE
        val status = HttpStatus.BAD_REQUEST

        val typeName = ex.requiredType?.simpleName ?: "unknown"
        val detail = "Parameter '${ex.name}' must be of type '$typeName'"

        val problemDetail = createProblemDetail(errorCode, status, detail)

        return buildResponseEntity(problemDetail, status)
    }

    @ExceptionHandler(Exception::class)
    fun handleAllUnexpectedExceptions(ex: Exception, request: WebRequest): ResponseEntity<Any> {
        val errorCode = AppErrorCode.INTERNAL_SERVER_ERROR
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        val problemDetail =
            createProblemDetail(errorCode, status, "An unexpected error occurred. Please contact support.")

        return buildResponseEntity(problemDetail, status)
    }

    private fun createProblemDetail(errorCode: AppErrorCode, status: HttpStatus, detail: String): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(status, detail).apply {
            title = errorCode.defaultTitle
            type = buildErrorTypeUri(errorCode)
            setProperty("code", errorCode.code)
            setProperty("timestamp", Instant.now())
            setProperty("trace_id", MDC.get("traceId") ?: UUID.randomUUID().toString())
        }
    }

    private fun buildErrorTypeUri(errorCode: AppErrorCode): URI =
        URI.create("$BASE_URI/${errorCode.name.lowercase().replace('_', '-')}")

    private fun buildResponseEntity(problemDetail: ProblemDetail, status: HttpStatus): ResponseEntity<Any> =
        ResponseEntity.status(status).body(problemDetail)
}
