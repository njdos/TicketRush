package com.rush.ticket.catalog.exceptions;

import java.util.UUID;

public class EventNotFoundException extends CatalogDomainException {

    public EventNotFoundException(UUID id) {
        super(AppErrorCode.EVENT_NOT_FOUND, String.format("Event with ID '%s' was not found", id));
    }

    public EventNotFoundException(String message) {
        super(AppErrorCode.EVENT_NOT_FOUND, message);
    }
}
