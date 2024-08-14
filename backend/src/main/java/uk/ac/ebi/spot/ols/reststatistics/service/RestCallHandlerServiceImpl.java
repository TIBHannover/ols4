package uk.ac.ebi.spot.ols.reststatistics.service;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.spot.ols.reststatistics.abstraction.service.RestCallHandlerService;
import uk.ac.ebi.spot.ols.reststatistics.abstraction.service.RestCallParserService;
import uk.ac.ebi.spot.ols.reststatistics.abstraction.service.RestCallService;
import uk.ac.ebi.spot.ols.reststatistics.entities.HttpServletRequestInfo;
import uk.ac.ebi.spot.ols.reststatistics.entities.RestCall;

@Service
public class RestCallHandlerServiceImpl implements RestCallHandlerService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RestCallParserService restCallParserService;
    private final RestCallService restCallService;

    @Autowired
    public RestCallHandlerServiceImpl(RestCallParserService restCallParserService,
                                      RestCallService restCallService) {
        this.restCallParserService = restCallParserService;
        this.restCallService = restCallService;
    }

    @Override
    public void handle(HttpServletRequest request) {
        HttpServletRequestInfo requestInfo = restCallParserService.parse(request);

        RestCall restCall = new RestCall(requestInfo.getUrl());
        restCall.addParameters(requestInfo.getPathVariables());
        restCall.addParameters(requestInfo.getQueryParameters());
        restCall.addParameters(requestInfo.getHeaders());

        RestCall saved = restCallService.save(restCall);

        log.debug("REST Call: {}", saved);
    }
}
