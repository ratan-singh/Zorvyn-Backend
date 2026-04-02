package com.zorvyn.financedashboard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zorvyn.financedashboard.dto.response.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwtToken = authorizationHeader.substring(BEARER_PREFIX.length());
            final String userEmail = jwtService.extractEmail(jwtToken);

            // Only authenticate if email was extracted and no authentication exists yet
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwtToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token for request: {} {}", request.getMethod(), request.getRequestURI());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Session expired. Please log in again.");
        } catch (MalformedJwtException | SignatureException ex) {
            log.warn("Invalid JWT token for request: {} {}", request.getMethod(), request.getRequestURI());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid authentication token.");
        } catch (Exception ex) {
            log.error("Unexpected error during JWT authentication for request: {} {}",
                    request.getMethod(), request.getRequestURI(), ex);
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Authentication failed.");
        }
    }

    /**
     * Writes a structured JSON error response directly to the servlet output stream.
     * This bypasses Spring's exception handling since filter exceptions occur before
     * the DispatcherServlet — @RestControllerAdvice cannot catch them.
     */
    private void writeErrorResponse(HttpServletResponse response, int statusCode, String message)
            throws IOException {
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> errorResponse = ApiResponse.error(message);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
