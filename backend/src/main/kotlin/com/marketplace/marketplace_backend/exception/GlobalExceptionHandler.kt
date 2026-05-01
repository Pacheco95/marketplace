package com.marketplace.marketplace_backend.exception

import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.http.ProblemDetail
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI
import java.time.Instant
import java.util.Locale
import java.util.UUID

@RestControllerAdvice
class GlobalExceptionHandler(
    private val messageSource: MessageSource,
) {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MarketplaceException::class)
    fun handleMarketplaceException(
        ex: MarketplaceException,
        locale: Locale,
    ): ProblemDetail {
        val incidentId = UUID.randomUUID()
        log.error("Incident {} — {}: {}", incidentId, ex.errorCode, ex.message, ex)

        val title = messageSource.getMessage(ex.errorCode.titleKey, null, locale)

        @Suppress("UNCHECKED_CAST")
        val msgArgs: Array<Any>? = if (ex.args.isNotEmpty()) ex.args as Array<Any> else null
        val detail =
            messageSource.getMessage(
                ex.errorCode.detailKey,
                msgArgs,
                locale,
            )

        val problem = ProblemDetail.forStatus(ex.errorCode.httpStatus)
        problem.type = URI.create("urn:marketplace:error:${ex.errorCode.typeUriSuffix}")
        problem.title = title
        problem.detail = detail
        problem.setProperty("instance", "urn:uuid:$incidentId")
        problem.setProperty("timestamp", Instant.now().toString())
        return problem
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        locale: Locale,
    ): ProblemDetail {
        val incidentId = UUID.randomUUID()
        val errors =
            ex.bindingResult.fieldErrors.associate { f: FieldError ->
                f.field to
                    (f.defaultMessage ?: "invalid")
            }
        log.warn("Incident {} — validation error: {}", incidentId, errors)

        val code = ErrorCode.VALIDATION_ERROR
        val title = messageSource.getMessage(code.titleKey, null, locale)
        val detail = messageSource.getMessage(code.detailKey, null, locale)

        val problem = ProblemDetail.forStatus(code.httpStatus)
        problem.type = URI.create("urn:marketplace:error:${code.typeUriSuffix}")
        problem.title = title
        problem.detail = detail
        problem.setProperty("instance", "urn:uuid:$incidentId")
        problem.setProperty("timestamp", Instant.now().toString())
        problem.setProperty("errors", errors)
        return problem
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        locale: Locale,
    ): ProblemDetail {
        val incidentId = UUID.randomUUID()
        log.error("Incident {} — unhandled exception", incidentId, ex)

        val code = ErrorCode.INTERNAL_ERROR
        val title = messageSource.getMessage(code.titleKey, null, locale)
        val detail = messageSource.getMessage(code.detailKey, null, locale)

        val problem = ProblemDetail.forStatus(code.httpStatus)
        problem.type = URI.create("urn:marketplace:error:${code.typeUriSuffix}")
        problem.title = title
        problem.detail = detail
        problem.setProperty("instance", "urn:uuid:$incidentId")
        problem.setProperty("timestamp", Instant.now().toString())
        return problem
    }
}
