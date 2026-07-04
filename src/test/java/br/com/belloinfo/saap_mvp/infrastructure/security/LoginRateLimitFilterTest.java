package br.com.belloinfo.saap_mvp.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginRateLimitFilterTest {

    private LoginRateLimitFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter();
        responseWriter = new StringWriter();
    }

    private void configureLoginRequest(String ip) {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn(ip);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    }

    @Test
    @DisplayName("requisições de login dentro do limite são permitidas")
    void doFilter_withinLimit_allowsRequest() throws ServletException, IOException {
        configureLoginRequest("192.168.1.1");

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(5)).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    @DisplayName("requisição de login excedendo o limite retorna 429")
    void doFilter_exceedingLimit_returns429() throws ServletException, IOException {
        configureLoginRequest("192.168.1.1");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType("application/json");
        verify(filterChain, times(5)).doFilter(request, response);
    }

    @Test
    @DisplayName("requisições que não são POST /auth/login passam direto")
    void doFilter_notLoginEndpoint_passesThrough() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    @DisplayName("GET /auth/login passa direto (apenas POST é limitado)")
    void doFilter_getLoginMethod_passesThrough() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("IPs diferentes têm contadores independentes")
    void doFilter_differentIps_haveIndependentCounters() throws ServletException, IOException {
        configureLoginRequest("192.168.1.1");
        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(6)).doFilter(request, response);
    }

    @Test
    @DisplayName("X-Forwarded-For é usado quando disponível")
    void doFilter_withXForwardedFor_usesForwardedIp() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 70.41.3.18");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType("application/json");
        verify(filterChain, times(5)).doFilter(request, response);
    }

    @Test
    @DisplayName("resposta 429 contém encoding UTF-8")
    void doFilter_rateLimited_returnsUtf8Encoding() throws ServletException, IOException {
        configureLoginRequest("192.168.1.1");
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setCharacterEncoding("UTF-8");
    }
}
