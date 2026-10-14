package com.rush.ticket.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rush.ticket.catalog.dto.req.EventRequestDto;
import com.rush.ticket.catalog.dto.resp.EventResponseDto;
import com.rush.ticket.catalog.services.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EventService eventService;

    @InjectMocks
    private CatalogController catalogController;

    private ObjectMapper objectMapper;

    private UUID sampleId;
    private UUID organizerId;
    private EventRequestDto requestDto;
    private EventResponseDto responseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(catalogController).build();

        // FIX: Initialize and configure ObjectMapper to handle LocalDateTime and Instant types smoothly
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        sampleId = UUID.randomUUID();
        organizerId = UUID.randomUUID();

        requestDto = new EventRequestDto(
                "Rock Concert",
                "Madison Square Garden",
                LocalDateTime.now().plusDays(2),
                100,
                new BigDecimal("49.99")
        );

        responseDto = new EventResponseDto(
                UUID.randomUUID(),
                "Opera",
                "Theater",
                LocalDateTime.now().plusDays(2),
                50,
                new BigDecimal("70.00"),
                UUID.randomUUID(),
                java.time.Instant.now()
        );
    }

    @Test
    void createEvent_Success() throws Exception {
        // Arrange
        when(eventService.createEvent(any(EventRequestDto.class), eq(organizerId)))
                .thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/catalog/events")
                        .header("X-User-Id", organizerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
    }

    @Test
    void listEvents_Success() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<EventResponseDto> page = new PageImpl<>(Collections.singletonList(responseDto), pageable, 1);

        when(eventService.listEvents(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/catalog/events")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getEventById_Success() throws Exception {
        // Arrange
        when(eventService.getEventById(sampleId)).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(get("/api/v1/catalog/events/{id}", sampleId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void updateEvent_Success() throws Exception {
        // Arrange
        when(eventService.updateEvent(eq(sampleId), any(EventRequestDto.class)))
                .thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(put("/api/v1/catalog/events/{id}", sampleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }
}
