package uk.ac.ebi.spot.ols.reststatistics.service;

import jakarta.servlet.http.HttpServletRequest;

public interface RestCallHandlerService {
    void handle(HttpServletRequest request);
}
