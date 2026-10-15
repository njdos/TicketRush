package com.rush.ticket.catalog.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void usesIncomingRequestId_exposesItInMdcAndEchoesInResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/catalog/events");
        request.addHeader("X-Request-Id", "req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> seenInChain.set(MDC.get("traceId"));

        filter.doFilter(request, response, chain);

        assertThat(seenInChain.get()).isEqualTo("req-123");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("req-123");
        assertThat(MDC.get("traceId")).isNull(); // cleaned up, no leaking between pooled threads
    }

    @Test
    void generatesRequestId_whenHeaderMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/x"), response, (req, res) -> { });

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(UUID.fromString(response.getHeader("X-Request-Id"))).isNotNull();
    }

    @Test
    void replacesUnsafeRequestId_toPreventLogInjection() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Request-Id", "abc def\" level=ERROR");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader("X-Request-Id")).doesNotContain(" ").doesNotContain("\"");
    }

    @Test
    void putsValidUserIdIntoMdc_andIgnoresGarbage() throws Exception {
        String userId = UUID.randomUUID().toString();
        MockHttpServletRequest valid = new MockHttpServletRequest("GET", "/x");
        valid.addHeader("X-User-Id", userId);
        AtomicReference<String> seen = new AtomicReference<>();
        filter.doFilter(valid, new MockHttpServletResponse(), (req, res) -> seen.set(MDC.get("userId")));
        assertThat(seen.get()).isEqualTo(userId);

        MockHttpServletRequest garbage = new MockHttpServletRequest("GET", "/x");
        garbage.addHeader("X-User-Id", "not-a-uuid");
        seen.set("unset");
        filter.doFilter(garbage, new MockHttpServletResponse(), (req, res) -> seen.set(MDC.get("userId")));
        assertThat(seen.get()).isNull();
    }

    @Test
    void skipsActuatorEndpoints() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/actuator/prometheus"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/v1/catalog/events"))).isFalse();
    }
}
