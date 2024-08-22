package uk.ac.ebi.spot.ols.reststatistics.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import uk.ac.ebi.spot.ols.reststatistics.dto.RestCallDto;
import uk.ac.ebi.spot.ols.reststatistics.dto.RestCallRequest;
import uk.ac.ebi.spot.ols.reststatistics.entity.RestCall;
import uk.ac.ebi.spot.ols.reststatistics.entity.RestCallParameter;

public interface RestCallService {

    RestCall save(RestCall entity);

    Page<RestCallDto> getList(RestCallRequest request, List<RestCallParameter> parameters, boolean intersection, Pageable pageable);

    List<RestCall> findAll();

    Long count(RestCallRequest request, List<RestCallParameter> parameters, boolean intersection);
}
