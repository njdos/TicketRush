package com.rush.ticket.catalog.exceptions;

public class CatalogDomainException extends RuntimeException {

    private final AppErrorCode errorCode;

    public CatalogDomainException(AppErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AppErrorCode getErrorCode() {
        return errorCode;
    }
}
