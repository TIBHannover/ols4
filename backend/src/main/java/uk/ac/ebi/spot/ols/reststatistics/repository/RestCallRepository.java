package uk.ac.ebi.spot.ols.reststatistics.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.spot.ols.reststatistics.entity.RestCall;

public interface RestCallRepository extends MongoRepository<RestCall, String>, RestCallRepositoryCustom {

}
