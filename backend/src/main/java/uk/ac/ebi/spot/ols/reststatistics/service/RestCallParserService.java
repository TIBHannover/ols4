package uk.ac.ebi.spot.ols.reststatistics.service;

import javax.servlet.http.HttpServletRequest;

import uk.ac.ebi.spot.ols.reststatistics.entity.HttpServletRequestInfo;

public interface RestCallParserService {
    HttpServletRequestInfo parse(HttpServletRequest request);
}
