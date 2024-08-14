package uk.ac.ebi.spot.ols.reststatistics.abstraction.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.spot.ols.reststatistics.entities.RestCall;

public interface RestCallRepository extends MongoRepository<RestCall, String>, RestCallRepositoryCustom {

}
