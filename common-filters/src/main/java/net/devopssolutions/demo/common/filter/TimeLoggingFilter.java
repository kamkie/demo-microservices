package net.devopssolutions.demo.common.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.stream.Collectors;

import static java.util.Collections.list;
import static javax.servlet.DispatcherType.*;

@Slf4j
@ConditionalOnProperty(name = "logging.custom.time.enable")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Component
@WebFilter(urlPatterns = {"/", "/*"}, asyncSupported = true, dispatcherTypes = {REQUEST, ASYNC, ERROR, FORWARD, INCLUDE})
public class TimeLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            long start = System.nanoTime();
            filterChain.doFilter(request, response);

            if (log.isInfoEnabled()) {
                logTime(request, start);
            }
        } finally {
            MDC.remove("userName");
        }
    }

    private void logTime(HttpServletRequest request, long start) {
        long nanos = System.nanoTime() - start;
        String duration = formatDuration(nanos);
        String headers = getHeadersAsString(request);

        if (log.isDebugEnabled()) {
            log.debug("request: url: {}, time {}, params: {}, headers: {}", request.getRequestURL(), duration,
                    createMessage(request, "", ""), headers);
        } else {
            log.info("request: url: {}, time {}, params: {}", request.getRequestURL(), duration,
                    createMessage(request, "", ""));
        }
    }

    private String formatDuration(long nanos) {
        return Duration.ofNanos(nanos).toString();
    }

    private String getHeadersAsString(HttpServletRequest request) {
        return list(request.getHeaderNames()).stream()
                .flatMap(s -> list(request.getHeaders(s)).stream().map(header -> s + "= " + header))
                .collect(Collectors.joining("\n"));
    }

    private String createMessage(HttpServletRequest request, String prefix, String suffix) {
        StringBuilder msg = new StringBuilder();
        msg.append(prefix);
        msg.append("uri=").append(request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null) {
            msg.append('?').append(queryString);
        }

        msg.append(";client=").append(request.getRemoteAddr());

        msg.append(suffix);
        return msg.toString();
    }

}
