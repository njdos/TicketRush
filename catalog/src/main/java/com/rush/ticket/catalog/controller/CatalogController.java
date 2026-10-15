package com.rush.ticket.catalog.controller;

import com.rush.ticket.catalog.dto.base.ApiResponse;
import com.rush.ticket.catalog.dto.req.EventRequestDto;
import com.rush.ticket.catalog.dto.resp.EventResponseDto;
import com.rush.ticket.catalog.services.EventService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/catalog/events")
public class CatalogController {

    private final EventService eventService;

    public CatalogController(EventService eventService) {
        this.eventService = eventService;
    }

    @RateLimiter(name = "catalogWrite")
    @PostMapping
    public ResponseEntity<ApiResponse<EventResponseDto>> createEvent(
            @Valid @RequestBody EventRequestDto requestDto,
            @RequestHeader("X-User-Id") UUID organizerId
    ) {
        EventResponseDto created = eventService.createEvent(requestDto, organizerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Bulkhead(name = "catalogRead")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventResponseDto>>> listEvents(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(eventService.listEvents(pageable)));
    }


    @Bulkhead(name = "catalogRead")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponseDto>> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(eventService.getEventById(id)));
    }

    @RateLimiter(name = "catalogWrite")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponseDto>> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody EventRequestDto requestDto
    ) {
        return ResponseEntity.ok(ApiResponse.success(eventService.updateEvent(id, requestDto)));
    }
}