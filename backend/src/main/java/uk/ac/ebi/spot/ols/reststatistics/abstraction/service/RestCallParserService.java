package uk.ac.ebi.spot.ols.reststatistics.abstraction.service;

import javax.servlet.http.HttpServletRequest;

import uk.ac.ebi.spot.ols.reststatistics.entities.HttpServletRequestInfo;

public interface RestCallParserService {
    HttpServletRequestInfo parse(HttpServletRequest request);
}
