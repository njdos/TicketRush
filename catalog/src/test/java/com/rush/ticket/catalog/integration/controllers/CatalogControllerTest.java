//package com.rush.ticket.catalog.integration.controllers;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.rush.ticket.catalog.controllers.CatalogController;
//import com.rush.ticket.catalog.dto.reqResp.EventRequestDto;
//import com.rush.ticket.catalog.dto.reqResp.EventResponseDto;
//import com.rush.ticket.catalog.services.EventService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
//import org.springframework.http.MediaType;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@ExtendWith(MockitoExtension.class)
//class CatalogControllerTest {
//
//    private ObjectMapper objectMapper;
//    private MockMvc mockMvc;
//
//    @Mock
//    private EventService eventService;
//
//    @BeforeEach
//    void setUp() {
//        objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//
//        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
//        CatalogController catalogController = new CatalogController(eventService);
//
//        this.mockMvc = MockMvcBuilders.standaloneSetup(catalogController)
//                .setMessageConverters(jacksonConverter)
//                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
//                .build();
//    }
//
//    @Test
//    void shouldReturn200AndPagedEvents() throws Exception {
//        EventResponseDto responseDto = new EventResponseDto(
//                UUID.randomUUID(),
//                "Opera",
//                "Theater",
//                LocalDateTime.now().plusDays(2),
//                50,
//                new BigDecimal("70.00"),
//                UUID.randomUUID(),
//                java.time.Instant.now()
//        );
//
//        Pageable pageable = PageRequest.of(0, 20);
//        Page<EventResponseDto> mockPage = new PageImpl<>(
//                new ArrayList<>(List.of(responseDto)), pageable, 1);
//
//        Mockito.when(eventService.listEvents(any(Pageable.class)))
//                .thenReturn(mockPage);
//
//        mockMvc.perform(get("/events")
//                        .param("page", "0")
//                        .param("size", "20")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.content[0].name").value("Opera"));
//    }
//
//    @Test
//    void shouldCreateEventAndReturn201() throws Exception {
//        UUID organizerId = UUID.randomUUID();
//        UUID eventId = UUID.randomUUID();
//        EventRequestDto requestDto = new EventRequestDto(
//                "Rock Festival", "Main Stage", LocalDateTime.now().plusDays(10), 500, new BigDecimal("50.00")
//        );
//        EventResponseDto responseDto = new EventResponseDto(
//                eventId, "Rock Festival", "Main Stage", requestDto.startsAt(), 500, new BigDecimal("50.00"), organizerId, Instant.now()
//        );
//
//        Mockito.when(eventService.createEvent(any(EventRequestDto.class), eq(organizerId)))
//                .thenReturn(responseDto);
//
//        mockMvc.perform(post("/events")
//                        .header("X-User-Id", organizerId.toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.data.id").value(eventId.toString()))
//                .andExpect(jsonPath("$.data.name").value("Rock Festival"));
//    }
//
//    @Test
//    void shouldReturnPagedEvents() throws Exception {
//        EventResponseDto responseDto = new EventResponseDto(
//                UUID.randomUUID(), "Jazz Night", "Club", LocalDateTime.now().plusDays(2), 100, new BigDecimal("25.00"), UUID.randomUUID(), Instant.now()
//        );
//
//        Page<EventResponseDto> pageResponse = new PageImpl<>(List.of(responseDto), PageRequest.of(0, 20), 1);
//        Mockito.when(eventService.listEvents(any(Pageable.class))).thenReturn(pageResponse);
//
//        mockMvc.perform(get("/events")
//                        .param("page", "0")
//                        .param("size", "20")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.content[0].name").value("Jazz Night")); // Твой изначальный синтаксис с [0] теперь сработает!
//    }
//
//    @Test
//    void shouldReturnEventById() throws Exception {
//        UUID eventId = UUID.randomUUID();
//        EventResponseDto responseDto = new EventResponseDto(
//                eventId, "Opera", "Theatre", LocalDateTime.now().plusDays(5), 300, new BigDecimal("80.00"), UUID.randomUUID(), Instant.now()
//        );
//
//        Mockito.when(eventService.getEventById(eventId)).thenReturn(responseDto);
//
//        mockMvc.perform(get("/events/{id}", eventId)
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.id").value(eventId.toString()))
//                .andExpect(jsonPath("$.data.name").value("Opera"));
//    }
//
//    @Test
//    void shouldUpdateEventAndReturn200() throws Exception {
//        UUID eventId = UUID.randomUUID();
//        EventRequestDto requestDto = new EventRequestDto(
//                "Updated Pop Show", "Arena", LocalDateTime.now().plusDays(12), 1000, new BigDecimal("65.00")
//        );
//        EventResponseDto responseDto = new EventResponseDto(
//                eventId, "Updated Pop Show", "Arena", requestDto.startsAt(), 1000, new BigDecimal("65.00"), UUID.randomUUID(), Instant.now()
//        );
//
//        Mockito.when(eventService.updateEvent(eq(eventId), any(EventRequestDto.class))).thenReturn(responseDto);
//
//        mockMvc.perform(put("/events/{id}", eventId)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestDto)))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data.name").value("Updated Pop Show"));
//    }
//}
