package uk.ac.ebi.spot.ols.reststatistics.repositories;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.web.util.UriUtils;

import uk.ac.ebi.spot.ols.reststatistics.abstraction.repositories.RestCallRepositoryCustom;
import uk.ac.ebi.spot.ols.reststatistics.dto.RestCallRequest;
import uk.ac.ebi.spot.ols.reststatistics.entities.RestCall;
import uk.ac.ebi.spot.ols.reststatistics.entities.RestCallParameter;

@Repository
public class RestCallRepositoryImpl implements RestCallRepositoryCustom {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MongoTemplate mongoTemplate;

    @Autowired
    public RestCallRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    @Override
    public List<RestCall> query(RestCallRequest request, List<RestCallParameter> parameters, boolean intersection, Pageable pageable) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        addCriteriaByDates(request, criteria);
        addCriteriaByUrl(request, criteria);
        if (parameters !=null)
        	if (parameters.size()>0)
		        addCriteriaByParameter(request, criteria, parameters, intersection);


        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        if (Objects.nonNull(pageable)) {
            query.with(pageable);
        }

        return mongoTemplate.find(query, RestCall.class);
    }
    
    @Override
    public Long count(RestCallRequest request, List<RestCallParameter> parameters, boolean intersection) {
        Query query = new Query();

        List<Criteria> criteria = new ArrayList<>();

        addCriteriaByDates(request, criteria);
        addCriteriaByUrl(request, criteria);
        if (parameters !=null)
        	if (parameters.size()>0)
		        addCriteriaByParameter(request, criteria, parameters, intersection);

        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        return mongoTemplate.count(query, RestCall.class);
    }

    private void addCriteriaByUrl(RestCallRequest request, List<Criteria> criteria) {
        if (request.getUrl() != null) {
            String url = getDecodedUrl(request);
            criteria.add(Criteria.where("url").is(url));
        }
    }

    private void addCriteriaByDates(RestCallRequest request, List<Criteria> criteria) {
        if (request.getDateTimeFrom() != null) {
            criteria.add(Criteria.where("createdAt").gte(request.getDateTimeFrom()));
        }

        if (request.getDateTimeTo() != null) {
            criteria.add(Criteria.where("createdAt").lte(request.getDateTimeTo()));
        }
    }
    
    private void addCriteriaByParameter(RestCallRequest request, List<Criteria> criteria, List<RestCallParameter> parameters, boolean intersection) {
    	if (parameters != null) {
    		if (intersection)
    			criteria.add(Criteria.where("parameters").all(parameters));
    		else
    			criteria.add(Criteria.where("parameters").in(parameters));
    	}
    		
    }

    private String getDecodedUrl(RestCallRequest request) {
        if (request.getUrl() == null) {
            return null;
        }

        String decodedUrl = null;
        try {
            decodedUrl = UriUtils.decode(request.getUrl(), StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            logger.error("Could not get query parameters: {}", e.getLocalizedMessage());
        }

        return decodedUrl;
    }
}
