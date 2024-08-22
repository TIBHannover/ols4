package uk.ac.ebi.spot.ols.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import uk.ac.ebi.spot.ols.reststatistics.service.RestCallHandlerService;

public class RestCallInterceptor implements HandlerInterceptor {
    private final RestCallHandlerService restCallHandlerService;

    @Autowired
    public RestCallInterceptor(RestCallHandlerService restCallHandlerService) {
        this.restCallHandlerService = restCallHandlerService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!request.getRequestURL().toString().contains("/api")
            || request.getRequestURL().toString().contains("/api/rest/statistics")) {
            return true;
        }


        restCallHandlerService.handle(request);

        return true;
    }
}
