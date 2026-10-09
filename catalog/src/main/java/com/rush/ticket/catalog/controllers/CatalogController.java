package com.rush.ticket.catalog.controllers;

import com.rush.ticket.catalog.dto.base.ApiResponse;
import com.rush.ticket.catalog.dto.req.EventRequestDto;
import com.rush.ticket.catalog.dto.resp.EventResponseDto;
import com.rush.ticket.catalog.services.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/events")
public class CatalogController {

    private final EventService eventService;

    public CatalogController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponseDto>> createEvent(
            @Valid @RequestBody EventRequestDto requestDto,
            @RequestHeader("X-User-Id") UUID organizerId // тимчасово, поки немає Keycloak
    ) {
        EventResponseDto created = eventService.createEvent(requestDto, organizerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventResponseDto>>> listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(eventService.listEvents(pageable)));
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponseDto>> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(eventService.getEventById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponseDto>> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody EventRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponse.success(eventService.updateEvent(id, requestDto)));
    }
}