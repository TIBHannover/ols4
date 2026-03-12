package uk.ac.ebi.spot.ols.config;

import org.matomo.java.tracking.MatomoRequest;
import org.matomo.java.tracking.MatomoTracker;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MatomoTrackingFilter extends OncePerRequestFilter {

    private final MatomoTracker tracker;

    public MatomoTrackingFilter(MatomoTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String caller = request.getHeader("caller");
        if (caller == null) {
            caller = "unknown";
        }


        String[] classificationArray = request.getParameterValues("classification");
        String classificationValue;

        if (classificationArray == null || classificationArray.length == 0) {
            classificationValue = "none";
        } else if (classificationArray.length == 1) {
            classificationValue = classificationArray[0];
        } else {
            classificationValue = String.join(", ", classificationArray);
        }

        Map<String, String[]> parameterMap = request.getParameterMap();
        String allParams = "none";

        if (!parameterMap.isEmpty()) {
            allParams = parameterMap.entrySet().stream()
                    .map(e -> e.getKey() + "=" + String.join(",", e.getValue()))
                    .collect(Collectors.joining("; "));
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            String statusCode = String.valueOf(response.getStatus());

            Map<String, Collection<Object>> params = new HashMap<>();

            params.put("rec", Collections.singletonList("1"));
            params.put("dimension1", Collections.singletonList(statusCode));
            params.put("dimension2", Collections.singletonList(caller));
            params.put("dimension3", Collections.singletonList(classificationValue));
            params.put("dimension4", Collections.singletonList(allParams));
            params.put("dimension5", Collections.singletonList(String.valueOf(duration)));

            MatomoRequest matomoRequest = MatomoRequest.builder()
                    .actionUrl(request.getRequestURL().toString())
                    .actionName(request.getMethod() + " " + request.getRequestURI())
                    .visitorIp(request.getRemoteAddr())
                    .headerUserAgent(request.getHeader("User-Agent"))
                    .additionalParameters(params)

                    .build();
            //System.out.println("MATOMO PAYLOAD: " + matomoRequest.getAdditionalParameters());
            //System.out.println("DEBUG - Action URL: " + matomoRequest.getActionUrl());
            tracker.sendRequestAsync(matomoRequest);
        }
    }
}
