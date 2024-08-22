package uk.ac.ebi.spot.ols.reststatistics.service;

import javax.servlet.http.HttpServletRequest;

public interface RestCallHandlerService {
    void handle(HttpServletRequest request);
}
