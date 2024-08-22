package uk.ac.ebi.spot.ols.reststatistics.repository;

import org.springframework.data.domain.Pageable;

import uk.ac.ebi.spot.ols.reststatistics.dto.RestCallRequest;
import uk.ac.ebi.spot.ols.reststatistics.entity.RestCall;
import uk.ac.ebi.spot.ols.reststatistics.entity.RestCallParameter;

import java.util.List;

public interface RestCallRepositoryCustom {

    List<RestCall> query(RestCallRequest request, List<RestCallParameter> parameters, boolean intersection, Pageable pageable);

    Long count(RestCallRequest request, List<RestCallParameter> parameters, boolean intersection);
}
