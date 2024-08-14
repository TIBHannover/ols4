package uk.ac.ebi.spot.ols.reststatistics.abstraction.service;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import uk.ac.ebi.spot.ols.reststatistics.dto.KeyValueResultDto;
import uk.ac.ebi.spot.ols.reststatistics.dto.RestCallRequest;
import uk.ac.ebi.spot.ols.reststatistics.entities.RestCallParameter;

public interface RestCallStatisticsService {
    Page<KeyValueResultDto> getRestCallsCountsByAddress(RestCallRequest request, List<RestCallParameter> parameters, boolean intersection, Pageable pageable);
    
    KeyValueResultDto getRestCallsTotalCount(RestCallRequest request, List<RestCallParameter> parameters, boolean intersection);

    Page<KeyValueResultDto> getStatisticsByParameter(RestCallRequest request, List<RestCallParameter> parameters, boolean intersection, Pageable pageable);

    Page<KeyValueResultDto> getStatisticsByDate(RestCallRequest request, List<RestCallParameter> parameters, boolean intersection, Pageable pageable);
}
