package uk.ac.ebi.spot.ols.reststatistics.abstraction.repositories;

import org.springframework.data.domain.Pageable;

import uk.ac.ebi.spot.ols.reststatistics.dto.RestCallRequest;
import uk.ac.ebi.spot.ols.reststatistics.entities.RestCall;
import uk.ac.ebi.spot.ols.reststatistics.entities.RestCallParameter;

import java.util.List;

public interface RestCallRepositoryCustom {
    
    List<RestCall> query(RestCallRequest request, List<RestCallParameter> parameters, boolean intersection, Pageable pageable);
    
    Long count(RestCallRequest request, List<RestCallParameter> parameters, boolean intersection);
}
