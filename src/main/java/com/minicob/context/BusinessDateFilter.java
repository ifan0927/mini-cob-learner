package com.minicob.context;

import com.minicob.businessdate.BusinessDateService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Binds the current business date to the request thread for the duration of an API call.
 */
@Component
public class BusinessDateFilter extends OncePerRequestFilter {

    private final BusinessDateService businessDateService;

    public BusinessDateFilter(BusinessDateService businessDateService) {
        this.businessDateService = businessDateService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        BusinessDates previous = BusinessDateContext.getOrNull();
        BusinessDateContext.set(BusinessDates.openDay(businessDateService.getBusinessDate()));
        try {
            chain.doFilter(request, response);
        } finally {
            BusinessDateContext.restore(previous);
        }
    }
}
