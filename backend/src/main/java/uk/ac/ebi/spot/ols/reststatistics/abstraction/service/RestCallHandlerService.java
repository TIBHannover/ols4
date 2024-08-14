package uk.ac.ebi.spot.ols.reststatistics.abstraction.service;

import javax.servlet.http.HttpServletRequest;

public interface RestCallHandlerService {
    void handle(HttpServletRequest request);
}
