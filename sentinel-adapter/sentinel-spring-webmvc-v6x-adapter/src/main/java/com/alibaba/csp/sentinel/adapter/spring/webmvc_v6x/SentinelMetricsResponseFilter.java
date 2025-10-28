package com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x;

import com.alibaba.csp.sentinel.slots.block.degrade.adaptive.util.AdaptiveUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Filter used to add passive notification service indicators to the response header.
 *
 * @author ylnxwlp
 */
public class SentinelMetricsResponseFilter implements Filter {

    private static final String METRICS_ENABLED_HEADER = "X-Sentinel-Adaptive";
    private static final String METRICS_ENABLED_VALUE = "enabled";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String metricsHeader = httpRequest.getHeader(METRICS_ENABLED_HEADER);
        if (METRICS_ENABLED_VALUE.equals(metricsHeader)) {
            ServerMetricsResponseWrapper wrapper = new ServerMetricsResponseWrapper(httpResponse);
            chain.doFilter(request, wrapper);
        } else {
            chain.doFilter(request, response);
        }
    }

    static class ServerMetricsResponseWrapper extends HttpServletResponseWrapper {
        private boolean headerInjected = false;

        public ServerMetricsResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        private void injectMetricsHeaderIfNeeded() {
            if (!headerInjected && !isCommitted()) {
                setHeader("X-Server-Metrics", AdaptiveUtils.packServerMetric());
                headerInjected = true;
            }
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            injectMetricsHeaderIfNeeded();
            return super.getOutputStream();
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            injectMetricsHeaderIfNeeded();
            return super.getWriter();
        }

        @Override
        public void flushBuffer() throws IOException {
            injectMetricsHeaderIfNeeded();
            super.flushBuffer();
        }
    }
}