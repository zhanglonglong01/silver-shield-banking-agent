package com.fintechathon.silvershield.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestTraceFilterTest {

    private final RequestTraceFilter filter = new RequestTraceFilter();

    @Test
    void returnsValidCallerTraceId() throws Exception {
        String traceId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts");
        request.addHeader(RequestTraceFilter.TRACE_HEADER, traceId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestTraceFilter.TRACE_HEADER)).isEqualTo(traceId);
    }

    @Test
    void replacesUntrustedTraceHeaderInsteadOfLoggingIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts");
        request.addHeader(RequestTraceFilter.TRACE_HEADER, "forged\nAuthorization: bearer secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestTraceFilter.TRACE_HEADER))
                .isNotEqualTo(request.getHeader(RequestTraceFilter.TRACE_HEADER))
                .satisfies(value -> assertThat(UUID.fromString(value)).isNotNull());
    }
}
